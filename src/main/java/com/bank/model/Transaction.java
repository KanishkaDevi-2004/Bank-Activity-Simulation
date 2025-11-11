package com.bank.model;

import java.time.LocalDateTime;

public class Transaction {

    private final Integer id;
    private final Integer senderAccount;
    private final Integer receiverAccount;
    private final double amount;
    private final String transactionType;
    private final String message;
    private final LocalDateTime createdAt;

    public Transaction(
            Integer id,
            Integer senderAccount,
            Integer receiverAccount,
            double amount,
            String transactionType,
            String message,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.amount = amount;
        this.transactionType = transactionType;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public Integer getSenderAccount() { return senderAccount; }
    public Integer getReceiverAccount() { return receiverAccount; }
    public double getAmount() { return amount; }
    public String getTransactionType() { return transactionType; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", sender=" + senderAccount +
                ", receiver=" + receiverAccount +
                ", amount=" + amount +
                ", type='" + transactionType + '\'' +
                ", message='" + message + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
