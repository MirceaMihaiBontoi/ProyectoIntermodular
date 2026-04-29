package com.reservas.app.dao;

import com.reservas.app.model.ForeignKey;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Explorer class that uses JDBC Metadata to understand the database schema.
 * This is what makes the application "Dynamic".
 */
public class MetadataDAO {

    private static final Logger logger = Logger.getLogger(MetadataDAO.class.getName());

    private MetadataDAO() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieves all table names from the database.
     */
    public static List<String> getTableNames() {
        List<String> tables = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e, () -> "Failed to fetch table names");
        }
        return tables;
    }

    /**
     * Gets all column names for a specific table.
     */
    public static List<String> getColumnNames(String tableName) {
        List<String> columns = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e, () -> "Failed to fetch columns for " + tableName);
        }
        return columns;
    }

    /**
     * Gets the data type of a specific column.
     * Returns the SQL type name (e.g., "DATE", "INTEGER", "TEXT").
     */
    public static String getColumnType(String tableName, String columnName) {
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            if (rs.next()) {
                return rs.getString("TYPE_NAME");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e, () -> "Failed to fetch column type for " + tableName + "." + columnName);
        }
        return "TEXT"; // Default to TEXT if type cannot be determined
    }

    /**
     * Gets allowed values for a column with CHECK constraints.
     * Currently hardcoded for known columns since SQLite doesn't expose CHECK constraints via JDBC metadata.
     */
    public static List<String> getAllowedValues(String tableName, String columnName) {
        // Hardcoded for the tipo_usuario CHECK constraint in usuario table
        if ("usuario".equals(tableName) && "tipo_usuario".equals(columnName)) {
            return List.of("Administrador", "Normal");
        }
        // Hardcoded for the dia_semana CHECK constraint in horario table
        if ("horario".equals(tableName) && "dia_semana".equals(columnName)) {
            return List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");
        }
        return List.of(); // No known allowed values
    }

    /**
     * Returns the primary key(s) of the specified table.
     */
    public static List<String> getPrimaryKeys(String tableName) {
        List<String> pks = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.getMetaData().getPrimaryKeys(null, null, tableName)) {
            while (rs.next()) {
                pks.add(rs.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e, () -> "Error getting primary keys: " + e.getMessage());
        }
        return pks;
    }

    /**
     * Convenience method for tables with a single primary key.
     * If the table has a composite key, returns the first column found.
     */
    public static String getPrimaryKey(String tableName) {
        List<String> pks = getPrimaryKeys(tableName);
        return pks.isEmpty() ? null : pks.get(0);
    }

    /**
     * Maps Foreign Keys (FK) to understand relationships between tables.
     */
    public static Map<String, ForeignKey> getForeignKeys(String tableName) {
        Map<String, ForeignKey> fks = new HashMap<>();
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.getMetaData().getImportedKeys(null, null, tableName)) {
            while (rs.next()) {
                String fkColumn = rs.getString("FKCOLUMN_NAME");
                fks.put(fkColumn, new ForeignKey(
                    fkColumn,
                    rs.getString("PKTABLE_NAME"),
                    rs.getString("PKCOLUMN_NAME")
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e, () -> "Failed to fetch foreign keys for " + tableName);
        }
        return fks;
    }

    /**
     * Utility method to fetch all values from a specific column.
     * Used to populate ComboBoxes (dropdowns) in the forms.
     */
    public static List<String> getAllRowsFromTable(String table, String column) {
        List<String> values = new ArrayList<>();
        // Note: identifiers come from our own metadata, not from user input.
        String sql = "SELECT " + column + " FROM " + table;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e, () -> "Failed to fetch rows from " + table);
        }
        return values;
    }
}
