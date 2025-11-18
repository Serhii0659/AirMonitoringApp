package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;

public class DataViewController {
    @FXML private ComboBox<String> tablesBox;
    @FXML private TableView<ObservableList<String>> tableView;
    @FXML private Label infoLabel;
    @FXML private Label usernameLabel;
    @FXML private Spinner<Integer> limitSpinner;
    @FXML private CheckBox showAllCheckBox;
    @FXML private CustomTitleBar titleBar;

    // Pagination components
    @FXML private HBox paginationBox;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private Label pageLabel;
    @FXML private Label totalRecordsLabel;

    // Loading overlay components
    @FXML private VBox loadingOverlay;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label progressPercent;
    @FXML private Button cancelButton;

    private Task<DataLoadResult> currentTask;

    // Pagination state
    private int currentPage = 1;
    private int totalRecords = 0;
    private int recordsPerPage = 500;

    // Previous state for cancellation
    private DataLoadResult previousResult;
    private String previousTable;
    private int previousTotalRecords;
    private boolean previousShowAllState;
    private int previousPage;
    private boolean isRestoringState = false; // Flag to prevent listener triggers during restore

    // Sorting configuration
    // TODO: Replace with DB-level sorting (ORDER BY in SQL query)
    private static final int SORTING_THRESHOLD = 5000; // Disable sorting if more records
    private static final boolean USE_DB_SORTING = false; // Set to true when implementing DB sorting

    @FXML
    private void initialize() {
        // Initialize title bar
        Stage stage = HelloApplication.getPrimaryStage();
        if (titleBar != null && stage != null) {
            titleBar.init("Air Monitoring - Дані", stage, true);

            // Update maximize icon after scene is fully loaded
            Platform.runLater(() -> {
                titleBar.updateMaximizeIcon();

                // Setup Esc key handler for disconnect
                stage.getScene().setOnKeyPressed(event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        onDisconnect();
                    }
                });
            });
        }

        // Display current username
        String username = HelloApplication.getCurrentUsername();
        if (usernameLabel != null && username != null && !username.isEmpty()) {
            usernameLabel.setText("👤 " + username);
        }

        limitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 10000, 500, 50));

        // Disable spinner when "Show All" is checked
        showAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!isRestoringState) {
                // Save state BEFORE the change (oldVal is the previous checkbox state)
                saveCurrentStateWithCheckbox(oldVal);

                limitSpinner.setDisable(newVal);
                resetPaginationAndLoad();
            }
        });

        // Reload when user changes selection or limit
        tablesBox.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (!isRestoringState && newV != null) {
                // Save state BEFORE changing tables - use oldV (previous table)
                saveCurrentStateWithTable(oldV);

                resetPaginationAndLoad();
            }
        });
        limitSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isRestoringState && !showAllCheckBox.isSelected()) {
                // Save state BEFORE changing limit
                saveCurrentState();

                recordsPerPage = newV != null ? newV : 500;
                resetPaginationAndLoad();
            }
        });

        refreshTables();
    }

    private void refreshTables() {
        tablesBox.getItems().setAll(DbManager.getTables());
        if (!tablesBox.getItems().isEmpty()) tablesBox.getSelectionModel().selectFirst();
        infoLabel.setText(DbManager.isConnected() ? "✓ Підключено до бази даних" : "✗ Немає з'єднання");
        loadSelectedTable();
    }

    @FXML
    private void onRefresh() {
        saveCurrentState();
        loadSelectedTable();
    }

    @FXML
    private void onDisconnect() {
        // Cancel any running task
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }

        // Save window state before disconnecting
        Stage stage = HelloApplication.getPrimaryStage();
        if (stage != null) {
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
            boolean isMaximized = (stage.getWidth() >= bounds.getWidth() - 50 &&
                                 stage.getHeight() >= bounds.getHeight() - 50);
            HelloApplication.setWasMaximized(isMaximized);
            System.out.println("Saving state on disconnect: " + isMaximized); // Debug
        }

        DbManager.disconnect();

        // Use new window system to avoid resize animation
        HelloApplication.showLoginWindow();
    }

    @FXML
    private void onPreviousPage() {
        if (currentPage > 1) {
            saveCurrentState();
            currentPage--;
            loadSelectedTable();
        }
    }

    @FXML
    private void onNextPage() {
        int totalPages = (int) Math.ceil((double) totalRecords / recordsPerPage);
        if (currentPage < totalPages) {
            saveCurrentState();
            currentPage++;
            loadSelectedTable();
        }
    }

    private void resetPaginationAndLoad() {
        currentPage = 1;
        loadSelectedTable();
    }

    @FXML
    private void onCancelLoad() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
            hideLoadingOverlay();

            // Restore previous state
            if (previousResult != null && previousTable != null) {
                isRestoringState = true; // Disable listeners during restore

                try {
                    // Restore table selection in ComboBox - must be done in UI thread
                    Platform.runLater(() -> {
                        tablesBox.getSelectionModel().select(previousTable);
                    });

                    // Restore checkbox state
                    showAllCheckBox.setSelected(previousShowAllState);

                    // Restore pagination state
                    currentPage = previousPage;
                    totalRecords = previousTotalRecords;

                    // Restore spinner state
                    limitSpinner.setDisable(previousShowAllState);

                    // Update pagination UI
                    updatePaginationUI();

                    // Restore table data
                    displayData(previousResult, previousTable, previousTotalRecords);
                    infoLabel.setText("⚠ Завантаження скасовано - відновлено попередній стан");
                } finally {
                    // Re-enable listeners after a short delay to ensure UI updates complete
                    Platform.runLater(() -> {
                        isRestoringState = false;
                    });
                }
            } else {
                infoLabel.setText("⚠ Завантаження скасовано");
            }
        }
    }


    /**
     * Save current state for potential cancellation/restore
     */
    private void saveCurrentState() {
        if (tableView.getItems() != null && !tableView.getItems().isEmpty()) {
            previousResult = new DataLoadResult();
            previousResult.data = javafx.collections.FXCollections.observableArrayList(tableView.getItems());
            previousResult.columnNames = new java.util.ArrayList<>();
            for (TableColumn<ObservableList<String>, ?> col : tableView.getColumns()) {
                previousResult.columnNames.add(col.getText());
            }
            previousTable = tablesBox.getValue();
            previousTotalRecords = totalRecords;
            previousShowAllState = showAllCheckBox.isSelected();
            previousPage = currentPage;
        }
    }

    /**
     * Save current state with explicit checkbox state (used in checkbox listener)
     */
    private void saveCurrentStateWithCheckbox(boolean checkboxState) {
        if (tableView.getItems() != null && !tableView.getItems().isEmpty()) {
            previousResult = new DataLoadResult();
            previousResult.data = javafx.collections.FXCollections.observableArrayList(tableView.getItems());
            previousResult.columnNames = new java.util.ArrayList<>();
            for (TableColumn<ObservableList<String>, ?> col : tableView.getColumns()) {
                previousResult.columnNames.add(col.getText());
            }
            previousTable = tablesBox.getValue();
            previousTotalRecords = totalRecords;
            previousShowAllState = checkboxState; // Use the provided checkbox state
            previousPage = currentPage;
        }
    }

    /**
     * Save current state with explicit table name (used in table selection listener)
     */
    private void saveCurrentStateWithTable(String tableName) {
        if (tableView.getItems() != null && !tableView.getItems().isEmpty() && tableName != null) {
            previousResult = new DataLoadResult();
            previousResult.data = javafx.collections.FXCollections.observableArrayList(tableView.getItems());
            previousResult.columnNames = new java.util.ArrayList<>();
            for (TableColumn<ObservableList<String>, ?> col : tableView.getColumns()) {
                previousResult.columnNames.add(col.getText());
            }
            previousTable = tableName; // Use the provided table name (old value from listener)
            previousTotalRecords = totalRecords;
            previousShowAllState = showAllCheckBox.isSelected();
            previousPage = currentPage;
        }
    }

    private void loadSelectedTable() {
        String table = tablesBox.getValue();
        if (table == null) return;

        // Cancel any running task
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }

        int limit = showAllCheckBox.isSelected() ? 0 : limitSpinner.getValue();
        recordsPerPage = limit > 0 ? limit : 500;

        // Calculate offset for pagination
        int offset = (currentPage - 1) * recordsPerPage;

        // Get total record count first
        try {
            totalRecords = DbManager.getTableRecordCount(table);
        } catch (SQLException e) {
            infoLabel.setText("❌ Помилка отримання кількості записів: " + e.getMessage());
            return;
        }

        // Update pagination UI
        updatePaginationUI();

        try {
            // Determine if we need async loading with predictive logic
            boolean needAsyncLoading;

            if (showAllCheckBox.isSelected()) {
                // "Show all" selected - we know exactly how much data we'll load
                // Use async for tables with more than 1000 records
                needAsyncLoading = totalRecords > 1000;
            } else {
                // Pagination mode - we're loading specific amount
                // Use async only if loading more than 1000 records
                int recordsToLoad = Math.min(recordsPerPage, totalRecords - offset);
                needAsyncLoading = recordsToLoad > 1000;
            }

            if (needAsyncLoading) {
                // Use async loading with progress for large datasets
                loadTableAsync(table, limit, offset, totalRecords);
            } else {
                // Quick synchronous load for small/medium datasets
                loadTableSync(table, limit, offset, totalRecords);
            }

        } catch (Exception e) {
            infoLabel.setText("❌ Помилка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadTableSync(String table, int limit, int offset, int totalCount) {
        try {
            // No loading overlay for sync operations - they should be fast
            DataLoadTask task = new DataLoadTask(table, limit, offset, totalCount);
            DataLoadResult result = task.call();

            if (result != null) {
                displayData(result, table, totalCount);
            }
        } catch (Exception e) {
            infoLabel.setText("❌ Помилка завантаження: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadTableAsync(String table, int limit, int offset, int totalCount) {
        currentTask = new DataLoadTask(table, limit, offset, totalCount);

        // Predictive loading overlay logic:
        // - For very large datasets (>5000 records to load): show immediately
        // - For medium datasets (1000-5000): show after 500ms delay
        int recordsToLoad = (limit == 0) ? totalCount : Math.min(limit, totalCount - offset);
        boolean showImmediately = recordsToLoad > 5000;

        final boolean[] loadingShown = {false};
        javafx.animation.PauseTransition delay = null;

        if (showImmediately) {
            // Show loading overlay immediately for large operations
            showLoadingOverlay();
            loadingShown[0] = true;

            // Bind progress
            progressBar.progressProperty().bind(currentTask.progressProperty());
            progressLabel.textProperty().bind(currentTask.messageProperty());

            // Update percentage label
            currentTask.progressProperty().addListener((obs, oldVal, newVal) -> {
                int percent = (int) (newVal.doubleValue() * 100);
                progressPercent.setText(percent + "%");
            });
        } else {
            // Delayed loading overlay for medium datasets - show only if takes > 500ms
            delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));

            delay.setOnFinished(e -> {
                if (currentTask != null && currentTask.isRunning()) {
                    showLoadingOverlay();
                    loadingShown[0] = true;

                    // Bind progress only when overlay is shown
                    progressBar.progressProperty().bind(currentTask.progressProperty());
                    progressLabel.textProperty().bind(currentTask.messageProperty());

                    // Update percentage label
                    currentTask.progressProperty().addListener((obs, oldVal, newVal) -> {
                        int percent = (int) (newVal.doubleValue() * 100);
                        progressPercent.setText(percent + "%");
                    });
                }
            });

            delay.play();
        }

        final javafx.animation.PauseTransition finalDelayRef = delay;

        // Handle success
        currentTask.setOnSucceeded(event -> {
            if (finalDelayRef != null) finalDelayRef.stop(); // Cancel delay if not yet shown
            if (loadingShown[0]) {
                hideLoadingOverlay();
            }
            DataLoadResult result = currentTask.getValue();
            if (result != null) {
                // Ensure displayData is called on UI thread to prevent race conditions
                Platform.runLater(() -> displayData(result, table, totalCount));
            }
        });

        // Handle failure
        currentTask.setOnFailed(event -> {
            if (finalDelayRef != null) finalDelayRef.stop();
            if (loadingShown[0]) {
                hideLoadingOverlay();
            }
            Throwable ex = currentTask.getException();
            infoLabel.setText("❌ Помилка: " + ex.getMessage());
            ex.printStackTrace();
        });

        // Handle cancellation
        currentTask.setOnCancelled(event -> {
            if (finalDelayRef != null) finalDelayRef.stop();
            if (loadingShown[0]) {
                hideLoadingOverlay();
            }
            // Don't update info label here - onCancelLoad() handles it
        });

        // Run in background thread
        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void displayData(DataLoadResult result, String table, int totalCount) {
        // Ensure we're on UI thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> displayData(result, table, totalCount));
            return;
        }

        // Clear any existing data and columns
        tableView.getItems().clear();
        tableView.getColumns().clear();

        // Clear any pending UI updates
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        boolean isLargeDataset = result.data.size() > SORTING_THRESHOLD;

        for (int i = 0; i < result.columnNames.size(); i++) {
            final int colIndex = i;
            String columnName = result.columnNames.get(i);
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(columnName);
            col.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().get(colIndex)));

            // Calculate optimal and max width based on actual content
            ColumnWidthInfo widthInfo = calculateColumnWidth(columnName, result.data, colIndex);

            col.setPrefWidth(widthInfo.optimalWidth);
            col.setMinWidth(widthInfo.minWidth);
            col.setMaxWidth(widthInfo.maxWidth); // Now based on actual widest content

            // Left align column headers
            col.setStyle("-fx-alignment: CENTER-LEFT;");

            // Configure sorting behavior
            configureSorting(col, columnName, result.data.size(), totalCount);

            // For large datasets: disable auto-resize to prevent freeze
            if (isLargeDataset) {
                col.setResizable(true); // Allow manual resize
                col.setReorderable(false); // Disable column reordering (causes freeze)

                // Additional protection: set fixed sizes to prevent auto-resize calculations
                col.setMinWidth(widthInfo.minWidth);
                col.setMaxWidth(widthInfo.maxWidth);
                col.setPrefWidth(widthInfo.optimalWidth);

                // Disable sorting to prevent any header interactions
                col.setSortable(false);
            }

            tableView.getColumns().add(col);
        }

        // Set data
        tableView.setItems(result.data);

        // Additional protections for large datasets
        if (isLargeDataset) {
            // Disable table menu button (causes freeze)
            tableView.setTableMenuButtonVisible(false);

            // Set custom resize policy that prevents auto-resize
            tableView.setColumnResizePolicy(createNoAutoResizePolicy());

            // Block header interactions that cause freeze
            Platform.runLater(() -> blockHeaderInteractions(tableView));
        } else {
            tableView.setTableMenuButtonVisible(true);
            // Use normal resize policy for small datasets
            tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        }

        // Update info label with sorting status
        String sortingInfo = getSortingStatusInfo(result.data.size(), totalCount);
        String info = String.format("✓ Таблиця: %s │ Показано: %,d з %,d записів%s",
            table, result.data.size(), totalCount, sortingInfo);
        infoLabel.setText(info);
    }

    /**
     * Block header interactions that cause freeze on large datasets
     */
    private void blockHeaderInteractions(TableView<?> table) {
        // Method 1: Block at skin level
        table.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                blockHeaderInSkin(table);
            }
        });

        // Method 2: If skin is already loaded, apply immediately
        if (table.getSkin() != null) {
            blockHeaderInSkin(table);
        }

        // Method 3: Set column resize policy that prevents auto-resize
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Method 4: Additional protection - schedule repeated blocking
        Platform.runLater(() -> {
            // Give UI time to fully render, then block again
            scheduleRepeatedBlocking(table);
        });
    }

    private void blockHeaderInSkin(TableView<?> table) {
        // Block all header-related elements
        String[] headerSelectors = {
            "TableHeaderRow",
            ".table-header-row",
            ".column-header",
            ".column-resize-line",
            ".column-drag-header"
        };

        for (String selector : headerSelectors) {
            javafx.scene.Node headerElement = table.lookup(selector);
            if (headerElement != null) {
                blockElementInteractions(headerElement);
            }
        }

        // Also find and block all individual column headers
        table.getColumns().forEach(column -> {
            // Try to find the specific header for this column
            javafx.scene.Node columnHeader = table.lookup(".column-header");
            if (columnHeader != null) {
                blockElementInteractions(columnHeader);
            }
        });
    }

    private void blockElementInteractions(javafx.scene.Node element) {
        // Block all mouse events that could trigger auto-resize
        element.addEventFilter(javafx.scene.input.MouseEvent.ANY, event -> {
            if (event.getClickCount() >= 2) {
                // Block any double-click or higher
                event.consume();
            }
        });

        // Also block mouse pressed events on resize areas
        element.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            // Check if cursor indicates resize operation
            if (element.getCursor() == javafx.scene.Cursor.H_RESIZE ||
                element.getCursor() == javafx.scene.Cursor.E_RESIZE ||
                element.getCursor() == javafx.scene.Cursor.W_RESIZE) {
                if (event.getClickCount() >= 2) {
                    event.consume();
                }
            }
        });
    }

    /**
     * Create a custom resize policy that prevents auto-resize operations for large datasets
     */
    private javafx.util.Callback<TableView.ResizeFeatures, Boolean> createNoAutoResizePolicy() {
        return new javafx.util.Callback<TableView.ResizeFeatures, Boolean>() {
            @Override
            public Boolean call(TableView.ResizeFeatures param) {
                // Always return false to prevent any auto-resize operations
                // This completely disables automatic column width calculations
                return false;
            }
        };
    }

    private void scheduleRepeatedBlocking(TableView<?> table) {
        // Schedule blocking multiple times to catch dynamically created elements
        for (int i = 1; i <= 5; i++) {
            final int delay = i * 200; // 200ms, 400ms, 600ms, 800ms, 1000ms
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(delay), e -> {
                    blockHeaderInSkin(table);
                })
            );
            timeline.play();
        }
    }

    /**
     * Configure sorting for a column.
     * TODO: When implementing DB sorting, replace this with SQL ORDER BY queries
     *
     * @param col The table column to configure
     * @param columnName Name of the column
     * @param displayedRecords Number of records currently displayed
     */
    private void configureSorting(TableColumn<ObservableList<String>, String> col,
                                  String columnName, int displayedRecords, int totalRecords) {
        if (USE_DB_SORTING) {
            // TODO: Implement DB-level sorting
            // When clicked, this should:
            // 1. Listen to tableView.sortOrderProperty() changes
            // 2. Get sort column and direction
            // 3. Call loadTableWithSort(tableName, columnName, sortDirection)
            // 4. That method should execute SQL with ORDER BY clause
            // Example: SELECT * FROM table ORDER BY columnName ASC/DESC LIMIT ? OFFSET ?

            col.setSortable(true);
            // Note: Use tableView.getSortOrder() listener to detect sort changes
        } else {
            // Current implementation: disable sorting for large datasets
            boolean canSort = displayedRecords <= SORTING_THRESHOLD;
            col.setSortable(canSort);

            if (!canSort) {
                // Add tooltip explaining why sorting is disabled - directly to column
                addSortingDisabledTooltip(col);
            }
        }
    }

    /**
     * Add tooltip for columns where sorting is disabled (without changing column text)
     */
    private void addSortingDisabledTooltip(TableColumn<ObservableList<String>, String> col) {
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
            "⚠ Обмеження для великої таблиці:\n" +
            "• Сортування вимкнено\n" +
            "• Автоматичне розширення вимкнено\n" +
            "• Перестановка колонок вимкнена\n\n" +
            "Зменште ліміт записів для активації цих функцій."
        );
        tooltip.setShowDelay(javafx.util.Duration.millis(300));

        // Add tooltip to the column header (need to wait for skin to be created)
        Platform.runLater(() -> {
            // Find the header for this specific column
            javafx.scene.Node header = tableView.lookup(".column-header");
            if (header != null) {
                javafx.scene.control.Tooltip.install(header, tooltip);
            }
        });
    }

    /**
     * Get sorting status info for the info label
     */
    private String getSortingStatusInfo(int displayedRecords, int totalRecords) {
        if (USE_DB_SORTING) {
            return " │ Сортування: БД";
        } else if (displayedRecords > SORTING_THRESHOLD) {
            return " │ ⚠ Сортування вимкнено";
        }
        return "";
    }

    // TODO: Method to implement when adding DB sorting
    /*
    private void loadTableWithSort(String tableName, String columnName, String sortOrder) {
        saveCurrentState();

        // Cancel any running task
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }

        int limit = showAllCheckBox.isSelected() ? 0 : limitSpinner.getValue();
        int offset = (currentPage - 1) * recordsPerPage;

        // Create task with sorting
        currentTask = new DataLoadTask(tableName, limit, offset, totalRecords, columnName, sortOrder);

        // ... rest of async loading logic
    }
    */

    /**
     * Calculate header width based on text length (approximation)
     */
    private int getHeaderWidth(String headerText) {
        // Approximate: 8 pixels per character + padding
        int textWidth = headerText.length() * 8 + 20; // 20px padding
        return Math.max(textWidth, 60); // Minimum 60px
    }

    /**
     * Calculate optimal and maximum column width based on header and content
     */
    private ColumnWidthInfo calculateColumnWidth(String columnName, ObservableList<ObservableList<String>> data, int colIndex) {
        // Start with header width as minimum
        int headerWidth = getHeaderWidth(columnName);

        // Sample rows to get content width estimate
        int maxContentWidth = headerWidth;
        // For small tables - check all rows, for large tables - sample 200 rows
        int sampleSize = data.size() <= SORTING_THRESHOLD ? data.size() : 200;

        for (int i = 0; i < sampleSize; i++) {
            ObservableList<String> row = data.get(i);
            if (colIndex < row.size()) {
                String cellValue = row.get(colIndex);
                if (cellValue != null && !cellValue.isEmpty()) {
                    // Approximate content width: 7 pixels per character
                    int contentWidth = cellValue.length() * 7 + 15; // 15px padding
                    maxContentWidth = Math.max(maxContentWidth, contentWidth);
                }
            }
        }

        // Calculate optimal width with smart rules
        int optimalWidth = applySmartLimits(columnName.toLowerCase(), maxContentWidth);

        // Calculate maximum width - allow wider than optimal but with reasonable limits
        int maxWidth = calculateMaxWidth(columnName.toLowerCase(), maxContentWidth);

        return new ColumnWidthInfo(optimalWidth, maxWidth, headerWidth);
    }

    /**
     * Calculate maximum allowed width for a column based on actual content
     */
    private int calculateMaxWidth(String columnName, int actualMaxContentWidth) {
        // ID columns - strict limit
        if (columnName.equals("id") || columnName.endsWith("_id") ||
            columnName.startsWith("id_") || columnName.matches(".*\\bid\\b.*")) {
            return Math.min(actualMaxContentWidth, 120); // Slightly more than optimal
        }

        // Date/time columns
        if (columnName.contains("date") || columnName.contains("time") ||
            columnName.contains("created") || columnName.contains("updated")) {
            return Math.min(actualMaxContentWidth, 220); // Allow for longer timestamps
        }

        // Measurement values
        if (columnName.contains("value") || columnName.contains("measurement") ||
            columnName.contains("level") || columnName.contains("concentration")) {
            return Math.min(actualMaxContentWidth, 200);
        }

        // Description/comment fields - allow much wider
        if (columnName.contains("description") || columnName.contains("comment") ||
            columnName.contains("note") || columnName.contains("remarks")) {
            return Math.min(actualMaxContentWidth, 500); // Wider for text content
        }

        // Coordinate/location columns
        if (columnName.contains("coordinate") || columnName.contains("location") ||
            columnName.contains("address") || columnName.contains("latitude") ||
            columnName.contains("longitude")) {
            return Math.min(actualMaxContentWidth, 350);
        }

        // Name/title columns
        if (columnName.contains("name") || columnName.contains("title") ||
            columnName.contains("type") || columnName.contains("status")) {
            return Math.min(actualMaxContentWidth, 250);
        }

        // General limit - based on actual content but reasonable
        return Math.min(actualMaxContentWidth, 400);
    }

    /**
     * Helper class to return both optimal and max width
     */
    private static class ColumnWidthInfo {
        final int optimalWidth;
        final int maxWidth;
        final int minWidth;

        ColumnWidthInfo(int optimalWidth, int maxWidth, int minWidth) {
            this.optimalWidth = optimalWidth;
            this.maxWidth = maxWidth;
            this.minWidth = minWidth;
        }
    }

    /**
     * Apply smart limits based on column type
     */
    private int applySmartLimits(String columnName, int calculatedWidth) {
        // ID columns - never more than 100px
        if (columnName.equals("id") || columnName.endsWith("_id") ||
            columnName.startsWith("id_") || columnName.matches(".*\\bid\\b.*")) {
            return Math.min(calculatedWidth, 100);
        }

        // Date/time columns - reasonable limit
        if (columnName.contains("date") || columnName.contains("time") ||
            columnName.contains("created") || columnName.contains("updated")) {
            return Math.min(calculatedWidth, 180);
        }

        // Measurement values - medium limit
        if (columnName.contains("value") || columnName.contains("measurement") ||
            columnName.contains("level") || columnName.contains("concentration")) {
            return Math.min(calculatedWidth, 150);
        }

        // Description/comment fields - larger but not huge
        if (columnName.contains("description") || columnName.contains("comment") ||
            columnName.contains("note") || columnName.contains("remarks")) {
            return Math.min(calculatedWidth, 350);
        }

        // Coordinate/location columns - wide but limited
        if (columnName.contains("coordinate") || columnName.contains("location") ||
            columnName.contains("address") || columnName.contains("latitude") ||
            columnName.contains("longitude")) {
            return Math.min(calculatedWidth, 280);
        }

        // Name/title columns - medium-wide
        if (columnName.contains("name") || columnName.contains("title") ||
            columnName.contains("type") || columnName.contains("status")) {
            return Math.min(calculatedWidth, 200);
        }

        // General limit - prevent extremely wide columns
        return Math.min(calculatedWidth, 300);
    }

    private void showLoadingOverlay() {
        Platform.runLater(() -> {
            loadingOverlay.setVisible(true);
            loadingOverlay.setManaged(true);
            progressBar.setProgress(0);
            progressPercent.setText("0%");
        });
    }

    private void hideLoadingOverlay() {
        Platform.runLater(() -> {
            loadingOverlay.setVisible(false);
            loadingOverlay.setManaged(false);
            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
        });
    }

    private void updatePaginationUI() {
        boolean showPagination = !showAllCheckBox.isSelected() && totalRecords > recordsPerPage;

        Platform.runLater(() -> {
            paginationBox.setVisible(showPagination);
            paginationBox.setManaged(showPagination);

            if (showPagination) {
                int totalPages = (int) Math.ceil((double) totalRecords / recordsPerPage);

                pageLabel.setText(String.format("Сторінка %d з %d", currentPage, totalPages));
                totalRecordsLabel.setText(String.format("Всього: %,d записів", totalRecords));

                prevPageBtn.setDisable(currentPage <= 1);
                nextPageBtn.setDisable(currentPage >= totalPages);
            }
        });
    }
}