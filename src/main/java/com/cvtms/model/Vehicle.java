package com.cvtms.model;

public class Vehicle {
    private int id;
    private String registrationNumber;
    private int ownerId;
    private VehicleType type;

    public Vehicle(int id, String registrationNumber, int ownerId, VehicleType type) {
        this.id = id;
        this.registrationNumber = registrationNumber;
        this.ownerId = ownerId;
        this.type = type;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public VehicleType getType() { return type; }
    public void setType(VehicleType type) { this.type = type; }
}
