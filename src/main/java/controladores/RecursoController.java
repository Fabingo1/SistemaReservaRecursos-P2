package controladores;

import modelo.Categoria;
import modelo.Recurso;
import servicios.IService;
import servicios.PDFService;
import servicios.ServicioRemoto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Frontend: las validaciones (id duplicado, categoría inexistente, borrar con reservas futuras)
// están en el Backend (ServicioRecursos); aquí solo se llama al IService. El PDF sí se genera
// aquí, en la máquina del usuario, con los datos que ya tiene en pantalla.
public class RecursoController {

    private final IService service; // null = usar ServicioRemoto
    private final PDFService pdfService = new PDFService();

    public RecursoController() {
        this(null);
    }

    /** Para pruebas con un IService falso. */
    public RecursoController(IService service) {
        this.service = service;
    }

    public List<Categoria> obtenerCategorias() throws Exception {
        return service().listarCategorias();
    }

    public List<Recurso> listar() throws Exception {
        return service().listarRecursos();
    }

    /** categoria o texto en null = sin ese filtro (mismo contrato que IService.buscarRecursos). */
    public List<Recurso> buscar(Categoria categoria, String texto) throws Exception {
        return service().buscarRecursos(categoria, texto);
    }

    public Recurso crear(String id, Categoria categoria, String descripcion) throws Exception {
        return service().crearRecurso(id, categoria, descripcion);
    }

    public void modificar(String id, Categoria categoria, String descripcion) throws Exception {
        service().modificarRecurso(id, categoria, descripcion);
    }

    public void borrar(String id) throws Exception {
        service().borrarRecurso(id);
    }

    public void generarReportePDF(List<Recurso> recursos, String rutaSalida) throws IOException {
        List<Object[]> filas = new ArrayList<>();
        for (Recurso r : recursos) {
            String categoriaTexto = r.getCategoria() != null ? r.getCategoria().getDescripcion() : "";
            filas.add(new Object[]{r.getId(), categoriaTexto, r.getDescripcion()});
        }
        pdfService.generarReporte("Listado de Recursos", new String[]{"Id", "Categoría", "Descripción"}, filas, rutaSalida);
    }

    private IService service() {
        return service != null ? service : ServicioRemoto.get();
    }
}