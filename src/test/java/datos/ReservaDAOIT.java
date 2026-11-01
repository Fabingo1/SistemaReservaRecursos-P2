package datos;

import modelo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Integración: ReservaDAO contra Oracle (con schema.sql cargado).
// Usa su propia categoría con dos recursos y fechas de 2099, así no choca con el seed.
// Si Oracle no está disponible, se saltan en vez de fallar.
class ReservaDAOIT {

    private final PruebaBD bd = new PruebaBD();
    private final ReservaDAO dao = new ReservaDAO();

    private Funcionario funcionario;
    private Categoria categoria;
    private Recurso recursoA;
    private Recurso recursoB;

    @BeforeAll
    static void revisarConexion() {
        assumeTrue(PruebaBD.hayConexion(), "Oracle no está disponible: se saltan las pruebas de los DAO");
    }

    @BeforeEach
    void setUp() throws Exception {
        bd.limpiar(); // por si una corrida anterior quedó a medias

        categoria = new Categoria();
        categoria.setId("IT-CAT");
        categoria.setDescripcion("Categoría de prueba");
        bd.ejecutar("INSERT INTO categoria (id, descripcion) VALUES (?, ?)",
                categoria.getId(), categoria.getDescripcion());

        recursoA = recurso("IT-REC-A", "Recurso de prueba A");
        recursoB = recurso("IT-REC-B", "Recurso de prueba B");
        funcionario = funcionario("IT-FUNC-A");
    }

    @AfterEach
    void tearDown() throws Exception {
        bd.limpiar();
    }

    @Test
    void insertar_yBuscarPorId_recuperaLaReservaConSuDetalle() throws Exception {
        dao.insertar(reserva("IT-RES-A", dia(15), 10, 12, recursoA));

        Reserva r = dao.buscarPorId("IT-RES-A");

        assertNotNull(r);
        assertEquals("Actividad IT-RES-A", r.getActividad());
        assertEquals(EstadoReserva.ACTIVA, r.getEstado());
        assertTrue(r.esDelDia(dia(15)));
        assertEquals(10 * 60, r.minutoInicio());
        assertEquals(12 * 60, r.minutoFin());
        assertEquals("IT-FUNC-A", r.getFuncionario().getId());
        assertEquals("Prueba Integración", r.getFuncionario().getNombre());

        assertEquals(1, r.getDetalles().size());
        DetalleReserva d = r.getDetalles().get(0);
        assertEquals("IT-CAT", d.getCategoriaSolicitada().getId());
        assertEquals("IT-REC-A", d.getRecursoAsignado().getId());
        assertEquals("Recurso de prueba A", d.getRecursoAsignado().getDescripcion());
    }

    @Test
    void buscarPorId_inexistente_devuelveNull() throws Exception {
        assertNull(dao.buscarPorId("IT-RES-NO"));
    }

    @Test
    void insertar_detalleQueFalla_noDejaNadaGuardado() throws Exception {
        // Dos detalles con la misma categoría violan la llave del detalle.
        // Como va en una transacción, la reserva tampoco debe quedar.
        Reserva r = reserva("IT-RES-A", dia(15), 10, 12, recursoA, recursoB);

        assertThrows(SQLException.class, () -> dao.insertar(r));
        assertNull(dao.buscarPorId("IT-RES-A"));
    }

    @Test
    void buscarRecursoDisponible_recursoOcupado_ofreceElSiguiente() throws Exception {
        dao.insertar(reserva("IT-RES-A", dia(15), 10, 12, recursoA));

        Recurso libre = dao.buscarRecursoDisponible(categoria, momento(15, 11), momento(15, 13));

        assertEquals("IT-REC-B", libre.getId());
    }

    @Test
    void buscarRecursoDisponible_horariosQueSoloSeTocan_noSeTraslapan() throws Exception {
        dao.insertar(reserva("IT-RES-A", dia(15), 10, 12, recursoA));

        assertEquals("IT-REC-A", dao.buscarRecursoDisponible(categoria, momento(15, 12), momento(15, 13)).getId());
        assertEquals("IT-REC-A", dao.buscarRecursoDisponible(categoria, momento(15, 8), momento(15, 10)).getId());
    }

    @Test
    void buscarRecursoDisponible_todosOcupados_devuelveNull() throws Exception {
        dao.insertar(reserva("IT-RES-A", dia(15), 10, 12, recursoA));
        dao.insertar(reserva("IT-RES-B", dia(15), 9, 11, recursoB));

        assertNull(dao.buscarRecursoDisponible(categoria, momento(15, 10), momento(15, 11)));
    }

    @Test
    void cancelar_existente_quedaCanceladaYLiberaElRecurso() throws Exception {
        dao.insertar(reserva("IT-RES-A", dia(15), 10, 12, recursoA));

        assertTrue(dao.cancelar("IT-RES-A"));

        assertEquals(EstadoReserva.CANCELADA, dao.buscarPorId("IT-RES-A").getEstado());
        assertEquals("IT-REC-A", dao.buscarRecursoDisponible(categoria, momento(15, 10), momento(15, 12)).getId());
    }

    @Test
    void cancelar_inexistente_devuelveFalse() throws Exception {
        assertFalse(dao.cancelar("IT-RES-NO"));
    }

    @Test
    void listarDeFuncionario_traeActivasYCanceladasSoloSuyas_laMasRecientePrimero() throws Exception {
        dao.insertar(reserva("IT-RES-A", dia(15), 10, 12, recursoA));
        dao.insertar(reserva("IT-RES-B", dia(16), 10, 12, recursoA));
        dao.cancelar("IT-RES-B");
        Reserva deOtro = reserva("IT-RES-C", dia(17), 10, 12, recursoA);
        deOtro.setFuncionario(funcionario("IT-FUNC-B"));
        dao.insertar(deOtro);

        List<Reserva> lista = dao.listarDeFuncionario("IT-FUNC-A");

        assertEquals(List.of("IT-RES-B", "IT-RES-A"), ids(lista));
    }

    @Test
    void listarActivasEntre_incluyeAmbosDiasCompletosYExcluyeCanceladas() throws Exception {
        dao.insertar(reserva("IT-RES-A", dia(15), 10, 12, recursoA)); // primer día del rango
        dao.insertar(reserva("IT-RES-B", dia(17), 20, 21, recursoA)); // último día, de noche
        dao.insertar(reserva("IT-RES-C", dia(16), 10, 12, recursoA)); // cancelada
        dao.cancelar("IT-RES-C");
        dao.insertar(reserva("IT-RES-D", dia(18), 8, 9, recursoA));   // fuera del rango

        List<Reserva> lista = dao.listarActivasEntre(dia(15), dia(17));

        assertEquals(List.of("IT-RES-A", "IT-RES-B"), ids(lista));
    }

    @Test
    void tieneReservasFuturas_soloCuentaActivasQueNoHanTerminado() throws Exception {
        assertFalse(dao.tieneReservasFuturas("IT-FUNC-A"));

        dao.insertar(reserva("IT-RES-A", diaPasado(), 10, 12, recursoA));
        assertFalse(dao.tieneReservasFuturas("IT-FUNC-A")); // la pasada no cuenta

        dao.insertar(reserva("IT-RES-B", dia(15), 10, 12, recursoA));
        assertTrue(dao.tieneReservasFuturas("IT-FUNC-A"));

        dao.cancelar("IT-RES-B");
        assertFalse(dao.tieneReservasFuturas("IT-FUNC-A")); // la cancelada tampoco
    }

    @Test
    void maxNumeroId_soloTomaIdsConFormatoRES() throws Exception {
        dao.insertar(reserva("IT-RES-A", dia(15), 10, 12, recursoA)); // no tiene formato RES-número
        int antes = dao.maxNumeroId();

        dao.insertar(reserva(String.format("RES-%06d", antes + 1), dia(16), 10, 12, recursoA));

        assertEquals(antes + 1, dao.maxNumeroId());
    }

    private Recurso recurso(String id, String descripcion) throws SQLException {
        bd.ejecutar("INSERT INTO recurso (id, descripcion, categoria_id) VALUES (?, ?, ?)",
                id, descripcion, categoria.getId());
        Recurso r = new Recurso();
        r.setId(id);
        r.setDescripcion(descripcion);
        r.setCategoria(categoria);
        return r;
    }

    private static Funcionario funcionario(String id) {
        Funcionario f = new Funcionario();
        f.setId(id);
        f.setNombre("Prueba Integración");
        f.setTelefono("8000-0000");
        return f;
    }

    // Un detalle por recurso, con la categoría de ese recurso
    private Reserva reserva(String id, Date dia, int horaInicio, int horaFin, Recurso... recursos) {
        Reserva r = new Reserva();
        r.setId(id);
        r.setActividad("Actividad " + id);
        r.setFecha(dia);
        r.setHoraInicio(conHora(dia, horaInicio));
        r.setHoraFin(conHora(dia, horaFin));
        r.setFuncionario(funcionario);
        for (Recurso rec : recursos) {
            DetalleReserva d = new DetalleReserva();
            d.setCategoriaSolicitada(rec.getCategoria());
            d.setRecursoAsignado(rec);
            r.getDetalles().add(d);
        }
        return r;
    }

    private static Date dia(int diaDelMes) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2099, Calendar.JUNE, diaDelMes);
        return cal.getTime();
    }

    private static Date diaPasado() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2001, Calendar.JANUARY, 10);
        return cal.getTime();
    }

    private static Date momento(int diaDelMes, int hora) {
        return conHora(dia(diaDelMes), hora);
    }

    private static Date conHora(Date dia, int hora) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dia);
        cal.set(Calendar.HOUR_OF_DAY, hora);
        return cal.getTime();
    }

    // Solo las de prueba, por si hay otras reservas en esas fechas
    private static List<String> ids(List<Reserva> reservas) {
        List<String> ids = new ArrayList<>();
        for (Reserva r : reservas) {
            if (r.getId().startsWith("IT-")) {
                ids.add(r.getId());
            }
        }
        return ids;
    }
}