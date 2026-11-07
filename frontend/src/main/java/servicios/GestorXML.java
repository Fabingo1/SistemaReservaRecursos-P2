package servicios;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GestorXML {

    public void guardarDatos(List<?> datos, String ruta) throws IOException {
        File archivo = new File(ruta);
        File carpeta = archivo.getParentFile();
        if (carpeta != null && !carpeta.exists()) {
            carpeta.mkdirs();
        }
        try (XMLEncoder encoder = new XMLEncoder(
                new BufferedOutputStream(new FileOutputStream(archivo)))) {
            encoder.writeObject(datos);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> cargarDatos(String ruta) throws IOException {
        File archivo = new File(ruta);
        if (!archivo.exists()) {
            return new ArrayList<>();
        }
        try (XMLDecoder decoder = new XMLDecoder(
                new BufferedInputStream(new FileInputStream(archivo)))) {
            return (List<T>) decoder.readObject();
        }
    }
}
