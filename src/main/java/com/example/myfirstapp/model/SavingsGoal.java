package com.example.myfirstapp.model;

import java.time.LocalDateTime;

/**
 * Represents a personalized savings goal where funds are locked.
 */
public class SavingsGoal {
    private int id;
    private int userId;
    private String title;
    private double targetAmount;
    private double currentAmount;
    private boolean locked;
    private String status;
    private LocalDateTime createdAt;

    public SavingsGoal() {
    }

    public SavingsGoal(int id, int userId, String title, double targetAmount, double currentAmount,
                       boolean locked, String status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.locked = locked;
        this.status = status;
        this.createdAt = createdAt;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isGoalReached() {
        return currentAmount >= targetAmount;
    }
}
