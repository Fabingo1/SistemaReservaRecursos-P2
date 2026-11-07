package datos;

import protocolo.Configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Base de todos los DAOs: abre una conexión nueva por operación.
// Los datos de conexión salen de config.properties (ver Configuracion). Si no está,
// usa la BD local por defecto (la que crea db/schema.sql): para probar en una sola computadora no hay que configurar nada.
public abstract class BaseDAO {

    private static final String URL_POR_DEFECTO = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USUARIO_POR_DEFECTO = "reservas";
    private static final String CLAVE_POR_DEFECTO = "reservas";

    protected Connection conectar() throws SQLException {
        return DriverManager.getConnection(
                Configuracion.get("db.url", URL_POR_DEFECTO),
                Configuracion.get("db.usuario", USUARIO_POR_DEFECTO),
                Configuracion.get("db.clave", CLAVE_POR_DEFECTO));
    }
}
