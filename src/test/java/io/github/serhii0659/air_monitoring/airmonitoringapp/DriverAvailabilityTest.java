package io.github.serhii0659.air_monitoring.airmonitoringapp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DriverAvailabilityTest {
    @Test
    void driverLoads() throws Exception {
        Class.forName("org.postgresql.Driver");
        assertTrue(true, "PostgreSQL driver is available");
    }
}