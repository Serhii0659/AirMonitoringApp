package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField dbNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private CustomTitleBar titleBar;

    @FXML
    private void initialize() {
        Stage stage = HelloApplication.getPrimaryStage();
        if (titleBar != null && stage != null) {
            titleBar.init("Air Monitoring - Вхід", stage, false, true);
            Platform.runLater(() -> {
                javafx.scene.Parent root = titleBar.getScene().getRoot();
                if (root != null) {
                    root.setStyle(root.getStyle() + "; -fx-background-radius: 0;");
                }
            });
        }

        dbNameField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) usernameField.requestFocus();
        });
        usernameField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) passwordField.requestFocus();
        });
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) onConnect();
        });

        Platform.runLater(() -> dbNameField.requestFocus());

        String configError = ConfigManager.getLastError();
        if (configError != null) {
            showError("Помилка конфігурації: " + configError);
            return;
        }

        String dbName = ConfigManager.getDbName();
        String user = ConfigManager.getDbUser();
        String password = ConfigManager.getDbPassword();

        if (dbName != null && !dbName.isEmpty()) dbNameField.setText(dbName);
        if (user != null && !user.isEmpty()) usernameField.setText(user);
        if (password != null && !password.isEmpty()) passwordField.setText(password);

        if (dbName != null && !dbName.isEmpty()) {
            Platform.runLater(() -> usernameField.requestFocus());
        }
    }

    @FXML
    private void onConnect() {
        String dbNameInput = dbNameField.getText().trim();
        String userInput = usernameField.getText().trim();
        String passInput = passwordField.getText();

        if (dbNameInput.isEmpty()) {
            showError("Введіть назву бази даних");
            return;
        }

        if (userInput.isEmpty()) {
            showError("Введіть логін");
            return;
        }

        ConfigManager.setDbName(dbNameInput);

        boolean ok = DbManager.connect(userInput, passInput);

        if (!ok) {
            showError("Помилка: " + DbManager.getLastError());
            return;
        }

        statusLabel.setText("Успішно підключено");
        statusLabel.getStyleClass().remove("label-error");
        statusLabel.getStyleClass().add("label-success");

        HelloApplication.setCurrentUsername(userInput);
        HelloApplication.showDataWindow();
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().remove("label-success");
        statusLabel.getStyleClass().add("label-error");
        resizeWindow();
    }

    private void resizeWindow() {
        Platform.runLater(() -> {
            Stage stage = HelloApplication.getPrimaryStage();
            if (stage != null && stage.isShowing()) {
                stage.sizeToScene();
            }
        });
    }
}