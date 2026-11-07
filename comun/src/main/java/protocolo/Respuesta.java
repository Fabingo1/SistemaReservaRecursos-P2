package protocolo;

import java.io.Serializable;

// Lo que el Backend contesta: si salió bien, el resultado; si no, el mensaje de error y su TIPO.
// El tipo le dice al ServiceProxy qué excepción volver a lanzar, para que las vistas muestren
// las validaciones como aviso (amarillo) y los errores técnicos como error (rojo).
public class Respuesta implements Serializable {

    private static final long serialVersionUID = 2L;

    public enum TipoError {
        VALIDACION, // dato inválido (IllegalArgumentException): "el teléfono no es válido"
        ESTADO,     // operación no permitida ahora (IllegalStateException): "la reserva ya está cancelada"
        TECNICO     // base de datos caída, error inesperado, etc.
    }

    private final boolean exito;
    private final Object datos;
    private final String error;
    private final TipoError tipoError;

    private Respuesta(boolean exito, Object datos, String error, TipoError tipoError) {
        this.exito = exito;
        this.datos = datos;
        this.error = error;
        this.tipoError = tipoError;
    }

    public static Respuesta ok(Object datos) {
        return new Respuesta(true, datos, null, null);
    }

    /** Error técnico. */
    public static Respuesta error(String mensaje) {
        return error(mensaje, TipoError.TECNICO);
    }

    public static Respuesta error(String mensaje, TipoError tipo) {
        return new Respuesta(false, null, mensaje, tipo == null ? TipoError.TECNICO : tipo);
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

    public TipoError getTipoError() {
        return tipoError;
    }
}
