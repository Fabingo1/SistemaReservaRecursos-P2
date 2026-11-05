package logica;

import datos.ReservaDAO;
import modelo.Categoria;
import modelo.DetalleReserva;
import modelo.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Unitarios: DAO falso en memoria, no necesita Oracle.
// El filtro por estado ACTIVA y por rango de fechas lo hace el SQL de ReservaDAO.listarActivasEntre
// (se prueba en ReservaDAOIT); aquí se prueba la agrupación que hace el servicio.
class ServicioEstadisticasTest {

    private List<Reserva> activas;
    private List<Date> fechasRecibidas;
    private ServicioEstadisticas servicio;
    private Categoria sala;
    private Categoria laptop;

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
        servicio = new ServicioEstadisticas(dao);

        sala = categoria("CAT-001", "Sala");
        laptop = categoria("CAT-002", "Laptop");
    }

    private static Categoria categoria(String id, String descripcion) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setDescripcion(descripcion);
        return c;
    }

    private static Date fecha(int anio, int mes, int dia) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(anio, mes - 1, dia);
        return cal.getTime();
    }

    private static Reserva reserva(Date dia, Categoria... categorias) {
        Reserva r = new Reserva();
        r.setFecha(dia);
        r.setHoraInicio(dia);
        r.setHoraFin(dia);
        for (Categoria c : categorias) {
            DetalleReserva d = new DetalleReserva();
            d.setCategoriaSolicitada(c);
            r.getDetalles().add(d);
        }
        return r;
    }

    // ---------- estadisticasRecursos ----------

    @Test
    void estadisticasRecursos_cuentaPorCategoriaOrdenadoDeMayorAMenor() throws Exception {
        activas.add(reserva(fecha(2026, 9, 7), laptop, sala));
        activas.add(reserva(fecha(2026, 9, 9), sala));
        activas.add(reserva(fecha(2026, 9, 10), sala));

        Map<String, Integer> r = servicio.estadisticasRecursos(fecha(2026, 9, 1), fecha(2026, 9, 30));

        assertEquals(Integer.valueOf(3), r.get("Sala"));
        assertEquals(Integer.valueOf(1), r.get("Laptop"));
        assertEquals("Sala", r.keySet().iterator().next(), "ordenado de mayor a menor");
    }

    @Test
    void estadisticasRecursos_leMandaElRangoAlDAO() throws Exception {
        servicio.estadisticasRecursos(fecha(2026, 9, 1), fecha(2026, 9, 30));
        assertEquals(List.of(fecha(2026, 9, 1), fecha(2026, 9, 30)), fechasRecibidas);
    }

    @Test
    void estadisticasRecursos_empateSeMantieneEnOrdenDeAparicion() throws Exception {
        activas.add(reserva(fecha(2026, 9, 7), laptop));
        activas.add(reserva(fecha(2026, 9, 8), sala));

        Map<String, Integer> r = servicio.estadisticasRecursos(fecha(2026, 9, 1), fecha(2026, 9, 30));

        assertEquals(List.of("Laptop", "Sala"), new ArrayList<>(r.keySet()));
    }

    @Test
    void estadisticasRecursos_descripcionesIgualesDeCategoriasDistintas_agregaElId() throws Exception {
        Categoria otraSala = categoria("CAT-009", "Sala");
        activas.add(reserva(fecha(2026, 9, 7), sala, sala));
        activas.add(reserva(fecha(2026, 9, 8), otraSala));

        Map<String, Integer> r = servicio.estadisticasRecursos(fecha(2026, 9, 1), fecha(2026, 9, 30));

        assertEquals(2, r.size());
        assertEquals(Integer.valueOf(2), r.get("Sala"));
        assertEquals(Integer.valueOf(1), r.get("Sala (CAT-009)"));
    }

    @Test
    void estadisticasRecursos_ignoraDetallesSinCategoria() throws Exception {
        activas.add(reserva(fecha(2026, 9, 7), sala, null));

        Map<String, Integer> r = servicio.estadisticasRecursos(fecha(2026, 9, 1), fecha(2026, 9, 30));

        assertEquals(1, r.size());
    }

    @Test
    void estadisticasRecursos_sinReservas_devuelveMapaVacio() throws Exception {
        assertTrue(servicio.estadisticasRecursos(fecha(2026, 9, 1), fecha(2026, 9, 30)).isEmpty());
    }

    // ---------- estadisticasActividades ----------

    @Test
    void estadisticasActividades_incluyeSemanasSinActividades() throws Exception {
        // Semana 07/09 tiene 2, semana 14/09 tiene 0, semana 21/09 tiene 1
        activas.add(reserva(fecha(2026, 9, 7)));
        activas.add(reserva(fecha(2026, 9, 13)));
        activas.add(reserva(fecha(2026, 9, 23)));

        Map<String, Integer> r = servicio.estadisticasActividades(fecha(2026, 9, 7), fecha(2026, 9, 27));

        assertEquals(3, r.size());
        assertEquals(List.of(2, 0, 1), new ArrayList<>(r.values()));
        assertEquals("07/09/26 - 13/09/26", r.keySet().iterator().next());
    }

    @Test
    void estadisticasActividades_rangoQueEmpiezaAMitadDeSemana_cuentaDesdeSuLunes() throws Exception {
        // 09/09/2026 es miércoles: la primera semana mostrada es la del lunes 07/09
        activas.add(reserva(fecha(2026, 9, 9)));

        Map<String, Integer> r = servicio.estadisticasActividades(fecha(2026, 9, 9), fecha(2026, 9, 9));

        assertEquals(1, r.size());
        assertEquals(Integer.valueOf(1), r.get("07/09/26 - 13/09/26"));
    }

    @Test
    void estadisticasActividades_sinReservas_devuelveLasSemanasEnCero() throws Exception {
        Map<String, Integer> r = servicio.estadisticasActividades(fecha(2026, 9, 7), fecha(2026, 9, 20));

        assertEquals(List.of(0, 0), new ArrayList<>(r.values()));
    }

    // ---------- validación del rango ----------

    @Test
    void rangoInvertido_lanzaExcepcionEnAmbasEstadisticas() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.estadisticasActividades(fecha(2026, 9, 30), fecha(2026, 9, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.estadisticasRecursos(fecha(2026, 9, 30), fecha(2026, 9, 1)));
        assertTrue(fechasRecibidas.isEmpty(), "no debe llegar a consultar la BD");
    }

    @Test
    void fechasNulas_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.estadisticasRecursos(null, fecha(2026, 9, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.estadisticasActividades(fecha(2026, 9, 1), null));
    }
}
