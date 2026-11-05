package logica;

import datos.ReservaDAO;
import modelo.Reserva;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

// Consulta de reservas por rango de días para Calendarización y Actividades (solo lectura).
// Va aparte de ServicioReservas para no mezclarla con crear/cancelar reservas.
public class ServicioConsultaReservas {

    private final ReservaDAO reservaDAO;

    public ServicioConsultaReservas() {
        this(new ReservaDAO());
    }

    /** Para pruebas con un DAO falso. */
    public ServicioConsultaReservas(ReservaDAO reservaDAO) {
        this.reservaDAO = reservaDAO;
    }

    /**
     * Reservas ACTIVAS que empiezan entre desde y hasta, ambos días incluidos.
     * Para un solo día se manda la misma fecha en los dos parámetros.
     */
    public List<Reserva> obtenerReservasEntre(Date desde, Date hasta) throws SQLException {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Debe indicar la fecha de inicio y la fecha final.");
        }
        // Se compara por día: la hora que traiga la fecha no importa (el DAO usa días completos)
        if (!Reserva.mismoDia(desde, hasta) && hasta.before(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha de inicio.");
        }
        return reservaDAO.listarActivasEntre(desde, hasta);
    }
}
