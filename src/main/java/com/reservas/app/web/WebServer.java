package com.reservas.app.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.reservas.app.dao.DatabaseManager;
import com.reservas.app.dao.GenericDAO;
import com.reservas.app.dao.MetadataDAO;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Web server for the reservation system staff interface.
 * Provides REST API endpoints and serves static HTML files.
 */
public class WebServer {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static Javalin app;

    public static void start(int port) {
        DatabaseManager.initializeDatabase();
        
        app = Javalin.create(config -> {
            config.staticFiles.add("web");
        }).start(port);

        // API Endpoints
        setupEndpoints();
        
        System.out.println("Web server started on http://localhost:" + port);
    }

    private static void setupEndpoints() {
        // Reservations
        app.get("/api/reservas", WebServer::getReservas);
        app.post("/api/reservas", WebServer::createReserva);
        app.delete("/api/reservas/:id_recurso/:id_reserva_local", WebServer::deleteReserva);

        // Users (read-only)
        app.get("/api/usuarios", WebServer::getUsuarios);

        // Resources (read-only)
        app.get("/api/recursos", WebServer::getRecursos);

        // Schedules (for dropdowns)
        app.get("/api/horarios", WebServer::getHorarios);
    }

    private static void getReservas(Context ctx) {
        try {
            List<Map<String, Object>> reservas = GenericDAO.fetchData("reserva", 
                MetadataDAO.getColumnNames("reserva"));
            ctx.json(reservas);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createReserva(Context ctx) {
        try {
            Map<String, Object> data = gson.fromJson(ctx.body(), Map.class);
            GenericDAO.insert("reserva", data);
            ctx.status(201).json(Map.of("message", "Reservation created successfully"));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteReserva(Context ctx) {
        try {
            String idRecurso = ctx.pathParam("id_recurso");
            String idReservaLocal = ctx.pathParam("id_reserva_local");
            
            // Delete using composite primary key
            try (var conn = DatabaseManager.getConnection();
                 var stmt = conn.prepareStatement(
                     "DELETE FROM reserva WHERE id_recurso = ? AND id_reserva_local = ?")) {
                stmt.setString(1, idRecurso);
                stmt.setString(2, idReservaLocal);
                stmt.executeUpdate();
            }
            
            ctx.json(Map.of("message", "Reservation cancelled successfully"));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void getUsuarios(Context ctx) {
        try {
            List<Map<String, Object>> usuarios = GenericDAO.fetchData("usuario", 
                MetadataDAO.getColumnNames("usuario"));
            // Remove password from response for security
            usuarios.forEach(u -> u.remove("contrasena"));
            ctx.json(usuarios);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void getRecursos(Context ctx) {
        try {
            List<Map<String, Object>> recursos = GenericDAO.fetchData("recurso", 
                MetadataDAO.getColumnNames("recurso"));
            ctx.json(recursos);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void getHorarios(Context ctx) {
        try {
            List<Map<String, Object>> horarios = GenericDAO.fetchData("horario", 
                MetadataDAO.getColumnNames("horario"));
            ctx.json(horarios);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public static void stop() {
        if (app != null) {
            app.stop();
        }
    }

    public static void main(String[] args) {
        start(8080);
    }
}
