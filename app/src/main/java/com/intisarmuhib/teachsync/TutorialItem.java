package com.intisarmuhib.teachsync;

public class TutorialItem {
    private String title;
    private String description;
    private String videoUrl;
    private int iconResId;

    public TutorialItem(String title, String description, String videoUrl, int iconResId) {
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.iconResId = iconResId;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getVideoUrl() { return videoUrl; }
    public int getIconResId() { return iconResId; }
}