package com.intisarmuhib.teachsync;

public class SubjectModel {

    private String id;
    private String name;
    private String code;
    private String teacher;

    public SubjectModel() {} // Required for Firestore

    public SubjectModel(String id, String name, String code, String teacher) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.teacher = teacher;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getTeacher() { return teacher; }

    public void setId(String id) { this.id = id; }
}