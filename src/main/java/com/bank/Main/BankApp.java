package com.bank.Main;

import com.bank.exception.DatabaseException;
import com.bank.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.InputMismatchException;
import java.util.Scanner;

public class BankApp {
    private static final Logger logger = LogManager.getLogger(BankApp.class);

    private final Scanner sc = new Scanner(System.in);
    private final AccountService accountService = new AccountService();
    private final TransactionService transactionService = new TransactionService();
    private final AuthService authService = new AuthService();

    public void start() {
        logger.info("===== Bank Activity Simulator Started =====");
        logger.info("==================================");
        logger.info("     🏦 Bank Activity Simulator    ");
        logger.info("==================================");

        while (true) {
            try {
                logger.info("\nChoose an option:");
                logger.info("1. Login");
                logger.info("2. Create Account");
                logger.info("3. Exit");
                logger.info("Enter your choice: ");

                int choice = InputValidator.getValidIntInput(sc);

                switch (choice) {
                    case 1:
                        loginUser();
                        break;
                    case 2:
                        createNewUser();
                        break;
                    case 3:
                        logger.info("👋 Thank you for using Bank Simulator!");
                        logger.info("User exited the simulator.");
                        return;
                    default:
                        logger.warn("❌ Invalid choice! Please enter a valid option (1–3).");
                        break;
                }

            } catch (InputMismatchException e) {
                logger.warn("⚠️ Invalid input type. Please enter numbers only for menu options.", e);
                sc.nextLine();
            } catch (DatabaseException e) {
                logger.error("⚠️ Database Exception while processing user request.", e);
            } catch (Exception e) {
                logger.error("⚠️ Unexpected Exception occurred.", e);
            }
        }
    }

    private void loginUser() throws DatabaseException {
        logger.info("Enter Name: ");
        String name = sc.nextLine().trim();
        logger.info("Enter Password: ");
        String password = sc.nextLine().trim();

        if (authService.login(name, password)) {
            logger.info("✅ Login successful! Welcome, {}", name);
            showUserMenu();
        } else {
            logger.warn("❌ Invalid name or password attempt for user: {}", name);
        }
    }

    private void createNewUser() throws DatabaseException {
        while (true) {
            try {
                logger.info("Enter Name (letters only): ");
                String name = sc.nextLine().trim();
                if (!name.matches("[a-zA-Z ]+")) {
                    logger.warn("⚠️ Invalid name! Only alphabets and spaces are allowed. Input: {}", name);
                    continue;
                }

                logger.info("Enter Password: ");
                String password = sc.nextLine().trim();

                String email;
                while (true) {
                    logger.info("Enter Email: ");
                    email = sc.nextLine().trim();

                    if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                        logger.warn("⚠️ Invalid email format! Input: {}", email);
                        continue;
                    }
                    break;
                }

                double balance = 0.0;
                while (true) {
                    logger.info("Enter Initial Balance (min ₹100): ");
                    String balanceInput = sc.nextLine().trim();

                    try {
                        balance = Double.parseDouble(balanceInput);
                    } catch (NumberFormatException e) {
                        logger.warn("❌ Invalid input. Please enter a numeric value. Input: {}", balanceInput);
                        continue;
                    }

                    if (balance < 100) {
                        logger.warn("⚠️ Minimum balance should be ₹100. Entered: {}", balance);
                        continue;
                    }
                    break;
                }

                logger.info("Attempting to create account for user: {}", name);
                int accountId = accountService.createAccount(name, password, email, balance);

                if (accountId != -1) {
                    logger.info("🎉 Account created successfully for user: {} | Account Number: {}", name, accountId);
                    loginUser(); // directly log user in after creation
                } else {
                    logger.error("⚠️ Account creation failed for user: {}", name);
                }

                break;

            } catch (DatabaseException e) {
                logger.error("⚠️ Database issue occurred while creating account.", e);
                break;
            } catch (Exception e) {
                logger.error("⚠️ Unexpected exception during account creation.", e);
            }
        }
    }

    private void showUserMenu() throws DatabaseException {
        while (true) {
            logger.info("\n--- Banking Options ---");
            logger.info("1. Deposit Money");
            logger.info("2. Withdraw Money");
            logger.info("3. View Account");
            logger.info("4. Generate Report");
            logger.info("5. Transfer Money");
            logger.info("6. Delete Account");
            logger.info("7. Logout");
            logger.info("Enter your choice: ");

            try {
                int choice = InputValidator.getValidIntInput(sc);

                switch (choice) {
                    case 1:
                        transactionService.deposit(sc);
                        break;
                    case 2:
                        transactionService.withdraw(sc);
                        break;
                    case 3:
                        accountService.viewAccounts(sc);
                        break;
                    case 4:
                        ReportService reportService = new ReportService(transactionService);
                        reportService.generateReportOption(sc);
                        break;
                    case 5:
                        transactionService.transfer(sc);
                        break;
                    case 6:
                        accountService.deleteAccountMenu(sc);
                        break;
                    case 7:
                        logger.info("🔒 Logged out successfully.");
                        return;
                    default:
                        logger.warn("❌ Invalid choice! Please enter a valid option (1–7).");
                        break;
                }

            } catch (InputMismatchException e) {
                logger.warn("⚠️ Please enter a valid number for the menu option.", e);
                sc.nextLine();
            } catch (DatabaseException e) {
                logger.error("⚠️ Database issue occurred in user menu.", e);
            } catch (Exception e) {
                logger.error("⚠️ Unexpected exception in user menu.", e);
            }
        }
    }

    public static void main(String[] args) {
        new BankApp().start();
    }
}
