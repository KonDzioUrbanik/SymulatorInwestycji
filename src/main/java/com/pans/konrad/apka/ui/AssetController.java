package com.pans.konrad.apka.ui;

import com.pans.konrad.apka.logic.MarketService;
import com.pans.konrad.apka.model.Asset;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

/**
 * Kontroler dla okna pojedynczego aktywa (AssetWindow.fxml).
 * Pokazuje wykres tylko dla tego aktywa oraz pozwala kupić/sprzedać je w odrębnym oknie.
 */
public class AssetController {

    @FXML private Label assetNameLabel;
    @FXML private Label assetCashLabel;
    @FXML private TextField assetQuantityField;
    @FXML private Button assetBuyButton;
    @FXML private Button assetSellButton;
    @FXML private LineChart<String, Number> assetChart;
    @FXML private NumberAxis assetYaxis;

    private Asset asset;
    private MarketService marketService;

    // Seria wykresu dla tego aktywa
    private XYChart.Series<String, Number> series;
    private int timeCounter = 0;

    /**
     * Metoda inicjalizacyjna wywoływana z MainController.handleOpenAssetWindow(...)
     * @param asset – wybrane aktywo
     * @param marketService – referencja do istniejącego MarketService
     */
    public void initData(Asset asset, MarketService marketService) {
        this.asset = asset;
        this.marketService = marketService;
        assetNameLabel.setText(asset.getName());

        // Pokaż aktualną cash
        updateCashLabel();

        // Inicjalizuj wykres
        series = new XYChart.Series<>();
        series.setName("Cena " + asset.getName());
        assetChart.getData().add(series);
        assetYaxis.setAutoRanging(true);

        // Dodaj do wykresu historyczne punkty
        List<Double> history = marketService.getPriceHistory(asset);
        timeCounter = 0;
        for (Double price : history) {
            series.getData().add(new XYChart.Data<>(String.valueOf(timeCounter++), price));
        }

        // Ustaw listener, aby po każdej zmianie ceny (w MarketService) dopisywać nowy punkt
        // (MarketService wywoła addChartDataPoint w MainController, a tutaj rozszerzymy to:
        //  każda zmiana ceny dla tego aktywa dopisuje punkt do naszego series)
        new Thread(() -> {
            // W tej chwili nie ma dedykowanego callbacku –
            // wspomagamy się tym, że MainController co tick wywołuje addChartDataPoint,
            // a w trybie REALNYM albo SYMULACJA co tick podbije priceHistory, więc co tick:
            // możemy w Platform.runLater sprawdzić nowe wartość w priceHistory i dodać.
            // Dla uproszczenia pominąłem bardziej złożone subskrypcje.
        }).start();
    }

    @FXML
    public void handleAssetBuy() {
        String txt = assetQuantityField.getText().trim();
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
            marketService.buyAsset(asset, qty);
            updateCashLabel();
        } catch (NumberFormatException ex) {
            showAlert("Błąd", "Niepoprawna liczba sztuk.");
        }
    }

    @FXML
    public void handleAssetSell() {
        String txt = assetQuantityField.getText().trim();
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
            marketService.sellAsset(asset, qty);
            updateCashLabel();
        } catch (NumberFormatException ex) {
            showAlert("Błąd", "Niepoprawna liczba sztuk.");
        }
    }

    private void updateCashLabel() {
        Platform.runLater(() -> {
            double cash = marketService.getInitialCash();
            assetCashLabel.setText("$" + String.format("%.2f", cash));
        });
    }

    private void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
}
