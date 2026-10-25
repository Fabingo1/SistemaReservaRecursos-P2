package servicios;

// Punto único del Frontend para obtener el IService. Main asigna el ServiceProxy al arrancar.
public final class ServicioRemoto {

    private static IService service;

    private ServicioRemoto() {
    }

    public static void set(IService nuevo) {
        service = nuevo;
    }

    public static IService get() {
        if (service == null) {
            throw new IllegalStateException("No hay conexión con el servidor.");
        }
        return service;
    }
}