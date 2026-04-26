package com.reservas.app.dao;

import com.reservas.app.model.ForeignKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MetadataDAO to verify metadata retrieval operations.
 */
class MetadataDAOTest {

    @BeforeEach
    void setUp() {
        DatabaseManager.initializeDatabase();
    }

    @Test
    void testGetTableNames() {
        List<String> tables = MetadataDAO.getTableNames();
        
        assertNotNull(tables, "Table names list should not be null");
        assertTrue(tables.size() > 0, "Should have at least one table");
        
        // Verify expected tables exist
        assertTrue(tables.contains("usuario"), "Should contain 'usuario' table");
        assertTrue(tables.contains("reserva"), "Should contain 'reserva' table");
        assertTrue(tables.contains("recurso"), "Should contain 'recurso' table");
    }

    @Test
    void testGetColumnNames() {
        List<String> columns = MetadataDAO.getColumnNames("usuario");
        
        assertNotNull(columns, "Column names list should not be null");
        assertTrue(columns.size() > 0, "Should have at least one column");
        
        // Verify expected columns exist
        assertTrue(columns.contains("id_usuario"), "Should contain 'id_usuario' column");
        assertTrue(columns.contains("correo_electronico"), "Should contain 'correo_electronico' column");
        assertTrue(columns.contains("nombre"), "Should contain 'nombre' column");
    }

    @Test
    void testGetPrimaryKey() {
        String pk = MetadataDAO.getPrimaryKey("usuario");
        
        assertNotNull(pk, "Primary key should not be null for usuario table");
        assertEquals("id_usuario", pk, "Primary key for usuario should be id_usuario");
    }

    @Test
    void testGetPrimaryKeyReserva() {
        String pk = MetadataDAO.getPrimaryKey("reserva");
        
        assertNotNull(pk, "Primary key should not be null for reserva table");
        // reserva has composite primary key (id_recurso, id_reserva_local)
        // SQLite returns the first column of the primary key
        assertTrue(pk.equals("id_recurso") || pk.equals("id_reserva_local"), 
            "Primary key for reserva should be one of the composite key columns");
    }

    @Test
    void testGetForeignKeys() {
        Map<String, ForeignKey> foreignKeys = MetadataDAO.getForeignKeys("reserva");
        
        assertNotNull(foreignKeys, "Foreign keys map should not be null");
        assertTrue(foreignKeys.size() > 0, "Reserva table should have foreign keys");
        
        // Verify expected foreign keys
        boolean hasRecursoFk = foreignKeys.values().stream()
            .anyMatch(fk -> fk.refTable().equals("recurso"));
        assertTrue(hasRecursoFk, "Reserva should have foreign key to recurso");
        
        boolean hasUsuarioFk = foreignKeys.values().stream()
            .anyMatch(fk -> fk.refTable().equals("usuarionormal"));
        assertTrue(hasUsuarioFk, "Reserva should have foreign key to usuarionormal");
    }

    @Test
    void testGetForeignKeysUsuario() {
        Map<String, ForeignKey> foreignKeys = MetadataDAO.getForeignKeys("usuario");
        
        assertNotNull(foreignKeys, "Foreign keys map should not be null");
        // usuario is a parent table, should have no foreign keys
        assertEquals(0, foreignKeys.size(), "Usuario table should have no foreign keys");
    }

    @Test
    void testGetForeignKeysAdministrador() {
        Map<String, ForeignKey> foreignKeys = MetadataDAO.getForeignKeys("administrador");
        
        assertNotNull(foreignKeys, "Foreign keys map should not be null");
        assertTrue(foreignKeys.size() > 0, "Administrador table should have foreign keys");
        
        // Verify foreign key to usuario
        boolean hasUsuarioFk = foreignKeys.values().stream()
            .anyMatch(fk -> fk.refTable().equals("usuario"));
        assertTrue(hasUsuarioFk, "Administrador should have foreign key to usuario");
    }

    @Test
    void testGetColumnNamesRecurso() {
        List<String> columns = MetadataDAO.getColumnNames("recurso");
        
        assertNotNull(columns, "Column names list should not be null");
        assertTrue(columns.size() > 0, "Should have at least one column");
        
        // Verify expected columns
        assertTrue(columns.contains("id_recurso"), "Should contain 'id_recurso' column");
        assertTrue(columns.contains("nombre"), "Should contain 'nombre' column");
        assertTrue(columns.contains("tipo"), "Should contain 'tipo' column");
        assertTrue(columns.contains("descripcion"), "Should contain 'descripcion' column");
        assertTrue(columns.contains("ubicacion"), "Should contain 'ubicacion' column");
        assertTrue(columns.contains("capacidad"), "Should contain 'capacidad' column");
    }

    @Test
    void testGetColumnNamesReserva() {
        List<String> columns = MetadataDAO.getColumnNames("reserva");
        
        assertNotNull(columns, "Column names list should not be null");
        assertTrue(columns.size() > 0, "Should have at least one column");
        
        // Verify expected columns
        assertTrue(columns.contains("id_recurso"), "Should contain 'id_recurso' column");
        assertTrue(columns.contains("id_reserva_local"), "Should contain 'id_reserva_local' column");
        assertTrue(columns.contains("id_usuario"), "Should contain 'id_usuario' column");
        assertTrue(columns.contains("fecha"), "Should contain 'fecha' column");
        assertTrue(columns.contains("hora_inicio"), "Should contain 'hora_inicio' column");
        assertTrue(columns.contains("hora_fin"), "Should contain 'hora_fin' column");
    }

    @Test
    void testGetAllTablesFromSchema() {
        List<String> tables = MetadataDAO.getTableNames();
        
        // Verify all expected tables from the schema exist
        String[] expectedTables = {"usuario", "administrador", "usuarionormal", 
                                   "recurso", "horario", "disponibleen", "reserva"};
        
        for (String expectedTable : expectedTables) {
            assertTrue(tables.contains(expectedTable), 
                "Should contain table: " + expectedTable);
        }
    }
}
