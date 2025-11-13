package com.bank.service;

import com.bank.db.DBConnection;
import com.bank.exception.DatabaseException;
import com.bank.util.LoggerUtil;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class AuthService {

    private static final Logger logger = LoggerUtil.getLogger(AuthService.class);

    public boolean login(String name, String password) throws DatabaseException {
        String query = "SELECT * FROM accounts WHERE name = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, name);
            ps.setString(2, password);

            logger.info("Attempting login for user: {}", name);

            ResultSet rs = ps.executeQuery();
            boolean isAuthenticated = rs.next();

            if (isAuthenticated) {
                logger.info("✅ Login successful for user: {}", name);
            } else {
                logger.warn("⚠️ Login failed for user: {}", name);
            }

            return isAuthenticated;

        } catch (SQLException e) {
            logger.error("❌ Database error during login for user: {} - {}", name, e.getMessage(), e);
            throw new DatabaseException("Login failed. Please check credentials or try again later.", e);
        }
    }
}
