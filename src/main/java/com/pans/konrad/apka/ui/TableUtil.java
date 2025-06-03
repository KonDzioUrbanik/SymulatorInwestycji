package com.pans.konrad.apka.ui;

import javafx.scene.control.TableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;

/**
 * Klasa pomocnicza do tworzenia komórek tabeli formatujących liczby
 * na 2 miejsca po przecinku.
 */
public class TableUtil {

    /** Zwraca TableCell<Double> formatującą wartość do dwóch miejsc po przecinku. */
    public static <S> TableCell<S, Double> createTwoDecimalCell() {
        TextFieldTableCell<S, Double> cell = new TextFieldTableCell<>();
        cell.setConverter(new StringConverter<>() {
            @Override
            public String toString(Double value) {
                return value == null ? "" : String.format("%.2f", value);
            }
            @Override
            public Double fromString(String string) {
                try {
                    return Double.parseDouble(string);
                } catch (Exception e) {
                    return 0.0;
                }
            }
        });
        return cell;
    }
}
