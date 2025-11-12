package com.bank.service;

import com.bank.db.DBConnection;
import com.bank.exception.*;
import com.bank.model.Transaction;
import com.bank.alert.EmailService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class TransactionService {

    private static final Logger logger = LogManager.getLogger(TransactionService.class);
    private static final List<Transaction> transactions = new ArrayList<>();

    // ✅ For ReportService
    public List<Transaction> getTransactions() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT id, sender_account, receiver_account, amount, transaction_type, message, created_at " +
                "FROM transactions ORDER BY created_at DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Transaction t = new Transaction(
                        rs.getInt("id"),
                        (Integer) rs.getObject("sender_account"),
                        (Integer) rs.getObject("receiver_account"),
                        rs.getDouble("amount"),
                        rs.getString("transaction_type"),
                        rs.getString("message"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                list.add(t);
            }
            logger.info("Fetched {} transactions from database.", list.size());

        } catch (Exception e) {
            logger.error("Error fetching transactions: {}", e.getMessage(), e);
        }
        return list;
    }

    // ✅ Global in-memory list
    public static List<Transaction> getGlobalTransactionList() {
        return transactions;
    }

    // ✅ Log transaction to DB
    public void logTransaction(Connection con, Integer fromAcc, Integer toAcc, double amount,
                               String type, String message) {
        String sql = "INSERT INTO transactions (sender_account, receiver_account, amount, transaction_type, message) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fromAcc);
            ps.setObject(2, toAcc);
            ps.setDouble(3, amount);
            ps.setString(4, type);
            ps.setString(5, message);
            ps.executeUpdate();

            transactions.add(new Transaction(null, fromAcc, toAcc, amount, type, message, LocalDateTime.now()));
            logger.info("Transaction logged | Type: {} | From: {} | To: {} | Amount: {} | Message: {}",
                    type, fromAcc, toAcc, amount, message);
        } catch (Exception e) {
            logger.error("Failed to log transaction: {}", e.getMessage(), e);
        }
    }

    // ✅ Withdraw helper
    private boolean debitAccount(Connection con, int accNo, double amount) throws Exception {
        String sql = "SELECT balance, email, name FROM accounts WHERE account_no = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, accNo);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            logger.warn("Attempted to debit non-existent account: {}", accNo);
            throw new AccountNotFoundException("Account not found!");
        }

        double balance = rs.getDouble("balance");
        String email = rs.getString("email");
        String name = rs.getString("name");

        if (balance - amount < 100) {
            logger.warn("Minimum balance violation for account {}. Current balance: {}", accNo, balance);

            if (email != null && !email.isEmpty()) {
                new EmailService().sendEmail(
                        email,
                        "⚠ Low Balance Alert",
                        "Dear " + name + ",\n" +
                                "Your withdrawal failed due to insufficient balance.\n" +
                                "Current Balance: ₹" + balance
                );
                logger.info("Low balance alert email sent to {}", email);
            }
            return false;
        }

        PreparedStatement upd = con.prepareStatement(
                "UPDATE accounts SET balance = balance - ? WHERE account_no = ?"
        );
        upd.setDouble(1, amount);
        upd.setInt(2, accNo);
        upd.executeUpdate();
        logger.debug("Account {} debited ₹{}", accNo, amount);
        return true;
    }

    // ✅ Deposit
    public void deposit(Scanner sc) {
        try (Connection con = DBConnection.getConnection()) {
            logger.info("Deposit operation initiated.");
            logger.info("Awaiting account number input...");
            int accNo = InputValidator.getValidIntInput(sc);

            logger.info("Awaiting deposit amount input...");
            double amt = InputValidator.getValidPositiveDouble(sc);

            PreparedStatement ps = con.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE account_no = ?"
            );
            ps.setDouble(1, amt);
            ps.setInt(2, accNo);

            if (ps.executeUpdate() == 0) {
                logger.warn("Deposit failed. Account not found: {}", accNo);
                throw new AccountNotFoundException("Account Not Found!");
            }

            logTransaction(con, accNo, null, amt, "DEPOSIT", "Deposit successful");
            logger.info("Deposit successful for account {}. Amount: ₹{}", accNo, amt);

        } catch (Exception e) {
            logger.error("Deposit failed: {}", e.getMessage(), e);
        }
    }

    // ✅ Withdraw
    public void withdraw(Scanner sc) {
        try (Connection con = DBConnection.getConnection()) {
            logger.info("Withdrawal operation initiated.");
            int accNo = InputValidator.getValidIntInput(sc);
            double amt = InputValidator.getValidPositiveDouble(sc);

            if (debitAccount(con, accNo, amt)) {
                logTransaction(con, accNo, null, amt, "WITHDRAW", "Withdrawal successful");
                logger.info("Withdrawal successful for account {}. Amount: ₹{}", accNo, amt);
            } else {
                logTransaction(con, accNo, null, amt, "WITHDRAW", "Insufficient balance");
                logger.warn("Withdrawal failed due to insufficient balance for account {}", accNo);
            }

        } catch (Exception e) {
            logger.error("Withdrawal failed: {}", e.getMessage(), e);
        }
    }

    // ✅ Transfer
    public void transfer(Scanner sc) {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            logger.info("Transfer operation initiated.");

            int sender = InputValidator.getValidIntInput(sc);
            int receiver = InputValidator.getValidIntInput(sc);
            double amt = InputValidator.getValidPositiveDouble(sc);

            if (sender == receiver) {
                logger.warn("Transfer attempt between same accounts: {}", sender);
                return;
            }

            if (!debitAccount(con, sender, amt)) {
                logTransaction(con, sender, receiver, amt, "TRANSFER", "Insufficient balance");
                con.rollback();
                logger.warn("Transfer aborted due to insufficient balance in account {}", sender);
                return;
            }

            PreparedStatement credit = con.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE account_no = ?"
            );
            credit.setDouble(1, amt);
            credit.setInt(2, receiver);

            if (credit.executeUpdate() == 0) {
                con.rollback();
                logger.warn("Receiver account not found: {}", receiver);
                throw new AccountNotFoundException("Receiver Account Not Found!");
            }

            con.commit();
            logTransaction(con, sender, receiver, amt, "TRANSFER", "Transfer successful");
            logger.info("Transfer successful. From: {} To: {} Amount: ₹{}", sender, receiver, amt);

        } catch (Exception e) {
            logger.error("Transfer failed: {}", e.getMessage(), e);
        }
    }
}
