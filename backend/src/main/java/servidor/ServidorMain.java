package servidor;

import datos.CategoriaDAO;
import logica.Service;
import protocolo.Configuracion;

import java.io.IOException;

// Punto de entrada del BACKEND (servidor sin interfaz gráfica).
// En IntelliJ: una configuración de ejecución con esta clase y otra con Main (Frontend).
public class ServidorMain {

    public static void main(String[] args) {
        int puerto = Configuracion.getInt("servidor.puerto", 5000);

        // Prueba rápida de la base de datos para avisar desde el inicio si config.properties está mal
        try {
            new CategoriaDAO().listar();
            System.out.println("Conexión con la base de datos: OK");
        } catch (Exception e) {
            System.err.println("ATENCIÓN: no se pudo conectar con la base de datos: " + e.getMessage());
            System.err.println("Revise config.properties (db.url, db.usuario, db.clave) y que Oracle esté encendido.");
        }

        Servidor servidor = new Servidor(puerto, new Service());
        try {
            servidor.iniciar();
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor en el puerto " + puerto + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
