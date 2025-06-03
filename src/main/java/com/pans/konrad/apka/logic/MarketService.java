package com.pans.konrad.apka.logic;

import com.pans.konrad.apka.model.Asset;
import com.pans.konrad.apka.model.AssetType;
import com.pans.konrad.apka.model.PortfolioEntry;
import com.pans.konrad.apka.ui.MainController;
import javafx.application.Platform;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MarketService – odpowiada za:
 * 1) Generowanie cen w trybie SYMULACJA (co 2,5 s losowa zmiana).
 * 2) Pobieranie cen w trybie REALNY (co 10 s w tej wersji testowej):
 *    – CRYPTO (BTC, DOGECOIN): CoinGecko (bez klucza, darmowe, JSON).
 *    – ORLEN (PKN.WA): Stooq CSV (ale najczęściej poza sesją zwraca N/D).
 *    – KAWA: zwraca null (pomijane).
 * 3) Obsługa portfela: kup/sprzedaj + autotrader (progi kupna/sprzedaży).
 */
public class MarketService {

    private final MainController controller;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<Asset> allAssets = new ArrayList<>();
    private final List<PortfolioEntry> portfolio = new ArrayList<>();
    private double cash = 10000.00;

    // Autotrader: progi kupna i sprzedaży
    private final Map<Asset, Double> buyThresholds = new HashMap<>();
    private final Map<Asset, Double> sellThresholds = new HashMap<>();

    private final Random random = new Random();
    private final boolean simulationMode;
    private final long startTime;

    // Dla wyboru API:
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Mapa: nazwa aktywa → symbol do API (CoinGecko lub Stooq)
    private final Map<String, String> apiSymbolMap = new HashMap<>();

    public MarketService(MainController controller, boolean simulationMode) {
        this.controller = controller;
        this.simulationMode = simulationMode;
        this.startTime = System.currentTimeMillis();

        initSymbolMap();
        initDefaultAssets();
        initDefaultPortfolio();

        if (simulationMode) {
            startPriceUpdater();
        } else {
            startRealPriceUpdater();
        }
    }

    private void initSymbolMap() {
        // Dla CoinGecko id używane w URL: "bitcoin", "dogecoin"
        apiSymbolMap.put("BTC", "bitcoin");
        apiSymbolMap.put("DOGECOIN", "dogecoin");
        // Dla ORLEN użyjemy Stooq: "pkn.w"
        apiSymbolMap.put("ORLEN", "pkn.w");
        // KAWA – nie ma realnej ceny
        apiSymbolMap.put("KAWA", null);
    }

    private void initDefaultAssets() {
        allAssets.add(new Asset("BTC", AssetType.CRYPTO, 393123.96));
        allAssets.add(new Asset("ORLEN", AssetType.STOCK, 73.99));
        allAssets.add(new Asset("KAWA", AssetType.COMMODITY, 348.5));
        allAssets.add(new Asset("DOGECOIN", AssetType.CRYPTO, 0.71));
        for (Asset a : allAssets) {
            a.getPriceHistory().clear();
            a.getPriceHistory().add(a.getPrice());
        }
    }

    private void initDefaultPortfolio() {
        // Portfel zaczyna pusty
    }

    // ===== SYMULACJA (losowe zmiany cen) =====

    private void startPriceUpdater() {
        System.out.println("SYMULACJA: uruchamiam losowe zmiany cen");
        scheduler.scheduleAtFixedRate(() -> {
            long elapsed = System.currentTimeMillis() - startTime;
            for (Asset asset : allAssets) {
                double oldPrice = asset.getPrice();
                double changePercent;
                if (elapsed <= 4000) {
                    changePercent = (random.nextDouble() * 2 - 1) * 0.40;
                } else {
                    changePercent = (random.nextDouble() * 2 - 1) * 0.10;
                }
                double newPrice = oldPrice * (1 + changePercent);
                asset.addPriceToHistory(newPrice);
                asset.setPrice(newPrice);
                checkAutoTrade(asset);

                Platform.runLater(() -> controller.addChartDataPoint(asset));
            }
            Platform.runLater(() -> {
                controller.refreshPortfolioTable();
                controller.updateCashLabel();
            });
        }, 0, 2500, TimeUnit.MILLISECONDS);
    }

    // ===== REALNY (pobieranie cen z internetu) =====

    private void startRealPriceUpdater() {
        System.out.println("REALNY: uruchamiam pobieranie cen z sieci");
        // Na czas testów co 10 s; potem możesz przywrócić 60 s
        scheduler.scheduleAtFixedRate(() -> {
            for (Asset asset : allAssets) {
                Double realPrice = fetchPriceForAsset(asset);
                if (realPrice != null) {
                    asset.addPriceToHistory(realPrice);
                    asset.setPrice(realPrice);
                    checkAutoTrade(asset);
                    Platform.runLater(() -> controller.addChartDataPoint(asset));
                } else {
                    System.out.println("DEBUG: wywaliło cene dla " + asset.getName());
                }
            }
            Platform.runLater(() -> {
                controller.refreshPortfolioTable();
                controller.updateCashLabel();
            });
        }, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * W zależności od typu aktywa:
     * – CRYPTO (BTC, DOGECOIN): CoinGecko
     * – STOCK (ORLEN): Stooq CSV
     * – COMMODITY (KAWA): zawsze null
     */
    private Double fetchPriceForAsset(Asset asset) {
        String name = asset.getName();
        String symbol = apiSymbolMap.get(name);
        if (symbol == null) {
            return null;
        }

        try {
            System.out.println("DEBUG: fetchPriceForAsset dla: " + name);
            if (asset.getType() == AssetType.CRYPTO) {
                return fetchCoinGeckoPrice(symbol);
            } else if (asset.getType() == AssetType.STOCK && name.equals("ORLEN")) {
                return fetchStooqClosePrice(symbol);
            }
        } catch (Exception e) {
            System.err.println("Błąd pobierania ceny dla " + name + ": " + e.getMessage());
        }
        return null;
    }

    // ===== COINGECKO: prosty URL bez klucza =====
    private Double fetchCoinGeckoPrice(String coinId) {
        try {
            String url = "https://api.coingecko.com/api/v3/simple/price?ids="
                    + coinId + "&vs_currencies=usd";
            System.out.println("DEBUG: URL CoinGecko: " + url);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            System.out.println("DEBUG: COINGECKO RAW: " + body);

            JSONObject json = new JSONObject(body);
            if (json.has(coinId)) {
                JSONObject sub = json.getJSONObject(coinId);
                if (sub.has("usd")) {
                    double price = sub.getDouble("usd");
                    System.out.println("DEBUG: Parsed CoinGecko price dla " + coinId + ": " + price);
                    return price;
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Błąd CoinGecko dla " + coinId + ": " + e.getMessage());
        }
        return null;
    }

    // ===== STOOQ CSV: "Close" dla PKN.WA =====
    private Double fetchStooqClosePrice(String symbol) {
        try {
            String url = "https://stooq.com/q/l/?s=" + symbol + "&f=sd2t2ohlcv&h&e=csv";
            System.out.println("DEBUG: fetchStooqClosePrice URL: " + url);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            System.out.println("DEBUG: Stooq CSV RAW:\n" + body);

            String[] lines = body.split("\\R");
            if (lines.length >= 2) {
                String[] cols = lines[1].split(",");
                if (cols.length >= 7) {
                    String closeStr = cols[6];
                    if (!"N/D".equals(closeStr)) {
                        double price = Double.parseDouble(closeStr);
                        System.out.println("DEBUG: Parsed Stooq Close dla " + symbol + ": " + price);
                        return price;
                    } else {
                        System.out.println("DEBUG: Stooq zwrócił N/D dla " + symbol);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Błąd Stooq dla " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    // ===== AUTOTRADER =====

    public void setAutoTradeThresholds(Asset asset, Double buyThr, Double sellThr) {
        if (buyThr == null) buyThresholds.remove(asset);
        else buyThresholds.put(asset, buyThr);
        if (sellThr == null) sellThresholds.remove(asset);
        else sellThresholds.put(asset, sellThr);
    }

    public Double getBuyThreshold(Asset asset) {
        return buyThresholds.get(asset);
    }

    public Double getSellThreshold(Asset asset) {
        return sellThresholds.get(asset);
    }

    private void checkAutoTrade(Asset asset) {
        List<Double> hist = asset.getPriceHistory();
        int size = hist.size();
        if (size < 2) return;
        double prev = hist.get(size - 2);
        double curr = hist.get(size - 1);

        Double buyThr = buyThresholds.get(asset);
        if (buyThr != null && prev > buyThr && curr <= buyThr) {
            if (asset.getPrice() <= cash) {
                buyAsset(asset, 1);
            }
        }

        Double sellThr = sellThresholds.get(asset);
        if (sellThr != null && prev < sellThr && curr >= sellThr) {
            PortfolioEntry entry = portfolio.stream()
                    .filter(pe -> pe.getAssetName().equals(asset.getName()))
                    .findFirst().orElse(null);
            if (entry != null && entry.getQuantity() > 0) {
                sellAsset(asset, 1);
            }
        }
    }

    // ===== RĘCZNE TRANSAKCJE =====

    public void buyAsset(Asset asset, int quantity) {
        double totalCost = asset.getPrice() * quantity;
        if (totalCost > cash) {
            controller.showAlert("Brak środków", "Nie masz wystarczająco gotówki.");
            return;
        }
        cash -= totalCost;
        PortfolioEntry entry = portfolio.stream()
                .filter(pe -> pe.getAssetName().equals(asset.getName()))
                .findFirst().orElse(null);
        if (entry != null) {
            int prevQty = entry.getQuantity();
            double prevAvg = entry.getAveragePrice();
            double newAvg = ((prevAvg * prevQty) + asset.getPrice() * quantity) / (prevQty + quantity);
            entry.setAveragePrice(newAvg);
            entry.setQuantity(prevQty + quantity);
        } else {
            portfolio.add(new PortfolioEntry(asset, quantity, asset.getPrice()));
        }
        Platform.runLater(() -> {
            controller.refreshPortfolioTable();
            controller.updateCashLabel();
        });
    }

    public void sellAsset(Asset asset, int quantity) {
        PortfolioEntry entry = portfolio.stream()
                .filter(pe -> pe.getAssetName().equals(asset.getName()))
                .findFirst().orElse(null);
        if (entry == null || entry.getQuantity() < quantity) {
            controller.showAlert("Błąd sprzedaży", "Nie masz wystarczającej ilości.");
            return;
        }
        double totalProceeds = asset.getPrice() * quantity;
        cash += totalProceeds;
        int rem = entry.getQuantity() - quantity;
        if (rem == 0) portfolio.remove(entry);
        else entry.setQuantity(rem);
        Platform.runLater(() -> {
            controller.refreshPortfolioTable();
            controller.updateCashLabel();
        });
    }

    // ===== GETTERY =====

    public List<Asset> getAllAssets() {
        return allAssets;
    }

    public List<PortfolioEntry> getPortfolio() {
        return portfolio;
    }

    public double getInitialCash() {
        return cash;
    }

    public List<Double> getPriceHistory(Asset asset) {
        return asset.getPriceHistory();
    }

    // ===== RESET DANYCH =====

    public void useSimulationData() {
        for (Asset asset : allAssets) {
            asset.getPriceHistory().clear();
            asset.getPriceHistory().add(asset.getPrice());
        }
    }

    public void useRealData() {
        for (Asset asset : allAssets) {
            asset.getPriceHistory().clear();
            asset.getPriceHistory().add(asset.getPrice());
        }
    }

    /**
     * Zatrzymuje scheduler (kończy pobieranie lub symulację cen).
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
