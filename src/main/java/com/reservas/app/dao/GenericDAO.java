package com.reservas.app.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Data Access Object (DAO) that handles generic database operations.
 * It uses dynamic SQL construction to avoid writing specific code for each table.
 */
public class GenericDAO {

    private static final Logger logger = Logger.getLogger(
        GenericDAO.class.getName()
    );

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
    public static ObservableList<ObservableList<StringProperty>> fetchData(
        String tableName,
        List<String> columns
    ) {
        ObservableList<ObservableList<StringProperty>> data =
            FXCollections.observableArrayList();
        // Dynamic SQL: We don't know the table name yet, so we concatenate it.
        String sql = "SELECT * FROM " + tableName;

        try (
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                ObservableList<StringProperty> row =
                    FXCollections.observableArrayList();
                for (String col : columns) {
                    String val = rs.getString(col);
                    // Wrapping the database value into a SimpleStringProperty for the UI
                    row.add(new SimpleStringProperty(val != null ? val : ""));
                }
                data.add(row);
            }
        } catch (SQLException e) {
            logger.log(
                Level.SEVERE,
                e,
                () -> "Failed to fetch data from " + tableName
            );
        }
        return data;
    }

    /**
     * Fetches all rows from a table and returns them as a List of Maps.
     * This method is suitable for web APIs and non-JavaFX contexts.
     */
    public static List<Map<String, Object>> fetchDataAsMaps(
        String tableName,
        List<String> columns
    ) {
        List<Map<String, Object>> data = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName;

        try (
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                Map<String, Object> row = new java.util.HashMap<>();
                for (String col : columns) {
                    Object val = rs.getObject(col);
                    // Convert SQL specific types to String to avoid serialization issues
                    if (
                        val instanceof java.sql.Date ||
                        val instanceof java.sql.Time ||
                        val instanceof java.sql.Timestamp
                    ) {
                        val = val.toString();
                    }
                    row.put(col, val);
                }
                data.add(row);
            }
        } catch (SQLException e) {
            logger.log(
                Level.SEVERE,
                e,
                () -> "Failed to fetch data from " + tableName
            );
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
    public static void insert(String tableName, Map<String, Object> data)
        throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            insert(conn, tableName, data);
        }
    }

    /**
     * Transaction-aware INSERT variant.
     *
     * Specialized DAOs use this overload when several statements must succeed or
     * fail together using the same connection. The public insert(table, data)
     * method remains available for simple generic CRUD operations.
     */
    public static void insert(
        Connection conn,
        String tableName,
        Map<String, Object> data
    ) throws SQLException {
        if (data == null || data.isEmpty()) {
            throw new SQLException(
                "No data provided for insert into " + tableName
            );
        }

        String columns = String.join(", ", data.keySet());
        // Java Streams: a modern way to process collections.
        // Here we create one "?" for each key in the map.
        String placeholders = data
            .keySet()
            .stream()
            .map(k -> "?")
            .collect(Collectors.joining(", "));

        String sql = String.format(
            "INSERT INTO %s (%s) VALUES (%s)",
            tableName,
            columns,
            placeholders
        );
        try {
            executeUpdate(conn, sql, data.values());
        } catch (SQLException e) {
            throw new SQLException(
                "Error inserting into " +
                    tableName +
                    ". Data: " +
                    data +
                    ". " +
                    e.getMessage(),
                e
            );
        }
    }

    /**
     * Overload for single primary key.
     */
    public static void update(
        String tableName,
        String pkName,
        Object pkValue,
        Map<String, Object> data
    ) throws SQLException {
        update(tableName, List.of(pkName), List.of(pkValue), data);
    }

    /**
     * Updates one or more rows in a table.
     * Use this for tables with single or composite primary keys.
     */
    public static void update(
        String tableName,
        List<String> pkNames,
        List<Object> pkValues,
        Map<String, Object> data
    ) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            update(conn, tableName, pkNames, pkValues, data);
        }
    }

    /**
     * Transaction-aware UPDATE variant.
     * Supports composite primary keys and is reused by specialized DAOs so they
     * can keep parent/child tables consistent inside one transaction.
     */
    public static void update(
        Connection conn,
        String tableName,
        List<String> pkNames,
        List<Object> pkValues,
        Map<String, Object> data
    ) throws SQLException {
        if (data == null || data.isEmpty()) {
            throw new SQLException(
                "No editable data provided for update on " + tableName
            );
        }
        if (
            pkNames == null ||
            pkValues == null ||
            pkNames.size() != pkValues.size() ||
            pkNames.isEmpty()
        ) {
            throw new SQLException(
                "Invalid primary key for update on " + tableName
            );
        }

        String setClause = data
            .keySet()
            .stream()
            .map(col -> col + " = ?")
            .collect(Collectors.joining(", "));
        String whereClause = pkNames
            .stream()
            .map(pk -> pk + " = ?")
            .collect(Collectors.joining(" AND "));
        String sql = String.format(
            "UPDATE %s SET %s WHERE %s",
            tableName,
            setClause,
            whereClause
        );

        List<Object> params = new ArrayList<>(data.values());
        params.addAll(pkValues);

        try {
            executeUpdate(conn, sql, params);
        } catch (SQLException e) {
            throw new SQLException(
                "Error updating " +
                    tableName +
                    " (PKs: " +
                    pkNames +
                    "=" +
                    pkValues +
                    "). Data: " +
                    data +
                    ". " +
                    e.getMessage(),
                e
            );
        }
    }

    /**
     * Overload for single primary key.
     */
    public static void delete(String tableName, String pkName, Object pkValue)
        throws SQLException {
        delete(tableName, List.of(pkName), List.of(pkValue));
    }

    /**
     * Deletes one or more rows from a table.
     * Use this for tables with single or composite primary keys.
     */
    public static void delete(
        String tableName,
        List<String> pkNames,
        List<Object> pkValues
    ) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            delete(conn, tableName, pkNames, pkValues);
        }
    }

    /**
     * Transaction-aware DELETE variant.
     * It is useful for explicit cascade logic, while normal database cascades
     * still work through the standard delete(table, pk, value) overload.
     */
    public static void delete(
        Connection conn,
        String tableName,
        List<String> pkNames,
        List<Object> pkValues
    ) throws SQLException {
        if (
            pkNames == null ||
            pkValues == null ||
            pkNames.size() != pkValues.size() ||
            pkNames.isEmpty()
        ) {
            throw new SQLException(
                "Invalid primary key for delete on " + tableName
            );
        }

        String whereClause = pkNames
            .stream()
            .map(pk -> pk + " = ?")
            .collect(Collectors.joining(" AND "));
        String sql = String.format(
            "DELETE FROM %s WHERE %s",
            tableName,
            whereClause
        );
        executeUpdate(conn, sql, pkValues);
    }

    /**
     * Common method to execute prepared statements with dynamic parameters.
     * IMPORTANT: Using PreparedStatement prevents "SQL Injection", a common
     * security vulnerability where hackers could send malicious commands.
     */
    private static void executeUpdate(
        Connection conn,
        String sql,
        Collection<Object> params
    ) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int i = 1;
            // We loop through the parameters and set them into the "?" placeholders
            for (Object param : params) {
                pstmt.setObject(i++, normalizeParam(param));
            }
            pstmt.executeUpdate();
        }
    }

    /**
     * Empty strings from JavaFX/Web forms are treated as NULL in the database.
     * This keeps optional columns semantically clean and avoids mixing NULL and
     * empty text as two different representations of "no value".
     */
    private static Object normalizeParam(Object param) {
        if (param instanceof String str && str.trim().isEmpty()) {
            return null;
        }
        return param;
    }

    /**
     * Executes a raw SQL statement (used by the text console in the app).
     * Returns the number of affected rows.
     */
    public static int executeRawUpdate(String sql) throws SQLException {
        try (
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement()
        ) {
            return stmt.executeUpdate(sql);
        }
    }
}
