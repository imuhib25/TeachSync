package com.intisarmuhib.teachsync;

public class BatchFinanceModel {
    private String batchId;
    private String batchName;
    private double collectedAmount;
    private double dueAmount;
    private int studentCount;

    public BatchFinanceModel(String batchId, String batchName, double collectedAmount, double dueAmount, int studentCount) {
        this.batchId = batchId;
        this.batchName = batchName;
        this.collectedAmount = collectedAmount;
        this.dueAmount = dueAmount;
        this.studentCount = studentCount;
    }

    public String getBatchId() { return batchId; }
    public String getBatchName() { return batchName; }
    public double getCollectedAmount() { return collectedAmount; }
    public double getDueAmount() { return dueAmount; }
    public int getStudentCount() { return studentCount; }

    public int getProgress() {
        double total = collectedAmount + dueAmount;
        if (total == 0) return 0;
        return (int) ((collectedAmount / total) * 100);
    }
}
