-- Script para SQLite
-- SISTEMA DE RESERVAS LITE

-- Desactivar claves foráneas momentáneamente para poder hacer drop si existieran
PRAGMA foreign_keys = OFF;

DROP TABLE IF EXISTS reserva;
DROP TABLE IF EXISTS disponibleen;
DROP TABLE IF EXISTS horario;
DROP TABLE IF EXISTS recurso;
DROP TABLE IF EXISTS usuarionormal;
DROP TABLE IF EXISTS administrador;
DROP TABLE IF EXISTS usuario;

PRAGMA foreign_keys = ON;

CREATE TABLE usuario (
    id_usuario INTEGER PRIMARY KEY,
    correo_electronico TEXT NOT NULL UNIQUE,
    contrasena TEXT NOT NULL,
    nombre TEXT NOT NULL UNIQUE,
    fecha_nacimiento DATE,
    tipo_usuario TEXT NOT NULL CHECK (tipo_usuario IN ('Administrador', 'Normal'))
);

CREATE TABLE administrador (
    id_usuario INTEGER PRIMARY KEY,
    telefono_guardia TEXT NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE usuarionormal (
    id_usuario INTEGER PRIMARY KEY,
    direccion TEXT,
    telefono_movil TEXT,
    fotografia TEXT,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE recurso (
    id_recurso INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL,
    descripcion TEXT,
    ubicacion TEXT,
    capacidad INTEGER
);

CREATE TABLE horario (
    id_horario INTEGER PRIMARY KEY,
    dia_semana TEXT NOT NULL CHECK (dia_semana IN ('Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo')),
    hora_inicio TEXT NOT NULL,
    hora_fin TEXT NOT NULL
);

CREATE TABLE disponibleen (
    id_recurso INTEGER NOT NULL,
    id_horario INTEGER NOT NULL,
    PRIMARY KEY (id_recurso, id_horario),
    FOREIGN KEY (id_recurso) REFERENCES recurso(id_recurso) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_horario) REFERENCES horario(id_horario) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE reserva (
    id_recurso INTEGER NOT NULL,
    id_reserva_local INTEGER NOT NULL,
    id_usuario INTEGER NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TEXT NOT NULL,
    hora_fin TEXT NOT NULL,
    coste NUMERIC(10,2),
    numero_plazas INTEGER,
    motivo TEXT, -- SQLite no limita el tamaño del texto en la definición de la columna
    PRIMARY KEY (id_recurso, id_reserva_local),
    FOREIGN KEY (id_recurso) REFERENCES recurso(id_recurso) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_usuario) REFERENCES usuarionormal(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
);

-- DATOS DE PRUEBA
-- USUARIO (5 administradores + 5 normales)
INSERT INTO usuario VALUES (1, 'admin1@email.com', 'pass123', 'Mircea Mihai Bontoi', '2000-05-02', 'Administrador');
INSERT INTO usuario VALUES (2, 'admin2@email.com', 'pass123', 'Carlos García', '1985-03-22', 'Administrador');
INSERT INTO usuario VALUES (3, 'admin3@email.com', 'pass123', 'Laura Martínez', '1992-07-10', 'Administrador');
INSERT INTO usuario VALUES (4, 'admin4@email.com', 'pass123', 'Pedro López', '1988-11-05', 'Administrador');
INSERT INTO usuario VALUES (5, 'admin5@email.com', 'pass123', 'Ana Sánchez', '1995-04-18', 'Administrador');
INSERT INTO usuario VALUES (6, 'normal1@email.com', 'pass123', 'María Torres', '1993-06-25', 'Normal');
INSERT INTO usuario VALUES (7, 'normal2@email.com', 'pass123', 'José Ruiz', '1991-09-14', 'Normal');
INSERT INTO usuario VALUES (8, 'normal3@email.com', 'pass123', 'Elena Díaz', '1996-02-28', 'Normal');
INSERT INTO usuario VALUES (9, 'normal4@email.com', 'pass123', 'Miguel Fernández', '1989-12-03', 'Normal');
INSERT INTO usuario VALUES (10, 'normal5@email.com', 'pass123', 'Sofía Moreno', '1994-08-19', 'Normal');

-- ADMINISTRADOR
INSERT INTO administrador VALUES (1, '600111222');
INSERT INTO administrador VALUES (2, '600222333');
INSERT INTO administrador VALUES (3, '600333444');
INSERT INTO administrador VALUES (4, '600444555');
INSERT INTO administrador VALUES (5, '600555666');

-- USUARIONORMAL
INSERT INTO usuarionormal VALUES (6, 'Calle Mayor 1', '611111111', 'foto1.jpg');
INSERT INTO usuarionormal VALUES (7, 'Calle Sol 2', '622222222', 'foto2.jpg');
INSERT INTO usuarionormal VALUES (8, 'Av. España 3', '633333333', NULL);
INSERT INTO usuarionormal VALUES (9, 'Calle Luna 4', '644444444', 'foto4.jpg');
INSERT INTO usuarionormal VALUES (10, 'Calle Mar 5', NULL, NULL);

-- RECURSO
INSERT INTO recurso VALUES (1, 'Sala de reuniones A', 'Sala grande', 'Planta 1', 20);
INSERT INTO recurso VALUES (2, 'Sala de reuniones B', 'Sala pequeña', 'Planta 1', 10);
INSERT INTO recurso VALUES (3, 'Gimnasio', 'Zona deportiva', 'Planta 0', 50);
INSERT INTO recurso VALUES (4, 'Pista de pádel', 'Exterior', 'Jardín', 4);
INSERT INTO recurso VALUES (5, 'Sala de conferencias', 'Sala principal', 'Planta 2', 100);

-- HORARIO
INSERT INTO horario VALUES (1, 'Lunes', '09:00:00', '10:00:00');
INSERT INTO horario VALUES (2, 'Martes', '10:00:00', '11:00:00');
INSERT INTO horario VALUES (3, 'Miércoles', '11:00:00', '12:00:00');
INSERT INTO horario VALUES (4, 'Jueves', '16:00:00', '17:00:00');
INSERT INTO horario VALUES (5, 'Viernes', '17:00:00', '18:00:00');

-- DISPONIBLEEN
INSERT INTO disponibleen VALUES (1, 1);
INSERT INTO disponibleen VALUES (1, 2);
INSERT INTO disponibleen VALUES (2, 3);
INSERT INTO disponibleen VALUES (3, 4);
INSERT INTO disponibleen VALUES (4, 5);

-- RESERVA
INSERT INTO reserva VALUES (1, 1, 6, '2025-03-01', '09:00:00', '10:00:00', 15.00, 5, 'Reunión equipo');
INSERT INTO reserva VALUES (1, 2, 6, '2025-03-02', '10:00:00', '11:00:00', 15.00, 3, 'Formación');
INSERT INTO reserva VALUES (2, 1, 6, '2025-03-03', '11:00:00', '12:00:00', 10.00, 8, 'Taller');
INSERT INTO reserva VALUES (3, 1, 7, '2025-03-04', '16:00:00', '17:00:00', 20.00, 10, 'Evento');
INSERT INTO reserva VALUES (4, 1, 8, '2025-03-05', '17:00:00', '18:00:00', 5.00, 2, 'Práctica');
