package com.example.myfirstapp.model;

import java.time.LocalDateTime;

/**
 * Represents a guardian or trusted close friend who can grant emergency release approval.
 */
public class Guardian {
    private int id;
    private int userId;
    private Integer guardianUserId;
    private String name;
    private String relationship; // "Parent", "Close Friend"
    private String status;
    private LocalDateTime createdAt;

    public Guardian() {
    }

    public Guardian(int id, int userId, Integer guardianUserId, String name, String relationship, String status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.guardianUserId = guardianUserId;
        this.name = name;
        this.relationship = relationship;
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

    public Integer getGuardianUserId() {
        return guardianUserId;
    }

    public void setGuardianUserId(Integer guardianUserId) {
        this.guardianUserId = guardianUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
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
}
