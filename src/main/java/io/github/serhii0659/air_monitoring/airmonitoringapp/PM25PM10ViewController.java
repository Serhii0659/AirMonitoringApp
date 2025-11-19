package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class PM25PM10ViewController {
    @FXML private CustomTitleBar titleBar;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TableView<RegionPMData> dataTable;
    @FXML private TableColumn<RegionPMData, String> regionColumn;
    @FXML private TableColumn<RegionPMData, Double> pm25Column;
    @FXML private TableColumn<RegionPMData, Double> pm10Column;
    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Label infoLabel;

    private ObservableList<RegionPMData> dataList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        System.out.println("PM25PM10ViewController.initialize() викликано");

        // Setup table columns
        regionColumn.setCellValueFactory(new PropertyValueFactory<>("region"));
        pm25Column.setCellValueFactory(new PropertyValueFactory<>("pm25Max"));
        pm10Column.setCellValueFactory(new PropertyValueFactory<>("pm10Max"));

        dataTable.setItems(dataList);

        // Set default dates (last 30 days)
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusDays(30));

        // Setup chart
        xAxis.setLabel("Область");
        yAxis.setLabel("Максимальне значення (μg/m³)");
        barChart.setTitle("Максимальні значення PM2.5 та PM10 по областях");

        // Initialize title bar - робимо безпечно
        Platform.runLater(() -> {
            try {
                if (titleBar != null && titleBar.getScene() != null && titleBar.getScene().getWindow() != null) {
                    Stage stage = (Stage) titleBar.getScene().getWindow();
                    titleBar.init("Візуалізація: PM2.5 та PM10", stage, true, true); // Дозволяємо максимізацію
                    System.out.println("Title bar ініціалізовано успішно");
                } else {
                    System.err.println("Title bar або scene ще не готові");
                }
            } catch (Exception e) {
                System.err.println("Помилка ініціалізації title bar: " + e.getMessage());
                e.printStackTrace();
            }
        });

        System.out.println("PM25PM10ViewController.initialize() завершено");
    }

    @SuppressWarnings("unused")
    public void setStage(Stage stage) {
        // Keep for compatibility, but initialization is now done in initialize()
    }

    @FXML
    private void onLoadData() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            infoLabel.setText("❌ Оберіть обидві дати");
            return;
        }

        if (startDate.isAfter(endDate)) {
            infoLabel.setText("❌ Початкова дата не може бути пізніше кінцевої");
            return;
        }

        infoLabel.setText("⏳ Завантаження даних...");

        // Load data in background
        new Thread(() -> {
            try {
                loadDataFromDatabase(startDate, endDate);
                Platform.runLater(() -> {
                    updateChart();
                    infoLabel.setText("✓ Дані завантажено: " + dataList.size() + " областей");
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    infoLabel.setText("❌ Помилка: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void loadDataFromDatabase(LocalDate startDate, LocalDate endDate) throws Exception {
        String sql = """
            SELECT 
                st.City AS region,
                MAX(CASE WHEN mu.Title = 'PM2.5' THEN m.Value END) AS pm25_max,
                MAX(CASE WHEN mu.Title = 'PM10' THEN m.Value END) AS pm10_max
            FROM Measurment m
            JOIN Station st ON m.ID_Station = st.ID_Station
            JOIN Measured_Unit mu ON m.ID_Measured_Unit = mu.ID_Measured_Unit
            WHERE m.Time BETWEEN ? AND ?
              AND mu.Title IN ('PM2.5', 'PM10')
            GROUP BY st.City
            ORDER BY st.City
            """;

        ObservableList<RegionPMData> newData = FXCollections.observableArrayList();

        Connection conn = DbManager.getConnection();
        if (conn == null) {
            throw new Exception("Немає з'єднання з БД");
        }

        // НЕ використовуємо try-with-resources для Connection - він керується DbManager
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(2, java.sql.Timestamp.valueOf(endDate.atTime(23, 59, 59)));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String region = rs.getString("region");
                    Double pm25 = rs.getDouble("pm25_max");
                    if (rs.wasNull()) pm25 = null;
                    Double pm10 = rs.getDouble("pm10_max");
                    if (rs.wasNull()) pm10 = null;

                    if (pm25 != null || pm10 != null) {
                        newData.add(new RegionPMData(region, pm25, pm10));
                    }
                }
            }
        }

        Platform.runLater(() -> {
            dataList.clear();
            dataList.addAll(newData);
        });
    }

    private void updateChart() {
        barChart.getData().clear();

        XYChart.Series<String, Number> pm25Series = new XYChart.Series<>();
        pm25Series.setName("PM2.5");

        XYChart.Series<String, Number> pm10Series = new XYChart.Series<>();
        pm10Series.setName("PM10");

        for (RegionPMData data : dataList) {
            if (data.getPm25Max() != null) {
                pm25Series.getData().add(new XYChart.Data<>(data.getRegion(), data.getPm25Max()));
            }
            if (data.getPm10Max() != null) {
                pm10Series.getData().add(new XYChart.Data<>(data.getRegion(), data.getPm10Max()));
            }
        }

        barChart.getData().addAll(pm25Series, pm10Series);
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.close();
    }

    // Data class for table
    public static class RegionPMData {
        private final String region;
        private final Double pm25Max;
        private final Double pm10Max;

        public RegionPMData(String region, Double pm25Max, Double pm10Max) {
            this.region = region;
            this.pm25Max = pm25Max;
            this.pm10Max = pm10Max;
        }

        public String getRegion() { return region; }
        public Double getPm25Max() { return pm25Max != null ? pm25Max : 0.0; }
        public Double getPm10Max() { return pm10Max != null ? pm10Max : 0.0; }
    }
}