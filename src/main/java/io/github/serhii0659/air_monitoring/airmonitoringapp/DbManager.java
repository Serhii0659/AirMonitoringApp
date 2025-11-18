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

    public static String getLastError() { return lastError; }

    public static List<String> getTables() {
        List<String> tables = new ArrayList<>();
        if (!isConnected()) return tables;
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) tables.add(rs.getString(1));
        } catch (SQLException e) {
            lastError = e.getMessage();
        }
        return tables;
    }

    public static ResultSet fetchTable(String tableName, int limit) throws SQLException {
        return fetchTable(tableName, limit, 0);
    }

    public static ResultSet fetchTable(String tableName, int limit, int offset) throws SQLException {
        if (!isConnected()) throw new SQLException("Not connected");
        // Basic safety: only allow letters, digits, underscore
        if (!tableName.matches("[A-Za-z0-9_]+")) throw new SQLException("Неприпустима назва таблиці");

        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName);
        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
            if (offset > 0) {
                sql.append(" OFFSET ").append(offset);
            }
        }

        Statement st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        return st.executeQuery(sql.toString());
    }

    public static int getTableRowCount(String tableName) throws SQLException {
        return getTableRecordCount(tableName);
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
}