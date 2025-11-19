package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class COViewController {
    @FXML private CustomTitleBar titleBar;
    @FXML private TableView<CategoryData> dataTable;
    @FXML private TableColumn<CategoryData, String> categoryColumn;
    @FXML private TableColumn<CategoryData, String> rangeColumn;
    @FXML private TableColumn<CategoryData, Integer> countColumn;
    @FXML private PieChart pieChart;
    @FXML private Label infoLabel;

    private ObservableList<CategoryData> dataList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        System.out.println("COViewController.initialize() викликано");

        // Setup table columns
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        rangeColumn.setCellValueFactory(new PropertyValueFactory<>("range"));
        countColumn.setCellValueFactory(new PropertyValueFactory<>("count"));

        dataTable.setItems(dataList);

        // Setup chart
        pieChart.setTitle("Розподіл вимірювань CO за категоріями");

        // Initialize title bar - робимо безпечно
        Platform.runLater(() -> {
            try {
                if (titleBar != null && titleBar.getScene() != null && titleBar.getScene().getWindow() != null) {
                    Stage stage = (Stage) titleBar.getScene().getWindow();
                    titleBar.init("Візуалізація: Чадний газ (CO)", stage, true, true); // Дозволяємо максимізацію
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

    @FXML
    private void onLoadData() {
        infoLabel.setText("⏳ Завантаження даних...");

        new Thread(() -> {
            try {
                loadDataFromDatabase();
                Platform.runLater(() -> {
                    updateChart();
                    int total = dataList.stream().mapToInt(CategoryData::getCount).sum();
                    infoLabel.setText("✓ Дані завантажено: " + total + " вимірювань");
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    infoLabel.setText("❌ Помилка: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void loadDataFromDatabase() throws Exception {
        // Спочатку отримуємо ID для CO
        String coId = null;
        Connection conn = DbManager.getConnection();
        if (conn == null) {
            throw new Exception("Немає з'єднання з БД");
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ID_Measured_Unit FROM Measured_Unit WHERE Title LIKE '%CO%' AND Title NOT LIKE '%CO2%' LIMIT 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    coId = rs.getString("ID_Measured_Unit");
                }
            }
        }

        if (coId == null) {
            throw new Exception("Не знайдено параметр CO у базі даних");
        }

        // Тепер отримуємо категорії та підраховуємо вимірювання
        String sql = """
            SELECT 
                c.Designation AS category,
                ov.Bottom_Border,
                ov.Upper_Border,
                COUNT(m.ID_Measurment) AS measurement_count
            FROM Category c
            JOIN Optimal_Value ov ON c.ID_Category = ov.ID_Category
            LEFT JOIN Measurment m ON m.ID_Measured_Unit = ov.ID_Measured_Unit
                AND m.Value >= ov.Bottom_Border
                AND (ov.Upper_Border IS NULL OR m.Value < ov.Upper_Border)
            WHERE ov.ID_Measured_Unit = ?
            GROUP BY c.ID_Category, c.Designation, ov.Bottom_Border, ov.Upper_Border
            ORDER BY ov.Bottom_Border
            """;

        ObservableList<CategoryData> newData = FXCollections.observableArrayList();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, coId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String category = rs.getString("category");
                    double bottomBorder = rs.getDouble("Bottom_Border");
                    Double upperBorder = rs.getDouble("Upper_Border");
                    if (rs.wasNull()) upperBorder = null;
                    int count = rs.getInt("measurement_count");

                    // Форматуємо діапазон
                    String range;
                    if (upperBorder == null) {
                        range = String.format("%.0f+", bottomBorder);
                    } else {
                        range = String.format("%.0f-%.0f", bottomBorder, upperBorder);
                    }

                    newData.add(new CategoryData(category, range, count));
                }
            }
        }

        Platform.runLater(() -> {
            dataList.clear();
            dataList.addAll(newData);
        });
    }

    private void updateChart() {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        for (CategoryData data : dataList) {
            pieData.add(new PieChart.Data(data.getCategory() + " (" + data.getCount() + ")", data.getCount()));
        }

        pieChart.setData(pieData);
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.close();
    }

    // Data class
    public static class CategoryData {
        private final String category;
        private final String range;
        private final int count;

        public CategoryData(String category, String range, int count) {
            this.category = category;
            this.range = range;
            this.count = count;
        }

        public String getCategory() { return category; }
        public String getRange() { return range + " μg/m³"; }
        public int getCount() { return count; }
    }
}