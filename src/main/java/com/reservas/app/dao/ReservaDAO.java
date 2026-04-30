package com.reservas.app.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralizes reservation business rules shared by JavaFX and the staff web UI.
 *
 * JavaFX is the administrator dashboard and the web UI is intentionally limited,
 * but both must validate reservations exactly the same way to avoid inconsistent
 * bookings in the same database.
 */
public class ReservaDAO {

    private static final String RESERVA_TABLE = "reserva";
    private static final String RECURSO_TABLE = "recurso";
    private static final String USUARIO_TABLE = "usuarionormal";
    private static final String ID_RECURSO = "id_recurso";
    private static final String ID_RESERVA_LOCAL = "id_reserva_local";
    private static final String ID_USUARIO = "id_usuario";

    private ReservaDAO() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a reservation atomically: validate availability, generate the local
     * reservation id for the selected resource, then insert the row.
     */
    public static void createReserva(Map<String, Object> data)
        throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String error = validateReserva(conn, data);
                if (error != null) throw new SQLException(error);

                generateLocalId(conn, data);
                GenericDAO.insert(conn, RESERVA_TABLE, data);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Validates updates with the same rules as creation.
     *
     * The current row is loaded first so partial updates from the generic JavaFX
     * dashboard can still be checked as a complete reservation.
     */
    public static void updateReserva(
        List<String> pkNames,
        List<Object> pkValues,
        Map<String, Object> data
    ) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            Map<String, Object> fullData = new HashMap<>(
                getCurrentReserva(conn, pkNames, pkValues)
            );
            fullData.putAll(data);
            for (int i = 0; i < pkNames.size(); i++) {
                fullData.putIfAbsent(pkNames.get(i), pkValues.get(i));
            }

            String error = validateReserva(
                conn,
                fullData,
                asInt(fullData.get(ID_RECURSO)),
                asInt(fullData.get(ID_RESERVA_LOCAL))
            );
            if (error != null) throw new SQLException(error);

            // PKs identify the selected reservation; changing them from the generic form is blocked.
            data.keySet().removeAll(pkNames);
            GenericDAO.update(conn, RESERVA_TABLE, pkNames, pkValues, data);
        }
    }

    public static void deleteReserva(Object idRecurso, Object idReservaLocal)
        throws SQLException {
        GenericDAO.delete(
            RESERVA_TABLE,
            List.of(ID_RECURSO, ID_RESERVA_LOCAL),
            List.of(idRecurso, idReservaLocal)
        );
    }

    public static String validateReserva(
        Connection conn,
        Map<String, Object> data
    ) throws SQLException {
        return validateReserva(conn, data, null, null);
    }

    /**
     * Full business validation for reservations.
     * currentIdRecurso/currentIdReservaLocal are provided on update to exclude
     * the row being edited from the overlap check.
     */
    private static String validateReserva(
        Connection conn,
        Map<String, Object> data,
        Integer currentIdRecurso,
        Integer currentIdReservaLocal
    ) throws SQLException {
        int idRecurso = asInt(data.get(ID_RECURSO));
        int idUsuario = asInt(data.get(ID_USUARIO));

        if (!recordExists(conn, RECURSO_TABLE, ID_RECURSO, idRecurso)) {
            return (
                "Resource with id " + data.get(ID_RECURSO) + " does not exist"
            );
        }
        if (!recordExists(conn, USUARIO_TABLE, ID_USUARIO, idUsuario)) {
            return (
                "User with id " +
                data.get(ID_USUARIO) +
                " does not exist or is not a standard user (Normal)"
            );
        }

        String fecha = asString(data.get("fecha"));
        String hInicio = normalizeTime(asString(data.get("hora_inicio")));
        String hFin = normalizeTime(asString(data.get("hora_fin")));
        data.put("hora_inicio", hInicio);
        data.put("hora_fin", hFin);

        if (LocalDate.parse(fecha).isBefore(LocalDate.now())) {
            return "Cannot create reservation for a past date";
        }
        if (!LocalTime.parse(hInicio).isBefore(LocalTime.parse(hFin))) {
            return "Start time must be before end time";
        }
        if (!isResourceAvailable(conn, idRecurso, fecha, hInicio, hFin)) {
            String diaSemana = getDiaSemanaSpanish(fecha);
            return String.format(
                "Resource is not available on %s (%s) from %s to %s according to its schedule",
                diaSemana,
                fecha,
                hInicio,
                hFin
            );
        }
        if (
            isReservationOverlapping(
                conn,
                idRecurso,
                fecha,
                hInicio,
                hFin,
                currentIdRecurso,
                currentIdReservaLocal
            )
        ) {
            return String.format(
                "Resource is already reserved on %s during the requested time",
                fecha
            );
        }

        return null;
    }

    /**
     * id_reserva_local is scoped per resource, so every resource has its own
     * sequence: (id_recurso=1, id_reserva_local=1), (id_recurso=2, id_reserva_local=1), etc.
     */
    public static void generateLocalId(
        Connection conn,
        Map<String, Object> data
    ) throws SQLException {
        String nextIdSql =
            "SELECT COALESCE(MAX(id_reserva_local), 0) + 1 FROM reserva WHERE id_recurso = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(nextIdSql)) {
            pstmt.setObject(1, asInt(data.get(ID_RECURSO)));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) data.put(ID_RESERVA_LOCAL, rs.getInt(1));
            }
        }
    }

    private static Map<String, Object> getCurrentReserva(
        Connection conn,
        List<String> pkNames,
        List<Object> pkValues
    ) throws SQLException {
        String whereClause = String.join(
            " AND ",
            pkNames
                .stream()
                .map(pk -> pk + " = ?")
                .toList()
        );
        String sql = "SELECT * FROM reserva WHERE " + whereClause;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < pkValues.size(); i++) {
                pstmt.setObject(i + 1, pkValues.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) throw new SQLException("Reservation not found");

                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
                }
                return row;
            }
        }
    }

    private static boolean recordExists(
        Connection conn,
        String table,
        String column,
        Object value
    ) throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, value);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Checks the resource schedule. A reservation is valid only when the whole
     * requested time range fits inside one configured horario for that weekday.
     */
    private static boolean isResourceAvailable(
        Connection conn,
        Integer idRecurso,
        String fecha,
        String hInicio,
        String hFin
    ) throws SQLException {
        String diaSemana = getDiaSemanaSpanish(fecha);
        String sql = """
            SELECT COUNT(*)
            FROM disponibleen de
            JOIN horario h ON de.id_horario = h.id_horario
            WHERE de.id_recurso = ?
              AND h.dia_semana = ?
              AND ? >= h.hora_inicio
              AND ? <= h.hora_fin
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, idRecurso);
            pstmt.setString(2, diaSemana);
            pstmt.setString(3, hInicio);
            pstmt.setString(4, hFin);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Standard overlap rule: newStart < existingEnd && newEnd > existingStart.
     * On update, the current reservation is excluded so it does not conflict with itself.
     */
    private static boolean isReservationOverlapping(
        Connection conn,
        Integer idRecurso,
        String fecha,
        String hInicio,
        String hFin,
        Integer currentIdRecurso,
        Integer currentIdReservaLocal
    ) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM reserva
            WHERE id_recurso = ?
              AND fecha = ?
              AND ? < hora_fin
              AND ? > hora_inicio
              AND NOT (id_recurso = COALESCE(?, -1) AND id_reserva_local = COALESCE(?, -1))
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, idRecurso);
            pstmt.setString(2, fecha);
            pstmt.setString(3, hInicio);
            pstmt.setString(4, hFin);
            pstmt.setObject(5, currentIdRecurso);
            pstmt.setObject(6, currentIdReservaLocal);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public static String getDiaSemanaSpanish(String dateStr) {
        try {
            DayOfWeek dow = LocalDate.parse(dateStr).getDayOfWeek();
            return switch (dow) {
                case MONDAY -> "Lunes";
                case TUESDAY -> "Martes";
                case WEDNESDAY -> "Miércoles";
                case THURSDAY -> "Jueves";
                case FRIDAY -> "Viernes";
                case SATURDAY -> "Sábado";
                case SUNDAY -> "Domingo";
            };
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Browser time inputs usually send HH:mm, while the database stores HH:mm:ss.
     * Normalizing avoids lexicographic comparison bugs in SQLite TEXT time columns.
     */
    private static String normalizeTime(String value) {
        if (value == null) throw new IllegalArgumentException(
            "Time is required"
        );
        String trimmed = value.trim();
        if (trimmed.length() == 5) trimmed += ":00";
        return LocalTime.parse(trimmed).toString().length() == 5
            ? LocalTime.parse(trimmed) + ":00"
            : LocalTime.parse(trimmed).toString();
    }

    private static String asString(Object value) {
        if (value == null) throw new IllegalArgumentException(
            "Required reservation field is missing"
        );
        return value.toString();
    }

    private static int asInt(Object obj) {
        if (obj instanceof Number number) return number.intValue();
        if (obj instanceof String str) {
            try {
                return (int) Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}
