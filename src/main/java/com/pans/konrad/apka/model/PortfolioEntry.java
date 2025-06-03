package com.pans.konrad.apka.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Pozycja w portfelu: nazwa aktywa, ilość oraz średnia cena zakupu.
 * Używamy prostych właściwości (JavaFX Property), aby wyświetlić to w TableView.
 */
public class PortfolioEntry {
    private final SimpleStringProperty assetName;
    private final SimpleIntegerProperty quantity;
    private final SimpleDoubleProperty averagePrice;

    public PortfolioEntry(Asset asset, int quantity, double avgPrice) {
        this.assetName    = new SimpleStringProperty(asset.getName());
        this.quantity     = new SimpleIntegerProperty(quantity);
        this.averagePrice = new SimpleDoubleProperty(avgPrice);
    }

    public String getAssetName() {
        return assetName.get();
    }

    public SimpleStringProperty assetNameProperty() {
        return assetName;
    }

    public void setAssetName(String name) {
        this.assetName.set(name);
    }

    public int getQuantity() {
        return quantity.get();
    }

    public SimpleIntegerProperty quantityProperty() {
        return quantity;
    }

    public void setQuantity(int qty) {
        this.quantity.set(qty);
    }

    public double getAveragePrice() {
        return averagePrice.get();
    }

    public SimpleDoubleProperty averagePriceProperty() {
        return averagePrice;
    }

    public void setAveragePrice(double avgPrice) {
        this.averagePrice.set(avgPrice);
    }
}
