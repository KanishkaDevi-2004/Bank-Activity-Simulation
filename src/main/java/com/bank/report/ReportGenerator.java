package com.bank.report;

import com.bank.model.Transaction;
import com.bank.db.DBConnection;
import com.bank.util.LoggerUtil;
import org.apache.logging.log4j.Logger;

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

    private static final Logger logger = LoggerUtil.getLogger(ReportGenerator.class);

    public static void generateReport(List<Transaction> transactions, String reportType) {
        Connection con = null;

        try {
            Path reportsDir = Paths.get("reports");
            if (!Files.exists(reportsDir)) {
                Files.createDirectory(reportsDir);
                logger.info("Created reports directory at {}", reportsDir.toAbsolutePath());
            }

            String date = LocalDate.now().toString();
            String fileName = "transactions_" +
                    (reportType.equals("TODAY") ? "today_" + date : "all_" + date) + ".txt";

            File file = new File("reports/" + fileName);
            con = DBConnection.getConnection();

            String sql = "SELECT account_no, name, email, balance FROM accounts";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            double totalBalance = 0;
            int totalAccounts = 0;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

                writer.write("===========================================\n");
                writer.write("        BANK TRANSACTION REPORT\n");
                writer.write("===========================================\n");
                writer.write("Date: " + date + "\n");
                writer.write("Report Type: " + reportType + "\n\n");

                writer.write(String.format(
                        "%-12s %-20s %-30s %-10s %-15s %-10s %-25s\n",
                        "AccountNo", "Name", "Email", "Balance", "LastTxType", "Amount", "TxDateTime"
                ));
                writer.write("---------------------------------------------------------------------------------------------------------------\n");

                while (rs.next()) {

                    int accNo = rs.getInt("account_no");
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    double balance = rs.getDouble("balance");

                    totalAccounts++;
                    totalBalance += balance;

                    Transaction lastTx = null;

                    for (Transaction t : transactions) {

                        boolean matchesAccount =
                                (t.getSenderAccount() != null && t.getSenderAccount() == accNo) ||
                                        (t.getReceiverAccount() != null && t.getReceiverAccount() == accNo);

                        boolean isToday = t.getCreatedAt().toLocalDate().equals(LocalDate.now());

                        if (matchesAccount &&
                                (reportType.equals("ALL") || (reportType.equals("TODAY") && isToday))) {

                            if (lastTx == null || t.getCreatedAt().isAfter(lastTx.getCreatedAt())) {
                                lastTx = t;
                            }
                        }
                    }

                    if (lastTx != null) {
                        writer.write(String.format(
                                "%-12d %-20s %-30s %-10.2f %-15s %-10.2f %-25s\n",
                                accNo, name, email, balance,
                                lastTx.getTransactionType(),
                                lastTx.getAmount(),
                                lastTx.getCreatedAt()
                        ));
                    } else {
                        writer.write(String.format(
                                "%-12d %-20s %-30s %-10.2f %-15s %-10s %-25s\n",
                                accNo, name, email, balance, "NO_TX", "0.00", "N/A"
                        ));
                    }
                }

                writer.write("\n===========================================\n");
                writer.write("Summary:\n");
                writer.write("===========================================\n");
                writer.write("Total Accounts : " + totalAccounts + "\n");
                writer.write("Total Balance  : " + totalBalance + "\n");
                writer.write("===========================================\n");

                writer.flush();
            }

            logger.info("✅ {} Report generated successfully: {}", reportType, file.getAbsolutePath());

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
