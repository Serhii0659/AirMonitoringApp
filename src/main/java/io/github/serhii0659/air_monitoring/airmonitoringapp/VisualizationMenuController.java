package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class VisualizationMenuController {
    @FXML private CustomTitleBar titleBar;

    @FXML
    private void initialize() {
        // Initialize title bar after stage is available
        Platform.runLater(() -> {
            Stage stage = getCurrentStage();
            if (titleBar != null && stage != null) {
                titleBar.init("Air Monitoring - Візуалізація", stage, false, false);
            }
        });
    }

    @FXML
    private void onPM25PM10Visualization() {
        openVisualization("PM2.5 та PM10 по областях", "pm25pm10-view.fxml");
    }

    @FXML
    private void onPM25HarmfulVisualization() {
        openVisualization("PM2.5 шкідливий рівень", "pm25harmful-view.fxml");
    }

    @FXML
    private void onSO2Visualization() {
        openVisualization("Діоксид сірки (SO₂)", "so2-view.fxml");
    }

    @FXML
    private void onCOVisualization() {
        openVisualization("Чадний газ (CO)", "co-view.fxml");
    }

    private void openVisualization(String title, String fxmlFile) {
        try {
            System.out.println("Відкриваємо візуалізацію: " + title + ", файл: " + fxmlFile);

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource(fxmlFile));

            if (loader.getLocation() == null) {
                String error = "Не знайдено файл: " + fxmlFile;
                System.err.println(error);
                showError(error);
                return;
            }

            System.out.println("Файл знайдено, завантажуємо FXML...");
            javafx.scene.Parent root = loader.load();
            System.out.println("FXML завантажено успішно");

            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1000, 700);

            String css = getClass().getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(css);
            System.out.println("Стилі додано");

            javafx.stage.Stage vizStage = new javafx.stage.Stage();
            vizStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            vizStage.setScene(scene);
            vizStage.setTitle("Air Monitoring - " + title);
            vizStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            vizStage.setResizable(true);
            vizStage.centerOnScreen();

            System.out.println("Показуємо вікно...");
            vizStage.show();
            System.out.println("Вікно показано успішно!");

            // НЕ закриваємо меню - користувач сам закриє натиснувши X
            // onClose();
        } catch (Exception e) {
            System.err.println("Помилка відкриття візуалізації: " + e.getMessage());
            e.printStackTrace();
            showError("Помилка відкриття візуалізації:\n" + e.getMessage() + "\n\nДеталі в консолі");
        }
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Помилка");
        alert.setHeaderText("Помилка візуалізації");
        alert.setContentText(message);
        alert.initOwner(getCurrentStage());
        alert.showAndWait();
    }

    @FXML
    private void onClose() {
        Stage stage = getCurrentStage();
        if (stage != null) {
            stage.close();
        }
    }

    private Stage getCurrentStage() {
        if (titleBar != null && titleBar.getScene() != null) {
            return (Stage) titleBar.getScene().getWindow();
        }
        return null;
    }
}