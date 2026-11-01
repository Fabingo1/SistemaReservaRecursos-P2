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

// Integración: FuncionarioDAO contra Oracle (con schema.sql cargado).
// Si Oracle no está disponible, se saltan en vez de fallar.
class FuncionarioDAOIT {

    private final PruebaBD bd = new PruebaBD();
    private final FuncionarioDAO dao = new FuncionarioDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();

    @BeforeAll
    static void revisarConexion() {
        assumeTrue(PruebaBD.hayConexion(), "Oracle no está disponible: se saltan las pruebas de los DAO");
    }

    @BeforeEach
    void setUp() throws Exception {
        bd.limpiar(); // por si una corrida anterior quedó a medias
        dao.insertar(funcionario("IT-FUNC-A", "Prueba Integración", "8000-0000"));
    }

    @AfterEach
    void tearDown() throws Exception {
        bd.limpiar();
    }

    @Test
    void buscarPorId_existente_devuelveDatosConClave() throws Exception {
        Funcionario f = dao.buscarPorId("IT-FUNC-A");

        assertNotNull(f);
        assertEquals("Prueba Integración", f.getNombre());
        assertEquals("8000-0000", f.getTelefono());
        assertEquals("IT-FUNC-A", f.getClave());
    }

    @Test
    void buscarPorId_inexistente_devuelveNull() throws Exception {
        assertNull(dao.buscarPorId("IT-NO-EXISTE"));
    }

    @Test
    void insertar_idRepetido_lanzaSQLException() {
        assertThrows(SQLException.class,
                () -> dao.insertar(funcionario("IT-FUNC-A", "Otro", "8222-2222")));
    }

    @Test
    void listar_loIncluyeSinClave() throws Exception {
        Funcionario f = buscarEn(dao.listar(), "IT-FUNC-A");

        assertNotNull(f);
        assertNull(f.getClave());
    }

    @Test
    void buscar_porParteDelNombreOIdSinImportarMayusculas_loEncuentra() throws Exception {
        assertNotNull(buscarEn(dao.buscar("INTEGRAC"), "IT-FUNC-A"));
        assertNotNull(buscarEn(dao.buscar("it-func"), "IT-FUNC-A"));
        assertNull(buscarEn(dao.buscar("zzz-nada"), "IT-FUNC-A"));
    }

    @Test
    void modificar_existente_actualizaTambienLaCopiaEnSusReservas() throws Exception {
        reservaDAO.insertar(reservaPasadaDe(dao.buscarPorId("IT-FUNC-A")));

        assertTrue(dao.modificar("IT-FUNC-A", "Nombre Nuevo", "8111-1111"));

        Funcionario f = dao.buscarPorId("IT-FUNC-A");
        assertEquals("Nombre Nuevo", f.getNombre());
        assertEquals("8111-1111", f.getTelefono());

        Funcionario copia = reservaDAO.buscarPorId("IT-RES-A").getFuncionario();
        assertEquals("Nombre Nuevo", copia.getNombre());
        assertEquals("8111-1111", copia.getTelefono());
    }

    @Test
    void modificar_inexistente_devuelveFalse() throws Exception {
        assertFalse(dao.modificar("IT-NO-EXISTE", "Nadie", "8000-0000"));
    }

    @Test
    void cambiarClave_existente_guardaLaNueva() throws Exception {
        assertTrue(dao.cambiarClave("IT-FUNC-A", "claveNueva"));
        assertEquals("claveNueva", dao.buscarPorId("IT-FUNC-A").getClave());
    }

    @Test
    void borrar_existente_loEliminaYLaSegundaVezDevuelveFalse() throws Exception {
        assertTrue(dao.borrar("IT-FUNC-A"));
        assertNull(dao.buscarPorId("IT-FUNC-A"));
        assertFalse(dao.borrar("IT-FUNC-A"));
    }

    @Test
    void borrar_conReservaPasada_noFallaYLaReservaConservaLaCopia() throws Exception {
        reservaDAO.insertar(reservaPasadaDe(dao.buscarPorId("IT-FUNC-A")));

        assertTrue(dao.borrar("IT-FUNC-A")); // reserva no tiene FK a funcionario

        Reserva r = reservaDAO.buscarPorId("IT-RES-A");
        assertNotNull(r);
        assertEquals("Prueba Integración", r.getFuncionario().getNombre());
    }

    private static Funcionario funcionario(String id, String nombre, String telefono) {
        Funcionario f = new Funcionario();
        f.setId(id);
        f.setClave(id);
        f.setNombre(nombre);
        f.setTelefono(telefono);
        return f;
    }

    private static Funcionario buscarEn(List<Funcionario> lista, String id) {
        for (Funcionario f : lista) {
            if (f.getId().equals(id)) {
                return f;
            }
        }
        return null;
    }

    // detalle_reserva no tiene FK a categoría ni recurso, así que no hace falta crearlos
    private static Reserva reservaPasadaDe(Funcionario f) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2001, Calendar.JANUARY, 10, 10, 0);
        Date inicio = cal.getTime();
        cal.set(Calendar.HOUR_OF_DAY, 11);
        Date fin = cal.getTime();

        Categoria cat = new Categoria();
        cat.setId("IT-CAT");
        cat.setDescripcion("Categoría de prueba");
        Recurso rec = new Recurso();
        rec.setId("IT-REC-A");
        rec.setDescripcion("Recurso de prueba");
        rec.setCategoria(cat);
        DetalleReserva d = new DetalleReserva();
        d.setCategoriaSolicitada(cat);
        d.setRecursoAsignado(rec);

        Reserva r = new Reserva();
        r.setId("IT-RES-A");
        r.setActividad("Reserva de prueba");
        r.setFecha(inicio);
        r.setHoraInicio(inicio);
        r.setHoraFin(fin);
        r.setFuncionario(f);
        r.getDetalles().add(d);
        return r;
    }
}