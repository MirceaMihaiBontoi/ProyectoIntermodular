package com.reservas.app.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DatabaseManager to verify database initialization and connection.
 */
class DatabaseManagerTest {

    @BeforeEach
    void setUp() {
        // Initialize database before each test
        DatabaseManager.initializeDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test data after each test if needed
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
        }
    }

    @Test
    void testGetConnection() throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        assertNotNull(conn, "Connection should not be null");
        assertFalse(conn.isClosed(), "Connection should be open");
        conn.close();
    }

    @Test
    void testInitializeDatabase() throws SQLException {
        // Verify that tables were created
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check if 'usuario' table exists
            ResultSet rs = conn.getMetaData().getTables(null, null, "usuario", null);
            assertTrue(rs.next(), "Usuario table should exist after initialization");
            rs.close();

            // Check if 'reserva' table exists
            rs = conn.getMetaData().getTables(null, null, "reserva", null);
            assertTrue(rs.next(), "Reserva table should exist after initialization");
            rs.close();

            // Check if 'recurso' table exists
            rs = conn.getMetaData().getTables(null, null, "recurso", null);
            assertTrue(rs.next(), "Recurso table should exist after initialization");
            rs.close();
        }
    }

    @Test
    void testDatabaseHasTestData() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check if usuario table has test data
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuario");
            if (rs.next()) {
                int count = rs.getInt(1);
                assertTrue(count > 0, "Usuario table should have test data");
            }
            rs.close();

            // Check if recurso table has test data
            rs = stmt.executeQuery("SELECT COUNT(*) FROM recurso");
            if (rs.next()) {
                int count = rs.getInt(1);
                assertTrue(count > 0, "Recurso table should have test data");
            }
            rs.close();
        }
    }
}
