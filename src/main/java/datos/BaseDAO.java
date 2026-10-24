package datos;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Base de todos los DAOs: lee config.properties una vez y abre una conexión nueva por operación.
public abstract class BaseDAO {

    private static final String ARCHIVO_CONFIG = "config.properties";
    private static Properties config;

    protected Connection conectar() throws SQLException {
        Properties p = obtenerConfig();
        return DriverManager.getConnection(
                p.getProperty("db.url"),
                p.getProperty("db.usuario"),
                p.getProperty("db.clave"));
    }

    private static synchronized Properties obtenerConfig() throws SQLException {
        if (config == null) {
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(ARCHIVO_CONFIG)) {
                p.load(in);
            } catch (IOException e) {
                throw new SQLException("No se encontró " + ARCHIVO_CONFIG
                        + " en la carpeta del proyecto. Copie config.example.properties y llénelo.");
            }
            config = p;
        }
        return config;
    }
}