package com.cvtms.model;

import java.sql.Timestamp;

public class Incident {
    private int id;
    private int vehicleId;
    private String description;
    private Timestamp timestamp;
    private String severity;

    public Incident(int id, int vehicleId, String description, Timestamp timestamp, String severity) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.description = description;
        this.timestamp = timestamp;
        this.severity = severity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
