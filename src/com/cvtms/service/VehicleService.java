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

    public boolean registerVehicle(String registrationNumber, String ownerName, String ownerContact, VehicleType type) {
        if (vehicleDAO.findVehicleByRegistrationNumber(registrationNumber) != null) {
            System.out.println("Vehicle with this registration number already exists.");
            return false;
        }

        int ownerId = -1;
        if (ownerName != null && !ownerName.isEmpty()) {
            ownerId = vehicleDAO.createOwner(ownerName, ownerContact);
        }

        return vehicleDAO.createVehicle(registrationNumber, ownerId, type);
    }
    
    public Vehicle getVehicle(String registrationNumber) {
        return vehicleDAO.findVehicleByRegistrationNumber(registrationNumber);
    }
    
    public VehicleOwner getVehicleOwner(int ownerId) {
        return vehicleDAO.findOwnerById(ownerId);
    }
}
