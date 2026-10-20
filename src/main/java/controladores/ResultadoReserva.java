package controladores;

import modelo.Categoria;
import modelo.Reserva;

import java.io.Serializable;
import java.util.List;

public class ResultadoReserva implements Serializable {
    private final boolean exitosa;
    private final Reserva reserva;
    private final List<Categoria> categoriasNoDisponibles;

    private ResultadoReserva(boolean exitosa, Reserva reserva, List<Categoria> categoriasNoDisponibles) {
        this.exitosa = exitosa;
        this.reserva = reserva;
        this.categoriasNoDisponibles = categoriasNoDisponibles;
    }

    public static ResultadoReserva exito(Reserva reserva) {
        return new ResultadoReserva(true, reserva, null);
    }

    public static ResultadoReserva fallo(List<Categoria> categoriasNoDisponibles) {
        return new ResultadoReserva(false, null, categoriasNoDisponibles);
    }

    public boolean isExitosa() {
        return exitosa;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public List<Categoria> getCategoriasNoDisponibles() {
        return categoriasNoDisponibles;
    }
}
