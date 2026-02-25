package com.intisarmuhib.teachsync;

public class ActivityModel {
    private String title;
    private String description;
    private String amount; // e.g., "+$50.00"

    public ActivityModel() {} // empty constructor for Firestore

    public ActivityModel(String title, String description, String amount) {
        this.title = title;
        this.description = description;
        this.amount = amount;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAmount() { return amount; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setAmount(String amount) { this.amount = amount; }
}
