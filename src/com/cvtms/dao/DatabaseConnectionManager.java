package com.cvtms.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnectionManager {
    // Modify these credentials as needed for the local PostgreSQL installation
    private static final String URL = "jdbc:postgresql://localhost:5432/cvtms";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin"; // Default placeholder

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id SERIAL PRIMARY KEY, " +
                "username VARCHAR(255) UNIQUE NOT NULL, " +
                "password_hash VARCHAR(255) NOT NULL, " +
                "role VARCHAR(50) NOT NULL" +
                ");";

        String createOwnersTable = "CREATE TABLE IF NOT EXISTS owners (" +
                "id SERIAL PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "contact VARCHAR(255)" +
                ");";

        String createVehiclesTable = "CREATE TABLE IF NOT EXISTS vehicles (" +
                "id SERIAL PRIMARY KEY, " +
                "registration_number VARCHAR(100) UNIQUE NOT NULL, " +
                "owner_id INTEGER, " +
                "type VARCHAR(50) NOT NULL, " +
                "FOREIGN KEY(owner_id) REFERENCES owners(id)" +
                ");";

        String createEntriesTable = "CREATE TABLE IF NOT EXISTS entry_logs (" +
                "id SERIAL PRIMARY KEY, " +
                "vehicle_id INTEGER NOT NULL, " +
                "entry_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "gate VARCHAR(100), " +
                "external_purpose TEXT, " +
                "FOREIGN KEY(vehicle_id) REFERENCES vehicles(id)" +
                ");";

        String createExitsTable = "CREATE TABLE IF NOT EXISTS exit_logs (" +
                "id SERIAL PRIMARY KEY, " +
                "entry_log_id INTEGER NOT NULL UNIQUE, " +
                "exit_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(entry_log_id) REFERENCES entry_logs(id)" +
                ");";

        String createOverstaysTable = "CREATE TABLE IF NOT EXISTS overstays (" +
                "id SERIAL PRIMARY KEY, " +
                "exit_log_id INTEGER NOT NULL UNIQUE, " +
                "justification TEXT, " +
                "FOREIGN KEY(exit_log_id) REFERENCES exit_logs(id)" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createOwnersTable);
            stmt.execute(createVehiclesTable);
            stmt.execute(createEntriesTable);
            stmt.execute(createExitsTable);
            stmt.execute(createOverstaysTable);
            
            // Seed default admin if none exists
            String adminQuery = "SELECT COUNT(*) FROM users";
            try (ResultSet rs = stmt.executeQuery(adminQuery)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    // hash password 'admin'
                    String hashed = org.mindrot.jbcrypt.BCrypt.hashpw("admin", org.mindrot.jbcrypt.BCrypt.gensalt());
                    String seedAdmin = "INSERT INTO users (username, password_hash, role) VALUES ('admin', '" + hashed + "', 'ADMIN')";
                    stmt.execute(seedAdmin);
                }
            }
        } catch (SQLException e) {
            System.err.println("\n========================================================");
            System.err.println("[FATAL ERROR] Failed to connect to PostgreSQL database.");
            System.err.println("Please ensure PostgreSQL is installed and running on localhost:5432.");
            System.err.println("Ensure the 'cvtms' database exists.");
            System.err.println("Expected Credentials -> User: " + USER + " | Password: " + PASSWORD);
            System.err.println("========================================================\n");
            System.exit(1);
        }
    }
}
