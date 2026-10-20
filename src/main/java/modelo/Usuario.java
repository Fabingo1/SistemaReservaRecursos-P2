package modelo;

import java.io.Serializable;

public abstract class Usuario implements Serializable {

    protected String id;
    protected String clave;

    protected Usuario() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public void cambiarClave(String nuevaClave) {
        if (nuevaClave == null || nuevaClave.isBlank()) {
            throw new IllegalArgumentException("La clave no puede estar vacía.");
        }
        this.clave = nuevaClave;
    }
}
