package datos;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Base de todos los DAOs: lee config.properties una vez y abre una conexión nueva por operación.
// Si no existe config.properties, usa la BD local por defecto (la que crea db/schema.sql):
// así, para probar en una sola computadora, no hace falta configurar nada.
public abstract class BaseDAO {

    private static final String ARCHIVO_CONFIG = "config.properties";
    private static final String URL_POR_DEFECTO = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USUARIO_POR_DEFECTO = "reservas";
    private static final String CLAVE_POR_DEFECTO = "reservas";
    private static Properties config;

    protected Connection conectar() throws SQLException {
        Properties p = obtenerConfig();
        return DriverManager.getConnection(
                p.getProperty("db.url", URL_POR_DEFECTO),
                p.getProperty("db.usuario", USUARIO_POR_DEFECTO),
                p.getProperty("db.clave", CLAVE_POR_DEFECTO));
    }

    private static synchronized Properties obtenerConfig() throws SQLException {
        if (config == null) {
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(ARCHIVO_CONFIG)) {
                p.load(in);
            } catch (IOException e) {
                System.out.println("Aviso: no se encontró " + ARCHIVO_CONFIG + "; se usa la base de datos local "
                        + "por defecto (" + USUARIO_POR_DEFECTO + " en " + URL_POR_DEFECTO + ").");
            }
            config = p;
        }
        return config;
    }
}