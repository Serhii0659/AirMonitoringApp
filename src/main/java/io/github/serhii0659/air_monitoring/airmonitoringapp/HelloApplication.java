package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class HelloApplication extends Application {

    private static Stage primaryStage;
    private static boolean wasMaximized = true; // Remember window state

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        showLoginWindow();
    }

    public static void showLoginWindow() {
        // Close old stage and create new one
        Stage oldStage = primaryStage;
        primaryStage = new Stage();

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
            if (fxmlLoader.getLocation() == null) {
                throw new IllegalStateException("login-view.fxml not found");
            }
            Scene scene = new Scene(fxmlLoader.load(), 500, 450);

            // Load custom CSS
            String css = HelloApplication.class.getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();
            primaryStage.show();

            // Close old window
            if (oldStage != null) {
                oldStage.close();
            }
        } catch (Throwable t) {
            System.err.println("Failed to show login window:");
            t.printStackTrace();
        }
    }

    public static void showDataWindow() {
        // Close old stage and create new one to avoid resize animation
        Stage oldStage = primaryStage;
        primaryStage = new Stage();

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("data-view.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 700);

            String css = HelloApplication.class.getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Air Monitoring - Дані");

            // Show new window first
            primaryStage.show();

            // Apply saved maximized state using screen bounds (avoiding taskbar)
            System.out.println("Applying maximized state: " + wasMaximized); // Debug
            if (wasMaximized) {
                javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                javafx.geometry.Rectangle2D bounds = screen.getVisualBounds(); // Visual bounds exclude taskbar
                primaryStage.setX(bounds.getMinX());
                primaryStage.setY(bounds.getMinY());
                primaryStage.setWidth(bounds.getWidth());
                primaryStage.setHeight(bounds.getHeight());
            } else {
                // Center the window if not maximized
                primaryStage.centerOnScreen();
            }

            // Close old window
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
}