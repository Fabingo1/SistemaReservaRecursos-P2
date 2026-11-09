# Sistema de Reserva de Recursos — Proyecto #2

EIF206 Programación 3 · Universidad Nacional

Repositorio: https://github.com/Fabingo1/SistemaReservaRecursos-P2

## Integrantes

- Luis Fabiano Arroyo Castillo
- Minor Castillo Loría
- Steven Gamboa Ledezma

## Cómo ejecutarlo

1. **Base de datos:** en SQL Developer, conectado como `system`, ejecutar con F5 `db/schema.sql` y luego `db/seed.sql`.
2. **Configuración:** copiar `config.example.properties` como `config.properties` en la raíz.
3. **Backend:** ejecutar `backend/.../servidor/ServidorMain.java`.
4. **Frontend:** ejecutar `frontend/.../Main.java` (se puede abrir varias veces).

## Usuarios de prueba

| Usuario | Clave | Rol |
|---|---|---|
| `admin1` | `admin1` | Administrador |
| `func1` | `func1` | Funcionario |
| `func2` | `func2` | Funcionario |
