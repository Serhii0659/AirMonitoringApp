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

public class PM25HarmfulViewController {
    @FXML private CustomTitleBar titleBar;
    @FXML private ComboBox<StationItem> stationCombo;
    @FXML private TableView<HarmfulLevelData> dataTable;
    @FXML private TableColumn<HarmfulLevelData, String> stationColumn;
    @FXML private TableColumn<HarmfulLevelData, Integer> countColumn;
    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Label infoLabel;

    private ObservableList<HarmfulLevelData> dataList = FXCollections.observableArrayList();
    // Harmful level для PM2.5: > 35.4 μg/m³ (EPA standard)
    private static final double PM25_HARMFUL_THRESHOLD = 35.4;

    @FXML
    private void initialize() {
        System.out.println("PM25HarmfulViewController.initialize() викликано");

        // Setup table columns
        stationColumn.setCellValueFactory(new PropertyValueFactory<>("stationName"));
        countColumn.setCellValueFactory(new PropertyValueFactory<>("harmfulCount"));

        dataTable.setItems(dataList);

        // Setup chart
        xAxis.setLabel("Станція");
        yAxis.setLabel("Кількість перевищень");
        barChart.setTitle("Кількість днів з шкідливим рівнем PM2.5");

        // Load stations
        loadStations();

        // Initialize title bar - робимо безпечно
        Platform.runLater(() -> {
            try {
                if (titleBar != null && titleBar.getScene() != null && titleBar.getScene().getWindow() != null) {
                    Stage stage = (Stage) titleBar.getScene().getWindow();
                    titleBar.init("Візуалізація: PM2.5 шкідливий рівень", stage, true, true); // Дозволяємо максимізацію
                    System.out.println("Title bar ініціалізовано успішно");
                } else {
                    System.err.println("Title bar або scene ще не готові");
                }
            } catch (Exception e) {
                System.err.println("Помилка ініціалізації title bar: " + e.getMessage());
            }
        });
    }

    @SuppressWarnings("unused")
    public void setStage(Stage stage) {
        // Keep for compatibility, but initialization is now done in initialize()
    }

    private void loadStations() {
        new Thread(() -> {
            try {
                ObservableList<StationItem> stations = FXCollections.observableArrayList();
                stations.add(new StationItem("ALL", "Усі станції", ""));

                // НЕ закриваємо Connection - він керується DbManager
                try (ResultSet rs = DbManager.getAllStations()) {
                    while (rs.next()) {
                        String id = rs.getString("ID_Station");
                        String name = rs.getString("Name");
                        String city = rs.getString("City");
                        stations.add(new StationItem(id, name, city));
                    }
                }

                Platform.runLater(() -> {
                    stationCombo.setItems(stations);
                    if (!stations.isEmpty()) {
                        stationCombo.getSelectionModel().selectFirst();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    infoLabel.setText("❌ Помилка завантаження станцій: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onLoadData() {
        StationItem selectedStation = stationCombo.getValue();
        if (selectedStation == null) {
            infoLabel.setText("❌ Оберіть станцію");
            return;
        }

        infoLabel.setText("⏳ Завантаження даних...");

        new Thread(() -> {
            try {
                loadDataFromDatabase(selectedStation);
                Platform.runLater(() -> {
                    updateChart();
                    infoLabel.setText("✓ Дані завантажено: " + dataList.size() + " станцій");
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    infoLabel.setText("❌ Помилка: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void loadDataFromDatabase(StationItem station) throws Exception {
        String sql;

        if ("ALL".equals(station.getId())) {
            // All stations
            sql = """
                SELECT 
                    st.Name || ' (' || st.City || ')' AS station_name,
                    COUNT(DISTINCT DATE(m.Time)) AS harmful_days
                FROM Measurment m
                JOIN Station st ON m.ID_Station = st.ID_Station
                JOIN Measured_Unit mu ON m.ID_Measured_Unit = mu.ID_Measured_Unit
                WHERE mu.Title = 'PM2.5'
                  AND m.Value > ?
                GROUP BY st.Name, st.City
                ORDER BY harmful_days DESC
                """;
        } else {
            // Specific station
            sql = """
                SELECT 
                    st.Name || ' (' || st.City || ')' AS station_name,
                    COUNT(DISTINCT DATE(m.Time)) AS harmful_days
                FROM Measurment m
                JOIN Station st ON m.ID_Station = st.ID_Station
                JOIN Measured_Unit mu ON m.ID_Measured_Unit = mu.ID_Measured_Unit
                WHERE mu.Title = 'PM2.5'
                  AND m.Value > ?
                  AND m.ID_Station = ?
                GROUP BY st.Name, st.City
                """;
        }

        ObservableList<HarmfulLevelData> newData = FXCollections.observableArrayList();

        Connection conn = DbManager.getConnection();
        if (conn == null) {
            throw new Exception("Немає з'єднання з БД");
        }

        // НЕ використовуємо try-with-resources для Connection - він керується DbManager
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, PM25_HARMFUL_THRESHOLD);
            if (!"ALL".equals(station.getId())) {
                stmt.setString(2, station.getId());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String stationName = rs.getString("station_name");
                    int harmfulDays = rs.getInt("harmful_days");
                    newData.add(new HarmfulLevelData(stationName, harmfulDays));
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

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Кількість днів з PM2.5 > " + PM25_HARMFUL_THRESHOLD + " μg/m³");

        for (HarmfulLevelData data : dataList) {
            series.getData().add(new XYChart.Data<>(data.getStationName(), data.getHarmfulCount()));
        }

        barChart.getData().add(series);
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.close();
    }

    // Data classes
    public static class StationItem {
        private final String id;
        private final String name;
        private final String city;

        public StationItem(String id, String name, String city) {
            this.id = id;
            this.name = name;
            this.city = city;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getCity() { return city; }

        @Override
        public String toString() {
            return "ALL".equals(id) ? name : name + " (" + city + ")";
        }
    }

    public static class HarmfulLevelData {
        private final String stationName;
        private final int harmfulCount;

        public HarmfulLevelData(String stationName, int harmfulCount) {
            this.stationName = stationName;
            this.harmfulCount = harmfulCount;
        }

        public String getStationName() { return stationName; }
        public int getHarmfulCount() { return harmfulCount; }
    }
}