package com.reservas.app.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Specialized DAO for handling usuario table operations.
 * The database already handles ON DELETE CASCADE and ON UPDATE CASCADE via foreign keys.
 * This DAO only handles what the DB cannot do: creating child table records based on tipo_usuario.
 */
public class UsuarioDAO {

    private UsuarioDAO() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Inserts a user and creates the corresponding child table record:
     * - If tipo_usuario is "Administrador", creates record in administrador table
     * - If tipo_usuario is "Normal", creates record in usuarionormal table
     */
    public static void insertWithCascade(Map<String, Object> data) throws SQLException {
        String tipoUsuario = (String) data.get("tipo_usuario");
        Object idUsuario = data.get("id_usuario");

        GenericDAO.insert("usuario", data);

        if ("Administrador".equals(tipoUsuario)) {
            createAdministrador(idUsuario);
        } else if ("Normal".equals(tipoUsuario)) {
            createUsuarioNormal(idUsuario);
        }
    }

    /**
     * Updates a user and handles tipo_usuario changes:
     * - If tipo_usuario changed from Administrador to Normal: delete from administrador, create in usuarionormal
     * - If tipo_usuario changed from Normal to Administrador: delete from usuarionormal, create in administrador
     */
    public static void updateWithCascade(String pkName, Object pkValue, Map<String, Object> data) throws SQLException {
        // Get the current tipo_usuario before update
        String currentTipo = getCurrentTipoUsuario(pkValue);
        String newTipo = (String) data.get("tipo_usuario");

        // Update usuario table
        GenericDAO.update("usuario", pkName, pkValue, data);

        // Handle tipo_usuario change if needed
        if (currentTipo != null && newTipo != null && !currentTipo.equals(newTipo)) {
            // Delete from old child table
            if ("Administrador".equals(currentTipo)) {
                deleteFromAdministrador(pkValue);
            } else if ("Normal".equals(currentTipo)) {
                deleteFromUsuarioNormal(pkValue);
            }

            // Create in new child table
            if ("Administrador".equals(newTipo)) {
                createAdministrador(pkValue);
            } else if ("Normal".equals(newTipo)) {
                createUsuarioNormal(pkValue);
            }
        }
    }

    /**
     * Standard delete - the DB handles CASCADE automatically via foreign key constraints.
     * This method just calls GenericDAO.delete.
     */
    public static void deleteWithCascade(String pkName, Object pkValue) throws SQLException {
        // DB's ON DELETE CASCADE handles deleting from administrador/usuarionormal automatically
        GenericDAO.delete("usuario", pkName, pkValue);
    }

    private static String getCurrentTipoUsuario(Object idUsuario) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT tipo_usuario FROM usuario WHERE id_usuario = ?")) {
            pstmt.setObject(1, idUsuario);
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("tipo_usuario");
            }
        }
        return null;
    }

    private static void createAdministrador(Object idUsuario) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO administrador (id_usuario, telefono_guardia) VALUES (?, ?)")) {
            pstmt.setObject(1, idUsuario);
            pstmt.setString(2, "");
            pstmt.executeUpdate();
        }
    }

    private static void createUsuarioNormal(Object idUsuario) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO usuarionormal (id_usuario, direccion, telefono_movil, fotografia) VALUES (?, ?, ?, ?)")) {
            pstmt.setObject(1, idUsuario);
            pstmt.setString(2, "");
            pstmt.setString(3, "");
            pstmt.setString(4, null);
            pstmt.executeUpdate();
        }
    }

    private static void deleteFromAdministrador(Object idUsuario) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "DELETE FROM administrador WHERE id_usuario = ?")) {
            pstmt.setObject(1, idUsuario);
            pstmt.executeUpdate();
        }
    }

    private static void deleteFromUsuarioNormal(Object idUsuario) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "DELETE FROM usuarionormal WHERE id_usuario = ?")) {
            pstmt.setObject(1, idUsuario);
            pstmt.executeUpdate();
        }
    }
}
