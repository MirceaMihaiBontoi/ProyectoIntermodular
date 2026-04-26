package com.reservas.app.dao;

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
 * Test class for GenericDAO to verify CRUD operations.
 */
class GenericDAOTest {

    @BeforeEach
    void setUp() {
        DatabaseManager.initializeDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test data after each test
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            // Remove test data if any
            stmt.execute("DELETE FROM recurso WHERE nombre = 'TEST RECURSO'");
        }
    }

    @Test
    void testFetchData() {
        List<String> columns = List.of("id_recurso", "nombre", "tipo", "descripcion", "ubicacion", "capacidad");
        ObservableList<ObservableList<StringProperty>> data = GenericDAO.fetchData("recurso", columns);
        
        assertNotNull(data, "Fetched data should not be null");
        assertTrue(data.size() > 0, "Should fetch at least one record from recurso table");
        
        // Verify structure
        ObservableList<StringProperty> firstRow = data.get(0);
        assertEquals(columns.size(), firstRow.size(), "Row should have same number of columns as requested");
    }

    @Test
    void testInsert() throws SQLException {
        // Prepare test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("id_recurso", 999);
        testData.put("nombre", "TEST RECURSO");
        testData.put("tipo", "TEST");
        testData.put("descripcion", "Test description");
        testData.put("ubicacion", "Test location");
        testData.put("capacidad", 10);

        // Execute insert
        GenericDAO.insert("recurso", testData);

        // Verify insertion
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM recurso WHERE id_recurso = 999")) {
            
            assertTrue(rs.next(), "Inserted record should exist in database");
            assertEquals("TEST RECURSO", rs.getString("nombre"), "Nombre should match");
            assertEquals("TEST", rs.getString("tipo"), "Tipo should match");
        }
    }

    @Test
    void testUpdate() throws SQLException {
        // First insert a test record
        Map<String, Object> insertData = new HashMap<>();
        insertData.put("id_recurso", 998);
        insertData.put("nombre", "ORIGINAL NAME");
        insertData.put("tipo", "ORIGINAL");
        insertData.put("descripcion", "Original description");
        insertData.put("ubicacion", "Original location");
        insertData.put("capacidad", 5);
        GenericDAO.insert("recurso", insertData);

        // Update the record
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("nombre", "UPDATED NAME");
        updateData.put("tipo", "UPDATED");
        updateData.put("descripcion", "Updated description");
        updateData.put("ubicacion", "Updated location");
        updateData.put("capacidad", 15);
        
        GenericDAO.update("recurso", "id_recurso", 998, updateData);

        // Verify update
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM recurso WHERE id_recurso = 998")) {
            
            assertTrue(rs.next(), "Updated record should exist");
            assertEquals("UPDATED NAME", rs.getString("nombre"), "Nombre should be updated");
            assertEquals("UPDATED", rs.getString("tipo"), "Tipo should be updated");
            assertEquals(15, rs.getInt("capacidad"), "Capacidad should be updated");
        }

        // Clean up
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM recurso WHERE id_recurso = 998");
        }
    }

    @Test
    void testDelete() throws SQLException {
        // First insert a test record
        Map<String, Object> insertData = new HashMap<>();
        insertData.put("id_recurso", 997);
        insertData.put("nombre", "TO DELETE");
        insertData.put("tipo", "DELETE");
        insertData.put("descripcion", "To be deleted");
        insertData.put("ubicacion", "Delete location");
        insertData.put("capacidad", 1);
        GenericDAO.insert("recurso", insertData);

        // Verify it exists
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM recurso WHERE id_recurso = 997")) {
            if (rs.next()) {
                assertEquals(1, rs.getInt(1), "Record should exist before delete");
            }
        }

        // Delete the record
        GenericDAO.delete("recurso", "id_recurso", 997);

        // Verify deletion
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM recurso WHERE id_recurso = 997")) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1), "Record should not exist after delete");
            }
        }
    }

    @Test
    void testExecuteRawSql() throws SQLException {
        // Execute raw SQL
        String sql = "INSERT INTO recurso (id_recurso, nombre, tipo, descripcion, ubicacion, capacidad) VALUES (996, 'RAW SQL TEST', 'RAW', 'Raw SQL insert', 'Raw location', 20)";
        GenericDAO.executeRawSql(sql);

        // Verify execution
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM recurso WHERE id_recurso = 996")) {
            
            assertTrue(rs.next(), "Record inserted via raw SQL should exist");
            assertEquals("RAW SQL TEST", rs.getString("nombre"));
        }

        // Clean up
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM recurso WHERE id_recurso = 996");
        }
    }

    @Test
    void testFullCrudCycle() throws SQLException {
        // CREATE
        Map<String, Object> data = new HashMap<>();
        data.put("id_recurso", 995);
        data.put("nombre", "CRUD TEST");
        data.put("tipo", "CRUD");
        data.put("descripcion", "Full CRUD test");
        data.put("ubicacion", "CRUD location");
        data.put("capacidad", 25);
        GenericDAO.insert("recurso", data);

        // READ
        List<String> columns = List.of("id_recurso", "nombre", "tipo", "descripcion", "ubicacion", "capacidad");
        ObservableList<ObservableList<StringProperty>> fetchedData = GenericDAO.fetchData("recurso", columns);
        assertTrue(fetchedData.stream().anyMatch(row -> row.get(1).getValue().equals("CRUD TEST")), 
            "Inserted record should be fetchable");

        // UPDATE
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("nombre", "CRUD UPDATED");
        updateData.put("tipo", "CRUD");
        updateData.put("descripcion", "Full CRUD test updated");
        updateData.put("ubicacion", "CRUD location");
        updateData.put("capacidad", 30);
        GenericDAO.update("recurso", "id_recurso", 995, updateData);

        // Verify update
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nombre FROM recurso WHERE id_recurso = 995")) {
            if (rs.next()) {
                assertEquals("CRUD UPDATED", rs.getString("nombre"));
            }
        }

        // DELETE
        GenericDAO.delete("recurso", "id_recurso", 995);

        // Verify deletion
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM recurso WHERE id_recurso = 995")) {
            if (rs.next()) {
                assertEquals(0, rs.getInt(1));
            }
        }
    }
}
