package com.cvtms.model;

import java.time.LocalDateTime;

public class EntryLog {
    private int id;
    private int vehicleId;
    private LocalDateTime entryTime;
    private String gate;
    private String externalPurpose;

    public EntryLog(int id, int vehicleId, LocalDateTime entryTime, String gate, String externalPurpose) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.entryTime = entryTime;
        this.gate = gate;
        this.externalPurpose = externalPurpose;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public String getGate() { return gate; }
    public void setGate(String gate) { this.gate = gate; }

    public String getExternalPurpose() { return externalPurpose; }
    public void setExternalPurpose(String externalPurpose) { this.externalPurpose = externalPurpose; }
}
