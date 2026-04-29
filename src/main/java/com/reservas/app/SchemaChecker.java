package com.reservas.app;

import com.reservas.app.dao.MetadataDAO;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SchemaChecker {
    private static final Logger logger = Logger.getLogger(SchemaChecker.class.getName());

    public static void main(String[] args) {
        String[] tables = {"disponibleen", "horario", "recurso", "reserva"};
        for (String table : tables) {
            logger.log(Level.INFO, "Table: {0}", table);
            List<String> columns = MetadataDAO.getColumnNames(table);
            logger.log(Level.INFO, "Columns: {0}", columns);
            for (String col : columns) {
                logger.log(Level.INFO, "  {0} ({1})", new Object[]{col, MetadataDAO.getColumnType(table, col)});
            }
        }
    }
}
