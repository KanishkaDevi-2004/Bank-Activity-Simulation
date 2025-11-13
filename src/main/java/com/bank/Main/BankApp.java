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
        logger.info("🏦 Welcome to the Bank Activity Simulator");

        while (true) {
            try {
                logger.info("Choose an option:\n1. Login\n2. Create Account\n3. Exit");
                logger.info("Awaiting user input...");

                int choice = InputValidator.getValidIntInput(sc);

                switch (choice) {
                    case 1:
                        loginUser();
                        break;
                    case 2:
                        createNewUser();
                        break;
                    case 3:
                        logger.info("👋 Thank you for using Bank Simulator! Exiting...");
                        return;
                    default:
                        logger.warn("Invalid choice entered: {}", choice);
                }

            } catch (InputMismatchException e) {
                sc.nextLine();
                logger.warn("Invalid input type. Expected number, got non-numeric input.", e);
            } catch (DatabaseException e) {
                logger.error("Database connection error encountered.", e);
            } catch (Exception e) {
                logger.error("Unexpected error in main menu.", e);
            }
        }
    }

    private void loginUser() throws DatabaseException {
        logger.info("User selected: Login");

        logger.info("Requesting username and password input.");
        System.out.print("Enter Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = sc.nextLine().trim();

        logger.debug("Attempting login for user: {}", name);

        if (authService.login(name, password)) {
            logger.info("✅ Login successful for user: {}", name);
            showUserMenu();
        } else {
            logger.warn("❌ Invalid credentials entered for username: {}", name);
        }
    }

    private void createNewUser() throws DatabaseException {
        logger.info("User selected: Create Account");

        while (true) {
            try {
                logger.info("Collecting account creation details...");

                System.out.print("Enter Name (letters only): ");
                String name = sc.nextLine().trim();
                if (!name.matches("[a-zA-Z ]+")) {
                    logger.warn("Invalid name input: {}", name);
                    continue;
                }

                System.out.print("Enter Password: ");
                String password = sc.nextLine().trim();

                String email;
                while (true) {
                    System.out.print("Enter Email: ");
                    email = sc.nextLine().trim();
                    if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                        logger.warn("Invalid email format entered: {}", email);
                        continue;
                    }
                    break;
                }

                double balance;
                while (true) {
                    System.out.print("Enter Initial Balance (min ₹100): ");
                    String balanceInput = sc.nextLine().trim();
                    try {
                        balance = Double.parseDouble(balanceInput);
                    } catch (NumberFormatException e) {
                        logger.warn("Non-numeric balance input: {}", balanceInput);
                        continue;
                    }

                    if (balance < 100) {
                        logger.warn("Balance below minimum limit: {}", balance);
                        continue;
                    }
                    break;
                }

                logger.info("Attempting to create new account for: {}", name);
                int accountId = accountService.createAccount(name, password, email, balance);

                if (accountId != -1) {
                    logger.info("🎉 Account successfully created for {} (Account ID: {})", name, accountId);
                    loginUser(); // automatically login after creation
                } else {
                    logger.error("Account creation failed for user: {}", name);
                }

                break;

            } catch (DatabaseException e) {
                logger.error("Database exception during account creation.", e);
                break;
            } catch (Exception e) {
                logger.error("Unexpected error during account creation.", e);
            }
        }
    }

    private void showUserMenu() throws DatabaseException {
        logger.info("Displaying user banking menu...");

        while (true) {
            logger.info(
                    "--- Banking Options ---\n" +
                            "1. Deposit Money\n" +
                            "2. Withdraw Money\n" +
                            "3. View Account\n" +
                            "4. Generate Report\n" +
                            "5. Transfer Money\n" +
                            "6. Delete Account\n" +
                            "7. Logout\n"
            );

            try {
                int choice = InputValidator.getValidIntInput(sc);
                logger.debug("User menu choice: {}", choice);

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
                        new ReportService(transactionService).generateReportOption(sc);
                        break;
                    case 5:
                        transactionService.transfer(sc);
                        break;
                    case 6:
                        accountService.deleteAccountMenu(sc);
                        break;
                    case 7:
                        logger.info("🔒 User logged out successfully.");
                        return;
                    default:
                        logger.warn("Invalid menu option entered: {}", choice);
                        break;
                }

            } catch (InputMismatchException e) {
                sc.nextLine();
                logger.warn("Invalid numeric input in user menu.", e);
            } catch (DatabaseException e) {
                logger.error("Database exception in user menu.", e);
            } catch (Exception e) {
                logger.error("Unexpected exception in user menu.", e);
            }
        }
    }

    public static void main(String[] args) {
        new BankApp().start();
    }
}