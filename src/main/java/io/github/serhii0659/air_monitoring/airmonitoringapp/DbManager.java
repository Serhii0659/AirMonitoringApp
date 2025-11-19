package io.github.serhii0659.air_monitoring.airmonitoringapp;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class DbManager {
    private static Connection connection; // active connection
    private static String lastError;

    private DbManager() {}

    public static boolean connect(String user, String password) {
        lastError = null;
        try {
            Class.forName("org.postgresql.Driver");
            String url = ConfigManager.getDbUrl();
            connection = DriverManager.getConnection(url, user, password);
            return true;
        } catch (Exception e) {
            lastError = e.getMessage();
            connection = null;
            return false;
        }
    }

    public static void disconnect() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
            connection = null;
        }
    }

    public static boolean isConnected() { return connection != null; }

    public static Connection getConnection() { return connection; }

    public static String getLastError() { return lastError; }

    public static List<String> getTables() {
        List<String> tables = new ArrayList<>();
        if (!isConnected()) return tables;
        String sql = "SELECT table_name FROM information_schema.tables " +
                     "WHERE table_schema='public' AND table_type='BASE TABLE' " +
                     "ORDER BY table_name";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) tables.add(rs.getString(1));
        } catch (SQLException e) {
            lastError = e.getMessage();
        }
        return tables;
    }

    public static int getTableRecordCount(String tableName) throws SQLException {
        if (!isConnected()) throw new SQLException("Not connected");
        if (!tableName.matches("[A-Za-z0-9_]+")) throw new SQLException("Неприпустима назва таблиці");
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /**
     * Fetch table data with limit and offset (used by DataLoadTask)
     */
    public static ResultSet fetchTable(String tableName, int limit, int offset) throws SQLException {
        if (!isConnected()) throw new SQLException("Not connected");
        if (!tableName.matches("[A-Za-z0-9_]+")) throw new SQLException("Неприпустима назва таблиці");

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
            if (offset > 0) {
                sql.append(" OFFSET ").append(offset);
            }
        }

        Statement st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        return st.executeQuery(sql.toString());
    }

    /**
     * Get report data: list of stations with parameters (uses VIEW)
     */
    public static ResultSet getStationsReport() throws SQLException {
        if (!isConnected()) throw new SQLException("Not connected");
        String sql = "SELECT * FROM Station_Parameters_View ORDER BY \"Назва\"";
        Statement st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        return st.executeQuery(sql);
    }

    /**
     * Get report data: measurement statistics for a station within time period
     */
    public static ResultSet getMeasurementStatisticsReport(String stationId, String startDate, String endDate) throws SQLException {
        if (!isConnected()) throw new SQLException("Not connected");
        if (!stationId.matches("[A-Za-z0-9_-]+")) throw new SQLException("Неприпустимий ID станції");

        String sql = "SELECT " +
                "mu.Title AS \"Назва параметру\", " +
                "mu.Unit AS \"Одиниця виміру\", " +
                "ROUND(AVG(m.Value)::numeric, 2) AS \"Середнє\", " +
                "ROUND(MIN(m.Value)::numeric, 2) AS \"Мінімальне\", " +
                "ROUND(MAX(m.Value)::numeric, 2) AS \"Максимальне\", " +
                "COUNT(*) AS \"Кількість вимірювань\" " +
                "FROM Measurment m " +
                "JOIN Measured_Unit mu ON m.ID_Measured_Unit = mu.ID_Measured_Unit " +
                "WHERE m.ID_Station = ? " +
                "AND m.Time >= ?::timestamp " +
                "AND m.Time <= ?::timestamp " +
                "GROUP BY mu.Title, mu.Unit " +
                "ORDER BY mu.Title";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, stationId);
        ps.setString(2, startDate);
        ps.setString(3, endDate);
        return ps.executeQuery();
    }

    /**
     * Get list of all stations for combobox
     */
    public static ResultSet getAllStations() throws SQLException {
        if (!isConnected()) throw new SQLException("Not connected");
        String sql = "SELECT ID_Station, Name, City FROM Station ORDER BY Name";
        Statement st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        return st.executeQuery(sql);
    }
}