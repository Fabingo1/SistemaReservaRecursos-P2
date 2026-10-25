package logica;

import controladores.ResultadoReserva;
import datos.FuncionarioDAO;
import datos.ReservaDAO;
import modelo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Unitarios: DAOs falsos en memoria, no necesita Oracle
class ServicioReservasTest {

    private Map<String, Reserva> reservas;
    private Map<String, Recurso> libresPorCategoria;
    private Funcionario func1;
    private Categoria catSala;
    private Categoria catLaptop;
    private ServicioReservas servicio;

    @BeforeEach
    void setUp() {
        reservas = new HashMap<>();
        libresPorCategoria = new HashMap<>();

        func1 = new Funcionario();
        func1.setId("func1");
        func1.setClave("func1");
        func1.setNombre("Juan Pérez");
        func1.setTelefono("8888-8888");

        catSala = categoria("CAT-001", "Sala para 10 personas");
        catLaptop = categoria("CAT-002", "Laptop windows 11");

        ReservaDAO reservaDAO = new ReservaDAO() {
            @Override
            public Recurso buscarRecursoDisponible(Categoria categoria, Date inicio, Date fin) {
                return libresPorCategoria.get(categoria.getId());
            }

            @Override
            public int maxNumeroId() {
                return 5;
            }

            @Override
            public void insertar(Reserva r) {
                reservas.put(r.getId(), r);
            }

            @Override
            public Reserva buscarPorId(String id) {
                return reservas.get(id);
            }

            @Override
            public boolean cancelar(String id) {
                reservas.get(id).cancelar();
                return true;
            }
        };
        FuncionarioDAO funcionarioDAO = new FuncionarioDAO() {
            @Override
            public Funcionario buscarPorId(String id) {
                return "func1".equals(id) ? func1 : null;
            }
        };

        servicio = new ServicioReservas(reservaDAO, funcionarioDAO, null);
    }

    //crearReserva

    @Test
    void crearReserva_hayDisponibilidad_asignaRecursosYGeneraSiguienteId() throws Exception {
        libresPorCategoria.put("CAT-001", recurso("SALA-1", catSala));
        libresPorCategoria.put("CAT-002", recurso("238715", catLaptop));

        ResultadoReserva r = servicio.crearReserva(func1, " Reunión ", manana(), hora(9), hora(10),
                List.of(catSala, catLaptop));

        assertTrue(r.isExitosa());
        assertEquals("RES-000006", r.getReserva().getId());
        assertEquals("Reunión", r.getReserva().getActividad());
        assertEquals(2, r.getReserva().getDetalles().size());
        assertTrue(reservas.containsKey("RES-000006"));
    }

    @Test
    void crearReserva_guardaCopiaDelFuncionarioSinClave() throws Exception {
        libresPorCategoria.put("CAT-001", recurso("SALA-1", catSala));

        ResultadoReserva r = servicio.crearReserva(func1, "Reunión", manana(), hora(9), hora(10),
                List.of(catSala));

        Funcionario guardado = r.getReserva().getFuncionario();
        assertEquals("Juan Pérez", guardado.getNombre());
        assertNull(guardado.getClave());
    }

    @Test
    void crearReserva_unaCategoriaSinRecursoLibre_fallaYNoGuarda() throws Exception {
        libresPorCategoria.put("CAT-001", recurso("SALA-1", catSala));

        ResultadoReserva r = servicio.crearReserva(func1, "Reunión", manana(), hora(9), hora(10),
                List.of(catSala, catLaptop));

        assertFalse(r.isExitosa());
        assertEquals(1, r.getCategoriasNoDisponibles().size());
        assertEquals("CAT-002", r.getCategoriasNoDisponibles().get(0).getId());
        assertTrue(reservas.isEmpty());
    }

    @Test
    void crearReserva_categoriasRepetidas_cuentanUnaVez() throws Exception {
        libresPorCategoria.put("CAT-001", recurso("SALA-1", catSala));

        ResultadoReserva r = servicio.crearReserva(func1, "Reunión", manana(), hora(9), hora(10),
                List.of(catSala, catSala));

        assertEquals(1, r.getReserva().getDetalles().size());
    }

    @Test
    void crearReserva_horaFinAntesDeInicio_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearReserva(func1, "Reunión", manana(), hora(10), hora(9), List.of(catSala)));
    }

    @Test
    void crearReserva_fechaPasada_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearReserva(func1, "Reunión", diasDesdeHoy(-1), hora(9), hora(10), List.of(catSala)));
    }

    @Test
    void crearReserva_sinCategorias_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearReserva(func1, "Reunión", manana(), hora(9), hora(10), new ArrayList<>()));
    }

    @Test
    void crearReserva_actividadVacia_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearReserva(func1, "  ", manana(), hora(9), hora(10), List.of(catSala)));
    }

    @Test
    void crearReserva_funcionarioQueNoExiste_lanzaExcepcion() {
        Funcionario fantasma = new Funcionario();
        fantasma.setId("nadie");

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearReserva(fantasma, "Reunión", manana(), hora(9), hora(10), List.of(catSala)));
    }

    //cancelarReserva

    @Test
    void cancelarReserva_propiaActivaFutura_quedaCancelada() throws Exception {
        reservas.put("RES-000001", reserva("RES-000001", "func1", manana()));

        servicio.cancelarReserva("RES-000001", func1);

        assertFalse(reservas.get("RES-000001").estaActiva());
    }

    @Test
    void cancelarReserva_deOtroFuncionario_lanzaExcepcion() {
        reservas.put("RES-000001", reserva("RES-000001", "func2", manana()));

        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarReserva("RES-000001", func1));
    }

    @Test
    void cancelarReserva_comoAdministrador_noRevisaDueno() throws Exception {
        reservas.put("RES-000001", reserva("RES-000001", "func2", manana()));

        servicio.cancelarReserva("RES-000001", null);

        assertFalse(reservas.get("RES-000001").estaActiva());
    }

    @Test
    void cancelarReserva_yaCancelada_lanzaExcepcion() {
        Reserva r = reserva("RES-000001", "func1", manana());
        r.cancelar();
        reservas.put("RES-000001", r);

        assertThrows(IllegalStateException.class, () -> servicio.cancelarReserva("RES-000001", func1));
    }

    @Test
    void cancelarReserva_queYaPaso_lanzaExcepcion() {
        reservas.put("RES-000001", reserva("RES-000001", "func1", diasDesdeHoy(-1)));

        assertThrows(IllegalStateException.class, () -> servicio.cancelarReserva("RES-000001", func1));
    }

    @Test
    void cancelarReserva_inexistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarReserva("RES-999999", func1));
    }

    //extraerDatos

    @Test
    void extraerDatos_fraseVacia_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.extraerDatos("  ", List.of(catSala)));
    }

    //utilidades

    private static Categoria categoria(String id, String descripcion) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setDescripcion(descripcion);
        return c;
    }

    private static Recurso recurso(String id, Categoria categoria) {
        Recurso r = new Recurso();
        r.setId(id);
        r.setDescripcion(id);
        r.setCategoria(categoria);
        return r;
    }

    private static Reserva reserva(String id, String funcionarioId, Date dia) {
        Funcionario f = new Funcionario();
        f.setId(funcionarioId);
        Reserva r = new Reserva();
        r.setId(id);
        r.setActividad("Reunión");
        r.setFecha(dia);
        r.setHoraInicio(hora(9));
        r.setHoraFin(hora(10));
        r.setFuncionario(f);
        return r;
    }

    private static Date manana() {
        return diasDesdeHoy(1);
    }

    private static Date diasDesdeHoy(int dias) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, dias);
        return cal.getTime();
    }

    private static Date hora(int h) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}