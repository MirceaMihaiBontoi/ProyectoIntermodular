package com.reservas.app.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton-like utility class to manage the SQLite database connection.
 */
public class DatabaseManager {

    private DatabaseManager() {
        throw new IllegalStateException("Utility class");
    }

    private static final String URL = "jdbc:sqlite:database/sistema_reservas.db";
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    /**
     * Gets a new connection to the database.
     * JDBC (Java Database Connectivity) is the standard Java API to interact with databases.
     * DriverManager is the factory that creates the actual connection using the URL.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /**
     * Initializes the database by executing the SQL script if the database is empty.
     * This is a "First Run" logic:
     * 1. Check if the database has tables.
     * 2. If not, read the .sql file as a String.
     * 3. Split the script by semicolons (;) because JDBC Statement.execute() 
     *    usually handles only one SQL command at a time.
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = conn.getMetaData().getTables(null, null, "usuario", null)) {

            // rs.next() returns true if there is at least one result (the 'usuario' table exists)
            if (!rs.next()) {
                logger.info("Database empty. Loading your script...");
                String scriptPath = "database/scripts/sistema_reservas_lite.sql";

                // Files.readString is a modern Java way (Java 11+) to read a whole file into text
                String content = java.nio.file.Files.readString(java.nio.file.Paths.get(scriptPath));

                // We split the script into individual SQL statements
                for (String sql : content.split(";")) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql);
                    }
                }
                logger.info("Schema and test data loaded successfully from script.");
            }
        } catch (SQLException | java.io.IOException e) {
            logger.log(Level.SEVERE, "Database initialization failed", e);
        }
    }
}
