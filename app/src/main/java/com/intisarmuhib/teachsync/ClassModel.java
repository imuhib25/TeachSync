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

    // ── monthlyNumber type-safety fix ─────────────────────────────────────
    // Old Firestore documents stored this as Long (integer).
    // New documents store it as String.
    // Firestore's toObject() inspects the setter's parameter type to decide
    // how to convert the raw value. A setter typed as (String) crashes with
    // RuntimeException when the stored value is a Long.
    //
    // Fix: field and setter typed as Object — Firestore passes the raw value
    // through without conversion. The app-facing getter (@Exclude so Firestore
    // ignores it) converts safely to String regardless of stored type.
    private Object monthlyNumber;

    public ClassModel() {}

    public ClassModel(String id, String topic, String batch, String batchId,
                      String classTime, String date,
                      String monthlyNumber, boolean extra,
                      int cycleNumber, int totalInCycle) {
        this.id = id;
        this.topic = topic;
        this.batch = batch;
        this.batchId = batchId;
        this.classTime = classTime;
        this.date = date;
        this.monthlyNumber = monthlyNumber;
        this.extra = extra;
        this.cycleNumber = cycleNumber;
        this.totalInCycle = totalInCycle;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public String getId()        { return id; }
    public String getTopic()     { return topic; }
    public String getBatch()     { return batch; }
    public String getBatchId()   { return batchId; }
    public String getClassTime() { return classTime; }
    public String getDate()      { return date; }
    public boolean isExtra()     { return extra; }
    public int getCycleNumber()  { return cycleNumber; }
    public int getTotalInCycle() { return totalInCycle; }

    /**
     * Always returns a String regardless of whether the Firestore document
     * stored monthlyNumber as a Long (legacy) or String (new).
     *
     * @Exclude tells Firestore NOT to use this getter for serialization or
     * deserialization — it reads/writes the raw Object field directly via
     * getMonthlyNumber()/setMonthlyNumber(Object) instead.
     */
    @Exclude
    public String getMonthlyNumber() {
        if (monthlyNumber == null)             return "";
        if (monthlyNumber instanceof Long)     return String.valueOf((Long) monthlyNumber);
        if (monthlyNumber instanceof Double)   return String.valueOf(((Double) monthlyNumber).longValue());
        if (monthlyNumber instanceof Integer)  return String.valueOf((Integer) monthlyNumber);
        return monthlyNumber.toString();
    }

    // ── Setters ───────────────────────────────────────────────────────────

    public void setId(String id)               { this.id = id; }
    public void setTopic(String topic)         { this.topic = topic; }
    public void setBatch(String batch)         { this.batch = batch; }
    public void setBatchId(String batchId)     { this.batchId = batchId; }
    public void setClassTime(String classTime) { this.classTime = classTime; }
    public void setDate(String date)           { this.date = date; }
    public void setExtra(boolean extra)        { this.extra = extra; }
    public void setCycleNumber(int n)          { this.cycleNumber = n; }
    public void setTotalInCycle(int n)         { this.totalInCycle = n; }

    /**
     * Accepts Object so Firestore never crashes regardless of stored type
     * (Long from old docs, String from new docs).
     */
    public void setMonthlyNumber(Object monthlyNumber) {
        this.monthlyNumber = monthlyNumber;
    }
}