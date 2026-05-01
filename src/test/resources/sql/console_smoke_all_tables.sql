-- Paste this block into the desktop SQL console (multiple statements separated by ;).
-- Compatible with seed data in database/scripts/sistema_reservas_lite.sql

SELECT 'usuario' AS tabla, COUNT(*) AS filas FROM usuario;
SELECT 'administrador' AS tabla, COUNT(*) AS filas FROM administrador;
SELECT 'usuarionormal' AS tabla, COUNT(*) AS filas FROM usuarionormal;
SELECT 'recurso' AS tabla, COUNT(*) AS filas FROM recurso;
SELECT 'horario' AS tabla, COUNT(*) AS filas FROM horario;
SELECT 'disponibleen' AS tabla, COUNT(*) AS filas FROM disponibleen;
SELECT 'reserva' AS tabla, COUNT(*) AS filas FROM reserva;

-- Read with JOIN (typical report-style query)
SELECT r.id_recurso, r.nombre, u.nombre AS usuario, rv.fecha
FROM reserva rv
JOIN recurso r ON rv.id_recurso = r.id_recurso
JOIN usuario u ON rv.id_usuario = u.id_usuario
LIMIT 3;

-- No-op update (1 row affected if id 1 exists)
UPDATE recurso SET nombre = nombre WHERE id_recurso = 1;

-- Test insert: Monday 2026-05-04 fits resource 1 schedule in the seed data; removed below
INSERT INTO reserva (id_recurso, id_reserva_local, id_usuario, fecha, hora_inicio, hora_fin, coste, numero_plazas, motivo)
SELECT 1, (SELECT COALESCE(MAX(id_reserva_local), 0) + 1 FROM reserva WHERE id_recurso = 1), 6, '2026-05-04', '09:00:00', '10:00:00', 2.00, 1, 'MANUAL_CONSOLE_TEST';

DELETE FROM reserva WHERE id_recurso = 1 AND motivo = 'MANUAL_CONSOLE_TEST';

-- Minimal DDL (console reloads tabs when it detects CREATE/DROP/ALTER)
CREATE TABLE IF NOT EXISTS z_consola_tmp (x INTEGER PRIMARY KEY);
DROP TABLE IF EXISTS z_consola_tmp;
