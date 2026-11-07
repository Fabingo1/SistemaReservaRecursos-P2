package controladores;

import modelo.Categoria;
import org.junit.jupiter.api.Test;
import servicios.IServiceFalso;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Frontend: el controller solo delega en el IService (aquí uno falso, sin red ni BD)
class CategoriaControllerTest {

    @Test
    void crear_mandaLaDescripcionAlServicioYDevuelveLaCategoria() throws Exception {
        CategoriaController controller = new CategoriaController(new IServiceFalso() {
            @Override
            public Categoria crearCategoria(String descripcion) {
                Categoria c = new Categoria();
                c.setId("CAT-001");
                c.setDescripcion(descripcion);
                return c;
            }
        });

        Categoria c = controller.crear("Sala");

        assertEquals("CAT-001", c.getId());
        assertEquals("Sala", c.getDescripcion());
    }

    @Test
    void buscarPorDescripcion_usaLaBusquedaDelServicio() throws Exception {
        CategoriaController controller = new CategoriaController(new IServiceFalso() {
            @Override
            public List<Categoria> buscarCategorias(String texto) {
                Categoria c = new Categoria();
                c.setId("CAT-001");
                c.setDescripcion("Sala " + texto);
                return List.of(c);
            }
        });

        assertEquals("Sala x", controller.buscarPorDescripcion("x").get(0).getDescripcion());
    }

    @Test
    void borrar_errorDelServidor_llegaConSuMensaje() {
        CategoriaController controller = new CategoriaController(new IServiceFalso() {
            @Override
            public void borrarCategoria(String id) throws Exception {
                throw new Exception("No se puede borrar la categoría: tiene 1 recurso(s) asociado(s).");
            }
        });

        Exception e = assertThrows(Exception.class, () -> controller.borrar("CAT-001"));
        assertTrue(e.getMessage().startsWith("No se puede borrar"));
    }
}
