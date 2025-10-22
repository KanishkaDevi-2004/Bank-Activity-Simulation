package com.bank.model;

import java.time.LocalDateTime;

public class Transaction {
    private int accountNo;
    private String type;
    private double amount;
    private LocalDateTime dateTime;

    public Transaction(int accountNo, String type, double amount, LocalDateTime dateTime) {
        this.accountNo = accountNo;
        this.type = type;
        this.amount = amount;
        this.dateTime = dateTime;
    }

    public int getAccountNo() { return accountNo; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public LocalDateTime getDateTime() { return dateTime; }

    @Override
    public String toString() {
        return "Transaction{" +
                "Account=" + accountNo +
                ", Type='" + type + '\'' +
                ", Amount=" + amount +
                ", Date=" + dateTime +
                '}';
    }
}
