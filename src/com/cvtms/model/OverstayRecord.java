package com.cvtms.model;

public class OverstayRecord {
    private int id;
    private int exitLogId;
    private String justification;

    public OverstayRecord(int id, int exitLogId, String justification) {
        this.id = id;
        this.exitLogId = exitLogId;
        this.justification = justification;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getExitLogId() { return exitLogId; }
    public void setExitLogId(int exitLogId) { this.exitLogId = exitLogId; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
}
