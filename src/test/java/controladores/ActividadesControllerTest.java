package controladores;

import modelo.Funcionario;
import modelo.Reserva;
import org.junit.jupiter.api.Test;
import servicios.IServiceFalso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Frontend: las reservas las da un IService falso (sin red ni BD). Qué reservas se devuelven por rango
// se prueba aparte en ServicioConsultaReservasTest; aquí se prueba el cálculo de la semana y de las celdas.
class ActividadesControllerTest {

    private final ActividadesController controller = new ActividadesController(new IServiceFalso());
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

    @Test
    void obtenerLunesDeSemana_retornaUnLunes() throws Exception {
        Date miercoles = formatoFecha.parse("09/09/2026"); // miércoles

        Date lunes = controller.obtenerLunesDeSemana(miercoles);

        Calendar cal = Calendar.getInstance();
        cal.setTime(lunes);
        assertEquals(Calendar.MONDAY, cal.get(Calendar.DAY_OF_WEEK));
        assertEquals("07/09/2026", formatoFecha.format(lunes));
    }

    @Test
    void obtenerLunesDeSemana_siLaFechaEsDomingo_devuelveElLunesAnterior() throws Exception {
        Date domingo = formatoFecha.parse("13/09/2026");
        assertEquals("07/09/2026", formatoFecha.format(controller.obtenerLunesDeSemana(domingo)));
    }

    @Test
    void obtenerDiasDeSemana_devuelveSieteDiasDeLunesADomingo() throws Exception {
        List<Date> dias = controller.obtenerDiasDeSemana(formatoFecha.parse("09/09/2026"));

        assertEquals(7, dias.size());
        assertEquals("07/09/2026", formatoFecha.format(dias.get(0)));
        assertEquals("13/09/2026", formatoFecha.format(dias.get(6)));
    }

    @Test
    void obtenerReservasSemana_pideAlBackendDeLunesADomingo() throws Exception {
        List<Date> recibidas = new ArrayList<>();
        Reserva r = new Reserva();
        r.setId("RES-000001");

        ActividadesController conServicio = new ActividadesController(new IServiceFalso() {
            @Override
            public List<Reserva> obtenerReservasEntre(Date desde, Date hasta) {
                recibidas.add(desde);
                recibidas.add(hasta);
                return List.of(r);
            }
        });

        List<Reserva> resultado = conServicio.obtenerReservasSemana(formatoFecha.parse("09/09/2026"));

        assertEquals(List.of(r), resultado);
        assertEquals("07/09/2026", formatoFecha.format(recibidas.get(0)));
        assertEquals("13/09/2026", formatoFecha.format(recibidas.get(1)));
    }

    @Test
    void obtenerReservasSemana_errorDelBackend_llegaConSuMensaje() {
        ActividadesController conServicio = new ActividadesController(new IServiceFalso() {
            @Override
            public List<Reserva> obtenerReservasEntre(Date desde, Date hasta) throws Exception {
                throw new Exception("Error de base de datos: sin conexión");
            }
        });

        Exception e = assertThrows(Exception.class, () -> conServicio.obtenerReservasSemana(new Date()));
        assertEquals("Error de base de datos: sin conexión", e.getMessage());
    }

    @Test
    void ocurreEn_detectaReservaDentroDelHorario() throws Exception {
        SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm");

        Reserva r = new Reserva();
        r.setActividad("Sesión de Junta Directiva");
        r.setFecha(formatoFecha.parse("09/09/2026"));
        r.setHoraInicio(formatoHora.parse("09:00"));
        r.setHoraFin(formatoHora.parse("11:00"));

        Funcionario f = new Funcionario();
        f.setId("func1");
        f.setNombre("Juan Pérez");
        r.setFuncionario(f);

        Date mismoDia = formatoFecha.parse("09/09/2026");
        Date otroDia = formatoFecha.parse("10/09/2026");

        assertTrue(controller.ocurreEn(r, mismoDia, 9));
        assertTrue(controller.ocurreEn(r, mismoDia, 10));
        assertFalse(controller.ocurreEn(r, mismoDia, 11)); // hora fin es exclusiva
        assertFalse(controller.ocurreEn(r, mismoDia, 8));
        assertFalse(controller.ocurreEn(r, otroDia, 9));
    }

    @Test
    void descripcionCelda_incluyeActividadYFuncionario() {
        Reserva r = new Reserva();
        r.setActividad("Capacitación");
        Funcionario f = new Funcionario();
        f.setNombre("Ana Solano");
        r.setFuncionario(f);

        assertEquals("Capacitación (Ana Solano)", controller.descripcionCelda(r));
    }
}
