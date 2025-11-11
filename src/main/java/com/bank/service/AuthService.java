package com.bank.service;

import com.bank.db.DBConnection;
import com.bank.exception.DatabaseException;
import java.sql.*;

public class AuthService {

    public boolean login(String name, String password) throws DatabaseException {
        String query = "SELECT * FROM accounts WHERE name = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, name);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new DatabaseException("❌ Login failed. Please check credentials.", e);
        }
    }
}
