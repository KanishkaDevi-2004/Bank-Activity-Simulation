package com.bank.core;

import com.bank.db.DBConnection;
import com.bank.model.Transaction;
import org.apache.log4j.Logger;
import com.bank.report.ReportGenerator;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class BankApp {

    private final Scanner sc = new Scanner(System.in);
    private final List<Transaction> transactions = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(BankApp.class);

    public void start() {
        logger.info("===== Bank Activity Simulator Started =====");
        System.out.println("==================================");
        System.out.println("     🏦 Bank Activity Simulator    ");
        System.out.println("==================================");

        while (true) {
            System.out.println("\nChoose an option:");
            System.out.println("1. Create Account");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. View Account ");
            System.out.println("5. Generate Report");
            System.out.println("6. Transfer Money");
            System.out.println("7. Delete Account(s)");
            System.out.println("8. Exit");
            System.out.print("Enter your choice: ");

            int choice = getValidIntInput();

            switch (choice) {
                case 1:
                    createAccount();
                    break;
                case 2:
                    deposit();
                    break;
                case 3:
                    withdraw();
                    break;
                case 4:
                    viewBalance();
                    break;
                case 5:
                    generateReportOption();
                    break;
                case 6:
                    transfer();
                    break;
                case 7:
                    deleteAccountOption();
                    break;
                case 8:
                    logger.info("Bank Simulator exited by user.");
                    System.out.println("Thank you for using Bank Simulator!");
                    return;
                default:
                    logger.warn("Invalid menu choice entered: " + choice);
                    System.out.println("❌ Invalid choice! Try again.");
                    break;
            }

        }
    }

    // Integer Input Validation
    private int getValidIntInput() {
        while (true) {
            if (sc.hasNextInt()) {
                return sc.nextInt();
            } else {
                logger.warn("Invalid integer input detected.");
                System.out.print("❌ Invalid input. Enter a valid number: ");
                sc.next();
            }
        }
    }

    // Positive Double Validation
    private double getValidPositiveDouble() {
        while (true) {
            if (sc.hasNextDouble()) {
                double val = sc.nextDouble();
                if (val > 0) return val;
                else {
                    logger.warn("Non-positive amount entered: " + val);
                    System.out.print("❌ Amount must be positive. Try again: ");
                }
            } else {
                logger.warn("Invalid double input detected.");
                System.out.print("❌ Invalid amount. Enter a number: ");
                sc.next();
            }
        }
    }

    // Create Account
    private void createAccount() {
        try (Connection con = DBConnection.getConnection()) {
            sc.nextLine(); // clear buffer
            String name;
            while (true) {
                System.out.print("Enter your name: ");
                name = sc.nextLine().trim();
                if (name.matches("^[A-Za-z ]+$")) break;
                else logger.warn("Invalid name entered: " + name);
            }

            // Check if account already exists
            String checkSql = "SELECT * FROM accounts WHERE name = ?";
            PreparedStatement checkStmt = con.prepareStatement(checkSql);
            checkStmt.setString(1, name);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                logger.warn("Account creation failed: Account already exists for " + name);
                System.out.println("⚠️ Account already exists for " + name + "!");
                return;
            }

            System.out.print("Enter initial balance: ");
            double balance = getValidPositiveDouble();

            String insertSql = "INSERT INTO accounts (name, balance) VALUES (?, ?)";
            PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setDouble(2, balance);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int accountNo = keys.getInt(1);
                logger.info("Account created successfully: " + accountNo + ", Name: " + name + ", Balance: " + balance);
                System.out.println("✅ Account created successfully! Account No: " + accountNo);
                transactions.add(new Transaction(accountNo, "ACCOUNT_CREATED", balance, LocalDateTime.now()));
            }

        } catch (SQLException e) {
            logger.error("Database error during account creation.", e);
        }
    }

    // Deposit Money
    private void deposit() {
        try (Connection con = DBConnection.getConnection()) {
            System.out.print("Enter account number: ");
            int accNo = getValidIntInput();
            System.out.print("Enter deposit amount: ");
            double amount = getValidPositiveDouble();

            String sql = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setDouble(1, amount);
            ps.setInt(2, accNo);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                logger.info("Deposit successful for Account: " + accNo + ", Amount: " + amount);
                System.out.println("✅ Deposit successful!");
                transactions.add(new Transaction(accNo, "DEPOSIT", amount, LocalDateTime.now()));
            } else {
                logger.warn("Deposit failed: Account not found " + accNo);
                System.out.println("❌ Account not found!");
            }

        } catch (SQLException e) {
            logger.error("Database error during deposit.", e);
        }
    }

    // Withdraw Money
    private void withdraw() {
        try (Connection con = DBConnection.getConnection()) {
            System.out.print("Enter account number: ");
            int accNo = getValidIntInput();
            System.out.print("Enter withdrawal amount: ");
            double amount = getValidPositiveDouble();

            String check = "SELECT balance FROM accounts WHERE account_no = ?";
            PreparedStatement ps1 = con.prepareStatement(check);
            ps1.setInt(1, accNo);
            ResultSet rs = ps1.executeQuery();

            if (rs.next()) {
                double currentBalance = rs.getDouble("balance");
                if (amount > currentBalance) {
                    logger.warn("Withdrawal failed: Insufficient balance for Account " + accNo);
                    System.out.println("❌ Insufficient balance! Your current balance is ₹" + currentBalance);
                    return;
                }

                String update = "UPDATE accounts SET balance = balance - ? WHERE account_no = ?";
                PreparedStatement ps2 = con.prepareStatement(update);
                ps2.setDouble(1, amount);
                ps2.setInt(2, accNo);
                ps2.executeUpdate();

                logger.info("Withdrawal successful: Account " + accNo + ", Amount: " + amount);
                System.out.println("✅ Withdrawal of ₹" + amount + " successful!");
                transactions.add(new Transaction(accNo, "WITHDRAW", amount, LocalDateTime.now()));

            } else {
                logger.warn("Withdrawal failed: Account not found " + accNo);
                System.out.println("❌ Account not found!");
            }

        } catch (SQLException e) {
            logger.error("Database error during withdrawal.", e);
        }
    }

    // View Balance
    // View Balance / Accounts
    private void viewBalance() {
        System.out.println("\nView Account Options:");
        System.out.println("1. View specific account by Account No");
        System.out.println("2. View all accounts");
        System.out.print("Enter your choice: ");
        int choice = getValidIntInput();

        try (Connection con = DBConnection.getConnection()) {

            if (choice == 1) {
                // View specific account
                System.out.print("Enter account number: ");
                int accNo = getValidIntInput();

                String sql = "SELECT account_no, name, balance FROM accounts WHERE account_no = ?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, accNo);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    logger.info("Viewed account: " + accNo);
                    System.out.println("Account No: " + rs.getInt("account_no"));
                    System.out.println("Name: " + rs.getString("name"));
                    System.out.println("Balance: ₹" + rs.getDouble("balance"));
                } else {
                    logger.warn("Account not found: " + accNo);
                    System.out.println("❌ Account not found!");
                }

            } else if (choice == 2) {
                // View all accounts
                String sql = "SELECT account_no, name, balance FROM accounts ORDER BY account_no";
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                System.out.println("\nAll Accounts:");
                System.out.printf("%-12s %-20s %-12s%n", "Account No", "Name", "Balance");
                System.out.println("-----------------------------------------------");

                boolean anyAccount = false;
                while (rs.next()) {
                    anyAccount = true;
                    System.out.printf("%-12d %-20s ₹%-12.2f%n",
                            rs.getInt("account_no"),
                            rs.getString("name"),
                            rs.getDouble("balance"));
                }

                if (!anyAccount) {
                    System.out.println("No accounts found!");
                }

                logger.info("Viewed all accounts");

            } else {
                logger.warn("Invalid view option entered: " + choice);
                System.out.println("❌ Invalid option!");
            }

        } catch (SQLException e) {
            logger.error("Database error during view balance/accounts.", e);
            System.out.println("❌ Error fetching accounts from DB!");
        }
    }


    // Fund Transfer
    private void transfer() {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            System.out.print("Enter Sender Account No: ");
            int senderAcc = getValidIntInput();
            System.out.print("Enter Receiver Account No: ");
            int receiverAcc = getValidIntInput();

            if (senderAcc == receiverAcc) {
                logger.warn("Transfer failed: Sender and receiver are the same account " + senderAcc);
                System.out.println("❌ Sender and receiver accounts cannot be the same!");
                return;
            }

            System.out.print("Enter Transfer Amount: ");
            double amount = getValidPositiveDouble();

            String checkSender = "SELECT balance FROM accounts WHERE account_no = ?";
            PreparedStatement ps1 = con.prepareStatement(checkSender);
            ps1.setInt(1, senderAcc);
            ResultSet rs1 = ps1.executeQuery();
            if (!rs1.next()) {
                logger.warn("Transfer failed: Sender account not found " + senderAcc);
                System.out.println("❌ Sender account not found!");
                return;
            }

            double senderBalance = rs1.getDouble("balance");
            if (senderBalance < amount) {
                logger.warn("Transfer failed: Insufficient funds in sender account " + senderAcc);
                System.out.println("❌ Insufficient funds! Sender balance: ₹" + senderBalance);
                return;
            }

            String checkReceiver = "SELECT * FROM accounts WHERE account_no = ?";
            PreparedStatement ps2 = con.prepareStatement(checkReceiver);
            ps2.setInt(1, receiverAcc);
            ResultSet rs2 = ps2.executeQuery();
            if (!rs2.next()) {
                logger.warn("Transfer failed: Receiver account not found " + receiverAcc);
                System.out.println("❌ Receiver account not found!");
                return;
            }

            String debit = "UPDATE accounts SET balance = balance - ? WHERE account_no = ?";
            String credit = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";

            PreparedStatement psDebit = con.prepareStatement(debit);
            psDebit.setDouble(1, amount);
            psDebit.setInt(2, senderAcc);

            PreparedStatement psCredit = con.prepareStatement(credit);
            psCredit.setDouble(1, amount);
            psCredit.setInt(2, receiverAcc);

            psDebit.executeUpdate();
            psCredit.executeUpdate();
            con.commit();

            logger.info("Transfer successful: ₹" + amount + " from Account " + senderAcc + " to " + receiverAcc);
            System.out.println("✅ Transfer of ₹" + amount + " from Account " + senderAcc + " to Account " + receiverAcc + " successful!");
            transactions.add(new Transaction(senderAcc, "TRANSFER_DEBIT", amount, LocalDateTime.now()));
            transactions.add(new Transaction(receiverAcc, "TRANSFER_CREDIT", amount, LocalDateTime.now()));

        } catch (SQLException e) {
            logger.error("Database error during transfer.", e);
        }
    }

    // Delete Account(s)
    private void deleteAccountOption() {
        System.out.println("\nChoose delete option:");
        System.out.println("1. Delete specific account");
        System.out.println("2. Delete ALL accounts");
        System.out.print("Enter your choice: ");

        int choice = getValidIntInput();

        try (Connection con = DBConnection.getConnection()) {
            if (choice == 1) {
                System.out.print("Enter account number to delete: ");
                int accNo = getValidIntInput();

                String sql = "DELETE FROM accounts WHERE account_no = ?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, accNo);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    logger.info("Deleted account: " + accNo);
                    System.out.println("✅ Account " + accNo + " deleted successfully!");
                    transactions.add(new Transaction(accNo, "ACCOUNT_DELETED", 0, LocalDateTime.now()));
                } else {
                    logger.warn("Delete failed: Account not found " + accNo);
                    System.out.println("❌ Account not found!");
                }

            } else if (choice == 2) {
                System.out.print("⚠️ Are you sure you want to delete ALL accounts? (yes/no): ");
                sc.nextLine();
                String confirm = sc.nextLine().trim().toLowerCase();

                if (confirm.equals("yes")) {
                    String sql = "DELETE FROM accounts";
                    Statement st = con.createStatement();
                    int rows = st.executeUpdate(sql);
                    logger.info("Deleted all accounts, rows affected: " + rows);
                    System.out.println("✅ All accounts deleted successfully! (" + rows + " rows)");
                    transactions.add(new Transaction(0, "ALL_ACCOUNTS_DELETED", 0, LocalDateTime.now()));
                } else {
                    logger.info("Deletion of all accounts cancelled by user.");
                    System.out.println("❎ Deletion cancelled.");
                }
            } else {
                logger.warn("Invalid delete option entered: " + choice);
                System.out.println("❌ Invalid option!");
            }
        } catch (SQLException e) {
            logger.error("Database error during account deletion.", e);
        }
    }

    // Generate Report
    private void generateReportOption() {
        System.out.println("\nChoose report type:");
        System.out.println("1. Transactions for today");
        System.out.println("2. All transactions");
        System.out.print("Enter your choice: ");
        int choice = getValidIntInput();

        if (transactions.isEmpty()) {
            logger.info("No transactions available for report.");
            System.out.println("No transactions available to report!");
            return;
        }

        List<Transaction> filtered = new ArrayList<>();
        if (choice == 1) {
            LocalDateTime today = LocalDateTime.now();
            for (Transaction t : transactions) {
                if (t.getDateTime().toLocalDate().equals(today.toLocalDate())) filtered.add(t);
            }
        } else filtered.addAll(transactions);

        logger.info("Report generated with " + filtered.size() + " transactions.");
        ReportGenerator.generateReport(filtered);
    }

    public static void main(String[] args) {
        BankApp app = new BankApp();
        app.start();
    }
}
