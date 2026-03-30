package com.cvtms.service;

import com.cvtms.model.VehicleType;
import org.junit.Before;
import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

public class ReportingServiceTest {
    private ReportingService reportingService;

    @Before
    public void setUp() {
        com.cvtms.dao.DatabaseConnectionManager.initializeDatabase();
        reportingService = new ReportingService();
    }

    @Test
    public void testGetDailyStatistics() {
        Map<String, Integer> stats = reportingService.getDailyStatistics();
        assertNotNull(stats);
        assertTrue(stats.containsKey("entries"));
        assertTrue(stats.containsKey("exits"));
    }

    @Test
    public void testGetVehicleTypeDistribution() {
        Map<VehicleType, Integer> distribution = reportingService.getVehicleTypeDistribution();
        assertNotNull(distribution);
    }
}
