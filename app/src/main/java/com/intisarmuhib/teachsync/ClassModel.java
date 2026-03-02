package com.intisarmuhib.teachsync;

public class ClassModel {

    private String id;
    private String topic;
    private String batch;
    private String batchId;
    private String classTime;
    private String date;
    private String tvRemaining;
    private String tvTaken;
    private int monthlyNumber;
    private boolean extra;
    private String monthKey;

    // NEW
    private long startMillis;
    private long endMillis;

    public ClassModel() {}

    public ClassModel(String id, String topic, String batch, String batchId,
                      String classTime, String date,
                      int monthlyNumber, String monthKey,
                      String tvRemaining, String tvTaken,
                      boolean extra, long startMillis, long endMillis) {

        this.id = id;
        this.topic = topic;
        this.batch = batch;
        this.batchId = batchId;
        this.classTime = classTime;
        this.date = date;
        this.monthlyNumber = monthlyNumber;
        this.monthKey = monthKey;
        this.tvRemaining = tvRemaining;
        this.tvTaken = tvTaken;
        this.extra = extra;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
    }

    public String getId() { return id; }
    public String getTopic() { return topic; }
    public String getBatch() { return batch; }
    public String getBatchId() { return batchId; }
    public String getClassTime() { return classTime; }
    public String getDate() { return date; }
    public int getMonthlyNumber() { return monthlyNumber; }
    public String getTvRemaining() { return tvRemaining; }
    public String getTvTaken() { return tvTaken; }
    public boolean isExtra() { return extra; }
    public String getMonthKey() { return monthKey; }
    public long getStartMillis() { return startMillis; }
    public long getEndMillis() { return endMillis; }

    public void setId(String id) { this.id = id; }
    public void setMonthlyNumber(int monthlyNumber) { this.monthlyNumber = monthlyNumber; }
    public void setDate(String date) { this.date = date; }
}