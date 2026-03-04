package com.intisarmuhib.teachsync;

public class ActivityModel {
    private String content;
    private String amount; // Optional extra info

    public ActivityModel() {} // empty constructor for Firestore

    public ActivityModel(String content, String amount) {
        this.content = content;
        this.amount = amount;
    }

    public String getContent() { return content; }
    public String getAmount() { return amount; }

    public void setContent(String content) { this.content = content; }
    public void setAmount(String amount) { this.amount = amount; }
}
