package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportsController {

    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private ComboBox<StationInfo> stationComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> formatComboBox;
    @FXML private Label statusLabel;
    @FXML private Button generateButton;
    @FXML private CustomTitleBar titleBar;

    // Containers for dynamic visibility
    @FXML private javafx.scene.layout.VBox stationBox;
    @FXML private javafx.scene.layout.HBox dateRangeBox;

    private Stage stage;
    private final List<StationInfo> stations = new ArrayList<>();

    private static class StationInfo {
        String id;
        String name;
        String city;

        @Override
        public String toString() {
            return city != null && !city.isEmpty() ? name + " (" + city + ")" : name;
        }
    }

    @FXML
    private void initialize() {
        // Initialize title bar - will be set properly when window is shown
        Platform.runLater(() -> {
            stage = (Stage) reportTypeComboBox.getScene().getWindow();
            if (titleBar != null && stage != null) {
                titleBar.init("Air Monitoring - Звіти", stage, false, false);

                // Remove rounded background for reports window (not resizable)
                javafx.scene.Parent root = stage.getScene().getRoot();
                if (root != null) {
                    root.setStyle(root.getStyle().replaceAll("-fx-background-radius:\\s*\\d+;?", "") + "; -fx-background-radius: 0;");
                }
            }
        });

        // Setup report types
        reportTypeComboBox.setItems(FXCollections.observableArrayList(
                "Список підключених станцій",
                "Статистика вимірювань станції"
        ));
        reportTypeComboBox.getSelectionModel().selectFirst();

        // Setup format types
        formatComboBox.setItems(FXCollections.observableArrayList("Excel (XLSX)", "PDF"));
        formatComboBox.getSelectionModel().selectFirst();

        // Load stations
        loadStations();

        // Setup listeners for dynamic form
        reportTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateFormVisibility();
        });

        // Set default dates (last 30 days)
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusDays(30));

        // Add date validation to DatePickers to limit year to 9999
        setupDatePickerValidation(startDatePicker);
        setupDatePickerValidation(endDatePicker);

        // Initial state - hide station/date fields for first report type
        updateFormVisibility();
    }

    /**
     * Setup validation for DatePicker to limit year input to current date
     */
    private void setupDatePickerValidation(DatePicker datePicker) {
        javafx.scene.control.TextFormatter<String> textFormatter = new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();

            // Allow empty or valid date formats
            if (newText.isEmpty()) {
                return change;
            }

            // Check if text contains year that exceeds 9999 (5+ consecutive digits)
            if (newText.matches(".*\\d{5,}.*")) {
                return null; // Reject change
            }

            return change;
        });

        datePicker.getEditor().setTextFormatter(textFormatter);

        // Additional converter to handle year validation
        datePicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(formatter) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null;
                }
                try {
                    LocalDate date = LocalDate.parse(string.trim(), formatter);
                    LocalDate today = LocalDate.now();

                    // If date is in the future, use today's date instead
                    if (date.isAfter(today)) {
                        return today;
                    }

                    // If year is less than 1, reject
                    if (date.getYear() < 1) {
                        return null;
                    }

                    return date;
                } catch (Exception e) {
                    return null;
                }
            }
        });

        // Apply custom day cell factory to ensure proper styling for other months and future dates
        datePicker.setDayCellFactory(picker -> {
            // Create a controller for this calendar instance
            final CalendarMonthDetector detector = new CalendarMonthDetector();

            return new javafx.scene.control.DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);

                    if (empty || date == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        LocalDate today = LocalDate.now();

                        // Disable and gray out future dates
                        if (date.isAfter(today)) {
                            setDisable(true);
                            setStyle("-fx-text-fill: rgba(168, 178, 193, 0.3); -fx-opacity: 0.4;");
                            getStyleClass().removeAll("other-month");
                            getStyleClass().add("future-date");
                        } else {
                            // Register this date with the detector
                            detector.registerDate(date);

                            // Get the detected display month
                            java.time.YearMonth displayedMonth = detector.getDisplayedMonth();
                            java.time.YearMonth cellMonth = java.time.YearMonth.from(date);

                            if (displayedMonth != null && !cellMonth.equals(displayedMonth)) {
                                // This date is from a different month - make it gray
                                setStyle("-fx-text-fill: rgba(168, 178, 193, 0.3); -fx-opacity: 0.4;");
                                getStyleClass().removeAll("other-month", "future-date");
                                getStyleClass().add("other-month");
                            } else {
                                // This date is from the displayed month - normal style
                                setStyle("-fx-text-fill: #e0e6ed; -fx-opacity: 1.0;");
                                getStyleClass().removeAll("other-month", "future-date");
                            }
                        }
                    }
                }
            };
        });
    }

    /**
     * Helper class to detect which month is currently displayed in the calendar
     */
    private static class CalendarMonthDetector {
        private final java.util.Map<java.time.YearMonth, Integer> monthCounts = new java.util.HashMap<>();
        private int cellCount = 0;
        private java.time.YearMonth detectedMonth = null;

        public void registerDate(LocalDate date) {
            java.time.YearMonth month = java.time.YearMonth.from(date);
            monthCounts.put(month, monthCounts.getOrDefault(month, 0) + 1);
            cellCount++;

            // After processing enough cells, determine the displayed month
            if (cellCount >= 35 && detectedMonth == null) {
                // Find the month that appears most frequently
                java.time.YearMonth mostFrequent = null;
                int maxCount = 0;

                for (java.util.Map.Entry<java.time.YearMonth, Integer> entry : monthCounts.entrySet()) {
                    if (entry.getValue() > maxCount) {
                        maxCount = entry.getValue();
                        mostFrequent = entry.getKey();
                    }
                }

                detectedMonth = mostFrequent;
            }
        }

        public java.time.YearMonth getDisplayedMonth() {
            return detectedMonth;
        }
    }

    private void updateFormVisibility() {
        boolean isStationReport = "Статистика вимірювань станції".equals(reportTypeComboBox.getValue());

        // Hide/show entire containers
        if (stationBox != null) {
            stationBox.setVisible(isStationReport);
            stationBox.setManaged(isStationReport);
        }

        if (dateRangeBox != null) {
            dateRangeBox.setVisible(isStationReport);
            dateRangeBox.setManaged(isStationReport);
        }

        // Adjust window height based on report type
        Platform.runLater(() -> {
            Stage currentStage = (Stage) reportTypeComboBox.getScene().getWindow();
            if (currentStage != null) {
                if (isStationReport) {
                    currentStage.setHeight(600); // Більше місця для полів станції та дат + хороший відступ знизу
                } else {
                    currentStage.setHeight(450); // Менше для простого звіту + хороший відступ знизу
                }
                currentStage.centerOnScreen();
            }
        });
    }

    private void loadStations() {
        try (ResultSet rs = DbManager.getAllStations()) {
            stations.clear();
            while (rs.next()) {
                StationInfo info = new StationInfo();
                info.id = rs.getString("ID_Station");
                info.name = rs.getString("Name");
                info.city = rs.getString("City");
                stations.add(info);
            }

            stationComboBox.setItems(FXCollections.observableArrayList(stations));
            if (!stations.isEmpty()) {
                stationComboBox.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Помилка завантаження станцій: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14px;");
        }
    }

    @FXML
    private void onGenerate() {
        String reportType = reportTypeComboBox.getValue();
        String format = formatComboBox.getValue();

        // Validation step 1: Check report type and format
        if (reportType == null || format == null) {
            statusLabel.setText("❌ Оберіть тип звіту та формат");
            statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14px;");
            return;
        }

        // Validation step 2: Check report-specific requirements
        if ("Статистика вимірювань станції".equals(reportType)) {
            StationInfo selectedStation = stationComboBox.getValue();
            if (selectedStation == null) {
                statusLabel.setText("❌ Оберіть станцію");
                statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14px;");
                return;
            }

            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            if (startDate == null || endDate == null) {
                statusLabel.setText("❌ Оберіть період (початкова та кінцева дати)");
                statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14px;");
                return;
            }

            // Strict date validation - check if editor text is valid
            if (!isDatePickerValid(startDatePicker) || !isDatePickerValid(endDatePicker)) {
                statusLabel.setText("❌ Некоректний формат дати. Використовуйте календар або формат ДД.ММ.РРРР");
                statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14px;");
                return;
            }

            if (startDate.isAfter(endDate)) {
                statusLabel.setText("❌ Початкова дата не може бути пізніше кінцевої");
                statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14px;");
                return;
            }

            // Additional validation: check reasonable date range
            LocalDate now = LocalDate.now();
            LocalDate minDate = LocalDate.of(1900, 1, 1);
            LocalDate maxDate = now; // No future dates allowed

            if (startDate.isBefore(minDate) || startDate.isAfter(maxDate)) {
                statusLabel.setText("❌ Початкова дата повинна бути між 1900 та сьогоднішньою датою");
                statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14px;");
                return;
            }

            if (endDate.isBefore(minDate) || endDate.isAfter(maxDate)) {
                statusLabel.setText("❌ Кінцева дата повинна бути між 1900 та сьогоднішньою датою");
                statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14px;");
                return;
            }
        }

        // All validations passed - now choose save location
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Зберегти звіт");
        fileChooser.setInitialFileName("report_" + System.currentTimeMillis());

        // Add extension filter based on selected format
        if ("PDF".equals(format)) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        } else {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        }

        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return; // User cancelled
        }

        // Start generation
        generateButton.setDisable(true);
        statusLabel.setText("⏳ Генерація звіту...");
        statusLabel.getStyleClass().clear();
        statusLabel.setStyle("-fx-text-fill: #00d9ff; -fx-font-size: 14px;");

        // Generate in background thread
        new Thread(() -> {
            try {
                ReportGenerator.ReportData reportData;

                if ("Список підключених станцій".equals(reportType)) {
                    reportData = generateStationsReport();
                } else {
                    reportData = generateMeasurementStatisticsReport();
                }

                // Generate file based on format
                if ("PDF".equals(format)) {
                    ReportGenerator.generatePDF(reportData, file.getAbsolutePath());
                } else {
                    ReportGenerator.generateExcel(reportData, file.getAbsolutePath());
                }

                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("✅ Звіт успішно згенеровано: " + file.getName());
                    statusLabel.getStyleClass().clear();
                    statusLabel.setStyle("-fx-text-fill: #00ff00; -fx-font-size: 14px;");
                    generateButton.setDisable(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("❌ Помилка: " + e.getMessage());
                    statusLabel.getStyleClass().clear();
                    statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14px;");
                    generateButton.setDisable(false);
                });
            }
        }).start();
    }

    private ReportGenerator.ReportData generateStationsReport() throws Exception {
        ResultSet rs = DbManager.getStationsReport();
        ReportGenerator.ReportData data = ReportGenerator.resultSetToReportData(rs,
                "Звіт: Список підключених станцій");
        rs.close();
        return data;
    }

    private ReportGenerator.ReportData generateMeasurementStatisticsReport() throws Exception {
        // All validations already done in onGenerate(), just get the data
        StationInfo selectedStation = stationComboBox.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String startDateStr = startDate.format(formatter) + " 00:00:00";
        String endDateStr = endDate.format(formatter) + " 23:59:59";

        ResultSet rs = DbManager.getMeasurementStatisticsReport(
                selectedStation.id, startDateStr, endDateStr);

        String title = String.format("Звіт: Статистика вимірювань\nСтанція: %s\nПеріод: %s - %s\nЗгенеровано: %s",
                selectedStation.name,
                startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

        ReportGenerator.ReportData data = ReportGenerator.resultSetToReportData(rs, title);
        rs.close();

        // Calculate totals for numeric columns
        if (!data.rows.isEmpty() && data.headers.size() >= 6) {
            double totalAvg = 0;
            double totalMin = Double.MAX_VALUE;
            double totalMax = Double.MIN_VALUE;
            int totalCount = 0;
            int rowCount = 0;

            for (List<String> row : data.rows) {
                try {
                    // Column indices: 0=Назва, 1=Одиниця, 2=Середнє, 3=Мінімальне, 4=Максимальне, 5=Кількість
                    double avg = Double.parseDouble(row.get(2));
                    double min = Double.parseDouble(row.get(3));
                    double max = Double.parseDouble(row.get(4));
                    int count = Integer.parseInt(row.get(5));

                    totalAvg += avg;
                    totalMin = Math.min(totalMin, min);
                    totalMax = Math.max(totalMax, max);
                    totalCount += count;
                    rowCount++;
                } catch (NumberFormatException ignored) {
                }
            }

            if (rowCount > 0) {
                data.hasTotalRow = true;
                data.totalRowData.add("Усього");
                data.totalRowData.add("-");
                data.totalRowData.add(String.format("%.2f", totalAvg / rowCount));
                data.totalRowData.add(String.format("%.2f", totalMin));
                data.totalRowData.add(String.format("%.2f", totalMax));
                data.totalRowData.add(String.valueOf(totalCount));
            }
        }

        return data;
    }

    /**
     * Validate if DatePicker contains a properly formatted date
     * Checks the editor text to ensure user didn't type invalid characters
     */
    private boolean isDatePickerValid(DatePicker datePicker) {
        if (datePicker == null || datePicker.getValue() == null) {
            return false;
        }

        // Get the editor text
        String editorText = datePicker.getEditor().getText();
        if (editorText == null || editorText.trim().isEmpty()) {
            return false;
        }

        // Check if text contains invalid characters
        // Valid: digits, dots, slashes, spaces
        if (!editorText.matches("[0-9./\\s-]+")) {
            return false;
        }

        // Try to parse the text with expected format
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate parsedDate = LocalDate.parse(editorText.trim(), formatter);

            // Check if parsed date matches the DatePicker value
            return parsedDate.equals(datePicker.getValue());
        } catch (Exception e) {
            // If parsing fails, check alternative format
            try {
                DateTimeFormatter altFormatter = DateTimeFormatter.ofPattern("d.M.yyyy");
                LocalDate parsedDate = LocalDate.parse(editorText.trim(), altFormatter);
                return parsedDate.equals(datePicker.getValue());
            } catch (Exception ex) {
                return false;
            }
        }
    }

}