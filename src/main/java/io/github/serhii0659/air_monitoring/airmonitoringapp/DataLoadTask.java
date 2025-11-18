package io.github.serhii0659.air_monitoring.airmonitoringapp;

import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataLoadTask extends Task<DataLoadResult> {
    private final String tableName;
    private final int limit;
    private final int offset;
    private final int totalCount;

    public DataLoadTask(String tableName, int limit, int totalCount) {
        this(tableName, limit, 0, totalCount);
    }

    public DataLoadTask(String tableName, int limit, int offset, int totalCount) {
        this.tableName = tableName;
        this.limit = limit;
        this.offset = offset;
        this.totalCount = totalCount;
    }

    @Override
    protected DataLoadResult call() throws Exception {
        updateMessage("Завантаження даних з таблиці " + tableName + "...");
        updateProgress(0, 100);

        if (isCancelled()) return null;

        DataLoadResult result = new DataLoadResult();

        try (ResultSet rs = DbManager.fetchTable(tableName, limit, offset)) {
            ResultSetMetaData md = rs.getMetaData();
            int colCount = md.getColumnCount();

            result.columnNames = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                result.columnNames.add(md.getColumnLabel(i));
            }

            updateProgress(10, 100);
            updateMessage("Читання записів...");

            if (isCancelled()) return null;

            result.data = FXCollections.observableArrayList();
            int rowCount = 0;
            int expectedRows = limit > 0 ? Math.min(limit, totalCount) : totalCount;

            while (rs.next()) {
                if (isCancelled()) {
                    updateMessage("Скасовано користувачем");
                    return null;
                }

                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= colCount; i++) {
                    Object val = rs.getObject(i);
                    row.add(val == null ? "" : val.toString());
                }
                result.data.add(row);
                rowCount++;

                if (rowCount % 100 == 0 || rowCount == expectedRows) {
                    double progress = Math.min(90, 10 + (80.0 * rowCount / expectedRows));
                    updateProgress(progress, 100);
                    updateMessage(String.format("Завантажено %,d з %,d записів", rowCount, expectedRows));
                }
            }

            updateProgress(100, 100);
            updateMessage("Завантаження завершено");

        } catch (SQLException e) {
            updateMessage("Помилка: " + e.getMessage());
            throw e;
        }

        return result;
    }
}

class DataLoadResult {
    ObservableList<ObservableList<String>> data;
    List<String> columnNames;
}