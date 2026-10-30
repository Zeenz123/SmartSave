package com.example.myfirstapp.model;

import java.time.LocalDateTime;

/**
 * Educational task model promoting self-discipline and knowledge acquisition.
 */
public class EducationalTask {
    private int id;
    private int userId;
    private String taskKey;
    private String title;
    private String category;
    private String resourceUrl;
    private int minSeconds;
    private int timeSpentSeconds;
    private boolean completed;
    private double coinsRewarded;
    private LocalDateTime completedAt;

    public EducationalTask() {
    }

    public EducationalTask(int id, int userId, String taskKey, String title, String category,
                           String resourceUrl, int minSeconds, int timeSpentSeconds,
                           boolean completed, double coinsRewarded, LocalDateTime completedAt) {
        this.id = id;
        this.userId = userId;
        this.taskKey = taskKey;
        this.title = title;
        this.category = category;
        this.resourceUrl = resourceUrl;
        this.minSeconds = minSeconds;
        this.timeSpentSeconds = timeSpentSeconds;
        this.completed = completed;
        this.coinsRewarded = coinsRewarded;
        this.completedAt = completedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public int getMinSeconds() {
        return minSeconds;
    }

    public void setMinSeconds(int minSeconds) {
        this.minSeconds = minSeconds;
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(int timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public double getCoinsRewarded() {
        return coinsRewarded;
    }

    public void setCoinsRewarded(double coinsRewarded) {
        this.coinsRewarded = coinsRewarded;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
