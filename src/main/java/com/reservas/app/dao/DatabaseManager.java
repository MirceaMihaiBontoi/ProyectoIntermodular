package com.reservas.app.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    // Database file moved to 'database' folder for better organization
    private static final String DB_DIR = "database";
    private static final String DB_FILE = "sistema_reservas.db";
    private static final String URL = "jdbc:sqlite:" + DB_DIR + "/" + DB_FILE;
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    private DatabaseManager() {
        throw new IllegalStateException("Utility class");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try {
            Path dirPath = Paths.get(DB_DIR);
            Path filePath = dirPath.resolve(DB_FILE);

            // Create directory if it doesn't exist
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                logger.info("Database directory created: " + DB_DIR);
            }

            // Check if database file already exists
            boolean dbExists = Files.exists(filePath);

            try (Connection conn = getConnection()) {
                if (!dbExists) {
                    logger.info("Database not found. Initializing new database...");
                    executeInitializationScript(conn);
                } else {
                    logger.info("Database found. Skipping initialization.");
                }
            }
        } catch (IOException | SQLException e) {
            logger.log(Level.SEVERE, "CRITICAL: Database initialization failed", e);
        }
    }

    private static void executeInitializationScript(Connection conn) {
        String scriptPath = "database/scripts/sistema_reservas_lite.sql";
        try {
            /* Read script as bytes and split by semicolon to execute commands individually
               SQLite JDBC doesn't support multiple SQL statements in a single executeUpdate() call
            */
            String content = new String(Files.readAllBytes(Paths.get(scriptPath)));
            String[] commands = content.split(";");
            
            try (Statement stmt = conn.createStatement()) {
                for (String command : commands) {
                    if (!command.trim().isEmpty()) {
                        stmt.execute(command);
                    }
                }
                logger.info("Database schema and seed data initialized successfully.");
            }
        } catch (IOException | SQLException e) {
            logger.log(Level.SEVERE, "CRITICAL: Failed to process or execute initialization script", e);
        }
    }
}
