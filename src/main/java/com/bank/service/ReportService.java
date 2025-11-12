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
        logger.info("===== 🧾 Generate Report Menu =====");
        logger.info("1. Generate report for today’s transactions");
        logger.info("2. Generate report for all transactions");
        logger.info("Awaiting user choice...");

        int option = InputValidator.getValidIntInput(sc);
        String reportType;

        switch (option) {
            case 1:
                reportType = "TODAY";
                logger.debug("User selected report type: TODAY");
                break;
            case 2:
                reportType = "ALL";
                logger.debug("User selected report type: ALL");
                break;
            default:
                logger.warn("Invalid option entered: {}. Returning to main menu.", option);
                return;
        }

        try {
            List<Transaction> transactions = transactionService.getTransactions();
            ReportGenerator.generateReport(transactions, reportType);
            logger.info("✅ Report generated successfully. Type: {}. Check the 'reports' folder.", reportType);
        } catch (Exception e) {
            logger.error("⚠️ Failed to generate report of type {}: {}", reportType, e.getMessage(), e);
        }
    }
}
