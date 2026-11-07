package datos;

import modelo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Integración: RecursoDAO contra Oracle (con schema.sql cargado).
// Si Oracle no está disponible, se saltan en vez de fallar.
class RecursoDAOIT {

    private static final int ORA_UNICO_VIOLADO = 1;   // ORA-00001
    private static final int ORA_PADRE_NO_EXISTE = 2291; // ORA-02291

    private final PruebaBD bd = new PruebaBD();
    private final RecursoDAO dao = new RecursoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();

    private Categoria catA;
    private Categoria catB;

    @BeforeAll
    static void revisarConexion() {
        assumeTrue(PruebaBD.hayConexion(), "Oracle no está disponible: se saltan las pruebas de los DAO");
    }

    @BeforeEach
    void setUp() throws Exception {
        bd.limpiar(); // por si una corrida anterior quedó a medias
        catA = categoria("IT-CAT-A", "Categoría IT A");
        catB = categoria("IT-CAT-B", "Categoría IT B");
        categoriaDAO.insertar(catA);
        categoriaDAO.insertar(catB);
        dao.insertar(recurso("IT-REC-A", catA, "Recurso de Prueba Integración"));
    }

    @AfterEach
    void tearDown() throws Exception {
        bd.limpiar();
    }

    // ---------- buscarPorId / listar ----------

    @Test
    void buscarPorId_existente_devuelveElRecursoConSuCategoriaCompleta() throws Exception {
        Recurso r = dao.buscarPorId("IT-REC-A");

        assertNotNull(r);
        assertEquals("Recurso de Prueba Integración", r.getDescripcion());
        assertEquals("IT-CAT-A", r.getCategoria().getId());
        assertEquals("Categoría IT A", r.getCategoria().getDescripcion());
    }

    @Test
    void buscarPorId_inexistente_devuelveNull() throws Exception {
        assertNull(dao.buscarPorId("IT-NO-EXISTE"));
    }

    @Test
    void listar_incluyeElRecursoConLaDescripcionDeSuCategoria() throws Exception {
        Recurso r = buscarEn(dao.listar(), "IT-REC-A");

        assertNotNull(r);
        assertEquals("Categoría IT A", r.getCategoria().getDescripcion());
    }

    // ---------- insertar ----------

    @Test
    void insertar_idRepetido_lanzaSQLExceptionDeLlaveUnica() {
        SQLException e = assertThrows(SQLException.class,
                () -> dao.insertar(recurso("IT-REC-A", catB, "Otro")));
        assertEquals(ORA_UNICO_VIOLADO, e.getErrorCode());
    }

    @Test
    void insertar_categoriaInexistente_lanzaSQLExceptionDeLlaveForanea() {
        SQLException e = assertThrows(SQLException.class,
                () -> dao.insertar(recurso("IT-REC-B", categoria("IT-CAT-NO-EXISTE", "Fantasma"), "Otro")));
        assertEquals(ORA_PADRE_NO_EXISTE, e.getErrorCode());
    }

    // ---------- buscar ----------

    @Test
    void buscar_porCategoria_devuelveSoloLasDeEsaCategoria() throws Exception {
        dao.insertar(recurso("IT-REC-B", catB, "Recurso en la otra categoría"));

        List<Recurso> deA = dao.buscar("IT-CAT-A", null);
        List<Recurso> deB = dao.buscar("IT-CAT-B", null);

        assertNotNull(buscarEn(deA, "IT-REC-A"));
        assertNull(buscarEn(deA, "IT-REC-B"));
        assertNotNull(buscarEn(deB, "IT-REC-B"));
        assertNull(buscarEn(deB, "IT-REC-A"));
    }

    @Test
    void buscar_porTextoSinImportarMayusculas_loEncuentra() throws Exception {
        assertNotNull(buscarEn(dao.buscar(null, "PRUEBA integr"), "IT-REC-A"));
        assertNull(buscarEn(dao.buscar(null, "zzz-nada"), "IT-REC-A"));
    }

    @Test
    void buscar_categoriaYTextoJuntos_aplicaLosDosFiltros() throws Exception {
        assertNotNull(buscarEn(dao.buscar("IT-CAT-A", "prueba"), "IT-REC-A"));
        assertNull(buscarEn(dao.buscar("IT-CAT-B", "prueba"), "IT-REC-A"));
    }

    @Test
    void buscar_sinFiltros_devuelveTodos() throws Exception {
        assertNotNull(buscarEn(dao.buscar(null, null), "IT-REC-A"));
        assertNotNull(buscarEn(dao.buscar(null, "   "), "IT-REC-A"));
    }

    // ---------- modificar ----------

    @Test
    void modificar_existente_cambiaCategoriaYDescripcion() throws Exception {
        assertTrue(dao.modificar("IT-REC-A", "IT-CAT-B", "Descripción nueva"));

        Recurso r = dao.buscarPorId("IT-REC-A");
        assertEquals("IT-CAT-B", r.getCategoria().getId());
        assertEquals("Descripción nueva", r.getDescripcion());
    }

    @Test
    void modificar_actualizaTambienLaCopiaDeLaDescripcionEnSusReservas() throws Exception {
        reservaDAO.insertar(reserva("IT-RES-R", "IT-REC-A", hace(1), Estado.ACTIVA));

        assertTrue(dao.modificar("IT-REC-A", "IT-CAT-A", "Descripción nueva"));

        Recurso copia = reservaDAO.buscarPorId("IT-RES-R").getDetalles().get(0).getRecursoAsignado();
        assertEquals("Descripción nueva", copia.getDescripcion());
    }

    @Test
    void modificar_inexistente_devuelveFalse() throws Exception {
        assertFalse(dao.modificar("IT-NO-EXISTE", "IT-CAT-A", "Nadie"));
    }

    @Test
    void modificar_categoriaInexistente_lanzaSQLExceptionYNoCambiaNada() throws Exception {
        reservaDAO.insertar(reserva("IT-RES-R", "IT-REC-A", hace(1), Estado.ACTIVA));

        SQLException e = assertThrows(SQLException.class,
                () -> dao.modificar("IT-REC-A", "IT-CAT-NO-EXISTE", "No debería guardarse"));
        assertEquals(ORA_PADRE_NO_EXISTE, e.getErrorCode());

        // Todo o nada: ni el recurso ni la copia en las reservas cambiaron
        assertEquals("Recurso de Prueba Integración", dao.buscarPorId("IT-REC-A").getDescripcion());
        assertEquals("Recurso de Prueba Integración",
                reservaDAO.buscarPorId("IT-RES-R").getDetalles().get(0).getRecursoAsignado().getDescripcion());
    }

    // ---------- borrar ----------

    @Test
    void borrar_existente_loEliminaYLaSegundaVezDevuelveFalse() throws Exception {
        assertTrue(dao.borrar("IT-REC-A"));
        assertNull(dao.buscarPorId("IT-REC-A"));
        assertFalse(dao.borrar("IT-REC-A"));
    }

    @Test
    void borrar_conReservaPasada_noFallaYLaReservaConservaLaCopia() throws Exception {
        reservaDAO.insertar(reserva("IT-RES-R", "IT-REC-A", hace(1), Estado.ACTIVA));

        assertTrue(dao.borrar("IT-REC-A")); // detalle_reserva.recurso_id no tiene FK

        Reserva r = reservaDAO.buscarPorId("IT-RES-R");
        assertNotNull(r);
        assertEquals("Recurso de Prueba Integración",
                r.getDetalles().get(0).getRecursoAsignado().getDescripcion());
    }

    // ---------- tieneReservasFuturas ----------

    @Test
    void tieneReservasFuturas_sinReservas_devuelveFalse() throws Exception {
        assertFalse(dao.tieneReservasFuturas("IT-REC-A"));
    }

    @Test
    void tieneReservasFuturas_reservaActivaFutura_devuelveTrue() throws Exception {
        reservaDAO.insertar(reserva("IT-RES-F", "IT-REC-A", enUnAnio(), Estado.ACTIVA));

        assertTrue(dao.tieneReservasFuturas("IT-REC-A"));
    }

    @Test
    void tieneReservasFuturas_reservaCanceladaFutura_devuelveFalse() throws Exception {
        reservaDAO.insertar(reserva("IT-RES-F", "IT-REC-A", enUnAnio(), Estado.CANCELADA));

        assertFalse(dao.tieneReservasFuturas("IT-REC-A"));
    }

    @Test
    void tieneReservasFuturas_reservaActivaYaTerminada_devuelveFalse() throws Exception {
        reservaDAO.insertar(reserva("IT-RES-R", "IT-REC-A", hace(1), Estado.ACTIVA));

        assertFalse(dao.tieneReservasFuturas("IT-REC-A"));
    }

    @Test
    void tieneReservasFuturas_soloCuentaLasReservasDeEseRecurso() throws Exception {
        dao.insertar(recurso("IT-REC-B", catA, "Otro recurso"));
        reservaDAO.insertar(reserva("IT-RES-F", "IT-REC-B", enUnAnio(), Estado.ACTIVA));

        assertFalse(dao.tieneReservasFuturas("IT-REC-A"));
        assertTrue(dao.tieneReservasFuturas("IT-REC-B"));
    }

    // ---------- utilidades ----------

    private enum Estado { ACTIVA, CANCELADA }

    private static Categoria categoria(String id, String descripcion) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setDescripcion(descripcion);
        return c;
    }

    private static Recurso recurso(String id, Categoria categoria, String descripcion) {
        Recurso r = new Recurso();
        r.setId(id);
        r.setCategoria(categoria);
        r.setDescripcion(descripcion);
        return r;
    }

    private static Recurso buscarEn(List<Recurso> lista, String id) {
        for (Recurso r : lista) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        return null;
    }

    /** Una fecha de hace N años: la reserva ya terminó. */
    private static Date hace(int anios) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -anios);
        return cal.getTime();
    }

    private static Date enUnAnio() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 1);
        return cal.getTime();
    }

    // reserva no tiene FK a funcionario y detalle_reserva no tiene FK a categoría ni recurso:
    // basta con ids que empiecen por "IT-" para que PruebaBD.limpiar() la borre al terminar.
    private static Reserva reserva(String id, String idRecurso, Date dia, Estado estado) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dia);
        cal.set(Calendar.HOUR_OF_DAY, 10);
        cal.set(Calendar.MINUTE, 0);
        Date inicio = cal.getTime();
        cal.set(Calendar.HOUR_OF_DAY, 11);
        Date fin = cal.getTime();

        Funcionario f = new Funcionario();
        f.setId("IT-FUNC-R");
        f.setNombre("Funcionario de prueba");
        f.setTelefono("8000-0000");

        Categoria cat = categoria("IT-CAT-A", "Categoría IT A");
        Recurso rec = recurso(idRecurso, cat, "Recurso de Prueba Integración");
        DetalleReserva d = new DetalleReserva();
        d.setCategoriaSolicitada(cat);
        d.setRecursoAsignado(rec);

        Reserva r = new Reserva();
        r.setId(id);
        r.setActividad("Reserva de prueba");
        r.setFecha(inicio);
        r.setHoraInicio(inicio);
        r.setHoraFin(fin);
        r.setFuncionario(f);
        r.getDetalles().add(d);
        if (estado == Estado.CANCELADA) {
            r.cancelar();
        }
        return r;
    }
}
