package com.bank.alert;

import com.bank.db.DBConnection;
import java.sql.*;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BalanceAlertTracker implements Runnable {

    private static final Logger logger = LogManager.getLogger(BalanceAlertTracker.class);

    private volatile boolean running = true;
    private static final long CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(1);

    @Override
    public void run() {
        logger.info("💡 Balance Alert Tracker started...");

        EmailService emailService = new EmailService();

        while (running) {
            try (Connection con = DBConnection.getConnection()) {

                String sql ="SELECT name, email, balance FROM accounts WHERE balance < 100";
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    double balance = rs.getDouble("balance");

                    if (email == null || email.trim().isEmpty()) {
                        logger.warn("⚠️ Skipping alert — no email for user: {}", name);
                        continue;
                    }

                    String subject = "⚠ Transaction Declined - Low Balance Alert";

                    String message =
                            "Dear " + name + ",\n\n" +
                                    "Your transfer/withdrawal request was declined due to insufficient balance.\n" +
                                    "Minimum balance of ₹100 must be maintained.\n\n" +
                                    "Current Balance: ₹" + balance + "\n\n" +
                                    "Regards,\n" +
                                    "Bank System";

                    emailService.sendEmail(email, subject, message);

                    logger.info("📩 Low balance decline alert sent to {}", email);
                }

                Thread.sleep(CHECK_INTERVAL);

            } catch (InterruptedException e) {
                logger.warn("⛔ Balance Alert Tracker interrupted");
                Thread.currentThread().interrupt();
                stop();
            } catch (Exception e) {
                logger.error("❌ Error in BalanceAlertTracker", e);
            }
        }

        logger.info("🛑 Balance Alert Tracker stopped.");
    }

    public void stop() {
        running = false;
    }
}
