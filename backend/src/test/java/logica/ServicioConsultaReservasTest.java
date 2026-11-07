package logica;

import datos.ReservaDAO;
import modelo.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Unitarios: DAO falso en memoria, no necesita Oracle
class ServicioConsultaReservasTest {

    private List<Reserva> activas;
    private List<Date> fechasRecibidas;
    private ServicioConsultaReservas servicio;

    @BeforeEach
    void setUp() {
        activas = new ArrayList<>();
        fechasRecibidas = new ArrayList<>();

        ReservaDAO dao = new ReservaDAO() {
            @Override
            public List<Reserva> listarActivasEntre(Date desde, Date hasta) {
                fechasRecibidas.add(desde);
                fechasRecibidas.add(hasta);
                return activas;
            }
        };
        servicio = new ServicioConsultaReservas(dao);
    }

    private static Date dia(int diaDelMes, int hora) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2026, Calendar.SEPTEMBER, diaDelMes, hora, 0);
        return cal.getTime();
    }

    @Test
    void obtenerReservasEntre_devuelveLoQueDaElDAOYLeMandaLasFechas() throws Exception {
        Reserva r = new Reserva();
        r.setId("RES-000001");
        activas.add(r);

        List<Reserva> resultado = servicio.obtenerReservasEntre(dia(14, 0), dia(20, 0));

        assertEquals(1, resultado.size());
        assertEquals(List.of(dia(14, 0), dia(20, 0)), fechasRecibidas);
    }

    @Test
    void obtenerReservasEntre_unSoloDia_sePermite() throws Exception {
        assertDoesNotThrow(() -> servicio.obtenerReservasEntre(dia(9, 0), dia(9, 0)));
    }

    @Test
    void obtenerReservasEntre_mismoDiaConHorasInvertidas_sePermite() throws Exception {
        // El DAO trabaja con días completos, así que la hora de la fecha no cuenta
        assertDoesNotThrow(() -> servicio.obtenerReservasEntre(dia(9, 15), dia(9, 8)));
    }

    @Test
    void obtenerReservasEntre_fechasNulas_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerReservasEntre(null, dia(9, 0)));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerReservasEntre(dia(9, 0), null));
        assertTrue(fechasRecibidas.isEmpty(), "no debe llegar a consultar la BD");
    }

    @Test
    void obtenerReservasEntre_fechaFinalAnterior_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerReservasEntre(dia(20, 0), dia(14, 0)));
        assertTrue(fechasRecibidas.isEmpty());
    }
}
