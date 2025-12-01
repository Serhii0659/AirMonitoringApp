package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class HelloApplication extends Application {

    private static Stage primaryStage;
    private static boolean wasMaximized = true;
    private static String currentUsername = "";

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        showLoginWindow();
    }

    public static void showLoginWindow() {
        Stage oldStage = primaryStage;
        primaryStage = new Stage();

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            String css = HelloApplication.class.getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);

            primaryStage.show();
            primaryStage.centerOnScreen();

            if (oldStage != null) {
                oldStage.close();
            }
        } catch (Throwable t) {
            System.err.println("Failed to show login window:");
            t.printStackTrace();
        }
    }

    public static void showDataWindow() {
        Stage oldStage = primaryStage;
        primaryStage = new Stage();

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("data-view.fxml"));
            // Базовий розмір для вікна даних
            Scene scene = new Scene(loader.load(), 1200, 700);

            String css = HelloApplication.class.getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Air Monitoring - Дані");

            if (wasMaximized) {
                javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();

                primaryStage.setX(bounds.getMinX());
                primaryStage.setY(bounds.getMinY());
                primaryStage.setWidth(bounds.getWidth());
                primaryStage.setHeight(bounds.getHeight());
            }

            primaryStage.show();

            if (!wasMaximized) {
                primaryStage.centerOnScreen();
            }

            if (oldStage != null) {
                oldStage.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void setWasMaximized(boolean maximized) {
        wasMaximized = maximized;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static void setCurrentUsername(String username) {
        currentUsername = username;
    }
}