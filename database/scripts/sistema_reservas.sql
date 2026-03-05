drop database if exists sistema_reservas;
create database sistema_reservas;
use sistema_reservas;

create table usuario(
id_usuario int primary key,
correo_electronico varchar(100) not null unique,
contrasena varchar(100) not null,
nombre varchar(100) not null unique,
fecha_nacimiento date,
tipo_usuario enum('Administrador', 'Normal')  not null
);

create table administrador(
id_usuario int primary key,
telefono_guardia varchar(20) not null,
foreign key (id_usuario) references usuario(id_usuario) 
on delete cascade -- si se borra el usuario se borra su perfil de administrador, ya no tiene sentido mantenerlo
on update cascade
);

create table usuarionormal(
id_usuario int primary key,
direccion varchar(200),
telefono_movil varchar(200),
fotografia varchar(255),
foreign key (id_usuario) references usuario(id_usuario) 
on delete cascade -- igual que administrador, sin usuario el perfil normal no tiene sentido
on update cascade
);

create table recurso(
id_recurso int primary key,
nombre varchar(100) not null,
descripcion varchar(100),
ubicacion varchar(200),
capacidad int
);

create table horario(
id_horario int primary key,
dia_semana enum('Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo') not null,
hora_inicio time(5) not null,
hora_fin time(5) not null
);

create table disponibleen(
id_recurso int not null,
id_horario int not null,
primary key (id_recurso,id_horario),
foreign key (id_recurso) references recurso(id_recurso) 
on delete cascade -- si se elimina un recurso, sus horarios disponibles ya no tienen sentido
on update cascade,
foreign key (id_horario) references horario(id_horario) 
on delete cascade -- si se elimina un horario, la disponibilidad asociada desaparece también
on update cascade
);

create table reserva(
id_recurso int not null,
id_reserva_local int not null,
id_usuario int not null,
fecha date not null,
hora_inicio time(5) not null,
hora_fin time(5) not null,
coste decimal(10,2),
numero_plazas int,
motivo text(20),
primary key (id_recurso,id_reserva_local),
foreign key (id_recurso) references recurso(id_recurso) 
on delete cascade -- si se elimina el recurso, sus reservas dejan de tener validez
on update cascade,
foreign key (id_usuario) references usuarionormal(id_usuario) 
on delete cascade -- si se elimina el usuario, sus reservas se eliminan también
on update cascade
);


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

-- RESERVA (id_usuario 6 aparece varias veces para comprobar cardinalidad)
INSERT INTO reserva VALUES (1, 1, 6, '2025-03-01', '09:00:00', '10:00:00', 15.00, 5, 'Reunión equipo');
INSERT INTO reserva VALUES (1, 2, 6, '2025-03-02', '10:00:00', '11:00:00', 15.00, 3, 'Formación');
INSERT INTO reserva VALUES (2, 1, 6, '2025-03-03', '11:00:00', '12:00:00', 10.00, 8, 'Taller');
INSERT INTO reserva VALUES (3, 1, 7, '2025-03-04', '16:00:00', '17:00:00', 20.00, 10, 'Evento');
INSERT INTO reserva VALUES (4, 1, 8, '2025-03-05', '17:00:00', '18:00:00', 5.00, 2, 'Práctica');