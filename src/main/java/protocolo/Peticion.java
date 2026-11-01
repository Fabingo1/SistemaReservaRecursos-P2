package protocolo;

import java.io.Serializable;

// Lo que el Frontend le manda al Backend: qué operación quiere y con qué parámetros.
public class Peticion implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String operacion;
    private final Object[] parametros;

    public Peticion(String operacion, Object... parametros) {
        this.operacion = operacion;
        this.parametros = parametros == null ? new Object[0] : parametros;
    }

    public String getOperacion() {
        return operacion;
    }

    public Object[] getParametros() {
        return parametros;
    }
}
