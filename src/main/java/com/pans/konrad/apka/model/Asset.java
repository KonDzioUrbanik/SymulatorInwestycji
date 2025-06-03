package com.pans.konrad.apka.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reprezentuje pojedyncze aktywo: nazwa, typ, bieżąca cena, historia cen.
 */
public class Asset {
    private final String name;
    private final AssetType type;
    private double price;
    private final List<Double> priceHistory = new ArrayList<>();

    public Asset(String name, AssetType type, double initialPrice) {
        this.name = name;
        this.type = type;
        this.price = initialPrice;
        priceHistory.add(initialPrice);
    }

    public String getName() {
        return name;
    }

    public AssetType getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public List<Double> getPriceHistory() {
        return priceHistory;
    }

    public void addPriceToHistory(double newPrice) {
        priceHistory.add(newPrice);
    }

    @Override
    public String toString() {
        // Dzięki temu obiekt wyświetli się w ComboBox jako nazwa
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset)) return false;
        Asset asset = (Asset) o;
        return Objects.equals(name, asset.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
