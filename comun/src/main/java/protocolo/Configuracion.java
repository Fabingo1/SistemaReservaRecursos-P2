package protocolo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// Lee config.properties. Si no existe o falta una clave, usa el valor por defecto:
// así una máquina que solo corre el Frontend puede tener únicamente servidor.host.
// Se busca en la carpeta actual y en la de arriba: al correr un módulo (backend/ o frontend/)
// la carpeta actual es la del módulo, y el archivo está en la raíz del proyecto.
public final class Configuracion {

    private static final String[] UBICACIONES = {"config.properties", "../config.properties"};
    private static Properties propiedades;

    private Configuracion() {
    }

    public static synchronized String get(String clave, String porDefecto) {
        if (propiedades == null) {
            propiedades = cargar();
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

    private static Properties cargar() {
        Properties p = new Properties();
        for (String ruta : UBICACIONES) {
            File archivo = new File(ruta);
            if (archivo.isFile()) {
                try (InputStream in = new FileInputStream(archivo)) {
                    p.load(in);
                    return p;
                } catch (IOException e) {
                    // Si no se puede leer, se prueba la siguiente ubicación
                }
            }
        }
        System.out.println("Aviso: no se encontró config.properties; se usan los valores por defecto.");
        return p;
    }
}
