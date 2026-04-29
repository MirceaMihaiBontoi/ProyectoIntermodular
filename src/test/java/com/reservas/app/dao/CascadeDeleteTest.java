package com.reservas.app.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CASCADE DELETE functionality in the database.
 * Verifies that foreign key constraints with ON DELETE CASCADE work correctly.
 */
class CascadeDeleteTest {

    private static final int TEST_USER_ID = 700;
    private static final int TEST_RECURSO_ID = 700;
    private static final int TEST_HORARIO_ID = 700;

    @BeforeEach
    void setUp() {
        DatabaseManager.initializeDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test data
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM disponibleen WHERE id_recurso = " + TEST_RECURSO_ID);
            stmt.execute("DELETE FROM disponibleen WHERE id_horario = " + TEST_HORARIO_ID);
            stmt.execute("DELETE FROM horario WHERE id_horario = " + TEST_HORARIO_ID);
            stmt.execute("DELETE FROM reserva WHERE id_recurso = " + TEST_RECURSO_ID);
            stmt.execute("DELETE FROM recurso WHERE id_recurso = " + TEST_RECURSO_ID);
            stmt.execute("DELETE FROM usuarionormal WHERE id_usuario = " + TEST_USER_ID);
            stmt.execute("DELETE FROM administrador WHERE id_usuario = " + TEST_USER_ID);
            stmt.execute("DELETE FROM usuario WHERE id_usuario = " + TEST_USER_ID);
        }
    }

    @Test
    void testCascadeDeleteUsuarioToAdministrador() throws SQLException {
        // Create usuario and administrador
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "admin@test.com");
        userData.put("contrasena", "admin123");
        userData.put("nombre", "Test Admin");
        userData.put("fecha_nacimiento", "1990-01-01");
        userData.put("tipo_usuario", "Administrador");
        UsuarioDAO.insertWithCascade(userData);

        // Verify both exist
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM administrador WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "Administrador should exist before delete");
            }
        }

        // Delete usuario
        GenericDAO.delete("usuario", List.of("id_usuario"), List.of(TEST_USER_ID));

        // Verify administrador was deleted by CASCADE
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM administrador WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Administrador should be deleted by CASCADE");
            }
        }
    }

    @Test
    void testCascadeDeleteUsuarioToUsuarioNormal() throws SQLException {
        // Create usuario and usuarionormal
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "normal@test.com");
        userData.put("contrasena", "normal123");
        userData.put("nombre", "Test Normal");
        userData.put("fecha_nacimiento", "1995-01-01");
        userData.put("tipo_usuario", "Normal");
        UsuarioDAO.insertWithCascade(userData);

        // Verify both exist
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarionormal WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "UsuarioNormal should exist before delete");
            }
        }

        // Delete usuario
        GenericDAO.delete("usuario", List.of("id_usuario"), List.of(TEST_USER_ID));

        // Verify usuarionormal was deleted by CASCADE
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarionormal WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "UsuarioNormal should be deleted by CASCADE");
            }
        }
    }

    @Test
    void testCascadeDeleteHorarioToDisponibleen() throws SQLException {
        // Create horario
        Map<String, Object> horarioData = new HashMap<>();
        horarioData.put("id_horario", TEST_HORARIO_ID);
        horarioData.put("dia_semana", "Lunes");
        horarioData.put("hora_inicio", "09:00");
        horarioData.put("hora_fin", "10:00");
        GenericDAO.insert("horario", horarioData);

        // Create recurso
        Map<String, Object> recursoData = new HashMap<>();
        recursoData.put("id_recurso", TEST_RECURSO_ID);
        recursoData.put("nombre", "Test Recurso");
        recursoData.put("tipo", "Sala");
        recursoData.put("descripcion", "Test");
        recursoData.put("ubicacion", "Test");
        recursoData.put("capacidad", 10);
        GenericDAO.insert("recurso", recursoData);

        // Create disponibleen relationship
        Map<String, Object> disponibleenData = new HashMap<>();
        disponibleenData.put("id_recurso", TEST_RECURSO_ID);
        disponibleenData.put("id_horario", TEST_HORARIO_ID);
        GenericDAO.insert("disponibleen", disponibleenData);

        // Verify disponibleen exists
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM disponibleen WHERE id_horario = " + TEST_HORARIO_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "Disponibleen should exist before delete");
            }
        }

        // Delete horario
        GenericDAO.delete("horario", List.of("id_horario"), List.of(TEST_HORARIO_ID));

        // Verify disponibleen was deleted by CASCADE
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM disponibleen WHERE id_horario = " + TEST_HORARIO_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Disponibleen should be deleted by CASCADE when horario is deleted");
            }
        }
    }

    @Test
    void testCascadeDeleteRecursoToDisponibleen() throws SQLException {
        // Create horario
        Map<String, Object> horarioData = new HashMap<>();
        horarioData.put("id_horario", TEST_HORARIO_ID);
        horarioData.put("dia_semana", "Lunes");
        horarioData.put("hora_inicio", "09:00");
        horarioData.put("hora_fin", "10:00");
        GenericDAO.insert("horario", horarioData);

        // Create recurso
        Map<String, Object> recursoData = new HashMap<>();
        recursoData.put("id_recurso", TEST_RECURSO_ID);
        recursoData.put("nombre", "Test Recurso");
        recursoData.put("tipo", "Sala");
        recursoData.put("descripcion", "Test");
        recursoData.put("ubicacion", "Test");
        recursoData.put("capacidad", 10);
        GenericDAO.insert("recurso", recursoData);

        // Create disponibleen relationship
        Map<String, Object> disponibleenData = new HashMap<>();
        disponibleenData.put("id_recurso", TEST_RECURSO_ID);
        disponibleenData.put("id_horario", TEST_HORARIO_ID);
        GenericDAO.insert("disponibleen", disponibleenData);

        // Verify disponibleen exists
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM disponibleen WHERE id_recurso = " + TEST_RECURSO_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "Disponibleen should exist before delete");
            }
        }

        // Delete recurso
        GenericDAO.delete("recurso", List.of("id_recurso"), List.of(TEST_RECURSO_ID));

        // Verify disponibleen was deleted by CASCADE
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM disponibleen WHERE id_recurso = " + TEST_RECURSO_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Disponibleen should be deleted by CASCADE when recurso is deleted");
            }
        }
    }

    @Test
    void testCascadeDeleteUsuarioNormalToReserva() throws SQLException {
        // Create usuario and usuarionormal
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "normal@test.com");
        userData.put("contrasena", "normal123");
        userData.put("nombre", "Test Normal");
        userData.put("fecha_nacimiento", "1995-01-01");
        userData.put("tipo_usuario", "Normal");
        UsuarioDAO.insertWithCascade(userData);

        // Create recurso
        Map<String, Object> recursoData = new HashMap<>();
        recursoData.put("id_recurso", TEST_RECURSO_ID);
        recursoData.put("nombre", "Test Recurso");
        recursoData.put("tipo", "Sala");
        recursoData.put("descripcion", "Test");
        recursoData.put("ubicacion", "Test");
        recursoData.put("capacidad", 10);
        GenericDAO.insert("recurso", recursoData);

        // Create reserva
        Map<String, Object> reservaData = new HashMap<>();
        reservaData.put("id_recurso", TEST_RECURSO_ID);
        reservaData.put("id_reserva_local", 1);
        reservaData.put("id_usuario", TEST_USER_ID);
        reservaData.put("fecha", "2026-04-27");
        reservaData.put("hora_inicio", "10:00:00");
        reservaData.put("hora_fin", "11:00:00");
        reservaData.put("coste", 25.50);
        reservaData.put("numero_plazas", 5);
        reservaData.put("motivo", "Test");
        GenericDAO.insert("reserva", reservaData);

        // Verify reserva exists
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM reserva WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "Reserva should exist before delete");
            }
        }

        // Delete usuarionormal (should cascade to reserva)
        GenericDAO.delete("usuarionormal", List.of("id_usuario"), List.of(TEST_USER_ID));

        // Verify reserva was deleted by CASCADE
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM reserva WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Reserva should be deleted by CASCADE when usuarionormal is deleted");
            }
        }
    }
}
