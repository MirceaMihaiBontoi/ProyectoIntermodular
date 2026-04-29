package com.reservas.app.dao;

import com.reservas.app.util.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Specialized DAO for handling usuario table operations.
 * The database already handles ON DELETE CASCADE and ON UPDATE CASCADE via foreign keys.
 * This DAO only handles what the DB cannot do: creating child table records based on tipo_usuario.
 */
public class UsuarioDAO {

    private static final String TIPO_USUARIO = "tipo_usuario";
    private static final String CONTRASENA = "contrasena";
    private static final String USUARIO = "usuario";
    private static final String ADMIN_ROLE = "Administrador";
    private static final String NORMAL_ROLE = "Normal";

    private UsuarioDAO() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Inserts a user and creates the corresponding child table record:
     * - If tipo_usuario is "Administrador", creates record in administrador table
     * - If tipo_usuario is "Normal", creates record in usuarionormal table
     * Password is automatically hashed before insertion.
     */
    public static void insertWithCascade(Map<String, Object> data) throws SQLException {
        String tipoUsuario = (String) data.get(TIPO_USUARIO);
        Object idUsuario = data.get("id_usuario");

        // Hash password before insertion
        if (data.containsKey(CONTRASENA)) {
            String plainPassword = (String) data.get(CONTRASENA);
            data.put(CONTRASENA, PasswordHasher.hashPassword(plainPassword));
        }

        GenericDAO.insert(USUARIO, data);

        if (ADMIN_ROLE.equals(tipoUsuario)) {
            createAdministrador(idUsuario);
        } else if (NORMAL_ROLE.equals(tipoUsuario)) {
            createUsuarioNormal(idUsuario);
        }
    }

    /**
     * Updates a user and handles tipo_usuario changes.
     * Overload for single primary key.
     */
    public static void updateWithCascade(String pkName, Object pkValue, Map<String, Object> data) throws SQLException {
        updateWithCascade(List.of(pkName), List.of(pkValue), data);
    }

    public static void updateWithCascade(List<String> pkNames, List<Object> pkValues, Map<String, Object> data) throws SQLException {
        // Get the current tipo_usuario before update. Assuming single PK for user.
        Object pkValue = pkValues.get(0);
        String currentTipo = getCurrentTipoUsuario(pkValue);
        String newTipo = (String) data.get(TIPO_USUARIO);

        // Hash password if provided in update
        if (data.containsKey(CONTRASENA)) {
            String plainPassword = (String) data.get(CONTRASENA);
            data.put(CONTRASENA, PasswordHasher.hashPassword(plainPassword));
        }

        // Update usuario table
        GenericDAO.update(USUARIO, pkNames, pkValues, data);

        // Handle tipo_usuario change if needed
        if (currentTipo != null && newTipo != null && !currentTipo.equals(newTipo)) {
            // Delete from old child table
            if (ADMIN_ROLE.equals(currentTipo)) {
                deleteFromAdministrador(pkValue);
            } else if (NORMAL_ROLE.equals(currentTipo)) {
                deleteFromUsuarioNormal(pkValue);
            }

            // Create in new child table
            if (ADMIN_ROLE.equals(newTipo)) {
                createAdministrador(pkValue);
            } else if (NORMAL_ROLE.equals(newTipo)) {
                createUsuarioNormal(pkValue);
            }
        }
    }

    /**
     * Standard delete - the DB handles CASCADE automatically via foreign key constraints.
     * Overload for single primary key.
     */
    public static void deleteWithCascade(String pkName, Object pkValue) throws SQLException {
        deleteWithCascade(List.of(pkName), List.of(pkValue));
    }

    public static void deleteWithCascade(List<String> pkNames, List<Object> pkValues) throws SQLException {
        // DB's ON DELETE CASCADE handles deleting from administrador/usuarionormal automatically
        GenericDAO.delete(USUARIO, pkNames, pkValues);
    }

    /**
     * Verifies if a plain text password matches the hashed password stored in the database.
     * 
     * @param idUsuario The user ID to check
     * @param plainPassword The plain text password to verify
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(Object idUsuario, String plainPassword) throws SQLException {
        String hashedPassword = getHashedPassword(idUsuario);
        if (hashedPassword == null) {
            return false;
        }
        return PasswordHasher.verifyPassword(plainPassword, hashedPassword);
    }

    private static String getHashedPassword(Object idUsuario) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT " + CONTRASENA + " FROM usuario WHERE id_usuario = ?")) {
            pstmt.setObject(1, idUsuario);
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString(CONTRASENA);
            }
        }
        return null;
    }

    private static String getCurrentTipoUsuario(Object idUsuario) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT " + TIPO_USUARIO + " FROM usuario WHERE id_usuario = ?")) {
            pstmt.setObject(1, idUsuario);
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString(TIPO_USUARIO);
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
