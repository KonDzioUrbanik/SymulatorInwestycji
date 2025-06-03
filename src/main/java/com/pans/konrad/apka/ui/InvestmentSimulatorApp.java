package com.pans.konrad.apka.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Klasa startowa aplikacji – uruchamia okno wyboru trybu (choice.fxml).
 */
public class InvestmentSimulatorApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/pans/konrad/apka/choice.fxml")
        );
        Scene scene = new Scene(loader.load(), 400, 200);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/com/pans/konrad/apka/style.css")).toExternalForm()
        );
        primaryStage.setTitle("Wybór trybu");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
