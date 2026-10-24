-- Datos de prueba. Ejecutar como "reservas" después de schema.sql.
-- Usuarios: admin1/admin1 (administrador), func1/func1 y func2/func2 (funcionarios).

INSERT INTO administrador (id, clave) VALUES ('admin1', 'admin1');

INSERT INTO funcionario (id, clave, nombre, telefono) VALUES ('func1', 'func1', 'Juan Pérez', '8888-8888');
INSERT INTO funcionario (id, clave, nombre, telefono) VALUES ('func2', 'func2', 'María Solís', '8777-7777');

INSERT INTO categoria (id, descripcion) VALUES ('CAT-001', 'Sala para 10 personas');
INSERT INTO categoria (id, descripcion) VALUES ('CAT-002', 'Laptop windows 11');
INSERT INTO categoria (id, descripcion) VALUES ('CAT-003', 'Proyector');

INSERT INTO recurso (id, descripcion, categoria_id) VALUES ('SALA-1', 'Sala 1 primer piso', 'CAT-001');
INSERT INTO recurso (id, descripcion, categoria_id) VALUES ('SALA-2', 'Sala 2 segundo piso', 'CAT-001');
INSERT INTO recurso (id, descripcion, categoria_id) VALUES ('238715', 'Laptop #238715', 'CAT-002');
INSERT INTO recurso (id, descripcion, categoria_id) VALUES ('238716', 'Laptop #238716', 'CAT-002');
INSERT INTO recurso (id, descripcion, categoria_id) VALUES ('PRY-01', 'Proyector Epson', 'CAT-003');

-- Reservas con fechas relativas a hoy: TRUNC(SYSDATE) = hoy 00:00, + días + horas/24
INSERT INTO reserva (id, actividad, inicio, fin, estado, funcionario_id, funcionario_nombre, funcionario_telefono)
VALUES ('RES-000001', 'Sesión de Junta Directiva', TRUNC(SYSDATE) + 1 + 9/24, TRUNC(SYSDATE) + 1 + 11/24,
        'ACTIVA', 'func1', 'Juan Pérez', '8888-8888');
INSERT INTO detalle_reserva VALUES ('RES-000001', 'CAT-001', 'Sala para 10 personas', 'SALA-1', 'Sala 1 primer piso');
INSERT INTO detalle_reserva VALUES ('RES-000001', 'CAT-003', 'Proyector', 'PRY-01', 'Proyector Epson');

INSERT INTO reserva (id, actividad, inicio, fin, estado, funcionario_id, funcionario_nombre, funcionario_telefono)
VALUES ('RES-000002', 'Capacitación Excel', TRUNC(SYSDATE) + 1 + 10.5/24, TRUNC(SYSDATE) + 1 + 12/24,
        'ACTIVA', 'func2', 'María Solís', '8777-7777');
INSERT INTO detalle_reserva VALUES ('RES-000002', 'CAT-001', 'Sala para 10 personas', 'SALA-2', 'Sala 2 segundo piso');
INSERT INTO detalle_reserva VALUES ('RES-000002', 'CAT-002', 'Laptop windows 11', '238715', 'Laptop #238715');

INSERT INTO reserva (id, actividad, inicio, fin, estado, funcionario_id, funcionario_nombre, funcionario_telefono)
VALUES ('RES-000003', 'Reunión de planificación', TRUNC(SYSDATE) + 3 + 14/24, TRUNC(SYSDATE) + 3 + 15.5/24,
        'ACTIVA', 'func1', 'Juan Pérez', '8888-8888');
INSERT INTO detalle_reserva VALUES ('RES-000003', 'CAT-001', 'Sala para 10 personas', 'SALA-1', 'Sala 1 primer piso');

INSERT INTO reserva (id, actividad, inicio, fin, estado, funcionario_id, funcionario_nombre, funcionario_telefono)
VALUES ('RES-000004', 'Revisión de presupuesto', TRUNC(SYSDATE) - 7 + 8/24, TRUNC(SYSDATE) - 7 + 9/24,
        'ACTIVA', 'func2', 'María Solís', '8777-7777');
INSERT INTO detalle_reserva VALUES ('RES-000004', 'CAT-002', 'Laptop windows 11', '238716', 'Laptop #238716');

COMMIT;
