package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Window control icons drawn on Canvas
 */
public class WindowIcons {

    /**
     * Create minimize icon (horizontal line)
     */
    public static Canvas createMinimizeIcon() {
        Canvas canvas = new Canvas(12, 12);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.web("#a8b2c1"));
        gc.setLineWidth(2);
        gc.strokeLine(2, 6, 10, 6);
        canvas.getStyleClass().add("icon-canvas");
        return canvas;
    }

    /**
     * Create maximize icon (square)
     */
    public static Canvas createMaximizeIcon() {
        Canvas canvas = new Canvas(12, 12);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.web("#a8b2c1"));
        gc.setLineWidth(1.5);
        gc.strokeRect(2, 2, 8, 8);
        canvas.getStyleClass().add("icon-canvas");
        return canvas;
    }

    /**
     * Create restore icon (two overlapping squares)
     */
    public static Canvas createRestoreIcon() {
        Canvas canvas = new Canvas(12, 12);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.web("#a8b2c1"));
        gc.setLineWidth(1.5);
        // Back square
        gc.strokeRect(4, 1, 7, 7);
        // Front square
        gc.strokeRect(1, 4, 7, 7);
        canvas.getStyleClass().add("icon-canvas");
        return canvas;
    }

    /**
     * Create close icon (X)
     */
    public static Canvas createCloseIcon() {
        Canvas canvas = new Canvas(12, 12);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.web("#a8b2c1"));
        gc.setLineWidth(2);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeLine(2, 2, 10, 10);
        gc.strokeLine(10, 2, 2, 10);
        canvas.getStyleClass().add("icon-canvas");
        return canvas;
    }

    /**
     * Update canvas color (for hover effects)
     */
    public static void updateIconColor(Canvas canvas, Color color, String type) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(color);

        switch(type) {
            case "minimize":
                gc.setLineWidth(2);
                gc.strokeLine(2, 6, 10, 6);
                break;
            case "maximize":
                gc.setLineWidth(1.5);
                gc.strokeRect(2, 2, 8, 8);
                break;
            case "restore":
                gc.setLineWidth(1.5);
                gc.strokeRect(4, 1, 7, 7);
                gc.strokeRect(1, 4, 7, 7);
                break;
            case "close":
                gc.setLineWidth(2);
                gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                gc.strokeLine(2, 2, 10, 10);
                gc.strokeLine(10, 2, 2, 10);
                break;
        }
    }
}