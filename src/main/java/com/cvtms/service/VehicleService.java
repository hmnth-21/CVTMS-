package com.cvtms.service;

import com.cvtms.dao.VehicleDAO;
import com.cvtms.model.Vehicle;
import com.cvtms.model.VehicleOwner;
import com.cvtms.model.VehicleType;

public class VehicleService {
    private VehicleDAO vehicleDAO;

    public VehicleService() {
        this.vehicleDAO = new VehicleDAO();
    }

    private String normalizeRegistration(String registrationNumber) {
        if (registrationNumber == null) {
            return null;
        }
        String normalized = registrationNumber.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    public boolean registerVehicle(String registrationNumber, String ownerName, String ownerContact, VehicleType type) {
        String normalizedReg = normalizeRegistration(registrationNumber);
        if (normalizedReg == null) {
            System.out.println("Registration number cannot be empty.");
            return false;
        }
        
        if (vehicleDAO.findVehicleByRegistrationNumber(normalizedReg) != null) {
            System.out.println("Vehicle with this registration number already exists.");
            return false;
        }

        String normalizedOwnerName = ownerName != null ? ownerName.trim() : "";
        String normalizedOwnerContact = ownerContact != null ? ownerContact.trim() : "";

        int ownerId = -1;
        if (!normalizedOwnerName.isEmpty()) {
            ownerId = vehicleDAO.createOwner(normalizedOwnerName, normalizedOwnerContact);
            if (ownerId == -1) {
                System.out.println("Failed to create vehicle owner.");
                return false;
            }
        }

        return vehicleDAO.createVehicle(normalizedReg, ownerId, type);
    }
    
    public Vehicle getVehicle(String registrationNumber) {
        String normalizedReg = normalizeRegistration(registrationNumber);
        if (normalizedReg == null) {
            return null;
        }
        return vehicleDAO.findVehicleByRegistrationNumber(normalizedReg);
    }
    
    public VehicleOwner getVehicleOwner(int ownerId) {
        return vehicleDAO.findOwnerById(ownerId);
    }
}
