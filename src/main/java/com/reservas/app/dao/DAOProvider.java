package com.reservas.app.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Provides a unified interface to execute database operations,
 * routing them to specialized DAOs (like UsuarioDAO) if they exist,
 * or falling back to GenericDAO otherwise.
 */
public class DAOProvider {

    private static final String USUARIO_TABLE = "usuario";

    private DAOProvider() {}

    public static void insert(String tableName, Map<String, Object> data)
        throws SQLException {
        if (USUARIO_TABLE.equals(tableName)) {
            UsuarioDAO.insertWithCascade(data);
        } else if ("reserva".equals(tableName)) {
            ReservaDAO.createReserva(data);
        } else {
            GenericDAO.insert(tableName, data);
        }
    }

    public static void update(
        String tableName,
        List<String> pkNames,
        List<Object> pkValues,
        Map<String, Object> data
    ) throws SQLException {
        if (USUARIO_TABLE.equals(tableName)) {
            UsuarioDAO.updateWithCascade(pkNames, pkValues, data);
        } else if ("reserva".equals(tableName)) {
            ReservaDAO.updateReserva(pkNames, pkValues, data);
        } else {
            GenericDAO.update(tableName, pkNames, pkValues, data);
        }
    }

    public static void delete(
        String tableName,
        List<String> pkNames,
        List<Object> pkValues
    ) throws SQLException {
        if (USUARIO_TABLE.equals(tableName)) {
            UsuarioDAO.deleteWithCascade(pkNames, pkValues);
        } else {
            GenericDAO.delete(tableName, pkNames, pkValues);
        }
    }
}
