package com.durableengine.engine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseHelper {
    // This creates a local SQLite file named durable_engine.db in your project root
    private static final String DB_URL = "jdbc:sqlite:durable_engine.db";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Enable Write-Ahead Logging (WAL) for better concurrency handling
            // This is a proactive step to handle parallel writes without SQLITE_BUSY errors
            stmt.execute("PRAGMA journal_mode=WAL;");

            // Create the steps table as per the assignment requirements
            String sql = "CREATE TABLE IF NOT EXISTS steps (" +
                         "workflow_id TEXT NOT NULL, " +
                         "step_key TEXT NOT NULL, " +
                         "status TEXT NOT NULL, " +
                         "output TEXT, " +
                         "PRIMARY KEY (workflow_id, step_key)" +
                         ");";
            
            stmt.execute(sql);
            System.out.println("Database and steps table initialized successfully!");

        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }

    // Helper method to get a connection when we need to read/write steps later
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }
}