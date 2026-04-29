package com.reservas.app.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.reservas.app.dao.DatabaseManager;
import com.reservas.app.dao.GenericDAO;
import com.reservas.app.dao.MetadataDAO;
import com.reservas.app.dao.ReservaDAO;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinGson;
import io.javalin.websocket.WsContext;
import static io.javalin.apibuilder.ApiBuilder.*;

import com.google.gson.reflect.TypeToken;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Portable and decoupled web server for the reservation system.
 * Serves the REST API and static files.
 */
public class WebServer {

    private static final String RESERVA_TABLE = "reserva";
    private static final String USUARIO_TABLE = "usuario";
    private static final String RECURSO_TABLE = "recurso";
    private static final String HORARIO_TABLE = "horario";
    private static final String ERROR_KEY = "error";
    private static final String UNKNOWN_ERROR = "Unknown error";
    private static final String ID_RECURSO = "id_recurso";
    private static final String CONTRASENA = "contrasena";
    private static final String TIPO_USUARIO = "tipo_usuario";

    private static final Logger logger = Logger.getLogger(WebServer.class.getName());
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final List<WsContext> clients = new CopyOnWriteArrayList<>();
    private static Javalin app;
    private static Runnable uiRefreshCallback;

    /**
     * Registers a callback to refresh the UI when data changes.
     */
    public static void setOnDataChange(Runnable callback) {
        uiRefreshCallback = callback;
    }

    /**
     * Starts the server.
     */
    public static void start(int port) {
        try {
            app = Javalin.create(config -> {
                config.staticFiles.add(staticFiles -> {
                    staticFiles.directory = "web";
                    staticFiles.location = Location.EXTERNAL;
                });
                config.jsonMapper(new JavalinGson(gson, false));
                
                // Routing in Javalin 7 using config.routes and ApiBuilder
                config.routes.apiBuilder(() -> {
                    get("/", ctx -> ctx.redirect("/sistema_reservas.html"));
                    
                    path("api", () -> {
                        get("reservas", WebServer::getReservas);
                        post("reservas", WebServer::createReserva);
                        delete("reservas/{id_recurso}/{id_reserva_local}", WebServer::deleteReserva);

                        get("usuarios", WebServer::getUsuarios);
                        get("recursos", WebServer::getRecursos);
                        get("horarios", WebServer::getHorarios);
                    });

                    ws("ws", ws -> {
                        ws.onConnect(ctx -> clients.add(ctx));
                        ws.onClose(ctx -> clients.remove(ctx));
                        ws.onMessage(ctx -> logger.info("WS Message received: " + ctx.message()));
                    });
                });
            });

            app.start(port);
            logger.log(Level.INFO, "Web server started on http://localhost:{0}", port);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start web server: {0}", e.getMessage());
        }
    }

    private static void setupEndpoints() {
        // Obsolete in Javalin 7 as routes must be defined in config block
    }

    private static void getReservas(Context ctx) {
        try {
            List<Map<String, Object>> reservas = GenericDAO.fetchDataAsMaps(RESERVA_TABLE, 
                MetadataDAO.getColumnNames(RESERVA_TABLE));
            ctx.json(reservas);
        } catch (Exception e) {
            handleError(ctx, "Error fetching reservations", e);
        }
    }

    private static void createReserva(Context ctx) {
        try {
            Map<String, Object> data = gson.fromJson(ctx.body(), new TypeToken<Map<String, Object>>(){}.getType());
            
            try (Connection conn = DatabaseManager.getConnection()) {
                String error = ReservaDAO.validateReserva(conn, data);
                if (error != null) {
                    ctx.status(400).json(Map.of(ERROR_KEY, error));
                    return;
                }
                ReservaDAO.generateLocalId(conn, data);
            }
            
            GenericDAO.insert(RESERVA_TABLE, data);
            ctx.status(201).json(Map.of("message", "Reservation created successfully"));
            broadcastRefresh();
        } catch (Exception e) {
            handleError(ctx, "Error creating reservation", e);
        }
    }

    private static void deleteReserva(Context ctx) {
        try {
            int idRecurso = Integer.parseInt(ctx.pathParam(ID_RECURSO));
            int idReservaLocal = Integer.parseInt(ctx.pathParam("id_reserva_local"));
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM reserva WHERE id_reserva_local = ? AND id_recurso = ?")) {
                stmt.setInt(1, idReservaLocal);
                stmt.setInt(2, idRecurso);
                stmt.executeUpdate();
            }
            
            ctx.json(Map.of("message", "Reservation cancelled successfully"));
            broadcastRefresh();
        } catch (Exception e) {
            handleError(ctx, "Error cancelling reservation", e);
        }
    }

    /**
     * Notifies only the web clients via WebSocket.
     */
    public static void notifyWeb() {
        for (WsContext ctx : clients) {
            if (ctx.session.isOpen()) {
                ctx.send("refresh");
            }
        }
    }

    /**
     * Notifies only the JavaFX UI.
     */
    private static void notifyUI() {
        if (uiRefreshCallback != null) {
            javafx.application.Platform.runLater(uiRefreshCallback);
        }
    }

    /**
     * Notifies both the UI and the web clients.
     */
    public static void broadcastRefresh() {
        notifyUI();
        notifyWeb();
    }

    private static void handleError(Context ctx, String message, Exception e) {
        logger.log(Level.SEVERE, "{0}: {1}", new Object[]{message, e.getMessage()});
        ctx.status(500).json(Map.of(ERROR_KEY, e.getMessage() != null ? e.getMessage() : UNKNOWN_ERROR));
    }

    private static void getUsuarios(Context ctx) {
        try {
            List<Map<String, Object>> usuarios = GenericDAO.fetchDataAsMaps(USUARIO_TABLE, 
                MetadataDAO.getColumnNames(USUARIO_TABLE));
            usuarios.removeIf(u -> {
                u.remove(CONTRASENA);
                return "Administrador".equals(u.get(TIPO_USUARIO));
            });
            ctx.json(usuarios);
        } catch (Exception e) {
            handleError(ctx, "Error fetching users", e);
        }
    }

    private static void getRecursos(Context ctx) {
        try {
            List<Map<String, Object>> recursos = GenericDAO.fetchDataAsMaps(RECURSO_TABLE, 
                MetadataDAO.getColumnNames(RECURSO_TABLE));
            ctx.json(recursos);
        } catch (Exception e) {
            handleError(ctx, "Error fetching resources", e);
        }
    }

    private static void getHorarios(Context ctx) {
        try {
            List<Map<String, Object>> horarios = GenericDAO.fetchDataAsMaps(HORARIO_TABLE, 
                MetadataDAO.getColumnNames(HORARIO_TABLE));
            ctx.json(horarios);
        } catch (Exception e) {
            handleError(ctx, "Error fetching schedules", e);
        }
    }

    public static void stop() {
        if (app != null) {
            app.stop();
        }
    }

    public static void main(String[] args) {
        start(3000);
    }
}

