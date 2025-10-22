package com.bank.report;

import com.bank.model.Transaction;
import com.bank.db.DBConnection;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReportGenerator {

    private static final Logger logger = Logger.getLogger(ReportGenerator.class);



    public static void generateReport(List<Transaction> transactions) {
        Connection con = null;
        try {
            // Create directory if missing
            Path reportsDir = Paths.get("reports");
            if (!Files.exists(reportsDir)) Files.createDirectory(reportsDir);

            String date = LocalDate.now().toString();
            File file = new File("reports/transactions_" + date + ".csv");

            con = DBConnection.getConnection();

            // Fetch all account details from DB
            String sql = "SELECT account_no, name, balance FROM accounts";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            double totalBalance = 0;
            int totalAccounts = 0;

            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader("Account No", "Name", "Balance", "Last Transaction Type", "Last Transaction Amount", "Transaction DateTime")
                    .build();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                 CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {

                while (rs.next()) {
                    int accNo = rs.getInt("account_no");
                    String name = rs.getString("name");
                    double balance = rs.getDouble("balance");
                    totalAccounts++;
                    totalBalance += balance;

                    // Find last transaction (if any)
                    Transaction lastTx = null;
                    for (Transaction t : transactions) {
                        if (t.getAccountNo() == accNo) {
                            if (lastTx == null || t.getDateTime().isAfter(lastTx.getDateTime())) {
                                lastTx = t;
                            }
                        }
                    }

                    if (lastTx != null) {
                        printer.printRecord(accNo, name, balance, lastTx.getType(), lastTx.getAmount(), lastTx.getDateTime());
                    } else {
                        printer.printRecord(accNo, name, balance, "NO_TRANSACTION", "0", "N/A");
                    }
                }

                // Summary section
                printer.println();
                printer.printRecord("TOTAL ACCOUNTS", totalAccounts);
                printer.printRecord("TOTAL BALANCE", totalBalance);
            }

            logger.info("✅ Report generated successfully: " + file.getAbsolutePath());

        } catch (Exception e) {
            logger.error("❌ Error generating report", e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                logger.warn("⚠️ Failed to close DB connection", e);
            }
        }
    }
}
