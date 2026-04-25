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
            logger.log(Level.SEVERE, "Failed to fetch table names", e);
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
            logger.log(Level.SEVERE, "Failed to fetch columns for " + tableName, e);
        }
        return columns;
    }

    /**
     * Finds the Primary Key (PK) of a table.
     */
    public static String getPrimaryKey(String tableName) {
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.getMetaData().getPrimaryKeys(null, null, tableName)) {
            if (rs.next()) return rs.getString("COLUMN_NAME");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to fetch primary key for " + tableName, e);
        }
        return null;
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
            logger.log(Level.SEVERE, "Failed to fetch foreign keys for " + tableName, e);
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
            logger.log(Level.SEVERE, "Failed to fetch rows from " + table, e);
        }
        return values;
    }
}
