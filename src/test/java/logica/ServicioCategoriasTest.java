package logica;

import datos.CategoriaDAO;
import modelo.Categoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

// Unitarios: DAO falso en memoria, no necesita Oracle
class ServicioCategoriasTest {

    private Map<String, String> categorias;      // id -> descripción
    private Map<String, Integer> recursosPorCat; // id -> cantidad de recursos
    private boolean simularErrorFK;
    private ServicioCategorias servicio;

    @BeforeEach
    void setUp() {
        categorias = new TreeMap<>();
        recursosPorCat = new HashMap<>();
        simularErrorFK = false;

        CategoriaDAO dao = new CategoriaDAO() {
            @Override
            public List<Categoria> listar() {
                List<Categoria> lista = new ArrayList<>();
                categorias.forEach((id, desc) -> lista.add(categoria(id, desc)));
                return lista;
            }

            @Override
            public List<Categoria> buscar(String texto) {
                List<Categoria> lista = new ArrayList<>();
                categorias.forEach((id, desc) -> {
                    if (desc.toLowerCase().contains(texto.trim().toLowerCase())) lista.add(categoria(id, desc));
                });
                return lista;
            }

            @Override
            public void insertar(Categoria c) {
                categorias.put(c.getId(), c.getDescripcion());
            }

            @Override
            public boolean modificar(String id, String descripcion) {
                return categorias.replace(id, descripcion) != null;
            }

            @Override
            public boolean borrar(String id) throws SQLException {
                if (simularErrorFK) {
                    throw new SQLException("ORA-02292: integrity constraint violated", "23000", 2292);
                }
                return categorias.remove(id) != null;
            }

            @Override
            public int contarRecursos(String idCategoria) {
                return recursosPorCat.getOrDefault(idCategoria, 0);
            }
        };
        servicio = new ServicioCategorias(dao);
    }

    @Test
    void crear_generaIdsConsecutivosDespuesDelMayor() throws Exception {
        categorias.put("CAT-003", "Proyector");
        assertEquals("CAT-004", servicio.crear("Sala").getId());
        assertEquals("CAT-005", servicio.crear("Laptop").getId());
        assertEquals(3, categorias.size());
    }

    @Test
    void crear_quitaEspaciosDeLaDescripcion() throws Exception {
        Categoria c = servicio.crear("  Sala para 10 personas  ");
        assertEquals("Sala para 10 personas", c.getDescripcion());
    }

    @Test
    void crear_descripcionVaciaOMuyLarga_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crear("   "));
        assertThrows(IllegalArgumentException.class, () -> servicio.crear("x".repeat(101)));
    }

    @Test
    void crear_descripcionRepetidaSinImportarMayusculas_lanzaExcepcion() throws Exception {
        servicio.crear("Sala");
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(" sala "));
    }

    @Test
    void buscar_textoVacio_devuelveTodas() throws Exception {
        servicio.crear("Sala para 10 personas");
        servicio.crear("Laptop windows 11");
        assertEquals(2, servicio.buscar("").size());
        assertEquals(1, servicio.buscar("SALA").size());
    }

    @Test
    void modificar_existente_cambiaLaDescripcion() throws Exception {
        Categoria c = servicio.crear("Sala");
        servicio.modificar(c.getId(), "Sala grande");
        assertEquals("Sala grande", categorias.get(c.getId()));
    }

    @Test
    void modificar_mismaDescripcionDeLaPropiaCategoria_sePermite() throws Exception {
        Categoria c = servicio.crear("Sala");
        servicio.modificar(c.getId(), "SALA");
        assertEquals("SALA", categorias.get(c.getId()));
    }

    @Test
    void modificar_descripcionDeOtraCategoria_lanzaExcepcion() throws Exception {
        servicio.crear("Sala");
        Categoria laptop = servicio.crear("Laptop");
        assertThrows(IllegalArgumentException.class, () -> servicio.modificar(laptop.getId(), "sala"));
    }

    @Test
    void modificar_inexistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.modificar("CAT-999", "Algo"));
    }

    @Test
    void borrar_sinRecursos_laElimina() throws Exception {
        Categoria c = servicio.crear("Sala");
        servicio.borrar(c.getId());
        assertFalse(categorias.containsKey(c.getId()));
    }

    @Test
    void borrar_conRecursos_lanzaExcepcionYNoBorra() throws Exception {
        Categoria c = servicio.crear("Sala");
        recursosPorCat.put(c.getId(), 2);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> servicio.borrar(c.getId()));
        assertTrue(e.getMessage().contains("2 recurso"));
        assertTrue(categorias.containsKey(c.getId()));
    }

    @Test
    void borrar_errorDeLlaveForaneaDeOracle_seTraduceAMensajeClaro() throws Exception {
        Categoria c = servicio.crear("Sala");
        simularErrorFK = true;
        assertThrows(IllegalArgumentException.class, () -> servicio.borrar(c.getId()));
    }

    @Test
    void borrar_inexistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.borrar("CAT-999"));
    }

    private static Categoria categoria(String id, String descripcion) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setDescripcion(descripcion);
        return c;
    }
}
