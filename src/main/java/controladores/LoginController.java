package controladores;

import modelo.Usuario;
import servicios.IService;
import servicios.ServicioRemoto;

// Frontend: la validación de usuario y clave se hace en el Backend (ServicioLogin);
// aquí solo se llama al IService, que en ejecución normal es el ServiceProxy.
public class LoginController {

    private final IService service; // null = usar ServicioRemoto

    public LoginController() {
        this(null);
    }

    /** Para pruebas con un IService falso. */
    public LoginController(IService service) {
        this.service = service;
    }

    /** Devuelve el usuario (sin clave) o null si el id o la clave no coinciden. */
    public Usuario autenticar(String id, String clave) throws Exception {
        if (id == null || id.isBlank() || clave == null || clave.isEmpty()) {
            return null;
        }
        return service().login(id.trim(), clave);
    }

    public void cambiarClave(String id, String claveActual, String claveNueva) throws Exception {
        service().cambiarClave(id, claveActual, claveNueva);
    }

    /** Avisa al Backend que se cerró la sesión (deja de recibir alertas). */
    public void logout() throws Exception {
        service().logout();
    }

    private IService service() {
        return service != null ? service : ServicioRemoto.get();
    }
}
