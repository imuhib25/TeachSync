package com.intisarmuhib.teachsync;

import com.google.firebase.Timestamp;

public class ClassModel {

    private String id;
    private String topic;
    private String batch;
    private String batchId;
    private String classTime;   // "hh:mm a - hh:mm a" display string
    private String date;        // "dd MMM yyyy"
    private String monthlyNumber; // Class number within cycle (stored as String)
    private boolean extra;
    private Timestamp createdAt;

    // New: cycle tracking (replaces month-key grouping)
    private int cycleNumber;    // which cycle this class belongs to (1, 2, 3…)
    private int totalInCycle;   // snapshot of batch's totalMonthlyClasses at save time
    
    // Status: "scheduled", "completed", "postponed", "rescheduled"
    private String status = "scheduled";

    public ClassModel() {}

    public ClassModel(String id, String topic, String batch, String batchId,
                      String classTime, String date,
                      String monthlyNumber, boolean extra,
                      int cycleNumber, int totalInCycle, Timestamp createdAt) {
        this.id = id;
        this.topic = topic;
        this.batch = batch;
        this.batchId = batchId;
        this.classTime = classTime;
        this.date = date;
        this.monthlyNumber = monthlyNumber;
        this.extra = extra;
        this.cycleNumber = cycleNumber;
        this.totalInCycle = totalInCycle;
        this.createdAt = createdAt;
        this.status = "scheduled";
    }

    // Getters
    public String getId() { return id; }
    public String getTopic() { return topic; }
    public String getBatch() { return batch; }
    public String getBatchId() { return batchId; }
    public String getClassTime() { return classTime; }
    public String getDate() { return date; }
    public String getMonthlyNumber() { return monthlyNumber; }
    public boolean isExtra() { return extra; }
    public int getCycleNumber() { return cycleNumber; }
    public int getTotalInCycle() { return totalInCycle; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }

    // Setters (all required for Firestore toObject() deserialization)
    public void setId(String id) { this.id = id; }
    public void setTopic(String topic) { this.topic = topic; }
    public void setBatch(String batch) { this.batch = batch; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public void setClassTime(String classTime) { this.classTime = classTime; }
    public void setDate(String date) { this.date = date; }
    public void setMonthlyNumber(String monthlyNumber) { this.monthlyNumber = monthlyNumber; }
    public void setExtra(boolean extra) { this.extra = extra; }
    public void setCycleNumber(int cycleNumber) { this.cycleNumber = cycleNumber; }
    public void setTotalInCycle(int totalInCycle) { this.totalInCycle = totalInCycle; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setStatus(String status) { this.status = status; }
}
