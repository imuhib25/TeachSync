package com.intisarmuhib.teachsync;

import com.google.firebase.Timestamp;

public class BatchModel {

    private String id;
    private String name;
    private String subject;
    private Timestamp startTime;
    private Timestamp endTime;
    private long durationMinutes;
    private int totalMonthlyClasses;

    private Timestamp createdAt;

    public BatchModel() {} // Required for Firestore

    public BatchModel(String id, String name, String subject,
                      Timestamp startTime, Timestamp endTime,
                      long durationMinutes, int totalMonthlyClasses, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.totalMonthlyClasses = totalMonthlyClasses;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public Timestamp getStartTime() { return startTime; }
    public Timestamp getEndTime() { return endTime; }
    public long getDurationMinutes() { return durationMinutes; }
    public Timestamp getCreatedAt() { return createdAt; }
    public int getTotalMonthlyClasses() {
        return totalMonthlyClasses;
    }

    public void setTotalMonthlyClasses(int totalMonthlyClasses) {
        this.totalMonthlyClasses = totalMonthlyClasses;
    }
    public void setId(String id) { this.id = id; }
}