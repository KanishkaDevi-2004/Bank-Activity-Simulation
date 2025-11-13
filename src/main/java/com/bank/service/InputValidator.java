package com.bank.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;
import java.util.regex.Pattern;

public class InputValidator {

    private static final Logger logger = LogManager.getLogger(InputValidator.class);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    // ✅ Validate integer input
    public static int getValidIntInput(Scanner sc) {
        while (true) {
            if (sc.hasNextInt()) {
                int val = sc.nextInt();
                sc.nextLine();
                return val;
            } else {
                logger.warn("Invalid integer input detected. Prompting user again.");
                logger.info("Please enter a valid number:");
                sc.next(); // discard invalid input
            }
        }
    }

    // ✅ Validate positive double input (minimum 100)
    public static double getValidPositiveDouble(Scanner sc) {
        while (true) {
            if (sc.hasNextDouble()) {
                double val = sc.nextDouble();
                sc.nextLine();
                if (val > 100) {
                    return val;
                } else {
                    logger.warn("Entered amount {} is below the minimum required value.", val);
                    logger.info("Amount must be greater than ₹100. Please re-enter:");
                }
            } else {
                logger.warn("Invalid amount entered (non-numeric input).");
                logger.info("Please enter a numeric value:");
                sc.next(); // discard invalid input
            }
        }
    }

    // ✅ Validate email format
    public static String getValidEmail(Scanner sc) {
        while (true) {
            logger.info("Please enter your email address:");
            String email = sc.nextLine().trim();

            if (EMAIL_PATTERN.matcher(email).matches()) {
                logger.debug("Valid email entered: {}", email);
                return email;
            } else {
                logger.warn("Invalid email format entered: {}", email);
                logger.info("Email format is invalid. Please try again:");
            }
        }
    }
}
