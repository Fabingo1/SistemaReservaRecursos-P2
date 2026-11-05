package controladores;

import modelo.*;
import org.junit.jupiter.api.Test;
import servicios.IServiceFalso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Frontend: los datos los da un IService falso (sin red ni BD). La regla de qué reservas se devuelven
// vive en ServicioConsultaReservas y se prueba aparte; aquí se prueba la lógica de la matriz.
class CalendarizacionControllerTest {

    private static Date fecha(int hora, int minuto) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2026, Calendar.SEPTEMBER, 9, hora, minuto);
        return cal.getTime();
    }

    private static Recurso recurso(String id) {
        Recurso r = new Recurso();
        r.setId(id);
        return r;
    }

    private static Reserva reservaCon(Recurso recurso, int horaIni, int minIni, int horaFin, int minFin) {
        Reserva r = new Reserva();
        r.setActividad("Reunion");
        r.setFecha(fecha(0, 0));
        r.setHoraInicio(fecha(horaIni, minIni));
        r.setHoraFin(fecha(horaFin, minFin));
        DetalleReserva d = new DetalleReserva();
        d.setRecursoAsignado(recurso);
        r.getDetalles().add(d);
        return r;
    }

    @Test
    void buscarReservaEnCelda_consideraMinutosYRecurso() {
        Recurso sala1 = recurso("S1");
        Recurso sala2 = recurso("S2");
        Reserva r = reservaCon(sala1, 9, 30, 10, 30);
        List<Reserva> delDia = List.of(r);

        CalendarizacionController controller = new CalendarizacionController(new IServiceFalso());

        assertNull(controller.buscarReservaEnCelda(delDia, sala1, 8));
        assertSame(r, controller.buscarReservaEnCelda(delDia, sala1, 9));
        assertSame(r, controller.buscarReservaEnCelda(delDia, sala1, 10));
        assertNull(controller.buscarReservaEnCelda(delDia, sala1, 11));
        assertNull(controller.buscarReservaEnCelda(delDia, sala2, 9), "otro recurso está libre");
    }

    @Test
    void descripcionCelda_incluyeActividadYFuncionario() {
        Funcionario f = new Funcionario();
        f.setNombre("Juan Pérez");
        Reserva r = new Reserva();
        r.setActividad("Reunion");
        r.setFuncionario(f);

        CalendarizacionController controller = new CalendarizacionController(new IServiceFalso());

        assertEquals("Reunion (Juan Pérez)", controller.descripcionCelda(r));
        r.setFuncionario(null);
        assertEquals("Reunion (N/D)", controller.descripcionCelda(r));
    }

    @Test
    void obtenerReservasDelDia_pideAlBackendElMismoDiaComoDesdeYHasta() throws Exception {
        Date dia = fecha(0, 0);
        List<Date> recibidas = new ArrayList<>();
        Reserva r = reservaCon(recurso("S1"), 9, 0, 10, 0);

        CalendarizacionController controller = new CalendarizacionController(new IServiceFalso() {
            @Override
            public List<Reserva> obtenerReservasEntre(Date desde, Date hasta) {
                recibidas.add(desde);
                recibidas.add(hasta);
                return List.of(r);
            }
        });

        List<Reserva> resultado = controller.obtenerReservasDelDia(dia);

        assertEquals(List.of(r), resultado);
        assertEquals(List.of(dia, dia), recibidas);
    }

    @Test
    void obtenerRecursosPorCategoria_pideLaCategoriaSinFiltroDeTexto() throws Exception {
        Categoria sala = new Categoria();
        sala.setId("CAT-001");
        List<Object> recibido = new ArrayList<>();

        CalendarizacionController controller = new CalendarizacionController(new IServiceFalso() {
            @Override
            public List<Recurso> buscarRecursos(Categoria categoria, String texto) {
                recibido.add(categoria);
                recibido.add(texto);
                return List.of(recurso("S1"));
            }
        });

        List<Recurso> resultado = controller.obtenerRecursosPorCategoria(sala);

        assertEquals(1, resultado.size());
        assertSame(sala, recibido.get(0));
        assertNull(recibido.get(1));
    }

    @Test
    void obtenerCategorias_devuelveLoQueManeElServicio() throws Exception {
        Categoria c = new Categoria();
        c.setId("CAT-001");

        CalendarizacionController controller = new CalendarizacionController(new IServiceFalso() {
            @Override
            public List<Categoria> listarCategorias() {
                return List.of(c);
            }
        });

        assertEquals(1, controller.obtenerCategorias().size());
    }

    @Test
    void obtenerReservasDelDia_errorDelBackend_llegaConSuMensaje() {
        CalendarizacionController controller = new CalendarizacionController(new IServiceFalso() {
            @Override
            public List<Reserva> obtenerReservasEntre(Date desde, Date hasta) throws Exception {
                throw new Exception("Debe indicar la fecha de inicio y la fecha final.");
            }
        });

        Exception e = assertThrows(Exception.class, () -> controller.obtenerReservasDelDia(null));
        assertEquals("Debe indicar la fecha de inicio y la fecha final.", e.getMessage());
    }
}
