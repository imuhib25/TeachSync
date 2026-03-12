package com.intisarmuhib.teachsync;

import com.google.firebase.Timestamp;
import java.util.List;

public class BatchModel {

    private String id;
    private String name;
    private String subject;
    private Timestamp startTime;
    private Timestamp endTime;
    private long durationMinutes;
    private int totalMonthlyClasses;   // total classes per cycle
    private int currentMonthCount;     // classes taken in current cycle
    private int cycleCount;            // how many complete cycles have been done
    private double paymentPerStudent;  // payment per student for this batch
    private Timestamp createdAt;
    private int enrolledCount;         // Number of students in this batch
    
    // Scheduling preferences
    private boolean autoSchedule;
    private int weeklyCount;
    private List<Integer> selectedDays; // For auto-schedule (1=Sun, 2=Mon...)
    private List<Timestamp> manualDates; // For manual-schedule

    public BatchModel() {} // Required for Firestore

    public BatchModel(String id, String name, String subject,
                      Timestamp startTime, Timestamp endTime,
                      long durationMinutes, int totalMonthlyClasses,
                      int currentMonthCount, int cycleCount,
                      double paymentPerStudent, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.totalMonthlyClasses = totalMonthlyClasses;
        this.currentMonthCount = currentMonthCount;
        this.cycleCount = cycleCount;
        this.paymentPerStudent = paymentPerStudent;
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public Timestamp getStartTime() { return startTime; }
    public Timestamp getEndTime() { return endTime; }
    public long getDurationMinutes() { return durationMinutes; }
    public int getTotalMonthlyClasses() { return totalMonthlyClasses; }
    public int getCurrentMonthCount() { return currentMonthCount; }
    public int getCycleCount() { return cycleCount; }
    public double getPaymentPerStudent() { return paymentPerStudent; }
    public Timestamp getCreatedAt() { return createdAt; }
    public int getEnrolledCount() { return enrolledCount; }
    public boolean isAutoSchedule() { return autoSchedule; }
    public int getWeeklyCount() { return weeklyCount; }
    public List<Integer> getSelectedDays() { return selectedDays; }
    public List<Timestamp> getManualDates() { return manualDates; }

    // Derived helpers (not stored in Firestore)
    public int getRemainingInCycle() {
        int rem = totalMonthlyClasses - currentMonthCount;
        return rem < 0 ? 0 : rem;
    }

    // Setters (all required for Firestore toObject())
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
    public void setDurationMinutes(long durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setTotalMonthlyClasses(int totalMonthlyClasses) { this.totalMonthlyClasses = totalMonthlyClasses; }
    public void setCurrentMonthCount(int currentMonthCount) { this.currentMonthCount = currentMonthCount; }
    public void setCycleCount(int cycleCount) { this.cycleCount = cycleCount; }
    public void setPaymentPerStudent(double paymentPerStudent) { this.paymentPerStudent = paymentPerStudent; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }
    public void setAutoSchedule(boolean autoSchedule) { this.autoSchedule = autoSchedule; }
    public void setWeeklyCount(int weeklyCount) { this.weeklyCount = weeklyCount; }
    public void setSelectedDays(List<Integer> selectedDays) { this.selectedDays = selectedDays; }
    public void setManualDates(List<Timestamp> manualDates) { this.manualDates = manualDates; }
}
