package com.reservas.app.dao;

import com.reservas.app.util.PasswordHasher;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
     * Note: SQLite requires explicit enabling of foreign keys via PRAGMA.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        // Enable foreign key constraints for this connection
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    /**
     * Initializes the database by executing the SQL script if the database is empty.
     * This is a "First Run" logic:
     * 1. Check if the database has tables.
     * 2. If not, read the .sql file as a String.
     * 3. Split the script by semicolons (;) because JDBC Statement.execute() 
     *    usually handles only one SQL command at a time.
     * 4. Hash all plain text passwords after loading the script.
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = conn.getMetaData().getTables(null, null, "usuario", null)) {

            if (!rs.next()) {
                logger.info("Database empty. Loading your script...");
                String scriptPath = "database/scripts/sistema_reservas_lite.sql";
                String content = java.nio.file.Files.readString(java.nio.file.Paths.get(scriptPath));

                for (String sql : content.split(";")) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql);
                    }
                }
                logger.info("Schema and test data loaded successfully from script.");
                
                // Hash all passwords in usuario table
                hashPasswordsInDatabase(conn);
            }
        } catch (SQLException | java.io.IOException e) {
            logger.log(Level.SEVERE, "Database initialization failed", e);
        }
    }

    /**
     * Hashes all plain text passwords in the usuario table after initial data load.
     */
    private static void hashPasswordsInDatabase(Connection conn) throws SQLException {
        logger.info("Hashing passwords in usuario table...");
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id_usuario, contrasena FROM usuario")) {
            
            while (rs.next()) {
                int userId = rs.getInt("id_usuario");
                String plainPassword = rs.getString("contrasena");
                
                // Only hash if not already hashed (BCrypt hashes start with $2a$ or $2b$)
                if (plainPassword != null && !plainPassword.startsWith("$2")) {
                    String hashedPassword = PasswordHasher.hashPassword(plainPassword);
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE usuario SET contrasena = ? WHERE id_usuario = ?")) {
                        pstmt.setString(1, hashedPassword);
                        pstmt.setInt(2, userId);
                        pstmt.executeUpdate();
                    }
                }
            }
        }
        
        logger.info("Passwords hashed successfully.");
    }
}
