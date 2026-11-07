package modelo;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas unitarias de la lógica de horario de Reserva (usada por ambas matrices). */
class ReservaTest {

    private static Date fecha(int anio, int mes, int dia, int hora, int minuto) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(anio, mes - 1, dia, hora, minuto);
        return cal.getTime();
    }

    private Reserva reserva(int hIni, int mIni, int hFin, int mFin) {
        Reserva r = new Reserva();
        r.setFecha(fecha(2026, 9, 9, 0, 0));
        r.setHoraInicio(fecha(2026, 9, 9, hIni, mIni));
        r.setHoraFin(fecha(2026, 9, 9, hFin, mFin));
        return r;
    }

    @Test
    void ocupaHora_conMediasHoras_apareceEnAmbasFranjas() {
        Reserva r = reserva(9, 30, 10, 30);
        Date dia = fecha(2026, 9, 9, 0, 0);

        assertFalse(r.ocupaHora(dia, 8));
        assertTrue(r.ocupaHora(dia, 9));   // 9:30-10:00
        assertTrue(r.ocupaHora(dia, 10));  // 10:00-10:30
        assertFalse(r.ocupaHora(dia, 11));
    }

    @Test
    void ocupaHora_horaFinEnPunto_esExclusiva() {
        Reserva r = reserva(9, 0, 11, 0);
        Date dia = fecha(2026, 9, 9, 0, 0);

        assertTrue(r.ocupaHora(dia, 9));
        assertTrue(r.ocupaHora(dia, 10));
        assertFalse(r.ocupaHora(dia, 11));
    }

    @Test
    void ocupaHora_otroDia_devuelveFalse() {
        Reserva r = reserva(9, 0, 11, 0);
        assertFalse(r.ocupaHora(fecha(2026, 9, 10, 0, 0), 9));
    }

    @Test
    void cancelar_dejaLaReservaInactiva() {
        Reserva r = reserva(9, 0, 10, 0);
        assertTrue(r.estaActiva());
        r.cancelar();
        assertFalse(r.estaActiva());
    }

    @Test
    void yaPaso_reservaAntigua_true_reservaFutura_false() {
        Reserva antigua = reserva(9, 0, 10, 0);
        assertTrue(antigua.yaPaso());

        Calendar manana = Calendar.getInstance();
        manana.add(Calendar.DAY_OF_MONTH, 1);
        Reserva futura = new Reserva();
        futura.setFecha(manana.getTime());
        futura.setHoraInicio(manana.getTime());
        futura.setHoraFin(manana.getTime());
        assertFalse(futura.yaPaso());
    }
}
