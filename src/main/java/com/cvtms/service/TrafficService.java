package com.cvtms.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.cvtms.dao.LogDAO;
import com.cvtms.model.EntryLog;
import com.cvtms.model.Vehicle;
import com.cvtms.model.VehicleType;

public class TrafficService {
    private LogDAO logDAO;
    private VehicleService vehicleService;

    // Overstay thresholds in hours
    private static final long MAX_HOURS_EXTERNAL = 12;

    public TrafficService(VehicleService vehicleService) {
        this.logDAO = new LogDAO();
        this.vehicleService = vehicleService;
    }

    public boolean recordEntry(String registrationNumber, String gate, String externalPurpose) {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            System.out.println("Registration number cannot be empty.");
            return false;
        }

        String normalizedReg = registrationNumber.trim().toUpperCase();

        if (gate == null || gate.trim().isEmpty()) {
            System.out.println("Gate name cannot be empty.");
            return false;
        }

        Vehicle vehicle = vehicleService.getVehicle(normalizedReg);
        if (vehicle == null) {
            System.out.println("Vehicle not found. Please register the vehicle first.");
            return false;
        }

        EntryLog activeLog = logDAO.findActiveEntryLogForVehicle(vehicle.getId());
        if (activeLog != null) {
            System.out.println("Error: Vehicle " + normalizedReg + " is already inside the campus (duplicate entry prevented).");
            return false;
        }

        int logId = logDAO.createEntryLog(vehicle.getId(), gate, externalPurpose);
        if (logId > 0) {
            System.out.println("Entry recorded successfully for " + normalizedReg);
            return true;
        }
        return false;
    }

    public boolean recordExit(String registrationNumber, String justificationIfNeeded) {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            System.out.println("Registration number cannot be empty.");
            return false;
        }

        String normalizedReg = registrationNumber.trim().toUpperCase();
        Vehicle vehicle = vehicleService.getVehicle(normalizedReg);
        if (vehicle == null) {
            System.out.println("Vehicle not found.");
            return false;
        }

        EntryLog activeLog = logDAO.findActiveEntryLogForVehicle(vehicle.getId());
        if (activeLog == null) {
            System.out.println("Error: Vehicle " + normalizedReg + " does not have an active entry record.");
            return false;
        }

        // Calculate if overstay
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(activeLog.getEntryTime(), now);
        long hoursStayed = duration.toHours();

        boolean isOverstay = false;
        if (vehicle.getType() == VehicleType.EXTERNAL && hoursStayed > MAX_HOURS_EXTERNAL) {
            isOverstay = true;
        } else if (hoursStayed > 24) {
            // General flag for other vehicle types if stayed > 24 hours (simplified logic)
            if (vehicle.getType() != VehicleType.CAMPUS && vehicle.getType() != VehicleType.WORK) {
                 isOverstay = true;
            }
        }

        if (isOverstay && (justificationIfNeeded == null || justificationIfNeeded.trim().isEmpty())) {
            System.out.println("WARNING: Overstay detected (" + hoursStayed + " hours). A justification is required to complete exit.");
            return false; // Prompt user for justification
        }

        int exitId = logDAO.createExitLog(activeLog.getId());
        if (exitId > 0) {
            if (isOverstay) {
                logDAO.createOverstayRecord(exitId, justificationIfNeeded);
                System.out.println("Exit recorded with overstay justification.");
            } else {
                System.out.println("Exit recorded successfully.");
            }
            return true;
        }
        return false;
    }
    
    public List<EntryLog> viewVehiclesCurrentlyInside() {
        return logDAO.getVehiclesCurrentlyInside();
    }

    public List<EntryLog> getVehicleMovementHistory(String registrationNumber) {
        Vehicle vehicle = vehicleService.getVehicle(registrationNumber);
        if (vehicle == null) {
            return null;
        }
        return logDAO.getMovementHistory(vehicle.getId());
    }

    public List<EntryLog> searchTrafficLogs(String regNumber, String dateFrom, String dateTo) {
        return logDAO.searchLogs(regNumber, dateFrom, dateTo);
    }
}
