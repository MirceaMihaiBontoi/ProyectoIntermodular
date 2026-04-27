package com.reservas.app.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.reservas.app.dao.DatabaseManager;
import com.reservas.app.dao.GenericDAO;
import com.reservas.app.dao.MetadataDAO;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.io.IOException;
import java.net.ServerSocket;
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

    /**
     * Checks if a port is available.
     */
    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Kills process using the specified port.
     */
    private static void killProcessOnPort(int port) {
        try {
            ProcessBuilder pb = new ProcessBuilder("netstat", "-ano");
            Process process = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(":" + port) && line.contains("LISTENING")) {
                    String[] parts = line.trim().split("\\s+");
                    String pid = parts[parts.length - 1];
                    try {
                        new ProcessBuilder("taskkill", "/F", "/PID", pid).start();
                        System.out.println("Killed process " + pid + " using port " + port);
                    } catch (IOException e) {
                        System.err.println("Failed to kill process " + pid);
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error killing process on port " + port + ": " + e.getMessage());
        }
    }

    public static void start(int port) {
        // Kill any process using the port before starting
        if (!isPortAvailable(port)) {
            System.out.println("Port " + port + " is in use, attempting to kill process...");
            killProcessOnPort(port);
            // Wait a moment for the process to be killed
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        DatabaseManager.initializeDatabase();
        
        app = Javalin.create(config -> {
            config.staticFiles.add("web", Location.EXTERNAL);
        }).start(port);

        // API Endpoints
        setupEndpoints();
        
        System.out.println("Web server started on http://localhost:" + port);
    }

    private static void setupEndpoints() {
        // Serve index.html at root
        app.get("/", ctx -> ctx.redirect("/sistema_reservas.html"));
        
        // Reservations
        app.get("/api/reservas", WebServer::getReservas);
        app.post("/api/reservas", WebServer::createReserva);
        app.delete("/api/reservas/{id_recurso}/{id_reserva_local}", WebServer::deleteReserva);

        // Users (read-only)
        app.get("/api/usuarios", WebServer::getUsuarios);

        // Resources (read-only)
        app.get("/api/recursos", WebServer::getRecursos);

        // Schedules (for dropdowns)
        app.get("/api/horarios", WebServer::getHorarios);
    }

    private static void getReservas(Context ctx) {
        List<Map<String, Object>> reservas = GenericDAO.fetchDataAsMaps("reserva", 
            MetadataDAO.getColumnNames("reserva"));
        ctx.json(reservas);
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
        List<Map<String, Object>> usuarios = GenericDAO.fetchDataAsMaps("usuario", 
            MetadataDAO.getColumnNames("usuario"));
        // Remove password from response for security
        usuarios.forEach(u -> u.remove("contrasena"));
        ctx.json(usuarios);
    }

    private static void getRecursos(Context ctx) {
        List<Map<String, Object>> recursos = GenericDAO.fetchDataAsMaps("recurso", 
            MetadataDAO.getColumnNames("recurso"));
        ctx.json(recursos);
    }

    private static void getHorarios(Context ctx) {
        List<Map<String, Object>> horarios = GenericDAO.fetchDataAsMaps("horario", 
            MetadataDAO.getColumnNames("horario"));
        ctx.json(horarios);
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
