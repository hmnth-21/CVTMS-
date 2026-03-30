package com.cvtms.service;

import com.cvtms.model.EntryLog;
import com.cvtms.model.VehicleType;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class TrafficServiceTest {
    private TrafficService trafficService;
    private VehicleService vehicleService;

    @Before
    public void setUp() {
        com.cvtms.dao.DatabaseConnectionManager.initializeDatabase();
        vehicleService = new VehicleService();
        trafficService = new TrafficService(vehicleService);
    }

    @Test
    public void testRecordEntryAndExit() {
        String regNumber = "TRAF-" + System.currentTimeMillis();
        vehicleService.registerVehicle(regNumber, "Traf Owner", "999", VehicleType.CAMPUS);
        
        boolean entryResult = trafficService.recordEntry(regNumber, "Main Gate", "Work");
        assertTrue(entryResult);
        
        boolean exitResult = trafficService.recordExit(regNumber, "");
        assertTrue(exitResult);
    }

    @Test
    public void testViewCurrentlyInside() {
        String regNumber = "INSIDE-" + System.currentTimeMillis();
        vehicleService.registerVehicle(regNumber, "Inside Owner", "888", VehicleType.WORK);
        trafficService.recordEntry(regNumber, "Back Gate", "Maintenance");
        
        List<EntryLog> inside = trafficService.viewVehiclesCurrentlyInside();
        assertFalse(inside.isEmpty());
        boolean found = false;
        for (EntryLog log : inside) {
            if (log.getGate().equals("Back Gate")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testGetVehicleMovementHistoryWithExit() {
        String regNumber = "HIST-" + System.currentTimeMillis();
        vehicleService.registerVehicle(regNumber, "Hist Owner", "777", VehicleType.EXTERNAL);
        
        trafficService.recordEntry(regNumber, "Gate A", "Visit");
        trafficService.recordExit(regNumber, "");
        
        List<EntryLog> history = trafficService.getVehicleMovementHistory(regNumber);
        assertNotNull(history);
        assertFalse(history.isEmpty());
        
        EntryLog log = history.get(0);
        assertEquals("Gate A", log.getGate());
        assertNotNull(log.getExitTime());
    }
}
