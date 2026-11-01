package protocolo;

import java.io.Serializable;

// Lo que el Backend contesta: si salió bien, el resultado; si no, el mensaje de error
// (el ServiceProxy lo vuelve a lanzar como Exception para que la vista lo muestre).
public class Respuesta implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean exito;
    private final Object datos;
    private final String error;

    private Respuesta(boolean exito, Object datos, String error) {
        this.exito = exito;
        this.datos = datos;
        this.error = error;
    }

    public static Respuesta ok(Object datos) {
        return new Respuesta(true, datos, null);
    }

    public static Respuesta error(String mensaje) {
        return new Respuesta(false, null, mensaje);
    }

    public boolean isExito() {
        return exito;
    }

    public Object getDatos() {
        return datos;
    }

    public String getError() {
        return error;
    }
}
