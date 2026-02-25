package com.intisarmuhib.teachsync;

public class StudentModel {

    private String name;
    private String email;
    private String phone;
    private String batch;
    private String id;

    // Constructor

    public StudentModel() {} // Required for Firestore

    public StudentModel(String id, String name, String email, String phone, String batch) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.batch = batch;
        this.id = id;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getBatch() { return batch; }

    public String getId() {return id;}

    public void setId(String id) { this.id = id;
    }
}