package com.bank.db;

import com.bank.exception.DatabaseException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection handles database connectivity using a configuration file (db.properties).
 */
public class DBConnection {

    private static final String url;
    private static final String username;
    private static final String password;

    static {
        try {
            // Load configuration file from classpath (src/main/resources)
            InputStream input = DBConnection.class.getClassLoader()
                    .getResourceAsStream("db.properties");

            if (input == null) {
                throw new DatabaseException("❌ db.properties not found in classpath!");
            }

            Properties props = new Properties();
            props.load(input);

            url = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");

            if (url == null || username == null || password == null) {
                throw new DatabaseException("❌ Database configuration missing in db.properties!");
            }

            // Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (Exception e) {
            throw new DatabaseException("❌ Failed to initialize DB configuration.", e);
        }
    }

    /**
     * Establishes a connection to the MySQL database.
     *
     * @return a Connection object
     * @throws DatabaseException if connection fails
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new DatabaseException("❌ Database connection failed. Check MySQL or credentials!", e);
        }
    }
}
