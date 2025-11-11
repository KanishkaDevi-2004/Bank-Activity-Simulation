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

        } catch (Exception e) {
            logger.error("❌ Error fetching transactions: {}", e.getMessage());
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

            // ✅ In-memory list (no status)
            transactions.add(new Transaction(
                    null, fromAcc, toAcc, amount, type, message, LocalDateTime.now()
            ));

            logger.info("✅ Transaction Logged | {} | From:{} | To:{} | Amount:{} | Msg:{}",
                    type, fromAcc, toAcc, amount, message);

        } catch (Exception e) {
            logger.error("❌ Log Transaction Failed: {}", e.getMessage());
        }
    }

    // ✅ Withdraw helper
    private boolean debitAccount(Connection con, int accNo, double amount) throws Exception {
        String sql = "SELECT balance, email, name FROM accounts WHERE account_no = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, accNo);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) throw new AccountNotFoundException("Account not found!");

        double balance = rs.getDouble("balance");
        String email = rs.getString("email");
        String name = rs.getString("name");

        if (balance - amount < 100) {
            System.out.println("❌ Minimum balance ₹100 must remain!");

            if (email != null && !email.isEmpty()) {
                new EmailService().sendEmail(
                        email,
                        "⚠ Low Balance Alert",
                        "Dear " + name + ",\n" +
                                "Your withdrawal failed due to insufficient balance.\n" +
                                "Current Balance: ₹" + balance
                );
            }
            return false;
        }

        PreparedStatement upd = con.prepareStatement(
                "UPDATE accounts SET balance = balance - ? WHERE account_no = ?"
        );
        upd.setDouble(1, amount);
        upd.setInt(2, accNo);
        upd.executeUpdate();

        return true;
    }

    // ✅ Deposit
    public void deposit(Scanner sc) {
        try (Connection con = DBConnection.getConnection()) {
            System.out.print("Enter Account No: ");
            int accNo = InputValidator.getValidIntInput(sc);

            System.out.print("Enter Deposit Amount: ");
            double amt = InputValidator.getValidPositiveDouble(sc);

            PreparedStatement ps = con.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE account_no = ?"
            );
            ps.setDouble(1, amt);
            ps.setInt(2, accNo);

            if (ps.executeUpdate() == 0) throw new AccountNotFoundException("Account Not Found!");

            System.out.println("✅ Deposit Successful!");
            logTransaction(con, accNo, null, amt, "DEPOSIT", "Deposit successful");

        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    // ✅ Withdraw
    public void withdraw(Scanner sc) {
        try (Connection con = DBConnection.getConnection()) {
            System.out.print("Enter Account No: ");
            int accNo = InputValidator.getValidIntInput(sc);

            System.out.print("Enter Withdrawal Amount: ");
            double amt = InputValidator.getValidPositiveDouble(sc);

            if (debitAccount(con, accNo, amt)) {
                System.out.println("✅ Withdrawal Successful!");
                logTransaction(con, accNo, null, amt, "WITHDRAW", "Withdrawal successful");
            } else {
                logTransaction(con, accNo, null, amt, "WITHDRAW", "Insufficient balance");
            }

        } catch (Exception e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    // ✅ Transfer
    public void transfer(Scanner sc) {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            System.out.print("Enter Sender Account No: ");
            int sender = InputValidator.getValidIntInput(sc);

            System.out.print("Enter Receiver Account No: ");
            int receiver = InputValidator.getValidIntInput(sc);

            if (sender == receiver) {
                System.out.println("❌ Cannot transfer to same account!");
                return;
            }

            System.out.print("Enter Amount: ");
            double amt = InputValidator.getValidPositiveDouble(sc);

            if (!debitAccount(con, sender, amt)) {
                logTransaction(con, sender, receiver, amt, "TRANSFER", "Insufficient balance");
                con.rollback();
                return;
            }

            PreparedStatement credit = con.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE account_no = ?"
            );
            credit.setDouble(1, amt);
            credit.setInt(2, receiver);

            if (credit.executeUpdate() == 0)
                throw new AccountNotFoundException("Receiver Account Not Found!");

            con.commit();
            System.out.println("✅ Transfer Successful!");
            logTransaction(con, sender, receiver, amt, "TRANSFER", "Transfer successful");

        } catch (Exception e) {
            System.out.println("❌ Transfer Failed: " + e.getMessage());
        }
    }
}
