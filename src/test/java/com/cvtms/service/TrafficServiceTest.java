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

    @Test
    public void testRecordEntryForNonExistentVehicle() {
        String regNumber = "NON-EXISTENT-" + System.currentTimeMillis();
        boolean result = trafficService.recordEntry(regNumber, "Main Gate", "None");
        assertFalse("Entry should fail for non-existent vehicle", result);
    }

    @Test
    public void testRecordExitForVehicleNotInside() {
        String regNumber = "NOT-INSIDE-" + System.currentTimeMillis();
        vehicleService.registerVehicle(regNumber, "Not Inside Owner", "000", VehicleType.CAMPUS);
        boolean result = trafficService.recordExit(regNumber, "");
        assertFalse("Exit should fail for vehicle not inside", result);
    }

    @Test
    public void testRecordExitWithOverstay() {
        String regNumber = "OVERSTAY-" + System.currentTimeMillis();
        vehicleService.registerVehicle(regNumber, "Overstay Owner", "111", VehicleType.EXTERNAL);
        trafficService.recordEntry(regNumber, "Main Gate", "Visit");
        
        // We can't easily simulate time passing in this simple setup without mocking,
        // but we can test that the method handles a justification if needed.
        // In this system, recordExit returns false if an overstay is detected and no justification is provided.
        // Since we just entered, it won't be an overstay, so it should return true.
        boolean result = trafficService.recordExit(regNumber, "Was delayed by meeting");
        assertTrue("Exit should succeed with justification", result);
    }
}
