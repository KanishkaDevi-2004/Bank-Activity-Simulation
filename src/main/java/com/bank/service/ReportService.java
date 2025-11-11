package com.bank.service;

import com.bank.model.Transaction;
import com.bank.report.ReportGenerator;
import com.bank.util.LoggerUtil;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Scanner;

public class ReportService {

    private static final Logger logger = LoggerUtil.getLogger(ReportService.class);
    private final TransactionService transactionService;

    public ReportService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void generateReportOption(Scanner sc) {
        System.out.println("\n===== 🧾 Generate Report =====");
        System.out.println("1. Generate report for today’s transactions");
        System.out.println("2. Generate report for All transactions");
        System.out.print("Enter your choice: ");

        int option = InputValidator.getValidIntInput(sc);
        String reportType;

        switch (option) {
            case 1:
                reportType = "TODAY";
                break;
            case 2:
                reportType = "ALL";
                break;
            default:
                System.out.println("❌ Invalid option! Returning to main menu.");
                return;
        }

        try {
            List<Transaction> transactions = transactionService.getTransactions();
            ReportGenerator.generateReport(transactions, reportType);
            System.out.println("✅ Report generated successfully! Check the 'reports' folder.");
            logger.info("Report generated successfully for type: {}", reportType);
        } catch (Exception e) {
            System.out.println("⚠️ Failed to generate report: " + e.getMessage());
            logger.error("Error generating report", e);
        }
    }
}
