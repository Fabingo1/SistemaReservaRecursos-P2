package protocolo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// Lee config.properties (carpeta del proyecto). Si no existe o falta una clave, usa el valor
// por defecto: así una máquina que solo corre el Frontend puede tener únicamente servidor.host.
public final class Configuracion {

    private static final String ARCHIVO = "config.properties";
    private static Properties propiedades;

    private Configuracion() {
    }

    public static synchronized String get(String clave, String porDefecto) {
        if (propiedades == null) {
            propiedades = new Properties();
            try (InputStream in = new FileInputStream(ARCHIVO)) {
                propiedades.load(in);
            } catch (IOException e) {
                // Sin archivo: se usan los valores por defecto
            }
        }
        String valor = propiedades.getProperty(clave);
        return (valor == null || valor.isBlank()) ? porDefecto : valor.trim();
    }

    public static int getInt(String clave, int porDefecto) {
        try {
            return Integer.parseInt(get(clave, String.valueOf(porDefecto)));
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
