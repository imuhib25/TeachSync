package com.intisarmuhib.teachsync;

import java.util.ArrayList;
import java.util.List;

public class StudentModel {

    private String name;
    private String email;
    private String phone;
    private List<String> batches; 
    private String parent;
    private String id;

    public StudentModel() {
        // Required for Firebase
        this.batches = new ArrayList<>();
    } 

    public StudentModel(String id, String name, String email, String phone, List<String> batches) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.batches = batches != null ? batches : new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public List<String> getBatches() { return batches; }
    public void setBatches(List<String> batches) { this.batches = batches; }

    public String getParent() { return parent; }
    public void setParent(String parent) { this.parent = parent; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBatchesDisplay() {
        if (batches == null || batches.isEmpty()) return "No Batch";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < batches.size(); i++) {
            sb.append(batches.get(i));
            if (i < batches.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }
}