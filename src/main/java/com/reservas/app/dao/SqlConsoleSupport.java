package com.reservas.app.dao;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits and classifies SQL batches the same way the JavaFX SQL console does.
 */
public final class SqlConsoleSupport {

    private SqlConsoleSupport() {
        throw new IllegalStateException("Utility class");
    }

    public static List<String> statements(String sql) {
        List<String> out = new ArrayList<>();
        for (String part : sql.split(";")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    public static boolean isSelect(String statement) {
        return statement.trim().toUpperCase().startsWith("SELECT");
    }

    public static boolean isSchemaChange(String statement) {
        String upper = statement.trim().toUpperCase();
        return upper.startsWith("CREATE")
            || upper.startsWith("DROP")
            || upper.startsWith("ALTER");
    }
}
