package com.intisarmuhib.teachsync;

public class BatchFinanceModel {
    private String batchId;
    private String batchName;
    private double collectedAmount;
    private double dueAmount;
    private int studentCount;
    private int paidCount;
    private int cycleCount;

    public BatchFinanceModel() {} // Empty constructor for Firestore

    public BatchFinanceModel(String batchId, String batchName, double collectedAmount, double dueAmount, int studentCount, int paidCount) {
        this(batchId, batchName, collectedAmount, dueAmount, studentCount, paidCount, 1);
    }

    public BatchFinanceModel(String batchId, String batchName, double collectedAmount, double dueAmount, int studentCount, int paidCount, int cycleCount) {
        this.batchId = batchId;
        this.batchName = batchName;
        this.collectedAmount = collectedAmount;
        this.dueAmount = dueAmount;
        this.studentCount = studentCount;
        this.paidCount = paidCount;
        this.cycleCount = cycleCount;
    }

    public String getBatchId() { return batchId; }
    public String getBatchName() { return batchName; }
    public double getCollectedAmount() { return collectedAmount; }
    public double getDueAmount() { return dueAmount; }
    public int getStudentCount() { return studentCount; }
    public int getPaidCount() { return paidCount; }
    public int getCycleCount() { return cycleCount; }

    public void setBatchId(String batchId) { this.batchId = batchId; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public void setCollectedAmount(double collectedAmount) { this.collectedAmount = collectedAmount; }
    public void setDueAmount(double dueAmount) { this.dueAmount = dueAmount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
    public void setPaidCount(int paidCount) { this.paidCount = paidCount; }
    public void setCycleCount(int cycleCount) { this.cycleCount = cycleCount; }

    public int getProgress() {
        double total = collectedAmount + dueAmount;
        if (total == 0) return 0;
        return (int) ((collectedAmount / total) * 100);
    }
}
