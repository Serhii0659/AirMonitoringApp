package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private CustomTitleBar titleBar;

    @FXML
    private void initialize() {
        // Initialize title bar
        Stage stage = HelloApplication.getPrimaryStage();
        if (titleBar != null && stage != null) {
            titleBar.init("Air Monitoring - Вхід", stage, false);
        }

        // Check if config is loaded
        String configError = ConfigManager.getLastError();
        if (configError != null) {
            statusLabel.setText("Помилка конфігурації: " + configError);
            statusLabel.getStyleClass().add("label-error");
            return;
        }

        // Auto-fill credentials from config if available
        String user = ConfigManager.getDbUser();
        String password = ConfigManager.getDbPassword();

        if (user != null && !user.isEmpty()) {
            usernameField.setText(user);
        }
        if (password != null && !password.isEmpty()) {
            passwordField.setText(password);
        }
    }

    @FXML
    private void onConnect() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        if (user.isEmpty()) {
            statusLabel.setText("Введіть логін");
            return;
        }
        boolean ok = DbManager.connect(user, pass);
        if (!ok) {
            statusLabel.setText("Помилка: " + DbManager.getLastError());
            statusLabel.getStyleClass().add("label-error");
            return;
        }
        statusLabel.setText("Успішно підключено");
        statusLabel.getStyleClass().add("label-success");

        HelloApplication.showDataWindow();
    }
}