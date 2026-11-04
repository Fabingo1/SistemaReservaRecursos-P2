package controladores;

import modelo.Categoria;
import modelo.Recurso;
import org.junit.jupiter.api.Test;
import servicios.IServiceFalso;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Frontend: el controller solo delega en el IService (aquí uno falso, sin red ni BD).
// Las validaciones reales (id duplicado, categoría inexistente, reservas futuras) viven en
// ServicioRecursos, en el Backend, y se prueban aparte en ServicioRecursosTest.
class RecursoControllerTest {

    private static Categoria categoria(String id, String descripcion) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setDescripcion(descripcion);
        return c;
    }

    @Test
    void listar_devuelveLoQueManeElServicio() throws Exception {
        Recurso r = new Recurso();
        r.setId("R1");
        r.setDescripcion("Sala 1 primer piso");
        r.setCategoria(categoria("CAT-001", "Sala"));

        RecursoController controller = new RecursoController(new IServiceFalso() {
            @Override
            public List<Recurso> listarRecursos() {
                return List.of(r);
            }
        });

        List<Recurso> resultado = controller.listar();

        assertEquals(1, resultado.size());
        assertEquals("R1", resultado.get(0).getId());
    }

    @Test
    void buscar_mandaCategoriaYTextoAlServicio() throws Exception {
        Categoria sala = categoria("CAT-001", "Sala");

        RecursoController controller = new RecursoController(new IServiceFalso() {
            @Override
            public List<Recurso> buscarRecursos(Categoria categoriaRecibida, String textoRecibido) {
                Recurso r = new Recurso();
                r.setId("R-" + categoriaRecibida.getId());
                r.setDescripcion("eco: " + textoRecibido);
                r.setCategoria(categoriaRecibida);
                return List.of(r);
            }
        });

        List<Recurso> resultado = controller.buscar(sala, "primer");

        assertEquals("R-CAT-001", resultado.get(0).getId());
        assertEquals("eco: primer", resultado.get(0).getDescripcion());
    }

    @Test
    void crear_devuelveElRecursoQueConstruyeElServicio() throws Exception {
        Categoria sala = categoria("CAT-001", "Sala");

        RecursoController controller = new RecursoController(new IServiceFalso() {
            @Override
            public Recurso crearRecurso(String id, Categoria categoriaRecibida, String descripcion) {
                Recurso r = new Recurso();
                r.setId(id);
                r.setCategoria(categoriaRecibida);
                r.setDescripcion(descripcion);
                return r;
            }
        });

        Recurso creado = controller.crear("R1", sala, "Sala 1 primer piso");

        assertEquals("R1", creado.getId());
        assertEquals("CAT-001", creado.getCategoria().getId());
        assertEquals("Sala 1 primer piso", creado.getDescripcion());
    }

    @Test
    void crear_errorDelBackend_llegaConSuMensaje() {
        RecursoController controller = new RecursoController(new IServiceFalso() {
            @Override
            public Recurso crearRecurso(String id, Categoria categoria, String descripcion) throws Exception {
                throw new Exception("Ya existe un recurso con id/activo R1");
            }
        });

        Exception e = assertThrows(Exception.class,
                () -> controller.crear("R1", categoria("CAT-001", "Sala"), "Descripción"));
        assertTrue(e.getMessage().startsWith("Ya existe un recurso"));
    }

    @Test
    void modificar_errorPorCategoriaInexistente_llegaConSuMensaje() {
        RecursoController controller = new RecursoController(new IServiceFalso() {
            @Override
            public void modificarRecurso(String id, Categoria categoria, String descripcion) throws Exception {
                throw new Exception("La categoría seleccionada ya no existe.");
            }
        });

        Exception e = assertThrows(Exception.class,
                () -> controller.modificar("R1", categoria("CAT-999", "Fantasma"), "Nueva descripción"));
        assertEquals("La categoría seleccionada ya no existe.", e.getMessage());
    }

    @Test
    void borrar_errorPorReservasFuturas_llegaConSuMensaje() {
        RecursoController controller = new RecursoController(new IServiceFalso() {
            @Override
            public void borrarRecurso(String id) throws Exception {
                throw new Exception("No se puede borrar el recurso " + id
                        + ": tiene reservas activas pendientes. Deben cancelarse primero.");
            }
        });

        Exception e = assertThrows(Exception.class, () -> controller.borrar("R1"));
        assertTrue(e.getMessage().contains("reservas activas pendientes"));
    }

    @Test
    void obtenerCategorias_devuelveLoQueManeElServicio() throws Exception {
        RecursoController controller = new RecursoController(new IServiceFalso() {
            @Override
            public List<Categoria> listarCategorias() {
                return List.of(categoria("CAT-001", "Sala"), categoria("CAT-002", "Laptop"));
            }
        });

        assertEquals(2, controller.obtenerCategorias().size());
    }
}