package controladores;

import modelo.Categoria;
import modelo.Recurso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import servicios.GestorXML;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecursoControllerTest {

    private RecursoController controller;
    private Categoria catSala;
    private Categoria catLaptop;

    @BeforeEach
    void configurar(@TempDir Path tempDir) throws IOException {
        String rutaRecursos = tempDir.resolve("recursos.xml").toString();
        String rutaCategorias = tempDir.resolve("categorias.xml").toString();

        // CategoriaController ahora habla con el Backend (sockets + Oracle), así que para esta
        // prueba del RecursoController con XML las categorías se escriben directo en el archivo.
        catSala = categoria("CAT-001", "Sala para 10 personas");
        catLaptop = categoria("CAT-002", "Laptop windows 11");
        new GestorXML().guardarDatos(new ArrayList<>(List.of(catSala, catLaptop)), rutaCategorias);

        controller = new RecursoController(rutaRecursos, rutaCategorias);
    }

    @Test
    void listar_sinArchivoPrevio_retornaListaVacia() throws IOException {
        assertTrue(controller.listar().isEmpty());
    }

    @Test
    void crear_conDatosValidos_persisteElRecurso() throws IOException {
        Recurso creado = controller.crear("R1", catSala, "Sala 1 primer piso");

        assertEquals("R1", creado.getId());
        assertEquals("Sala 1 primer piso", creado.getDescripcion());
        assertEquals(catSala.getId(), creado.getCategoria().getId());
        assertEquals(1, controller.listar().size());
    }

    @Test
    void crear_idDuplicado_lanzaExcepcion() throws IOException {
        controller.crear("R1", catSala, "Sala 1 primer piso");

        assertThrows(IllegalArgumentException.class,
                () -> controller.crear("R1", catLaptop, "Otro recurso"));
    }

    @Test
    void crear_idVacio_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> controller.crear("   ", catSala, "Descripción"));
    }

    @Test
    void crear_sinCategoria_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> controller.crear("R1", null, "Descripción"));
    }

    @Test
    void crear_descripcionVacia_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> controller.crear("R1", catSala, ""));
    }

    @Test
    void crear_conCategoriaQueYaNoExiste_lanzaExcepcion() {
        // Simula un combo de la UI desactualizado: referencia a una
        // categoría con un id que no está en categorias.xml.
        Categoria categoriaFantasma = new Categoria();
        categoriaFantasma.setId("CAT-999");
        categoriaFantasma.setDescripcion("Ya no existe");

        assertThrows(IllegalArgumentException.class,
                () -> controller.crear("R1", categoriaFantasma, "Descripción"));
    }

    @Test
    void buscarPorId_existente_retornaElRecurso() throws IOException {
        controller.crear("R1", catSala, "Sala 1 primer piso");

        Recurso encontrado = controller.buscarPorId("R1");

        assertNotNull(encontrado);
        assertEquals("Sala 1 primer piso", encontrado.getDescripcion());
    }

    @Test
    void buscarPorId_inexistente_retornaNull() throws IOException {
        assertNull(controller.buscarPorId("R999"));
    }

    @Test
    void filtrarPorCategoria_soloTraeLosDeEsaCategoria() throws IOException {
        controller.crear("R1", catSala, "Sala 1 primer piso");
        controller.crear("R2", catSala, "Sala 2 segundo piso");
        controller.crear("R3", catLaptop, "Laptop #238715");

        List<Recurso> resultado = controller.filtrarPorCategoria(catSala);

        assertEquals(2, resultado.size());
    }

    @Test
    void filtrarPorCategoria_conNull_retornaTodos() throws IOException {
        controller.crear("R1", catSala, "Sala 1 primer piso");
        controller.crear("R3", catLaptop, "Laptop #238715");

        assertEquals(2, controller.filtrarPorCategoria(null).size());
    }

    @Test
    void buscar_combinaCategoriaYTextoDeDescripcion() throws IOException {
        controller.crear("R1", catSala, "Sala 1 primer piso");
        controller.crear("R2", catSala, "Sala 2 segundo piso");
        controller.crear("R3", catLaptop, "Laptop #238715");

        List<Recurso> resultado = controller.buscar(catSala, "primer");

        assertEquals(1, resultado.size());
        assertEquals("R1", resultado.get(0).getId());
    }

    @Test
    void buscar_sinFiltros_retornaTodos() throws IOException {
        controller.crear("R1", catSala, "Sala 1 primer piso");
        controller.crear("R3", catLaptop, "Laptop #238715");

        assertEquals(2, controller.buscar(null, "").size());
    }

    @Test
    void modificar_actualizaCategoriaYDescripcionSinCambiarId() throws IOException {
        controller.crear("R1", catSala, "Sala 1 primer piso");

        controller.modificar("R1", catLaptop, "Reasignado a laptop");

        Recurso actualizado = controller.buscarPorId("R1");
        assertEquals(catLaptop.getId(), actualizado.getCategoria().getId());
        assertEquals("Reasignado a laptop", actualizado.getDescripcion());
    }

    @Test
    void modificar_idInexistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.modificar("R999", catSala, "Descripción"));
    }

    @Test
    void modificar_conCategoriaQueYaNoExiste_lanzaExcepcion() throws IOException {
        controller.crear("R1", catSala, "Sala 1 primer piso");
        Categoria categoriaFantasma = new Categoria();
        categoriaFantasma.setId("CAT-999");

        assertThrows(IllegalArgumentException.class,
                () -> controller.modificar("R1", categoriaFantasma, "Nueva descripción"));
    }

    @Test
    void borrar_existente_loElimina() throws IOException {
        controller.crear("R1", catSala, "Sala 1 primer piso");

        controller.borrar("R1");

        assertTrue(controller.listar().isEmpty());
    }

    @Test
    void borrar_inexistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> controller.borrar("R999"));
    }

    @Test
    void borrar_noAfectaOtrosRecursos() throws IOException {
        controller.crear("R1", catSala, "Sala 1 primer piso");
        controller.crear("R2", catSala, "Sala 2 segundo piso");

        controller.borrar("R1");

        List<Recurso> restantes = controller.listar();
        assertEquals(1, restantes.size());
        assertEquals("R2", restantes.get(0).getId());
    }

    private static Categoria categoria(String id, String descripcion) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setDescripcion(descripcion);
        return c;
    }
}
