package controladores;

import modelo.Funcionario;
import modelo.Reserva;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActividadesControllerTest {

    private final ActividadesController controller = new ActividadesController();

    @Test
    void obtenerLunesDeSemana_retornaUnLunes() throws Exception {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
        Date miercoles = formato.parse("09/09/2026"); // miércoles

        Date lunes = controller.obtenerLunesDeSemana(miercoles);

        Calendar cal = Calendar.getInstance();
        cal.setTime(lunes);
        assertEquals(Calendar.MONDAY, cal.get(Calendar.DAY_OF_WEEK));
        assertEquals("07/09/2026", formato.format(lunes));
    }

    @Test
    void ocurreEn_detectaReservaDentroDelHorario() throws Exception {
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
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
