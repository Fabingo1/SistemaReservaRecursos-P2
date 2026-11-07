package modelo;

import java.io.Serializable;

public class DetalleReserva implements Serializable {

    private Categoria categoriaSolicitada;
    private Recurso recursoAsignado;

    public DetalleReserva() {
    }

    public Categoria getCategoriaSolicitada() {
        return categoriaSolicitada;
    }

    public void setCategoriaSolicitada(Categoria categoriaSolicitada) {
        this.categoriaSolicitada = categoriaSolicitada;
    }

    public Recurso getRecursoAsignado() {
        return recursoAsignado;
    }

    public void setRecursoAsignado(Recurso recursoAsignado) {
        this.recursoAsignado = recursoAsignado;
    }
}
