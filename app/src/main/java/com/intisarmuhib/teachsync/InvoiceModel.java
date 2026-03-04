package com.intisarmuhib.teachsync;

import com.google.firebase.Timestamp;

public class InvoiceModel {
    private String id;
    private String studentId;
    private String studentName;
    private String batchId;
    private String batchName;
    private double amount;
    private double paidAmount;
    private String status; // "Paid", "Due", "Overdue"
    private Timestamp month; // Represents the billing month
    private Timestamp createdAt;

    public InvoiceModel() {}

    public InvoiceModel(String id, String studentId, String studentName, String batchId, 
                        String batchName, double amount, double paidAmount, 
                        String status, Timestamp month, Timestamp createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.batchId = batchId;
        this.batchName = batchName;
        this.amount = amount;
        this.paidAmount = paidAmount;
        this.status = status;
        this.month = month;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getMonth() { return month; }
    public void setMonth(Timestamp month) { this.month = month; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public double getDueAmount() {
        return amount - paidAmount;
    }
}
