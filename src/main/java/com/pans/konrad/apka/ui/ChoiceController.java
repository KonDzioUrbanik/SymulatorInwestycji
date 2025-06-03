package com.pans.konrad.apka.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Kontroler dla okienka wyboru trybu (choice.fxml).
 * Pozwala użytkownikowi wybrać SYMULACJĘ lub REALNY tryb,
 * a następnie otwiera główne okno aplikacji.
 */
public class ChoiceController {
    public enum Mode { SIMULATION, REAL }

    @FXML
    public void onSimulation(ActionEvent event) {
        openMainWindow(Mode.SIMULATION, event);
    }

    @FXML
    public void onReal(ActionEvent event) {
        openMainWindow(Mode.REAL, event);
    }

    private void openMainWindow(Mode mode, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/pans/konrad/apka/layout.fxml")
            );
            Scene scene = new Scene(loader.load(), 800, 600);
            scene.getStylesheets().add(
                    getClass().getResource("/com/pans/konrad/apka/style.css").toExternalForm()
            );

            MainController controller = loader.getController();
            controller.initWithMode(mode);

            Stage mainStage = new Stage();
            mainStage.setTitle("Investment Simulator");
            mainStage.setResizable(true);
            mainStage.setScene(scene);
            mainStage.show();

            // Zamknij (ukryj) okno Choice
            Stage choiceStage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();
            choiceStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
