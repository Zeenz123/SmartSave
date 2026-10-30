package com.example.myfirstapp.model;

import java.time.LocalDateTime;

/**
 * Audit log of every virtual financial movement in the system.
 */
public class Transaction {
    public enum Type {
        DEPOSIT,
        TRANSFER,
        WITHDRAWAL,
        LOCK_SAVINGS,
        UNLOCK_SAVINGS,
        COIN_REWARD,
        COIN_REDEEM
    }

    public enum Status {
        SUCCESS,
        FAILED,
        PENDING
    }

    private int id;
    private String transactionId;
    private Integer senderId;
    private Integer recipientId;
    private double amount;
    private Type type;
    private Status status;
    private String description;
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(int id, String transactionId, Integer senderId, Integer recipientId, double amount,
                       Type type, Status status, String description, LocalDateTime createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public Integer getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Integer recipientId) {
        this.recipientId = recipientId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
