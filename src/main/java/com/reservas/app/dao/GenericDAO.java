package com.reservas.app.dao;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Data Access Object (DAO) that handles generic database operations.
 * It uses dynamic SQL construction to avoid writing specific code for each table.
 */
public class GenericDAO {

    private static final Logger logger = Logger.getLogger(GenericDAO.class.getName());

    private GenericDAO() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Fetches all rows from a table and returns them as an ObservableList of rows.
     * 
     * WHAT IS ObservableList? 
     * It's a special list used by JavaFX. When you add or remove items from it, 
     * the UI (like a TableView) automatically updates itself.
     * 
     * WHAT IS StringProperty?
     * It's a wrapper for a String. It allows "binding", meaning the UI 
     * "listens" to changes in this value and reflects them instantly.
     */
    public static ObservableList<ObservableList<StringProperty>> fetchData(String tableName, List<String> columns) {
        ObservableList<ObservableList<StringProperty>> data = FXCollections.observableArrayList();
        // Dynamic SQL: We don't know the table name yet, so we concatenate it.
        String sql = "SELECT * FROM " + tableName;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ObservableList<StringProperty> row = FXCollections.observableArrayList();
                for (String col : columns) {
                    String val = rs.getString(col);
                    // Wrapping the database value into a SimpleStringProperty for the UI
                    row.add(new SimpleStringProperty(val != null ? val : ""));
                }
                data.add(row);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to fetch data from " + tableName, e);
        }
        return data;
    }

    /**
     * Dynamically builds and executes an INSERT statement.
     * 
     * How it works:
     * 1. It takes a Map (key=column name, value=data).
     * 2. It creates a string of columns: "name, email, age".
     * 3. It creates a string of placeholders: "?, ?, ?".
     * 4. PreparedStatement will safely replace those "?" with the actual data.
     */
    public static void insert(String tableName, Map<String, Object> data) throws SQLException {
        String columns = String.join(", ", data.keySet());
        // Java Streams: a modern way to process collections. 
        // Here we create one "?" for each key in the map.
        String placeholders = data.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
        executeUpdate(sql, data.values());
    }

    /**
     * Dynamically builds and executes an UPDATE statement.
     * @param pkName The name of the Primary Key column (e.g., "id_usuario")
     * @param pkValue The value of the row we want to update.
     */
    public static void update(String tableName, String pkName, Object pkValue, Map<String, Object> data) throws SQLException {
        // We create the "COLUMN = ?" part for the SQL SET clause
        String setClause = data.keySet().stream().map(col -> col + " = ?").collect(Collectors.joining(", "));
        String sql = String.format("UPDATE %s SET %s WHERE %s = ?", tableName, setClause, pkName);

        // Merge the new values with the Primary Key value at the end (last "?" is the WHERE clause)
        List<Object> params = new ArrayList<>(data.values());
        params.add(pkValue);

        executeUpdate(sql, params);
    }

    /**
     * Deletes a row based on its Primary Key.
     */
    public static void delete(String tableName, String pkName, Object pkValue) throws SQLException {
        String sql = String.format("DELETE FROM %s WHERE %s = ?", tableName, pkName);
        executeUpdate(sql, List.of(pkValue));
    }

    /**
     * Common method to execute prepared statements with dynamic parameters.
     * IMPORTANT: Using PreparedStatement prevents "SQL Injection", a common 
     * security vulnerability where hackers could send malicious commands.
     */
    private static void executeUpdate(String sql, Collection<Object> params) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int i = 1;
            // We loop through the parameters and set them into the "?" placeholders
            for (Object param : params) {
                pstmt.setObject(i++, param);
            }
            pstmt.executeUpdate();
        }
    }

    /**
     * Executes a raw SQL statement (used by the text console in the app).
     */
    public static void executeRawSql(String sql) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
}
