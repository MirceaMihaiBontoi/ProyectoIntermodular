package com.reservas.app.dao;

import com.reservas.app.util.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specialized DAO for usuario + subtype tables.
 * The three tables are managed as one logical aggregate to keep data consistent.
 */
public class UsuarioDAO {

    private static final String TIPO_USUARIO = "tipo_usuario";
    private static final String CONTRASENA = "contrasena";
    private static final String ID_USUARIO = "id_usuario";
    private static final String USUARIO = "usuario";
    private static final String ADMIN_ROLE = "Administrador";
    private static final String NORMAL_ROLE = "Normal";

    private UsuarioDAO() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates the logical user aggregate: one row in usuario plus exactly one
     * subtype row in administrador or usuarionormal.
     *
     * Both inserts run in the same transaction. Without this, a failure in the
     * subtype insert could leave a usuario row without its specialization.
     */
    public static void insertWithCascade(Map<String, Object> data)
        throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                Map<String, Object> workingData = new HashMap<>(data);
                // Never mutate the map received from UI/Web directly: callers may reuse it.
                preparePasswordForInsert(workingData);

                String tipoUsuario = (String) workingData.get(TIPO_USUARIO);
                Object idUsuario = workingData.get(ID_USUARIO);

                GenericDAO.insert(
                    conn,
                    USUARIO,
                    filterUsuarioData(workingData)
                );

                if (ADMIN_ROLE.equals(tipoUsuario)) {
                    createAdministrador(conn, idUsuario, workingData);
                } else if (NORMAL_ROLE.equals(tipoUsuario)) {
                    createUsuarioNormal(conn, idUsuario, workingData);
                }

                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public static void updateWithCascade(
        String pkName,
        Object pkValue,
        Map<String, Object> data
    ) throws SQLException {
        updateWithCascade(List.of(pkName), List.of(pkValue), data);
    }

    /**
     * Updates usuario and its subtype as one aggregate.
     *
     * The generic JavaFX dashboard sends form values as a flat map, but the
     * database stores subtype fields in separate tables. This method separates
     * base user fields from subtype fields and keeps everything transactional.
     */
    public static void updateWithCascade(
        List<String> pkNames,
        List<Object> pkValues,
        Map<String, Object> data
    ) throws SQLException {
        if (pkValues == null || pkValues.isEmpty()) {
            throw new SQLException("Missing usuario primary key");
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                Object idUsuario = pkValues.get(0);
                String currentTipo = getCurrentTipoUsuario(conn, idUsuario);

                Map<String, Object> workingData = new HashMap<>(data);
                // Primary keys identify the selected row; they are not editable through the generic form.
                pkNames.forEach(workingData::remove);
                preparePasswordForUpdate(workingData);

                String newTipo = (String) workingData.getOrDefault(
                    TIPO_USUARIO,
                    currentTipo
                );
                Map<String, Object> usuarioData = filterUsuarioData(
                    workingData
                );
                if (!usuarioData.isEmpty()) {
                    GenericDAO.update(
                        conn,
                        USUARIO,
                        pkNames,
                        pkValues,
                        usuarioData
                    );
                }

                if (
                    currentTipo != null &&
                    newTipo != null &&
                    !currentTipo.equals(newTipo)
                ) {
                    deleteSubtype(conn, currentTipo, idUsuario);
                    createSubtype(conn, newTipo, idUsuario, workingData);
                } else if (ADMIN_ROLE.equals(currentTipo)) {
                    updateAdministrador(conn, idUsuario, workingData);
                } else if (NORMAL_ROLE.equals(currentTipo)) {
                    updateUsuarioNormal(conn, idUsuario, workingData);
                }

                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public static void deleteWithCascade(String pkName, Object pkValue)
        throws SQLException {
        deleteWithCascade(List.of(pkName), List.of(pkValue));
    }

    public static void deleteWithCascade(
        List<String> pkNames,
        List<Object> pkValues
    ) throws SQLException {
        GenericDAO.delete(USUARIO, pkNames, pkValues);
    }

    public static boolean verifyPassword(Object idUsuario, String plainPassword)
        throws SQLException {
        String hashedPassword = getHashedPassword(idUsuario);
        return (
            hashedPassword != null &&
            PasswordHasher.verifyPassword(plainPassword, hashedPassword)
        );
    }

    /**
     * Loads subtype fields so the JavaFX usuario form can edit the full logical
     * user without requiring the admin to switch to administrador/usuarionormal tabs.
     */
    public static Map<String, Object> getSubtypeData(
        Object idUsuario,
        String tipoUsuario
    ) throws SQLException {
        if (ADMIN_ROLE.equals(tipoUsuario)) {
            return fetchOne(
                "SELECT telefono_guardia FROM administrador WHERE id_usuario = ?",
                idUsuario
            );
        }
        if (NORMAL_ROLE.equals(tipoUsuario)) {
            return fetchOne(
                "SELECT direccion, telefono_movil, fotografia FROM usuarionormal WHERE id_usuario = ?",
                idUsuario
            );
        }
        return Map.of();
    }

    /**
     * Hash plain passwords on insert, but accept already-hashed values for seed
     * data or migration scenarios.
     */
    private static void preparePasswordForInsert(Map<String, Object> data) {
        Object value = data.get(CONTRASENA);
        if (
            value instanceof String password &&
            !password.isBlank() &&
            !isBcryptHash(password)
        ) {
            data.put(CONTRASENA, PasswordHasher.hashPassword(password));
        }
    }

    /**
     * On update, an empty password means "do not change it". This prevents the
     * dashboard from rehashing the existing BCrypt hash when editing other fields.
     */
    private static void preparePasswordForUpdate(Map<String, Object> data) {
        Object value = data.get(CONTRASENA);
        if (
            !(value instanceof String password) ||
            password.isBlank() ||
            isBcryptHash(password)
        ) {
            data.remove(CONTRASENA);
            return;
        }
        data.put(CONTRASENA, PasswordHasher.hashPassword(password));
    }

    private static boolean isBcryptHash(String value) {
        return (
            value.startsWith("$2a$") ||
            value.startsWith("$2b$") ||
            value.startsWith("$2y$")
        );
    }

    private static String getHashedPassword(Object idUsuario)
        throws SQLException {
        try (
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT contrasena FROM usuario WHERE id_usuario = ?"
            )
        ) {
            pstmt.setObject(1, idUsuario);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString(CONTRASENA) : null;
            }
        }
    }

    private static String getCurrentTipoUsuario(
        Connection conn,
        Object idUsuario
    ) throws SQLException {
        try (
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT tipo_usuario FROM usuario WHERE id_usuario = ?"
            )
        ) {
            pstmt.setObject(1, idUsuario);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString(TIPO_USUARIO) : null;
            }
        }
    }

    private static Map<String, Object> fetchOne(String sql, Object idUsuario)
        throws SQLException {
        try (
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setObject(1, idUsuario);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) return Map.of();

                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
                }
                return row;
            }
        }
    }

    private static void createSubtype(
        Connection conn,
        String tipoUsuario,
        Object idUsuario,
        Map<String, Object> data
    ) throws SQLException {
        if (ADMIN_ROLE.equals(tipoUsuario)) {
            createAdministrador(conn, idUsuario, data);
        } else if (NORMAL_ROLE.equals(tipoUsuario)) {
            createUsuarioNormal(conn, idUsuario, data);
        }
    }

    private static void deleteSubtype(
        Connection conn,
        String tipoUsuario,
        Object idUsuario
    ) throws SQLException {
        if (ADMIN_ROLE.equals(tipoUsuario)) {
            deleteFromAdministrador(conn, idUsuario);
        } else if (NORMAL_ROLE.equals(tipoUsuario)) {
            deleteFromUsuarioNormal(conn, idUsuario);
        }
    }

    private static void createAdministrador(
        Connection conn,
        Object idUsuario,
        Map<String, Object> data
    ) throws SQLException {
        try (
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO administrador (id_usuario, telefono_guardia) VALUES (?, ?)"
            )
        ) {
            pstmt.setObject(1, idUsuario);
            pstmt.setObject(2, data.getOrDefault("telefono_guardia", ""));
            pstmt.executeUpdate();
        }
    }

    private static void createUsuarioNormal(
        Connection conn,
        Object idUsuario,
        Map<String, Object> data
    ) throws SQLException {
        try (
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO usuarionormal (id_usuario, direccion, telefono_movil, fotografia) VALUES (?, ?, ?, ?)"
            )
        ) {
            pstmt.setObject(1, idUsuario);
            pstmt.setObject(2, blankToNull(data.get("direccion")));
            pstmt.setObject(3, blankToNull(data.get("telefono_movil")));
            pstmt.setObject(4, blankToNull(data.get("fotografia")));
            pstmt.executeUpdate();
        }
    }

    private static void updateAdministrador(
        Connection conn,
        Object idUsuario,
        Map<String, Object> data
    ) throws SQLException {
        if (!hasValue(data.get("telefono_guardia"))) return;

        try (
            PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE administrador SET telefono_guardia = ? WHERE id_usuario = ?"
            )
        ) {
            pstmt.setObject(1, data.get("telefono_guardia"));
            pstmt.setObject(2, idUsuario);
            pstmt.executeUpdate();
        }
    }

    private static void updateUsuarioNormal(
        Connection conn,
        Object idUsuario,
        Map<String, Object> data
    ) throws SQLException {
        Map<String, Object> values = new HashMap<>();
        putIfPresent(values, data, "direccion");
        putIfPresent(values, data, "telefono_movil");
        putIfPresent(values, data, "fotografia");
        if (values.isEmpty()) return;

        GenericDAO.update(
            conn,
            "usuarionormal",
            List.of(ID_USUARIO),
            List.of(idUsuario),
            values
        );
    }

    private static void deleteFromAdministrador(
        Connection conn,
        Object idUsuario
    ) throws SQLException {
        try (
            PreparedStatement pstmt = conn.prepareStatement(
                "DELETE FROM administrador WHERE id_usuario = ?"
            )
        ) {
            pstmt.setObject(1, idUsuario);
            pstmt.executeUpdate();
        }
    }

    private static void deleteFromUsuarioNormal(
        Connection conn,
        Object idUsuario
    ) throws SQLException {
        try (
            PreparedStatement pstmt = conn.prepareStatement(
                "DELETE FROM usuarionormal WHERE id_usuario = ?"
            )
        ) {
            pstmt.setObject(1, idUsuario);
            pstmt.executeUpdate();
        }
    }

    /**
     * Keeps only columns that physically belong to usuario. Subtype fields are
     * processed separately to avoid invalid dynamic SQL against the parent table.
     */
    private static Map<String, Object> filterUsuarioData(
        Map<String, Object> data
    ) {
        List<String> validColumns = MetadataDAO.getColumnNames(USUARIO);
        Map<String, Object> filtered = new HashMap<>();
        for (String col : validColumns) {
            if (data.containsKey(col)) {
                filtered.put(col, data.get(col));
            }
        }
        return filtered;
    }

    /**
     * Only update subtype columns that were actually provided with content.
     * This avoids wiping existing subtype data when the generic form does not
     * send or does not modify those values.
     */
    private static void putIfPresent(
        Map<String, Object> values,
        Map<String, Object> source,
        String key
    ) {
        if (hasValue(source.get(key))) {
            values.put(key, source.get(key));
        }
    }

    private static boolean hasValue(Object value) {
        return value != null && !(value instanceof String str && str.isBlank());
    }

    private static Object blankToNull(Object value) {
        return value instanceof String str && str.isBlank() ? null : value;
    }
}
