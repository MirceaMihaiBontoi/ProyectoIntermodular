package com.reservas.app.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the same split / SELECT detection the JavaFX SQL console uses, and runs a
 * representative batch against the real SQLite file (see {@link DatabaseManager}).
 */
class SqlConsoleSupportTest {

    @BeforeEach
    void setUp() {
        DatabaseManager.initializeDatabase();
    }

    @Test
    void statements_trimsAndSkipsEmptyParts() {
        List<String> parts =
            SqlConsoleSupport.statements(" SELECT 1;  ;; \n SELECT 2 ; ");
        assertEquals(List.of("SELECT 1", "SELECT 2"), parts);
    }

    @Test
    void isSelect_respectsLeadingWhitespace() {
        assertTrue(SqlConsoleSupport.isSelect("\nselect 1"));
        assertFalse(SqlConsoleSupport.isSelect("UPDATE usuario SET nombre = nombre WHERE 1=0"));
    }

    @Test
    void isSchemaChange_detectsDdlPrefixes() {
        assertTrue(SqlConsoleSupport.isSchemaChange("CREATE TABLE t (x INT)"));
        assertTrue(SqlConsoleSupport.isSchemaChange("drop table t"));
        assertTrue(SqlConsoleSupport.isSchemaChange("alter table t add y text"));
        assertFalse(SqlConsoleSupport.isSchemaChange("INSERT INTO t VALUES (1)"));
    }

    @Test
    void runBatch_likeConsole_allCoreTables() throws SQLException {
        String batch =
            """
            SELECT COUNT(*) AS c FROM usuario;
            SELECT COUNT(*) AS c FROM administrador;
            SELECT COUNT(*) AS c FROM usuarionormal;
            SELECT COUNT(*) AS c FROM recurso;
            SELECT COUNT(*) AS c FROM horario;
            SELECT COUNT(*) AS c FROM disponibleen;
            SELECT COUNT(*) AS c FROM reserva;
            UPDATE recurso SET nombre = nombre WHERE id_recurso = 1;
            INSERT INTO reserva (id_recurso, id_reserva_local, id_usuario, fecha, hora_inicio, hora_fin, coste, numero_plazas, motivo)
            SELECT 1, (SELECT COALESCE(MAX(id_reserva_local), 0) + 1 FROM reserva WHERE id_recurso = 1), 6, '2026-05-04', '09:00:00', '10:00:00', 1.00, 1, 'SQL_CONSOLE_JUNIT';
            DELETE FROM reserva WHERE id_recurso = 1 AND motivo = 'SQL_CONSOLE_JUNIT';
            CREATE TABLE IF NOT EXISTS z_sql_console_probe (id INTEGER PRIMARY KEY);
            DROP TABLE IF EXISTS z_sql_console_probe;
            """;

        for (String statement : SqlConsoleSupport.statements(batch)) {
            if (SqlConsoleSupport.isSelect(statement)) {
                try (Connection conn = DatabaseManager.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(statement)) {
                    assertTrue(rs.next(), "SELECT should return one row: " + statement);
                }
            } else {
                assertDoesNotThrow(
                    () -> GenericDAO.executeRawUpdate(statement),
                    statement
                );
            }
        }
    }
}
