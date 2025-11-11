package com.bank.alert;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class EmailService {
    private static final Logger logger = LogManager.getLogger(EmailService.class);
    private final Properties props = new Properties();

    public EmailService() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) throw new RuntimeException("config.properties not found");
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public void sendEmail(String to, String subject, String body) {

        if (to == null || to.trim().isEmpty()) {
            logger.warn("⚠️ Email not sent — No email address provided");
            return;
        }

        String username = props.getProperty("mail.username");
        String password = props.getProperty("mail.password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            logger.error("❌ Email not sent — Missing SMTP credentials in config.properties");
            return;
        }

        Properties sessionProps = new Properties();
        sessionProps.put("mail.smtp.auth", "true");
        sessionProps.put("mail.smtp.starttls.enable", "true");
        sessionProps.put("mail.smtp.host", "smtp.gmail.com");
        sessionProps.put("mail.smtp.port", "587");
        sessionProps.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(sessionProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(username));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

            msg.setSubject(subject, StandardCharsets.UTF_8.name());
            msg.setText(body, StandardCharsets.UTF_8.name());

            Transport.send(msg);
            logger.info("✅ Email sent successfully to {}", to);

            Thread.sleep(500);

        } catch (MessagingException e) {
            logger.error("❌ Email sending failed to {}: {}", to, e.getMessage());
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
