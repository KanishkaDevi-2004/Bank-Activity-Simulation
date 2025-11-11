package com.bank.model;

public class Account {

    private final int accountNo;   // Account number is immutable
    private final String name;     // Name is immutable
    private final String email;    // Email is immutable
    private double balance;        // Balance can change

    public Account(int accountNo, String name, String email, double balance) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        this.accountNo = accountNo;
        this.name = name;
        this.email = email;
        this.balance = balance;
    }

    // Getters
    public int getAccountNo() {
        return accountNo;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public double getBalance() {
        return balance;
    }

    // Deposit money, must be positive
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        balance += amount;
    }

    // Withdraw money, returns true if successful, false if insufficient funds
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (amount > balance) {
            return false;
        }
        balance -= amount;
        return true;
    }

    @Override
    public String toString() {
        return "Account No: " + accountNo + ", Name: " + name + ", Email: " + email + ", Balance: ₹" + balance;
    }
}