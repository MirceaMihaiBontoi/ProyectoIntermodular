package com.reservas.app.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for UsuarioDAO to verify cascade logic for usuario table.
 */
class UsuarioDAOTest {

    private static final int TEST_USER_ID = 800;

    @BeforeEach
    void setUp() {
        DatabaseManager.initializeDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test data
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM administrador WHERE id_usuario = " + TEST_USER_ID);
            stmt.execute("DELETE FROM usuarionormal WHERE id_usuario = " + TEST_USER_ID);
            stmt.execute("DELETE FROM usuario WHERE id_usuario = " + TEST_USER_ID);
        }
    }

    @Test
    void testInsertWithCascadeAdministrador() throws SQLException {
        // Insert an administrator user
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "admin@test.com");
        userData.put("contrasena", "admin123");
        userData.put("nombre", "Test Admin");
        userData.put("fecha_nacimiento", "1990-01-01");
        userData.put("tipo_usuario", "Administrador");

        UsuarioDAO.insertWithCascade(userData);

        // Verify usuario was created
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM usuario WHERE id_usuario = " + TEST_USER_ID)) {
            assertTrue(rs.next(), "Usuario should be created");
            assertEquals("Administrador", rs.getString("tipo_usuario"));
        }

        // Verify administrador record was created
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM administrador WHERE id_usuario = " + TEST_USER_ID)) {
            assertTrue(rs.next(), "Administrador record should be created automatically");
        }

        // Verify usuarionormal record was NOT created
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarionormal WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "UsuarioNormal record should NOT be created for Administrador");
            }
        }
    }

    @Test
    void testInsertWithCascadeNormal() throws SQLException {
        // Insert a normal user
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "normal@test.com");
        userData.put("contrasena", "normal123");
        userData.put("nombre", "Test Normal");
        userData.put("fecha_nacimiento", "1995-01-01");
        userData.put("tipo_usuario", "Normal");

        UsuarioDAO.insertWithCascade(userData);

        // Verify usuario was created
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM usuario WHERE id_usuario = " + TEST_USER_ID)) {
            assertTrue(rs.next(), "Usuario should be created");
            assertEquals("Normal", rs.getString("tipo_usuario"));
        }

        // Verify usuarionormal record was created
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM usuarionormal WHERE id_usuario = " + TEST_USER_ID)) {
            assertTrue(rs.next(), "UsuarioNormal record should be created automatically");
        }

        // Verify administrador record was NOT created
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM administrador WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Administrador record should NOT be created for Normal user");
            }
        }
    }

    @Test
    void testUpdateWithCascadeNormalToAdministrador() throws SQLException {
        // First insert as Normal user
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "normal@test.com");
        userData.put("contrasena", "normal123");
        userData.put("nombre", "Test Normal");
        userData.put("fecha_nacimiento", "1995-01-01");
        userData.put("tipo_usuario", "Normal");
        UsuarioDAO.insertWithCascade(userData);

        // Verify usuarionormal exists
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarionormal WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "UsuarioNormal should exist initially");
            }
        }

        // Update to Administrador
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("tipo_usuario", "Administrador");
        UsuarioDAO.updateWithCascade("id_usuario", TEST_USER_ID, updateData);

        // Verify tipo_usuario changed
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT tipo_usuario FROM usuario WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals("Administrador", rs.getString("tipo_usuario"));
            }
        }

        // Verify usuarionormal was deleted
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarionormal WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "UsuarioNormal should be deleted after type change");
            }
        }

        // Verify administrador was created
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM administrador WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "Administrador should be created after type change");
            }
        }
    }

    @Test
    void testUpdateWithCascadeAdministradorToNormal() throws SQLException {
        // First insert as Administrador
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "admin@test.com");
        userData.put("contrasena", "admin123");
        userData.put("nombre", "Test Admin");
        userData.put("fecha_nacimiento", "1990-01-01");
        userData.put("tipo_usuario", "Administrador");
        UsuarioDAO.insertWithCascade(userData);

        // Verify administrador exists
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM administrador WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "Administrador should exist initially");
            }
        }

        // Update to Normal
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("tipo_usuario", "Normal");
        UsuarioDAO.updateWithCascade("id_usuario", TEST_USER_ID, updateData);

        // Verify tipo_usuario changed
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT tipo_usuario FROM usuario WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals("Normal", rs.getString("tipo_usuario"));
            }
        }

        // Verify administrador was deleted
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM administrador WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Administrador should be deleted after type change");
            }
        }

        // Verify usuarionormal was created
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarionormal WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "UsuarioNormal should be created after type change");
            }
        }
    }

    @Test
    void testDeleteWithCascade() throws SQLException {
        // Insert as Administrador
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "admin@test.com");
        userData.put("contrasena", "admin123");
        userData.put("nombre", "Test Admin");
        userData.put("fecha_nacimiento", "1990-01-01");
        userData.put("tipo_usuario", "Administrador");
        UsuarioDAO.insertWithCascade(userData);

        // Verify both records exist
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuario WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "Usuario should exist before delete");
            }
        }

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM administrador WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "Administrador should exist before delete");
            }
        }

        // Delete usuario (should cascade to administrador via DB)
        UsuarioDAO.deleteWithCascade("id_usuario", TEST_USER_ID);

        // Verify usuario was deleted
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuario WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Usuario should be deleted");
            }
        }

        // Verify administrador was deleted by CASCADE
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM administrador WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Administrador should be deleted by CASCADE");
            }
        }
    }
}
