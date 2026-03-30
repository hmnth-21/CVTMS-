package com.cvtms.service;

import com.cvtms.dao.IncidentDAO;
import com.cvtms.dao.VehicleDAO;
import com.cvtms.model.Incident;
import com.cvtms.model.Vehicle;
import java.util.List;

public class IncidentService {
    private IncidentDAO incidentDAO = new IncidentDAO();
    private VehicleDAO vehicleDAO = new VehicleDAO();

    public boolean recordIncident(String registrationNumber, String description, String severity) {
        Vehicle vehicle = vehicleDAO.findVehicleByRegistrationNumber(registrationNumber);
        if (vehicle == null) {
            System.out.println("Vehicle not found!");
            return false;
        }
        return incidentDAO.createIncident(vehicle.getId(), description, severity);
    }

    public List<Incident> getVehicleIncidents(String registrationNumber) {
        Vehicle vehicle = vehicleDAO.findVehicleByRegistrationNumber(registrationNumber);
        if (vehicle == null) {
            return null;
        }
        return incidentDAO.getIncidentsByVehicleId(vehicle.getId());
    }

    public List<Incident> getAllIncidents() {
        return incidentDAO.getAllIncidents();
    }
}
