package com.reservas.app.dao;

import com.reservas.app.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO {
    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());

    // Retrieves all users using specific columns to avoid unnecessary data overhead and SQL injection risks
    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id_usuario, correo_electronico, contrasena, nombre, fecha_nacimiento, tipo_usuario FROM usuario";

        // Try-with-resources ensures Connection, Statement, and ResultSet are closed automatically, avoiding memory leaks
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // SQLite stores dates as strings; manual parsing ensures compatibility across different regional settings
                String birthDateStr = rs.getString("fecha_nacimiento");
                LocalDate birthDate = (birthDateStr != null) ? LocalDate.parse(birthDateStr) : null;

                User user = new User(
                    rs.getInt("id_usuario"),
                    rs.getString("correo_electronico"),
                    rs.getString("contrasena"),
                    rs.getString("nombre"),
                    birthDate,
                    rs.getString("tipo_usuario")
                );
                users.add(user);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error listing users", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "General error processing users", e);
        }
        return users;
    }

    // Inserts a new user into the database
    public void save(User user) {
        // Using PreparedStatement is mandatory to prevent SQL Injection attacks
        String sql = "INSERT INTO usuario (id_usuario, correo_electronico, contrasena, nombre, fecha_nacimiento, tipo_usuario) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, user.getId());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getName());
            // Store date in ISO format (YYYY-MM-DD) for guaranteed SQLite retrieval compatibility
            pstmt.setString(5, user.getBirthDate() != null ? user.getBirthDate().toString() : null);
            pstmt.setString(6, user.getUserType());
            
            pstmt.executeUpdate();
            logger.info("User saved successfully.");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error saving user", e);
        }
    }

    // Deletes a user by ID
    public void delete(int id) {
        String sql = "DELETE FROM usuario WHERE id_usuario = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            logger.log(Level.INFO, "User with ID {0} deleted successfully.", id);
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting user", e);
        }
    }

    // Updates an existing user's information
    public void update(User user) {
        String sql = "UPDATE usuario SET correo_electronico = ?, contrasena = ?, nombre = ?, fecha_nacimiento = ?, tipo_usuario = ? WHERE id_usuario = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getName());
            pstmt.setString(4, user.getBirthDate() != null ? user.getBirthDate().toString() : null);
            pstmt.setString(5, user.getUserType());
            pstmt.setInt(6, user.getId());
            
            pstmt.executeUpdate();
            logger.log(Level.INFO, "User with ID {0} updated successfully.", user.getId());
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating user", e);
        }
    }

    // Executes a raw SQL command (DML/DDL)
    public void executeRawSql(String sql) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            logger.log(Level.INFO, "Raw SQL executed successfully: {0}", sql);
        }
    }
}
