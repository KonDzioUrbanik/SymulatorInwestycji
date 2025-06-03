package com.pans.konrad.apka.ui;

import com.pans.konrad.apka.logic.MarketService;
import com.pans.konrad.apka.model.Asset;
import com.pans.konrad.apka.model.PortfolioEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableCell;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.List;

/**
 * Kontroler dla głównego okna aplikacji (layout.fxml).
 * Tutaj jest wykres, tabela portfela, przyciski Kup/Sprzedaj, Autotrader, zmiana trybu.
 */
public class MainController {

    private MarketService marketService;

    // === Wstrzykiwane z layout.fxml ===
    @FXML private Label modeLabel;
    @FXML private ComboBox<Asset> assetComboBox;
    @FXML private Button openAssetWindowButton;
    @FXML private TextField manualQuantityField;
    @FXML private Button buyButton;
    @FXML private Button sellButton;
    @FXML private Label cashLabel;
    @FXML private TextField buyThresholdField;
    @FXML private TextField sellThresholdField;
    @FXML private Button setThresholdsButton;
    @FXML private Label noThresholdsLabel;

    @FXML private TableView<PortfolioEntry> portfolioTable;
    @FXML private TableColumn<PortfolioEntry, String> assetNameColumn;
    @FXML private TableColumn<PortfolioEntry, Integer> quantityColumn;
    @FXML private TableColumn<PortfolioEntry, Double> averagePriceColumn;

    @FXML private LineChart<String, Number> priceHistoryChart;
    @FXML private NumberAxis yAxis;

    // Seria danych wykresu
    private XYChart.Series<String, Number> priceSeries;
    private int timeCounter = 0;


    @FXML
    public void initialize() {
        // 1) Inicjalizacja wykresu
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Cena");
        priceHistoryChart.getData().add(priceSeries);
        yAxis.setAutoRanging(true);

        // 2) Inicjalizacja tabeli portfela
        assetNameColumn.setCellValueFactory(new PropertyValueFactory<>("assetName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        // 3) „Aktualna cena” → własny cellFactory ze strzałkami
        averagePriceColumn.setCellValueFactory(new Callback<CellDataFeatures<PortfolioEntry, Double>, javafx.beans.value.ObservableValue<Double>>() {
            @Override
            public javafx.beans.value.ObservableValue<Double> call(CellDataFeatures<PortfolioEntry, Double> param) {
                return null; // nie wykorzystujemy PropertyValueFactory, bo generujemy w cellFactory
            }
        });
        averagePriceColumn.setCellFactory(new Callback<TableColumn<PortfolioEntry, Double>, TableCell<PortfolioEntry, Double>>() {
            @Override
            public TableCell<PortfolioEntry, Double> call(TableColumn<PortfolioEntry, Double> col) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(Double avgPrice, boolean empty) {
                        super.updateItem(avgPrice, empty);
                        if (empty || getTableRow() == null) {
                            setText(null);
                            return;
                        }
                        PortfolioEntry entry = getTableRow().getItem();
                        if (entry == null) {
                            setText(null);
                            return;
                        }
                        // Znajdź odpowiadający obiekt Asset w marketService:
                        Asset a = marketService.getAllAssets().stream()
                                .filter(x -> x.getName().equals(entry.getAssetName()))
                                .findFirst().orElse(null);
                        if (a == null) {
                            setText("—");
                            return;
                        }
                        double currentPrice = a.getPrice();
                        double purchasePrice = entry.getAveragePrice();

                        // Format do 2 miejsc po przecinku:
                        String formatted = String.format("%.2f", currentPrice);

                        // Strzałka:
                        String arrow = "";
                        if (currentPrice > purchasePrice) {
                            arrow = " ↑";
                        } else if (currentPrice < purchasePrice) {
                            arrow = " ↓";
                        }
                        setText(formatted + arrow);
                    }
                };
            }
        });
    }

    /**
     * Wywoływane z ChoiceController po otwarciu głównego okna:
     * tworzy nowe MarketService (SYMULACJA lub REALNY) i wypełnia UI danymi.
     */
    public void initWithMode(ChoiceController.Mode mode) {
        if (marketService != null) {
            marketService.shutdown();
        }
        boolean simulation = (mode == ChoiceController.Mode.SIMULATION);
        this.marketService = new MarketService(this, simulation);

        // Ustawienie labelki trybu
        modeLabel.setText(simulation ? "Tryb: SYMULACJA" : "Tryb: REALNY");

        // Wypełnienie ComboBox aktywami
        List<Asset> assets = marketService.getAllAssets();
        ObservableList<Asset> items = FXCollections.observableArrayList(assets);
        assetComboBox.setItems(items);
        assetComboBox.getSelectionModel().selectFirst();

        // Reset wykresu
        timeCounter = 0;
        priceSeries.getData().clear();
        Asset first = assetComboBox.getValue();
        if (first != null) {
            priceSeries.getData().add(new XYChart.Data<>(String.valueOf(timeCounter++), first.getPrice()));
        }

        // Odśwież portfel i saldo
        refreshPortfolioTable();
        updateCashLabel();

        // Przełączanie aktywa → odśwież wykres
        assetComboBox.setOnAction(e -> {
            Asset selected = assetComboBox.getValue();
            if (selected != null) {
                timeCounter = 0;
                priceSeries.getData().clear();
                List<Double> history = marketService.getPriceHistory(selected);
                for (Double price : history) {
                    priceSeries.getData().add(new XYChart.Data<>(String.valueOf(timeCounter++), price));
                }
                yAxis.setAutoRanging(false);
                yAxis.setAutoRanging(true);
            }
        });
    }

    // === Obsługa przycisków Kup/Sprzedaj ===

    @FXML
    public void handleBuy() {
        Asset selected = assetComboBox.getValue();
        if (selected == null) return;
        String txt = manualQuantityField.getText().trim();
        if (txt.isEmpty()) {
            showAlert("Błąd", "Podaj liczbę sztuk do kupna.");
            return;
        }
        try {
            int qty = Integer.parseInt(txt);
            if (qty <= 0) {
                showAlert("Błąd", "Liczba sztuk musi być dodatnia.");
                return;
            }
            marketService.buyAsset(selected, qty);
            // Po kupnie wymuś odświeżenie tabeli (żeby zobaczyć strzałkę, jeśli cena się zmieniła)
            portfolioTable.refresh();
        } catch (NumberFormatException ex) {
            showAlert("Błąd", "Niepoprawna liczba sztuk.");
        }
    }

    @FXML
    public void handleSell() {
        Asset selected = assetComboBox.getValue();
        if (selected == null) return;
        String txt = manualQuantityField.getText().trim();
        if (txt.isEmpty()) {
            showAlert("Błąd", "Podaj liczbę sztuk do sprzedaży.");
            return;
        }
        try {
            int qty = Integer.parseInt(txt);
            if (qty <= 0) {
                showAlert("Błąd", "Liczba sztuk musi być dodatnia.");
                return;
            }
            marketService.sellAsset(selected, qty);
            portfolioTable.refresh();
        } catch (NumberFormatException ex) {
            showAlert("Błąd", "Niepoprawna liczba sztuk.");
        }
    }

    // === Obsługa Autotradera ===

    @FXML
    public void handleSetAutoTrader() {
        Asset selected = assetComboBox.getValue();
        if (selected == null) return;
        String buyTxt = buyThresholdField.getText().trim();
        String sellTxt = sellThresholdField.getText().trim();

        Double buyThr = null, sellThr = null;
        try {
            if (!buyTxt.isEmpty()) {
                buyThr = Double.parseDouble(buyTxt);
                if (buyThr <= 0) throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            showAlert("Błąd", "Niepoprawny próg kupna.");
            return;
        }
        try {
            if (!sellTxt.isEmpty()) {
                sellThr = Double.parseDouble(sellTxt);
                if (sellThr <= 0) throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            showAlert("Błąd", "Niepoprawny próg sprzedaży.");
            return;
        }

        marketService.setAutoTradeThresholds(selected, buyThr, sellThr);
        updateThresholdsLabel(selected);
    }

    private void updateThresholdsLabel(Asset asset) {
        Double buyThr = marketService.getBuyThreshold(asset);
        Double sellThr = marketService.getSellThreshold(asset);
        if (buyThr == null && sellThr == null) {
            noThresholdsLabel.setText("Brak progów");
        } else {
            String text = "";
            if (buyThr != null) text += "Kup ≤ " + String.format("%.2f", buyThr);
            if (sellThr != null) {
                if (!text.isEmpty()) text += "  ";
                text += "Sprzedaj ≥ " + String.format("%.2f", sellThr);
            }
            noThresholdsLabel.setText(text);
        }
    }

    // === Otwarcie okna konkretnego aktywa ===

    @FXML
    public void handleOpenAssetWindow() {
        Asset selected = assetComboBox.getValue();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/pans/konrad/apka/AssetWindow.fxml")
            );
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/com/pans/konrad/apka/style.css").toExternalForm()
            );

            AssetController ac = loader.getController();
            ac.initData(selected, marketService);

            Stage assetStage = new Stage();
            assetStage.setTitle("Okno aktywa: " + selected.getName());
            assetStage.setScene(scene);
            assetStage.setResizable(true);
            assetStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // === Zmiana trybu (powrót do wyboru) ===

    @FXML
    public void handleChangeMode() {
        Stage stage = (Stage) assetComboBox.getScene().getWindow();
        stage.close();

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/pans/konrad/apka/choice.fxml")
            );
            Scene scene = new Scene(loader.load(), 400, 200);
            scene.getStylesheets().add(
                    getClass().getResource("/com/pans/konrad/apka/style.css").toExternalForm()
            );
            Stage choiceStage = new Stage();
            choiceStage.setTitle("Wybór trybu");
            choiceStage.setScene(scene);
            choiceStage.setResizable(false);
            choiceStage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // === Odświeżanie tabeli portfela ===

    public void refreshPortfolioTable() {
        ObservableList<PortfolioEntry> list = FXCollections.observableArrayList(
                marketService.getPortfolio()
        );
        portfolioTable.setItems(list);
        // Upewnij się, że cellFactory się wywoła od razu:
        portfolioTable.refresh();
    }

    // === Aktualizacja etykiety Cash ===

    public void updateCashLabel() {
        double cash = marketService.getInitialCash();
        cashLabel.setText("Cash: $" + String.format("%.2f", cash));
    }

    // === Pomocniczy alert ===

    public void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Wywoływane z MarketService przy każdej zmianie ceny (symulacja lub realne).
     * Jeśli wykres aktualnie pokazuje tę samą instancję asset co w ComboBox, to dopisujemy nowy punkt,
     * a także odświeżamy tabelę portfela (żeby strzałka zmiany ceny się pokazała).
     */
    public void addChartDataPoint(Asset asset) {
        Asset selected = assetComboBox.getValue();
        System.out.println("DEBUG: addChartDataPoint wywołane dla " + asset.getName() +
                ", current selected = " + (selected == null ? "null" : selected.getName()));
        if (selected != null && selected.equals(asset)) {
            double currentPrice = asset.getPrice();
            System.out.println("DEBUG: dodaję punkt: czas=" + timeCounter + ", cena=" + currentPrice);
            priceSeries.getData().add(new XYChart.Data<>(String.valueOf(timeCounter++), currentPrice));
            yAxis.setAutoRanging(false);
            yAxis.setAutoRanging(true);
        }
        // Po każdej zmianie ceny odświeżamy tabelę, aby cellFactory mógł pokazać strzałkę:
        portfolioTable.refresh();
    }
}
