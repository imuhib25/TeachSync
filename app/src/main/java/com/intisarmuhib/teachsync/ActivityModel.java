package com.intisarmuhib.teachsync;

import com.google.firebase.Timestamp;

public class ActivityModel {
    private String content;
    private String amount; // Optional extra info
    private String title; // Added for categorization (e.g., "Cycle Completed")
    private Timestamp timestamp;

    public ActivityModel() {} // empty constructor for Firestore

    public ActivityModel(String content, String amount) {
        this.content = content;
        this.amount = amount;
        this.timestamp = Timestamp.now();
    }

    public ActivityModel(String title, String content, String amount) {
        this.title = title;
        this.content = content;
        this.amount = amount;
        this.timestamp = Timestamp.now();
    }

    public String getContent() { return content; }
    public String getAmount() { return amount; }
    public String getTitle() { return title; }
    public Timestamp getTimestamp() { return timestamp; }

    public void setContent(String content) { this.content = content; }
    public void setAmount(String amount) { this.amount = amount; }
    public void setTitle(String title) { this.title = title; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
