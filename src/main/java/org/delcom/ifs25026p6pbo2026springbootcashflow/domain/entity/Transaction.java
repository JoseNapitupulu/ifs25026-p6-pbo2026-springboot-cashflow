package org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity;

import java.time.LocalDateTime;

public class Transaction {
    private final int id;
    private final int userId;
    private final String description;
    private final long amount;
    private final TransactionType type;
    private final String proofPath;
    private final LocalDateTime createdAt;

    public Transaction(
            int id,
            int userId,
            String description,
            long amount,
            TransactionType type,
            String proofPath,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.proofPath = proofPath;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getDescription() { return description; }
    public long getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public String getProofPath() { return proofPath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
