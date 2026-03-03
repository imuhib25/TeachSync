package com.intisarmuhib.teachsync;

import com.google.firebase.Timestamp;

public class BatchModel {

    private String id;
    private String name;
    private String subject;
    private Timestamp startTime;
    private Timestamp endTime;
    private long durationMinutes;
    private int totalMonthlyClasses;   // total classes per cycle (kept same field name for compatibility)
    private int currentMonthCount;     // classes taken in current cycle
    private int cycleCount;            // how many complete cycles have been done
    private Timestamp createdAt;

    public BatchModel() {} // Required for Firestore

    public BatchModel(String id, String name, String subject,
                      Timestamp startTime, Timestamp endTime,
                      long durationMinutes, int totalMonthlyClasses,
                      int currentMonthCount, int cycleCount, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.totalMonthlyClasses = totalMonthlyClasses;
        this.currentMonthCount = currentMonthCount;
        this.cycleCount = cycleCount;
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public Timestamp getStartTime() { return startTime; }
    public Timestamp getEndTime() { return endTime; }
    public long getDurationMinutes() { return durationMinutes; }
    public int getTotalMonthlyClasses() { return totalMonthlyClasses; }
    public int getCurrentMonthCount() { return currentMonthCount; }
    public int getCycleCount() { return cycleCount; }
    public Timestamp getCreatedAt() { return createdAt; }

    // Derived helpers (not stored in Firestore)
    public int getRemainingInCycle() {
        int rem = totalMonthlyClasses - currentMonthCount;
        return rem < 0 ? 0 : rem;
    }

    // Setters (all required for Firestore toObject())
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
    public void setDurationMinutes(long durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setTotalMonthlyClasses(int totalMonthlyClasses) { this.totalMonthlyClasses = totalMonthlyClasses; }
    public void setCurrentMonthCount(int currentMonthCount) { this.currentMonthCount = currentMonthCount; }
    public void setCycleCount(int cycleCount) { this.cycleCount = cycleCount; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
