package com.intisarmuhib.teachsync;

import com.google.firebase.Timestamp;

public class TransactionModel {
    private String id;
    private String studentId;
    private String studentName;
    private String batchId;
    private String invoiceId;
    private double amount;
    private String method; // "Cash", "bKash", "Nagad", "Bank"
    private Timestamp timestamp;
    private String note;

    public TransactionModel() {}

    public TransactionModel(String id, String studentId, String studentName, String batchId, 
                            String invoiceId, double amount, String method, 
                            Timestamp timestamp, String note) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.batchId = batchId;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.method = method;
        this.timestamp = timestamp;
        this.note = note;
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
    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
