package com.cvtms.model;

import java.time.LocalDateTime;

public class ExitLog {
    private int id;
    private int entryLogId;
    private LocalDateTime exitTime;

    public ExitLog(int id, int entryLogId, LocalDateTime exitTime) {
        this.id = id;
        this.entryLogId = entryLogId;
        this.exitTime = exitTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEntryLogId() { return entryLogId; }
    public void setEntryLogId(int entryLogId) { this.entryLogId = entryLogId; }

    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }
}
