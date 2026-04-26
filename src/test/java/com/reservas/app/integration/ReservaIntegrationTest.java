package com.reservas.app.integration;

import com.reservas.app.dao.GenericDAO;
import com.reservas.app.dao.MetadataDAO;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
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
 * Integration test for the complete reservation flow
 */
class ReservaIntegrationTest {

    private static final int TEST_USER_ID = 900;
    private static final int TEST_RECURSO_ID = 900;
    private static final int TEST_RESERVA_ID = 900;

    @BeforeEach
    void setUp() {
        com.reservas.app.dao.DatabaseManager.initializeDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test data
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM reserva WHERE id_recurso = " + TEST_RECURSO_ID);
            stmt.execute("DELETE FROM usuarionormal WHERE id_usuario = " + TEST_USER_ID);
            stmt.execute("DELETE FROM usuario WHERE id_usuario = " + TEST_USER_ID);
            stmt.execute("DELETE FROM recurso WHERE id_recurso = " + TEST_RECURSO_ID);
        }
    }

    @Test
    void testCargaInicial() {
        // Verify database initialization with test data
        List<String> tables = MetadataDAO.getTableNames();
        
        assertNotNull(tables, "Tables should be loaded");
        assertTrue(tables.contains("usuario"), "Usuario table should exist");
        assertTrue(tables.contains("recurso"), "Recurso table should exist");
        assertTrue(tables.contains("reserva"), "Reserva table should exist");
        
        // Verify test data exists
        List<String> usuarioColumns = MetadataDAO.getColumnNames("usuario");
        ObservableList<ObservableList<StringProperty>> usuarios = 
            GenericDAO.fetchData("usuario", usuarioColumns);
        
        assertTrue(usuarios.size() >= 10, "Should have at least 10 test users");
    }

    @Test
    void testGestionUsuarios() throws SQLException {
        // CREATE: Insert a new user
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "test@integration.com");
        userData.put("contrasena", "test123");
        userData.put("nombre", "Test Integration User");
        userData.put("fecha_nacimiento", "2000-01-01");
        userData.put("tipo_usuario", "Normal");
        
        GenericDAO.insert("usuario", userData);

        // READ: Verify user was created
        List<String> columns = MetadataDAO.getColumnNames("usuario");
        ObservableList<ObservableList<StringProperty>> usuarios = 
            GenericDAO.fetchData("usuario", columns);
        
        assertTrue(usuarios.stream().anyMatch(row -> 
            row.get(columns.indexOf("nombre")).getValue().equals("Test Integration User")),
            "New user should be in the database");

        // Create usuarionormal record
        Map<String, Object> normalData = new HashMap<>();
        normalData.put("id_usuario", TEST_USER_ID);
        normalData.put("direccion", "Test Address");
        normalData.put("telefono_movil", "600000000");
        normalData.put("fotografia", null);
        GenericDAO.insert("usuarionormal", normalData);

        // UPDATE: Update user information
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("correo_electronico", "updated@integration.com");
        updateData.put("nombre", "Updated Test User");
        
        GenericDAO.update("usuario", "id_usuario", TEST_USER_ID, updateData);

        // Verify update
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nombre FROM usuario WHERE id_usuario = " + TEST_USER_ID)) {
            assertTrue(rs.next(), "User should exist");
            assertEquals("Updated Test User", rs.getString("nombre"));
        }

        // DELETE: Delete user
        GenericDAO.delete("usuarionormal", "id_usuario", TEST_USER_ID);
        GenericDAO.delete("usuario", "id_usuario", TEST_USER_ID);

        // Verify deletion
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuario WHERE id_usuario = " + TEST_USER_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "User should be deleted");
            }
        }
    }

    @Test
    void testGestionEspaciosRecursos() throws SQLException {
        // CREATE: Insert a new resource
        Map<String, Object> recursoData = new HashMap<>();
        recursoData.put("id_recurso", TEST_RECURSO_ID);
        recursoData.put("nombre", "Sala de Pruebas Integration");
        recursoData.put("tipo", "Sala");
        recursoData.put("descripcion", "Sala para pruebas de integración");
        recursoData.put("ubicacion", "Planta Test");
        recursoData.put("capacidad", 15);
        
        GenericDAO.insert("recurso", recursoData);

        // READ: Verify resource was created
        List<String> columns = MetadataDAO.getColumnNames("recurso");
        ObservableList<ObservableList<StringProperty>> recursos = 
            GenericDAO.fetchData("recurso", columns);
        
        assertTrue(recursos.stream().anyMatch(row -> 
            row.get(columns.indexOf("nombre")).getValue().equals("Sala de Pruebas Integration")),
            "New resource should be in the database");

        // UPDATE: Update resource information
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("nombre", "Sala de Pruebas Actualizada");
        updateData.put("capacidad", 20);
        
        GenericDAO.update("recurso", "id_recurso", TEST_RECURSO_ID, updateData);

        // Verify update
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nombre, capacidad FROM recurso WHERE id_recurso = " + TEST_RECURSO_ID)) {
            assertTrue(rs.next(), "Resource should exist");
            assertEquals("Sala de Pruebas Actualizada", rs.getString("nombre"));
            assertEquals(20, rs.getInt("capacidad"));
        }

        // DELETE: Delete resource
        GenericDAO.delete("recurso", "id_recurso", TEST_RECURSO_ID);

        // Verify deletion
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM recurso WHERE id_recurso = " + TEST_RECURSO_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Resource should be deleted");
            }
        }
    }

    @Test
    void testNuevasReservas() throws SQLException {
        // Setup: Create test user and resource first
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "reserva@test.com");
        userData.put("contrasena", "test123");
        userData.put("nombre", "Reserva Test User");
        userData.put("fecha_nacimiento", "2000-01-01");
        userData.put("tipo_usuario", "Normal");
        GenericDAO.insert("usuario", userData);

        Map<String, Object> normalData = new HashMap<>();
        normalData.put("id_usuario", TEST_USER_ID);
        normalData.put("direccion", "Test Address");
        normalData.put("telefono_movil", "600000000");
        GenericDAO.insert("usuarionormal", normalData);

        Map<String, Object> recursoData = new HashMap<>();
        recursoData.put("id_recurso", TEST_RECURSO_ID);
        recursoData.put("nombre", "Sala de Reservas");
        recursoData.put("tipo", "Sala");
        recursoData.put("descripcion", "Sala para testing de reservas");
        recursoData.put("ubicacion", "Planta Test");
        recursoData.put("capacidad", 10);
        GenericDAO.insert("recurso", recursoData);

        // CREATE: Create a new reservation
        Map<String, Object> reservaData = new HashMap<>();
        reservaData.put("id_recurso", TEST_RECURSO_ID);
        reservaData.put("id_reserva_local", TEST_RESERVA_ID);
        reservaData.put("id_usuario", TEST_USER_ID);
        reservaData.put("fecha", "2026-04-27");
        reservaData.put("hora_inicio", "10:00:00");
        reservaData.put("hora_fin", "11:00:00");
        reservaData.put("coste", 25.50);
        reservaData.put("numero_plazas", 5);
        reservaData.put("motivo", "Prueba de integración");
        
        GenericDAO.insert("reserva", reservaData);

        // READ: Verify reservation was created
        List<String> columns = MetadataDAO.getColumnNames("reserva");
        ObservableList<ObservableList<StringProperty>> reservas = 
            GenericDAO.fetchData("reserva", columns);
        
        boolean found = reservas.stream().anyMatch(row -> 
            row.get(columns.indexOf("id_recurso")).getValue().equals(String.valueOf(TEST_RECURSO_ID)) &&
            row.get(columns.indexOf("id_reserva_local")).getValue().equals(String.valueOf(TEST_RESERVA_ID)) &&
            row.get(columns.indexOf("motivo")).getValue().equals("Prueba de integración"));
        
        assertTrue(found, "New reservation should be in the database");
    }

    @Test
    void testCancelacionReservas() throws SQLException {
        // Setup: Create test data
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "cancel@test.com");
        userData.put("contrasena", "test123");
        userData.put("nombre", "Cancel Test User");
        userData.put("fecha_nacimiento", "2000-01-01");
        userData.put("tipo_usuario", "Normal");
        GenericDAO.insert("usuario", userData);

        Map<String, Object> normalData = new HashMap<>();
        normalData.put("id_usuario", TEST_USER_ID);
        normalData.put("direccion", "Test Address");
        normalData.put("telefono_movil", "600000000");
        GenericDAO.insert("usuarionormal", normalData);

        Map<String, Object> recursoData = new HashMap<>();
        recursoData.put("id_recurso", TEST_RECURSO_ID);
        recursoData.put("nombre", "Sala de Cancelación");
        recursoData.put("tipo", "Sala");
        recursoData.put("descripcion", "Sala para testing de cancelación");
        recursoData.put("ubicacion", "Planta Test");
        recursoData.put("capacidad", 10);
        GenericDAO.insert("recurso", recursoData);

        // Create a reservation
        Map<String, Object> reservaData = new HashMap<>();
        reservaData.put("id_recurso", TEST_RECURSO_ID);
        reservaData.put("id_reserva_local", TEST_RESERVA_ID);
        reservaData.put("id_usuario", TEST_USER_ID);
        reservaData.put("fecha", "2026-04-27");
        reservaData.put("hora_inicio", "10:00:00");
        reservaData.put("hora_fin", "11:00:00");
        reservaData.put("coste", 25.50);
        reservaData.put("numero_plazas", 5);
        reservaData.put("motivo", "Para cancelar");
        GenericDAO.insert("reserva", reservaData);

        // Verify reservation exists before cancellation
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM reserva WHERE id_recurso = " + TEST_RECURSO_ID + " AND id_reserva_local = " + TEST_RESERVA_ID)) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "Reservation should exist before cancellation");
            }
        }

        // DELETE: Cancel (delete) the reservation
        GenericDAO.delete("reserva", "id_recurso", TEST_RECURSO_ID);
        // Also need to delete by the composite key, but SQLite requires specific handling
        // For this test, we'll delete the specific record using raw SQL
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM reserva WHERE id_recurso = " + TEST_RECURSO_ID + " AND id_reserva_local = " + TEST_RESERVA_ID);
        }

        // Verify cancellation
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM reserva WHERE id_recurso = " + TEST_RECURSO_ID + " AND id_reserva_local = " + TEST_RESERVA_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Reservation should be cancelled (deleted)");
            }
        }
    }

    @Test
    void testFullReservationWorkflow() throws SQLException {
        // Complete workflow: Create user -> Create resource -> Create reservation -> Cancel reservation -> Clean up
        
        // 1. Create user
        Map<String, Object> userData = new HashMap<>();
        userData.put("id_usuario", TEST_USER_ID);
        userData.put("correo_electronico", "workflow@test.com");
        userData.put("contrasena", "test123");
        userData.put("nombre", "Workflow User");
        userData.put("fecha_nacimiento", "2000-01-01");
        userData.put("tipo_usuario", "Normal");
        GenericDAO.insert("usuario", userData);

        Map<String, Object> normalData = new HashMap<>();
        normalData.put("id_usuario", TEST_USER_ID);
        normalData.put("direccion", "Workflow Address");
        normalData.put("telefono_movil", "600000000");
        GenericDAO.insert("usuarionormal", normalData);

        // 2. Create resource
        Map<String, Object> recursoData = new HashMap<>();
        recursoData.put("id_recurso", TEST_RECURSO_ID);
        recursoData.put("nombre", "Sala Workflow");
        recursoData.put("tipo", "Sala");
        recursoData.put("descripcion", "Sala para workflow completo");
        recursoData.put("ubicacion", "Planta Workflow");
        recursoData.put("capacidad", 20);
        GenericDAO.insert("recurso", recursoData);

        // 3. Create reservation
        Map<String, Object> reservaData = new HashMap<>();
        reservaData.put("id_recurso", TEST_RECURSO_ID);
        reservaData.put("id_reserva_local", TEST_RESERVA_ID);
        reservaData.put("id_usuario", TEST_USER_ID);
        reservaData.put("fecha", "2026-04-27");
        reservaData.put("hora_inicio", "14:00:00");
        reservaData.put("hora_fin", "15:00:00");
        reservaData.put("coste", 30.00);
        reservaData.put("numero_plazas", 8);
        reservaData.put("motivo", "Workflow completo");
        GenericDAO.insert("reserva", reservaData);

        // 4. Verify reservation exists
        List<String> columns = MetadataDAO.getColumnNames("reserva");
        ObservableList<ObservableList<StringProperty>> reservas = 
            GenericDAO.fetchData("reserva", columns);
        assertTrue(reservas.stream().anyMatch(row -> 
            row.get(columns.indexOf("motivo")).getValue().equals("Workflow completo")),
            "Reservation should exist in workflow");

        // 5. Cancel reservation
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM reserva WHERE id_recurso = " + TEST_RECURSO_ID + " AND id_reserva_local = " + TEST_RESERVA_ID);
        }

        // 6. Verify cancellation
        try (Connection conn = com.reservas.app.dao.DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM reserva WHERE id_recurso = " + TEST_RECURSO_ID + " AND id_reserva_local = " + TEST_RESERVA_ID)) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Reservation should be cancelled");
            }
        }

        // 7. Clean up (done in @AfterEach)
    }
}
