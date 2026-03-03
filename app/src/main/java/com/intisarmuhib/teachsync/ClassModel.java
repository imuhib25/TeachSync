package com.intisarmuhib.teachsync;

import com.google.firebase.firestore.Exclude;

public class ClassModel {

    private String id;
    private String topic;
    private String batch;
    private String batchId;
    private String classTime;
    private String date;
    private boolean extra;
    private int cycleNumber;
    private int totalInCycle;

    // Object type so Firestore never crashes whether old docs stored this
    // as Long or new docs store it as String. @Exclude on the getter
    // prevents Firestore from using it during deserialization.
    private Object monthlyNumber;

    public ClassModel() {}

    public String getId()        { return id; }
    public String getTopic()     { return topic; }
    public String getBatch()     { return batch; }
    public String getBatchId()   { return batchId; }
    public String getClassTime() { return classTime; }
    public String getDate()      { return date; }
    public boolean isExtra()     { return extra; }
    public int getCycleNumber()  { return cycleNumber; }
    public int getTotalInCycle() { return totalInCycle; }

    @Exclude
    public String getMonthlyNumber() {
        if (monthlyNumber == null)            return "";
        if (monthlyNumber instanceof Long)    return String.valueOf((Long) monthlyNumber);
        if (monthlyNumber instanceof Integer) return String.valueOf((Integer) monthlyNumber);
        if (monthlyNumber instanceof Double)  return String.valueOf(((Double) monthlyNumber).longValue());
        return monthlyNumber.toString();
    }

    public void setId(String id)               { this.id = id; }
    public void setTopic(String topic)         { this.topic = topic; }
    public void setBatch(String batch)         { this.batch = batch; }
    public void setBatchId(String batchId)     { this.batchId = batchId; }
    public void setClassTime(String classTime) { this.classTime = classTime; }
    public void setDate(String date)           { this.date = date; }
    public void setExtra(boolean extra)        { this.extra = extra; }
    public void setCycleNumber(int n)          { this.cycleNumber = n; }
    public void setTotalInCycle(int n)         { this.totalInCycle = n; }

    // Object setter — Firestore passes Long or String through without crashing
    public void setMonthlyNumber(Object monthlyNumber) {
        this.monthlyNumber = monthlyNumber;
    }
}