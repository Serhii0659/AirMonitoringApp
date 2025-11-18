module io.github.serhii0659.air_monitoring.airmonitoringapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    requires java.sql;
    requires org.postgresql.jdbc; // added for PostgreSQL driver

    opens io.github.serhii0659.air_monitoring.airmonitoringapp to javafx.fxml;
    exports io.github.serhii0659.air_monitoring.airmonitoringapp;
}