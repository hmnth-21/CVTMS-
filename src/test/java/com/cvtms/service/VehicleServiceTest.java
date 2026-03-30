package com.cvtms.service;

import com.cvtms.model.Vehicle;
import com.cvtms.model.VehicleType;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class VehicleServiceTest {
    private VehicleService vehicleService;

    @Before
    public void setUp() {
        com.cvtms.dao.DatabaseConnectionManager.initializeDatabase();
        vehicleService = new VehicleService();
    }

    @Test
    public void testRegisterAndGetVehicle() {
        String regNumber = "TEST-" + System.currentTimeMillis();
        boolean result = vehicleService.registerVehicle(regNumber, "Test Owner", "1234567890", VehicleType.CAMPUS);
        assertTrue(result);

        Vehicle vehicle = vehicleService.getVehicle(regNumber);
        assertNotNull(vehicle);
        assertEquals(regNumber, vehicle.getRegistrationNumber());
        assertEquals(VehicleType.CAMPUS, vehicle.getType());
    }

    @Test
    public void testRegisterDuplicateVehicle() {
        String regNumber = "DUP-" + System.currentTimeMillis();
        vehicleService.registerVehicle(regNumber, "Owner 1", "111", VehicleType.CAB);
        boolean result = vehicleService.registerVehicle(regNumber, "Owner 2", "222", VehicleType.EXTERNAL);
        assertFalse(result);
    }
}
