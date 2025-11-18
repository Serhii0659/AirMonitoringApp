package io.github.serhii0659.air_monitoring.airmonitoringapp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigManager {
    private static Properties properties;
    private static String lastError;

    private ConfigManager() {}

    static {
        loadConfig();
    }

    private static void loadConfig() {
        properties = new Properties();
        lastError = null;

        java.io.File configFile = new java.io.File("config.properties");

        // Try to load from file system first
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                properties.load(input);
                return;
            } catch (IOException e) {
                lastError = "Помилка читання config.properties: " + e.getMessage();
            }
        }

        // Try to load from classpath
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
                return;
            }
        } catch (IOException e) {

        }

        // Config not found - create default template
        createDefaultConfig(configFile);
        setDefaults();
    }

    private static void createDefaultConfig(java.io.File configFile) {
        try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
            writer.write("# Air Monitoring Application Configuration\n");
            writer.write("# Створено автоматично - заповніть своїми даними\n\n");
            writer.write("db.host=localhost\n");
            writer.write("db.port=5432\n");
            writer.write("db.name=air_monitoring\n");
            writer.write("db.user=your_username\n");
            writer.write("db.password=your_password\n");
            lastError = "Файл config.properties створено. Заповніть параметри підключення до БД та перезапустіть додаток.";
        } catch (IOException e) {
            lastError = "Не вдалося створити config.properties: " + e.getMessage();
        }
    }

    private static void setDefaults() {
        properties.setProperty("db.host", "localhost");
        properties.setProperty("db.port", "5432");
        properties.setProperty("db.name", "air_monitoring");
        properties.setProperty("db.user", "");
        properties.setProperty("db.password", "");
    }

    public static String getDbHost() {
        return properties.getProperty("db.host");
    }

    public static int getDbPort() {
        try {
            String port = properties.getProperty("db.port");
            return port != null ? Integer.parseInt(port) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String getDbName() {
        return properties.getProperty("db.name");
    }

    public static String getDbUser() {
        return properties.getProperty("db.user");
    }

    public static String getDbPassword() {
        return properties.getProperty("db.password");
    }

    public static String getDbUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s",
            getDbHost(), getDbPort(), getDbName());
    }

    public static String getLastError() {
        return lastError;
    }
}