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
        System.out.println("==================================");
        System.out.println("     🏦 Bank Activity Simulator    ");
        System.out.println("==================================");

        while (true) {
            try {
                System.out.println("\nChoose an option:");
                System.out.println("1. Login");
                System.out.println("2. Create Account");
                System.out.println("3. Exit");
                System.out.print("Enter your choice: ");

                int choice = InputValidator.getValidIntInput(sc);

                switch (choice) {
                    case 1:
                        loginUser();
                        break;
                    case 2:
                        createNewUser();
                        break;
                    case 3:
                        System.out.println("👋 Thank you for using Bank Simulator!");
                        logger.info("User exited the simulator.");
                        return;
                    default:
                        System.out.println("❌ Invalid choice! Please enter a valid option (1–3).");
                }

            } catch (InputMismatchException e) {
                System.out.println("⚠️ Invalid input type. Please enter numbers only for menu options.");
                sc.nextLine();
                logger.warn("Invalid input type by user", e);
            } catch (DatabaseException e) {
                System.out.println("⚠️ Something went wrong while connecting to the system. Please try again later.");
                logger.error("Database Exception", e);
            } catch (Exception e) {
                System.out.println("⚠️ Unexpected error: Please check your input and try again.");
                logger.error("Unexpected Exception", e);
            }
        }
    }

    private void loginUser() throws DatabaseException {
        System.out.print("Enter Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = sc.nextLine().trim();

        if (authService.login(name, password)) {
            System.out.println("✅ Login successful! Welcome, " + name + ".");
            showUserMenu();
        } else {
            System.out.println("❌ Invalid name or password. Please try again.");
        }
    }


    private void createNewUser() throws DatabaseException {
        while (true) {
            try {


                // --- NAME ---
                System.out.print("Enter Name (letters only): ");
                String name = sc.nextLine().trim();
                if (!name.matches("[a-zA-Z ]+")) {
                    System.out.println("⚠️ Invalid name! Only alphabets and spaces are allowed. Try again.");
                    logger.warn("Invalid name input: {}", name);
                    continue;
                }

                // --- PASSWORD ---
                System.out.print("Enter Password: ");
                String password = sc.nextLine().trim();

                // --- EMAIL ---
                String email;
                while (true) {
                    System.out.print("Enter Email: ");
                    email = sc.nextLine().trim();

                    if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                        System.out.println("⚠️ Invalid email format! Try again.");
                        logger.warn("Invalid email input: {}", email);
                        continue;
                    }
                    break;
                }
                double balance = 0.0;
                while (true) {
                    System.out.print("Enter Initial Balance (min ₹100): ");
                    String balanceInput = sc.nextLine().trim();

                    try {
                        balance = Double.parseDouble(balanceInput);
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Invalid input. Please enter a numeric value.");
                        logger.warn("Non-numeric balance input: {}", balanceInput);
                        continue;
                    }

                    if (balance < 100) {
                        System.out.println("⚠️ Minimum balance should be ₹100. Try again.");
                        logger.warn("Balance below minimum: {}", balance);
                        continue;
                    }
                    break;
                }

                // --- CREATE ACCOUNT ---
                logger.info("Attempting to create account for user: {}", name);
                int accountId = accountService.createAccount(name, password, email, balance);

                if (accountId != -1) {
                    System.out.println("🎉 Welcome, " + name + "! Your Account Number is: " + accountId);
                    logger.info("Account created successfully for user: {} with ID: {}", name, accountId);
                    loginUser(); // directly log user in after creation
                } else {
                    System.out.println("⚠️ Account creation failed. Please try again.");
                    logger.error("Account creation failed for user: {}", name);
                }

                break; // success — exit loop

            } catch (DatabaseException e) {
                System.out.println("⚠️ Database issue occurred while creating account. Try again later.");
                logger.error("Database Exception during account creation", e);
                break; // stop retrying on DB issues

            } catch (Exception e) {
                System.out.println("⚠️ Unexpected error: " + e.getMessage());
                logger.error("Unexpected exception during account creation", e);
            }
        }
    }



    private void showUserMenu() throws DatabaseException {
        while (true) {
            System.out.println("\n--- Banking Options ---");
            System.out.println("1. Deposit Money");
            System.out.println("2. Withdraw Money");
            System.out.println("3. View Account");
            System.out.println("4. Generate Report");
            System.out.println("5. Transfer Money");
            System.out.println("6. Delete Account");
            System.out.println("7. Logout");
            System.out.print("Enter your choice: ");

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
                        accountService.deleteAccountMenu(sc); // Show delete menu (specific/all)
                        break;

                    case 7:
                        System.out.println("🔒 Logged out successfully.");
                        return;
                    default:
                        System.out.println("❌ Invalid choice! Please enter a valid option (1–7).");
                }

            } catch (InputMismatchException e) {
                System.out.println("⚠️ Please enter a valid number for the menu option.");
                sc.nextLine();
                logger.warn("Invalid numeric input from user", e);
            } catch (DatabaseException e) {
                System.out.println("⚠️ Database issue occurred. Please try again.");
                logger.error("Database error in user menu", e);
            } catch (Exception e) {
                System.out.println("⚠️ Something went wrong. Please check your input and try again.");
                logger.error("Unexpected exception in user menu", e);
            }
        }
    }

    public static void main(String[] args) {
        new BankApp().start();
    }
}
