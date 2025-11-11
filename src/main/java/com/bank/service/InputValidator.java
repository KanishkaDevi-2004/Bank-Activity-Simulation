package com.bank.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;
import java.util.regex.Pattern;

public class InputValidator {
    private static final Logger logger = LogManager.getLogger(InputValidator.class);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    public static int getValidIntInput(Scanner sc) {
        while (true) {
            if (sc.hasNextInt()) {
                int val = sc.nextInt();
                sc.nextLine();
                return val;
            } else {
                logger.warn("Invalid integer input detected.");
                System.out.print("❌ Invalid input. Enter a valid number: ");
                sc.next();
            }
        }
    }

    public static double getValidPositiveDouble(Scanner sc) {
        while (true) {
            if (sc.hasNextDouble()) {
                double val = sc.nextDouble();
                sc.nextLine();
                if (val > 100) return val;
                else System.out.print("❌ Amount must be positive and initial deposit should be greater than 100. Try again: ");
            } else {
                logger.warn("Invalid double input detected.");
                System.out.print("❌ Invalid amount. Enter a number: ");
                sc.next();
            }
        }
    }

    public static String getValidEmail(Scanner sc) {
        while (true) {
            System.out.print("Enter your email: ");
            String email = sc.nextLine().trim();
            if (EMAIL_PATTERN.matcher(email).matches()) {
                return email;
            } else {
                logger.warn("Invalid email entered: {}", email);
                System.out.println("❌ Invalid email format! Please try again.");
            }
        }
    }
}
