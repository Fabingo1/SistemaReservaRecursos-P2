package modelo;

import java.io.Serializable;

public class Recurso implements Serializable {
    private String id;
    private Categoria categoria;
    private String descripcion;

    public Recurso() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id=id;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria=categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion=descripcion;
    }
}
