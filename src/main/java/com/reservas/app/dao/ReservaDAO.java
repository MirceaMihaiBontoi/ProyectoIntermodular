package com.reservas.app.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/**
 * Data Access Object for reservations.
 * Handles validation and database operations for the reserva table.
 */
public class ReservaDAO {

    private static final String RECURSO_TABLE = "recurso";
    private static final String USUARIO_TABLE = "usuario";
    private static final String ID_RECURSO = "id_recurso";
    private static final String ID_USUARIO = "id_usuario";

    private ReservaDAO() {
        // Utility class
    }

    public static String validateReserva(Connection conn, Map<String, Object> data) throws SQLException {
        // 1. Existence check
        if (!recordExists(conn, RECURSO_TABLE, ID_RECURSO, data.get(ID_RECURSO))) {
            return "Resource with id " + data.get(ID_RECURSO) + " does not exist";
        }
        if (!recordExists(conn, USUARIO_TABLE, ID_USUARIO, data.get(ID_USUARIO))) {
            return "User with id " + data.get(ID_USUARIO) + " does not exist";
        }

        String fecha = (String) data.get("fecha");
        String hInicio = (String) data.get("hora_inicio");
        String hFin = (String) data.get("hora_fin");

        // 2. Date and time range check
        if (LocalDate.parse(fecha).isBefore(LocalDate.now())) {
            return "Cannot create reservation for a past date";
        }
        if (hInicio.compareTo(hFin) >= 0) {
            return "Start time must be before end time";
        }

        // 3. Availability check
        if (!isResourceAvailable(conn, asInt(data.get(ID_RECURSO)), fecha, hInicio, hFin)) {
            String diaSemana = getDiaSemanaSpanish(fecha);
            return String.format("Resource is not available on %s (%s) from %s to %s according to its schedule", 
                diaSemana, fecha, hInicio, hFin);
        }

        // 4. Overlap check
        if (isReservationOverlapping(conn, asInt(data.get(ID_RECURSO)), fecha, hInicio, hFin)) {
            return String.format("Resource is already reserved on %s during the requested time", fecha);
        }

        return null;
    }

    public static void generateLocalId(Connection conn, Map<String, Object> data) throws SQLException {
        String nextIdSql = "SELECT COALESCE(MAX(id_reserva_local), 0) + 1 FROM reserva WHERE id_recurso = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(nextIdSql)) {
            pstmt.setObject(1, data.get(ID_RECURSO));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    data.put("id_reserva_local", rs.getInt(1));
                }
            }
        }
    }

    private static boolean recordExists(Connection conn, String table, String column, Object value) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, value);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean isResourceAvailable(Connection conn, Integer idRecurso, String fecha, String hInicio, String hFin) throws SQLException {
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

    private static boolean isReservationOverlapping(Connection conn, Integer idRecurso, String fecha, String hInicio, String hFin) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM reserva 
            WHERE id_recurso = ? 
              AND fecha = ? 
              AND ? < hora_fin 
              AND ? > hora_inicio
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, idRecurso);
            pstmt.setString(2, fecha);
            pstmt.setString(3, hInicio);
            pstmt.setString(4, hFin);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public static String getDiaSemanaSpanish(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            DayOfWeek dow = date.getDayOfWeek();
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

    private static int asInt(Object obj) {
        if (obj instanceof Number number) {
            return number.intValue();
        }
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
