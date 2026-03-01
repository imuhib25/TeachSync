package com.intisarmuhib.teachsync;

public class ClassModel {

    private String id;
    private String topic;
    private String batch;
    private String batchId;
    private String classTime;
    private String date;
    private int monthlyNumber;
    private boolean extra;
    private String monthKey;
    public ClassModel() {}

    public ClassModel(String id, String topic, String batch, String batchId,
                      String classTime, String date,
                      int monthlyNumber, String monthKey, boolean extra) {
        this.id = id;
        this.topic = topic;
        this.batch = batch;
        this.batchId = batchId;
        this.classTime = classTime;
        this.date = date;
        this.monthlyNumber = monthlyNumber;
        this.monthKey = monthKey;
        this.extra = extra;
    }

    public String getId() { return id; }
    public String getTopic() { return topic; }
    public String getBatch() { return batch; }
    public String getBatchId() { return batchId; }
    public String getClassTime() { return classTime; }
    public String getDate() { return date; }
    public int getMonthlyNumber() { return monthlyNumber; }
    public boolean isExtra() { return extra; }

    public void setId(String id) { this.id = id; }
    public String getMonthKey() { return monthKey; }
    public void setMonthlyNumber(int monthlyNumber) { this.monthlyNumber = monthlyNumber; }
    public void setDate(String date) {this.date = date;
    }
}