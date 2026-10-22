-- Esquema del Sistema de Reserva de Recursos (Oracle 11g XE)
-- Ejecutar conectado como el usuario "reservas". Se puede correr varias veces.

-- Borrado previo (ignora el error si la tabla no existe)
BEGIN EXECUTE IMMEDIATE 'DROP TABLE detalle_reserva CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE reserva CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE recurso CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE categoria CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE funcionario CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE administrador CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- Usuarios (igual que en el #1: administradores y funcionarios por separado)
CREATE TABLE administrador (
    id     VARCHAR2(20) PRIMARY KEY,
    clave  VARCHAR2(50) NOT NULL
);

CREATE TABLE funcionario (
    id        VARCHAR2(20)  PRIMARY KEY,
    clave     VARCHAR2(50)  NOT NULL,
    nombre    VARCHAR2(100) NOT NULL,
    telefono  VARCHAR2(20)  NOT NULL
);

-- Categorías y recursos
CREATE TABLE categoria (
    id           VARCHAR2(20)  PRIMARY KEY,
    descripcion  VARCHAR2(100) NOT NULL
);

CREATE TABLE recurso (
    id            VARCHAR2(20)  PRIMARY KEY,
    descripcion   VARCHAR2(100) NOT NULL,
    categoria_id  VARCHAR2(20)  NOT NULL REFERENCES categoria(id)
);

-- Reservas: guarda copia del funcionario (sin FK) para conservar el histórico
CREATE TABLE reserva (
    id                    VARCHAR2(20)  PRIMARY KEY,
    actividad             VARCHAR2(200) NOT NULL,
    inicio                DATE          NOT NULL,  -- fecha + hora de inicio
    fin                   DATE          NOT NULL,  -- fecha + hora de fin
    estado                VARCHAR2(10)  DEFAULT 'ACTIVA' NOT NULL
                          CHECK (estado IN ('ACTIVA', 'CANCELADA')),
    funcionario_id        VARCHAR2(20)  NOT NULL,
    funcionario_nombre    VARCHAR2(100),
    funcionario_telefono  VARCHAR2(20),
    CHECK (fin > inicio)
);

-- Detalle: una fila por categoría pedida; también guarda copia (sin FK) de categoría y recurso
CREATE TABLE detalle_reserva (
    reserva_id             VARCHAR2(20)  NOT NULL REFERENCES reserva(id) ON DELETE CASCADE,
    categoria_id           VARCHAR2(20)  NOT NULL,
    categoria_descripcion  VARCHAR2(100),
    recurso_id             VARCHAR2(20)  NOT NULL,
    recurso_descripcion    VARCHAR2(100),
    PRIMARY KEY (reserva_id, categoria_id)
);

-- Acelera la búsqueda de disponibilidad por recurso
CREATE INDEX idx_detalle_recurso ON detalle_reserva(recurso_id);
