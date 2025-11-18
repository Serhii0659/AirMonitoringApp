package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * Custom title bar for the application
 */
public class CustomTitleBar extends HBox {

    private double xOffset = 0;
    private double yOffset = 0;
    private Button maximizeBtn;
    private Stage currentStage;

    /**
     * Default constructor for FXML
     */
    public CustomTitleBar() {
        super(10);
        getStyleClass().add("custom-title-bar");
    }

    /**
     * Initialize the title bar with stage and settings
     */
    public void init(String title, Stage stage, boolean showMaximize, boolean showMinimize) {
        this.currentStage = stage;

        getChildren().clear();

        // Title label
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title-bar-label");
        titleLabel.setStyle("-fx-font-size: 14px;");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Minimize button (optional)
        final Button minimizeBtn;
        if (showMinimize) {
            minimizeBtn = new Button();
            javafx.scene.canvas.Canvas minIcon = WindowIcons.createMinimizeIcon();
            minimizeBtn.setGraphic(minIcon);
            minimizeBtn.getStyleClass().addAll("title-bar-button", "minimize-button");
            minimizeBtn.setOnAction(e -> currentStage.setIconified(true));
            minimizeBtn.setOnMouseEntered(e -> WindowIcons.updateIconColor(minIcon, javafx.scene.paint.Color.web("#00d9ff"), "minimize"));
            minimizeBtn.setOnMouseExited(e -> WindowIcons.updateIconColor(minIcon, javafx.scene.paint.Color.web("#a8b2c1"), "minimize"));
        } else {
            minimizeBtn = null;
        }

        if (showMaximize) {
            final Button maximizeBtn = new Button();
            this.maximizeBtn = maximizeBtn; // Save reference
            javafx.scene.canvas.Canvas maxIcon = WindowIcons.createMaximizeIcon();
            maximizeBtn.setGraphic(maxIcon);
            maximizeBtn.getStyleClass().addAll("title-bar-button", "maximize-button");

            // Store original size for restore
            final double[] originalSize = {1200, 700}; // Default size

            maximizeBtn.setOnAction(e -> {
                javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();

                // Check if currently "maximized" (covers most of screen)
                boolean isCurrentlyMaximized = (currentStage.getWidth() >= bounds.getWidth() - 50 &&
                                              currentStage.getHeight() >= bounds.getHeight() - 50);

                if (isCurrentlyMaximized) {
                    // Restore to original size
                    currentStage.setWidth(originalSize[0]);
                    currentStage.setHeight(originalSize[1]);
                    currentStage.centerOnScreen();

                    // Update icon to maximize
                    javafx.scene.canvas.Canvas maxIcon2 = WindowIcons.createMaximizeIcon();
                    maximizeBtn.setGraphic(maxIcon2);
                    maximizeBtn.setOnMouseEntered(ev -> WindowIcons.updateIconColor(maxIcon2, javafx.scene.paint.Color.web("#00d9ff"), "maximize"));
                    maximizeBtn.setOnMouseExited(ev -> WindowIcons.updateIconColor(maxIcon2, javafx.scene.paint.Color.web("#a8b2c1"), "maximize"));
                } else {
                    // Save current size and maximize
                    originalSize[0] = currentStage.getWidth();
                    originalSize[1] = currentStage.getHeight();

                    currentStage.setX(bounds.getMinX());
                    currentStage.setY(bounds.getMinY());
                    currentStage.setWidth(bounds.getWidth());
                    currentStage.setHeight(bounds.getHeight());

                    // Update icon to restore
                    javafx.scene.canvas.Canvas restoreIcon = WindowIcons.createRestoreIcon();
                    maximizeBtn.setGraphic(restoreIcon);
                    maximizeBtn.setOnMouseEntered(ev -> WindowIcons.updateIconColor(restoreIcon, javafx.scene.paint.Color.web("#00d9ff"), "restore"));
                    maximizeBtn.setOnMouseExited(ev -> WindowIcons.updateIconColor(restoreIcon, javafx.scene.paint.Color.web("#a8b2c1"), "restore"));
                }
            });
            maximizeBtn.setOnMouseEntered(e -> WindowIcons.updateIconColor(maxIcon, javafx.scene.paint.Color.web("#00d9ff"), "maximize"));
            maximizeBtn.setOnMouseExited(e -> WindowIcons.updateIconColor(maxIcon, javafx.scene.paint.Color.web("#a8b2c1"), "maximize"));
        }

        final Button closeBtn = new Button();
        javafx.scene.canvas.Canvas closeIcon = WindowIcons.createCloseIcon();
        closeBtn.setGraphic(closeIcon);
        closeBtn.getStyleClass().addAll("title-bar-button", "close-button");
        closeBtn.setOnAction(e -> currentStage.close());
        closeBtn.setOnMouseEntered(e -> WindowIcons.updateIconColor(closeIcon, javafx.scene.paint.Color.WHITE, "close"));
        closeBtn.setOnMouseExited(e -> WindowIcons.updateIconColor(closeIcon, javafx.scene.paint.Color.web("#a8b2c1"), "close"));

        // Add all elements
        getChildren().addAll(titleLabel, spacer);
        if (minimizeBtn != null) {
            getChildren().add(minimizeBtn);
        }
        if (maximizeBtn != null) {
            getChildren().add(maximizeBtn);
        }
        getChildren().add(closeBtn);

        // Make window draggable (only when not maximized)
        setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        setOnMouseDragged(event -> {
            // Check if currently "maximized" by comparing window size with screen
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
            boolean isCurrentlyMaximized = (currentStage.getWidth() >= bounds.getWidth() - 50 &&
                                          currentStage.getHeight() >= bounds.getHeight() - 50);

            if (!isCurrentlyMaximized) {
                currentStage.setX(event.getScreenX() - xOffset);
                currentStage.setY(event.getScreenY() - yOffset);
            }
        });

        setOnMouseEntered(e -> {
            // Show hand cursor only if not maximized
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
            boolean isCurrentlyMaximized = (currentStage.getWidth() >= bounds.getWidth() - 50 &&
                                          currentStage.getHeight() >= bounds.getHeight() - 50);
            setCursor(isCurrentlyMaximized ? Cursor.DEFAULT : Cursor.HAND);
        });
        setOnMouseExited(e -> setCursor(Cursor.DEFAULT));
    }

    /**
     * Update maximize/restore icon based on current window state
     */
    public void updateMaximizeIcon() {
        if (maximizeBtn != null && currentStage != null) {
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
            boolean isCurrentlyMaximized = (currentStage.getWidth() >= bounds.getWidth() - 50 &&
                                          currentStage.getHeight() >= bounds.getHeight() - 50);

            if (isCurrentlyMaximized) {
                // Show restore icon
                javafx.scene.canvas.Canvas restoreIcon = WindowIcons.createRestoreIcon();
                maximizeBtn.setGraphic(restoreIcon);
                maximizeBtn.setOnMouseEntered(e -> WindowIcons.updateIconColor(restoreIcon, javafx.scene.paint.Color.web("#00d9ff"), "restore"));
                maximizeBtn.setOnMouseExited(e -> WindowIcons.updateIconColor(restoreIcon, javafx.scene.paint.Color.web("#a8b2c1"), "restore"));
            } else {
                // Show maximize icon
                javafx.scene.canvas.Canvas maxIcon = WindowIcons.createMaximizeIcon();
                maximizeBtn.setGraphic(maxIcon);
                maximizeBtn.setOnMouseEntered(e -> WindowIcons.updateIconColor(maxIcon, javafx.scene.paint.Color.web("#00d9ff"), "maximize"));
                maximizeBtn.setOnMouseExited(e -> WindowIcons.updateIconColor(maxIcon, javafx.scene.paint.Color.web("#a8b2c1"), "maximize"));
            }
        }
    }
}