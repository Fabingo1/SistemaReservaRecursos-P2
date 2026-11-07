package logica;

import datos.CategoriaDAO;
import datos.RecursoDAO;
import modelo.Categoria;
import modelo.Recurso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Unitarios: DAOs falsos en memoria, no necesita Oracle.
// A diferencia de ServicioCategorias, aquí el id lo escribe el administrador (no se autogenera),
// y validarCategoriaExiste siempre vuelve a buscar la categoría en su propio DAO falso, nunca
// confía en el objeto Categoria que llega como parámetro.
class ServicioRecursosTest {

    private Map<String, Recurso> recursos;     // id -> recurso guardado
    private Map<String, Categoria> categorias; // id -> categoría existente en la "base de datos"
    private Map<String, Boolean> reservasFuturasPorRecurso;
    private boolean simularErrorIdDuplicado;
    private boolean simularErrorCategoriaDesaparecida;
    private ServicioRecursos servicio;

    @BeforeEach
    void setUp() {
        recursos = new HashMap<>();
        categorias = new HashMap<>();
        reservasFuturasPorRecurso = new HashMap<>();
        simularErrorIdDuplicado = false;
        simularErrorCategoriaDesaparecida = false;

        categorias.put("CAT-001", categoria("CAT-001", "Sala"));
        categorias.put("CAT-002", categoria("CAT-002", "Laptop"));

        RecursoDAO recursoDAO = new RecursoDAO() {
            @Override
            public List<Recurso> listar() {
                return new ArrayList<>(recursos.values());
            }

            @Override
            public List<Recurso> buscar(String idCategoria, String texto) {
                List<Recurso> lista = new ArrayList<>();
                for (Recurso r : recursos.values()) {
                    boolean coincideCategoria = idCategoria == null || r.getCategoria().getId().equals(idCategoria);
                    boolean coincideTexto = texto == null
                            || r.getDescripcion().toLowerCase().contains(texto.toLowerCase());
                    if (coincideCategoria && coincideTexto) lista.add(r);
                }
                return lista;
            }

            @Override
            public Recurso buscarPorId(String id) {
                return recursos.get(id);
            }

            @Override
            public void insertar(Recurso recurso) throws SQLException {
                if (simularErrorIdDuplicado) {
                    throw new SQLException("ORA-00001: unique constraint violated", "23000", 1);
                }
                recursos.put(recurso.getId(), recurso);
            }

            @Override
            public boolean modificar(String id, String idCategoria, String descripcion) throws SQLException {
                if (simularErrorCategoriaDesaparecida) {
                    throw new SQLException("ORA-02291: integrity constraint violated - parent key not found",
                            "23000", 2291);
                }
                Recurso existente = recursos.get(id);
                if (existente == null) {
                    return false;
                }
                existente.setCategoria(categorias.get(idCategoria));
                existente.setDescripcion(descripcion);
                return true;
            }

            @Override
            public boolean borrar(String id) {
                return recursos.remove(id) != null;
            }

            @Override
            public boolean tieneReservasFuturas(String idRecurso) {
                return reservasFuturasPorRecurso.getOrDefault(idRecurso, false);
            }
        };

        CategoriaDAO categoriaDAO = new CategoriaDAO() {
            @Override
            public Categoria buscarPorId(String id) {
                return categorias.get(id);
            }
        };

        servicio = new ServicioRecursos(recursoDAO, categoriaDAO);
    }

    private static Categoria categoria(String id, String descripcion) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setDescripcion(descripcion);
        return c;
    }

    // ---------- listar / buscar ----------

    @Test
    void listar_devuelveLosRecursosGuardados() throws Exception {
        servicio.crear("R1", categoria("CAT-001", "Sala"), "Sala 1");
        servicio.crear("R2", categoria("CAT-002", "Laptop"), "Laptop Dell");

        assertEquals(2, servicio.listar().size());
    }

    @Test
    void buscar_sinFiltros_devuelveTodos() throws Exception {
        servicio.crear("R1", categoria("CAT-001", "Sala"), "Sala 1");
        assertEquals(1, servicio.buscar(null, null).size());
    }

    @Test
    void buscar_porCategoria_filtraSoloEsa() throws Exception {
        servicio.crear("R1", categoria("CAT-001", "Sala"), "Sala 1");
        servicio.crear("R2", categoria("CAT-002", "Laptop"), "Laptop Dell");

        List<Recurso> resultado = servicio.buscar(categoria("CAT-001", "Sala"), null);

        assertEquals(1, resultado.size());
        assertEquals("R1", resultado.get(0).getId());
    }

    @Test
    void buscar_porTextoVacio_seIgnoraComoFiltro() throws Exception {
        servicio.crear("R1", categoria("CAT-001", "Sala"), "Sala 1");
        assertEquals(1, servicio.buscar(null, "   ").size());
    }

    // ---------- crear ----------

    @Test
    void crear_guardaElRecursoConLaCategoriaRealDelDAO() throws Exception {
        // La categoría que llega como parámetro viene "vieja" (sin descripción actualizada);
        // el servicio debe reemplazarla por la que devuelve categoriaDAO.buscarPorId.
        Categoria categoriaDesactualizada = categoria("CAT-001", "Descripción vieja");

        Recurso creado = servicio.crear("R1", categoriaDesactualizada, "Sala 1 primer piso");

        assertEquals("R1", creado.getId());
        assertEquals("Sala", creado.getCategoria().getDescripcion());
        assertEquals("Sala 1 primer piso", creado.getDescripcion());
    }

    @Test
    void crear_quitaEspaciosDelIdYDeLaDescripcion() throws Exception {
        Recurso creado = servicio.crear("  R1  ", categoria("CAT-001", "Sala"), "  Sala 1  ");
        assertEquals("R1", creado.getId());
        assertEquals("Sala 1", creado.getDescripcion());
    }

    @Test
    void crear_idVacioOMuyLargo_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("   ", categoria("CAT-001", "Sala"), "Descripción"));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("x".repeat(21), categoria("CAT-001", "Sala"), "Descripción"));
    }

    @Test
    void crear_descripcionVaciaOMuyLarga_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("R1", categoria("CAT-001", "Sala"), "   "));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("R1", categoria("CAT-001", "Sala"), "x".repeat(101)));
    }

    @Test
    void crear_sinCategoria_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crear("R1", null, "Descripción"));
    }

    @Test
    void crear_categoriaQueNoExisteEnLaBaseDeDatos_lanzaExcepcion() {
        Categoria categoriaFantasma = categoria("CAT-999", "Fantasma");
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("R1", categoriaFantasma, "Descripción"));
    }

    @Test
    void crear_idRepetido_lanzaExcepcionYNoDuplica() throws Exception {
        servicio.crear("R1", categoria("CAT-001", "Sala"), "Sala 1");
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("R1", categoria("CAT-002", "Laptop"), "Otra descripción"));
        assertEquals(1, servicio.listar().size());
    }

    @Test
    void crear_errorDeLlaveDuplicadaDeOracle_seTraduceAMensajeClaro() {
        simularErrorIdDuplicado = true;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("R1", categoria("CAT-001", "Sala"), "Sala 1"));
        assertTrue(e.getMessage().contains("Ya existe un recurso"));
    }

    // ---------- modificar ----------

    @Test
    void modificar_existente_cambiaCategoriaYDescripcion() throws Exception {
        servicio.crear("R1", categoria("CAT-001", "Sala"), "Sala 1");

        servicio.modificar("R1", categoria("CAT-002", "Laptop"), "Ahora es laptop");

        Recurso actualizado = servicio.listar().get(0);
        assertEquals("CAT-002", actualizado.getCategoria().getId());
        assertEquals("Ahora es laptop", actualizado.getDescripcion());
    }

    @Test
    void modificar_inexistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.modificar("R-999", categoria("CAT-001", "Sala"), "Descripción"));
    }

    @Test
    void modificar_categoriaQueYaNoExiste_lanzaExcepcion() throws Exception {
        servicio.crear("R1", categoria("CAT-001", "Sala"), "Sala 1");
        Categoria categoriaFantasma = categoria("CAT-999", "Fantasma");
        assertThrows(IllegalArgumentException.class,
                () -> servicio.modificar("R1", categoriaFantasma, "Descripción"));
    }

    @Test
    void modificar_errorDePadreNoExisteDeOracle_seTraduceAMensajeClaro() throws Exception {
        servicio.crear("R1", categoria("CAT-001", "Sala"), "Sala 1");
        simularErrorCategoriaDesaparecida = true;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.modificar("R1", categoria("CAT-002", "Laptop"), "Descripción"));
        assertTrue(e.getMessage().contains("categoría"));
    }

    // ---------- borrar ----------

    @Test
    void borrar_sinReservasFuturas_loElimina() throws Exception {
        servicio.crear("R1", categoria("CAT-001", "Sala"), "Sala 1");
        servicio.borrar("R1");
        assertTrue(servicio.listar().isEmpty());
    }

    @Test
    void borrar_conReservasFuturas_lanzaExcepcionYNoBorra() throws Exception {
        servicio.crear("R1", categoria("CAT-001", "Sala"), "Sala 1");
        reservasFuturasPorRecurso.put("R1", true);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> servicio.borrar("R1"));

        assertTrue(e.getMessage().contains("reservas activas pendientes"));
        assertEquals(1, servicio.listar().size());
    }

    @Test
    void borrar_inexistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.borrar("R-999"));
    }
}
