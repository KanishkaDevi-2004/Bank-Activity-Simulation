package com.bank.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;
import java.util.regex.Pattern;

public class InputValidator {
    private static final Logger logger = LogManager.getLogger(InputValidator.class);

    // Email regex pattern (RFC-compliant simple version)
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    /**
     * Safely reads a valid integer from the scanner.
     */
    public static int getValidIntInput(Scanner sc) {
        while (true) {
            if (sc.hasNextInt()) {
                int val = sc.nextInt();
                sc.nextLine(); // clear newline
                logger.debug("Valid integer input received: {}", val);
                return val;
            } else {
                String invalid = sc.next();
                logger.warn("Invalid integer input detected: {}", invalid);
            }
        }
    }

    /**
     * Safely reads a positive double (> 100) from the scanner.
     */
    public static double getValidPositiveDouble(Scanner sc) {
        while (true) {
            if (sc.hasNextDouble()) {
                double val = sc.nextDouble();
                sc.nextLine();
                if (val > 100) {
                    logger.debug("Valid positive double input received: {}", val);
                    return val;
                } else {
                    logger.warn("Invalid amount (<= 100) entered: {}", val);
                }
            } else {
                String invalid = sc.next();
                logger.warn("Invalid double input detected: {}", invalid);
            }
        }
    }

    /**
     * Validates email input using a regex pattern.
     */
    public static String getValidEmail(Scanner sc) {
        while (true) {
            String email = sc.nextLine().trim();
            if (EMAIL_PATTERN.matcher(email).matches()) {
                logger.debug("Valid email entered: {}", email);
                return email;
            } else {
                logger.warn("Invalid email format entered: {}", email);
            }
        }
    }
}
