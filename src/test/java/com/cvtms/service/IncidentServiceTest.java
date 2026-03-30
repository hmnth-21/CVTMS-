package com.cvtms.service;

import com.cvtms.model.Incident;
import com.cvtms.model.VehicleType;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class IncidentServiceTest {
    private IncidentService incidentService;
    private VehicleService vehicleService;

    @Before
    public void setUp() {
        com.cvtms.dao.DatabaseConnectionManager.initializeDatabase();
        incidentService = new IncidentService();
        vehicleService = new VehicleService();
    }

    @Test
    public void testRecordAndGetIncidents() {
        String regNumber = "INC-" + System.currentTimeMillis();
        vehicleService.registerVehicle(regNumber, "Inc Owner", "777", VehicleType.EXTERNAL);
        
        boolean result = incidentService.recordIncident(regNumber, "Speeding", "HIGH");
        assertTrue(result);
        
        List<Incident> incidents = incidentService.getVehicleIncidents(regNumber);
        assertNotNull(incidents);
        assertFalse(incidents.isEmpty());
        assertEquals("Speeding", incidents.get(0).getDescription());
        assertEquals("HIGH", incidents.get(0).getSeverity());
    }
}
