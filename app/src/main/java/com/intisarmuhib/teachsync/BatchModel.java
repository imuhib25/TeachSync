package com.intisarmuhib.teachsync;

import com.google.firebase.Timestamp;

public class BatchModel {

    private String id;
    private String name;
    private String subject;
    private Timestamp startTime;
    private Timestamp endTime;
    private long durationMinutes;
    private int totalMonthlyClasses;
    private int currentMonthCount;
    private int cycleCount;
    private Timestamp createdAt;

    public BatchModel() {}

    public BatchModel(String id, String name, String subject,
                      Timestamp startTime, Timestamp endTime,
                      long durationMinutes, int totalMonthlyClasses,
                      int currentMonthCount, int cycleCount,
                      Timestamp createdAt) {
        this.id = id; this.name = name; this.subject = subject;
        this.startTime = startTime; this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.totalMonthlyClasses = totalMonthlyClasses;
        this.currentMonthCount = currentMonthCount;
        this.cycleCount = cycleCount; this.createdAt = createdAt;
    }

    public String getId()               { return id; }
    public String getName()             { return name; }
    public String getSubject()          { return subject; }
    public Timestamp getStartTime()     { return startTime; }
    public Timestamp getEndTime()       { return endTime; }
    public long getDurationMinutes()    { return durationMinutes; }
    public int getTotalMonthlyClasses() { return totalMonthlyClasses; }
    public int getCurrentMonthCount()   { return currentMonthCount; }
    public int getCycleCount()          { return cycleCount; }
    public Timestamp getCreatedAt()     { return createdAt; }
    public int getRemainingInCycle()    { return Math.max(0, totalMonthlyClasses - currentMonthCount); }

    public void setId(String id)                  { this.id = id; }
    public void setName(String name)              { this.name = name; }
    public void setSubject(String subject)        { this.subject = subject; }
    public void setStartTime(Timestamp t)         { this.startTime = t; }
    public void setEndTime(Timestamp t)           { this.endTime = t; }
    public void setDurationMinutes(long d)        { this.durationMinutes = d; }
    public void setTotalMonthlyClasses(int t)     { this.totalMonthlyClasses = t; }
    public void setCurrentMonthCount(int c)       { this.currentMonthCount = c; }
    public void setCycleCount(int c)              { this.cycleCount = c; }
    public void setCreatedAt(Timestamp t)         { this.createdAt = t; }
}