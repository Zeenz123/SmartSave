package com.example.myfirstapp.model;

import java.time.LocalDateTime;

/**
 * Represents an early withdrawal approval request (Parent, Friend, Medical AI, Task, Release Coin).
 */
public class ApprovalRequest {
    public enum Type {
        PARENT,
        FRIEND,
        MEDICAL,
        TASK,
        COIN,
        TARGET_CHANGE
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    private int id;
    private int userId;
    private Type approvalType;
    private Status status;
    private String requesterNotes;
    private String verificationData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ApprovalRequest() {
    }

    public ApprovalRequest(int id, int userId, Type approvalType, Status status, String requesterNotes,
                           String verificationData, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.approvalType = approvalType;
        this.status = status;
        this.requesterNotes = requesterNotes;
        this.verificationData = verificationData;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Type getApprovalType() {
        return approvalType;
    }

    public void setApprovalType(Type approvalType) {
        this.approvalType = approvalType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getRequesterNotes() {
        return requesterNotes;
    }

    public void setRequesterNotes(String requesterNotes) {
        this.requesterNotes = requesterNotes;
    }

    public String getVerificationData() {
        return verificationData;
    }

    public void setVerificationData(String verificationData) {
        this.verificationData = verificationData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
