package com.bank.service;

import com.bank.db.DBConnection;
import com.bank.exception.*;
import com.bank.model.Transaction;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.bank.alert.EmailService;

import java.sql.*;
import java.util.List;
import java.util.Scanner;

public class AccountService {
    private static final Logger logger = LogManager.getLogger(AccountService.class);
    private final List<Transaction> transactions = TransactionService.getGlobalTransactionList();
    private final TransactionService transactionService = new TransactionService();
    private final EmailService emailService = new EmailService();



    // ✅ Create Account (DB part unchanged)
    public int createAccount(String name, String password, String email, double balance) throws DatabaseException {
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement checkEmail = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE email = ?");
            checkEmail.setString(1, email);
            ResultSet rs = checkEmail.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                System.out.println("⚠️ This email is already registered. Please use a different one.");
                return -1;
            }

            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO accounts (name, password, email, balance) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.setString(2, password);
            ps.setString(3, email);
            ps.setDouble(4, balance);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int accNo = keys.getInt(1);
                System.out.println("✅ Account created successfully! Account Number: " + accNo);
                logger.info("Account created successfully: {}", accNo);
                return accNo;
            } else {
                throw new DatabaseException("Failed to retrieve generated account number.");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("⚠️ This email is already registered. Please use a different one.");
            return -1;
        } catch (SQLException e) {
            throw new DatabaseException("⚠️ Could not create account. Please try again later.", e);
        }
    }
    // ✅ View Accounts
    public void viewAccounts(Scanner sc) {
        try (Connection con = DBConnection.getConnection()) {
            System.out.println("\n1. View a Single Account");
            System.out.println("2. View All Accounts");
            System.out.print("Enter choice: ");
            int choice = InputValidator.getValidIntInput(sc);

            if (choice == 1) {
                System.out.print("Enter Account Number: ");
                int accNo = InputValidator.getValidIntInput(sc);

                PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE account_no = ?");
                ps.setInt(1, accNo);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    System.out.println("\n✅ Account Details");
                    System.out.println("----------------------------------");
                    System.out.println("Account No: " + rs.getInt("account_no"));
                    System.out.println("Name       : " + rs.getString("name"));
                    System.out.println("Email      : " + rs.getString("email"));
                    System.out.println("Balance    : ₹" + rs.getDouble("balance"));
                } else {
                    System.out.println("❌ Account not found!");
                }

            } else if (choice == 2) {
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM accounts");

                System.out.println("\n📋 All Accounts List");
                System.out.println("----------------------------------");

                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.println(
                            "Acc No: " + rs.getInt("account_no") +
                                    " | Name: " + rs.getString("name") +
                                    " | Email: " + rs.getString("email") +
                                    " | Balance: ₹" + rs.getDouble("balance")
                    );
                }

                if (!found) System.out.println("⚠ No accounts found.");
            } else {
                System.out.println("❌ Invalid option!");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error viewing accounts", e);
        }
    }

    // ✅ Delete Menu
    public void deleteAccountMenu(Scanner sc) {
        try (Connection conn = DBConnection.getConnection()) {

            System.out.println("\n--- Delete Account ---");
            System.out.println("1. Delete Specific Account");
            System.out.println("2. Delete All Accounts");
            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();

            if (choice == 1) {
                System.out.print("Enter Account Number to Delete: ");
                int accountNo = sc.nextInt();
                deleteAccount(accountNo);
            } else if (choice == 2) {
                deleteAllAccounts();
                // 🧠 After deleting all, return to main menu
                System.out.println("Returning to main menu...");
                logger.info("All accounts deleted. Returning to main menu.");
                return;
            } else {
                System.out.println("⚠️ Invalid choice! Please select 1 or 2.");
                logger.warn("Invalid delete menu choice entered: {}", choice);
            }

        } catch (SQLException e) {
            System.out.println("❌ Error while deleting account: " + e.getMessage());
            logger.error("SQL error while deleting account", e);
        }
    }

    // ✅ Delete specific account
    public void deleteAccount(int accountNumber) {
        String sql = "DELETE FROM accounts WHERE account_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, accountNumber);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Account deleted successfully.");
                logger.info("Account {} deleted successfully.", accountNumber);
            } else {
                System.out.println("⚠️ Account not found.");
                logger.warn("Attempted to delete non-existing account: {}", accountNumber);
            }

        } catch (SQLException e) {
            System.out.println("❌ Error deleting account: " + e.getMessage());
            logger.error("Error deleting account {}: {}", accountNumber, e.getMessage());
        }
    }

    // ✅ Delete all accounts and reset numbering
    public void deleteAllAccounts() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            int rows = stmt.executeUpdate("DELETE FROM accounts");
            if (rows > 0) {
                System.out.println("✅ All accounts deleted successfully.");
                logger.info("{} accounts deleted from database.", rows);
            } else {
                System.out.println("⚠️ No accounts found to delete.");
                logger.warn("No accounts found when trying to delete all.");
            }

            // Reset auto-increment
            stmt.executeUpdate("ALTER TABLE accounts AUTO_INCREMENT = 1");
            System.out.println("🔄 Account numbering has been reset to start from 1.");
            logger.info("Account numbering reset to 1 after deleting all accounts.");

        } catch (SQLException e) {
            System.out.println("❌ Error deleting all accounts: " + e.getMessage());
            logger.error("Error deleting all accounts: {}", e.getMessage());
        }
    }

    // ✅ Deposit Money
    public void depositMoney(Scanner sc) {
        System.out.print("Enter Account Number: ");
        int acc = InputValidator.getValidIntInput(sc);

        System.out.print("Enter Amount: ");
        double amt = InputValidator.getValidPositiveDouble(sc);

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE account_no = ?");
            ps.setDouble(1, amt);
            ps.setInt(2, acc);

            int rows = ps.executeUpdate();
            if (rows == 0) throw new InvalidInputException("Account not found");

            con.commit();
            System.out.println("✅ Amount Deposited Successfully!");
            transactionService.logTransaction(con, acc, null, amt, "DEPOSIT", "Deposit successful");
        } catch (Exception e) {
            System.out.println("❌ Deposit Failed: " + e.getMessage());
        }
    }

    // ✅ Withdraw Money
    public void withdrawMoney(Scanner sc) {
        System.out.print("Enter Account Number: ");
        int acc = InputValidator.getValidIntInput(sc);

        System.out.print("Enter Amount: ");
        double amt = InputValidator.getValidPositiveDouble(sc);

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            ResultSet rs = con.prepareStatement("SELECT balance, email, name FROM accounts WHERE account_no = " + acc).executeQuery();
            if (!rs.next()) throw new InvalidInputException("Account not found");

            double balance = rs.getDouble("balance");
            String email = rs.getString("email");
            String name = rs.getString("name");

            if (balance - amt < 100) {
                System.out.println("❌ Minimum balance ₹100 must remain.");
                transactionService.logTransaction(con, acc, null, amt, "WITHDRAW", "Minimum balance rule violated");
                return;
            }

            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE account_no = ?");
            ps.setDouble(1, amt);
            ps.setInt(2, acc);
            ps.executeUpdate();

            con.commit();
            System.out.println("✅ Withdrawal Successful!");

            transactionService.logTransaction(con, acc, null, amt, "WITHDRAW", "Withdrawal successful");

            if (balance - amt < 200) {
                emailService.sendEmail(email, "⚠ Low Balance Alert",
                        "Dear " + name + ", your account balance is low. Maintain minimum ₹100.");
            }
        } catch (Exception e) {
            System.out.println("❌ Withdrawal Failed: " + e.getMessage());
        }
    }

    // ✅ Transfer Money
    public void transferMoney(Scanner sc) {
        System.out.print("Enter Sender Account: ");
        int sender = InputValidator.getValidIntInput(sc);

        System.out.print("Enter Receiver Account: ");
        int receiver = InputValidator.getValidIntInput(sc);

        System.out.print("Enter Amount: ");
        double amt = InputValidator.getValidPositiveDouble(sc);

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            ResultSet rs = con.prepareStatement("SELECT balance FROM accounts WHERE account_no = " + sender).executeQuery();
            if (!rs.next()) throw new InvalidInputException("Sender account not found");
            double bal = rs.getDouble(1);

            if (bal - amt < 100) {
                System.out.println("❌ Minimum balance ₹100 must remain.");
                return;
            }

            con.prepareStatement("UPDATE accounts SET balance = balance - " + amt + " WHERE account_no = " + sender).executeUpdate();
            con.prepareStatement("UPDATE accounts SET balance = balance + " + amt + " WHERE account_no = " + receiver).executeUpdate();

            con.commit();
            System.out.println("✅ Transfer Successful!");

            transactionService.logTransaction(con, sender, receiver, amt, "TRANSFER", "Transfer successful");
        } catch (Exception e) {
            System.out.println("❌ Transfer Failed: " + e.getMessage());
        }
    }
}
