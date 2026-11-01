package controladores;

import modelo.Categoria;
import servicios.IService;
import servicios.PDFService;
import servicios.ServicioRemoto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Frontend: las validaciones (descripción vacía o repetida, borrar con recursos) están en el
// Backend (ServicioCategorias); aquí solo se llama al IService. El PDF sí se genera aquí,
// en la máquina del usuario, con los datos que ya tiene en pantalla.
public class CategoriaController {

    private final IService service; // null = usar ServicioRemoto
    private final PDFService pdfService = new PDFService();

    public CategoriaController() {
        this(null);
    }

    /** Para pruebas con un IService falso. */
    public CategoriaController(IService service) {
        this.service = service;
    }

    public List<Categoria> listar() throws Exception {
        return service().listarCategorias();
    }

    public List<Categoria> buscarPorDescripcion(String texto) throws Exception {
        return service().buscarCategorias(texto);
    }

    public Categoria crear(String descripcion) throws Exception {
        return service().crearCategoria(descripcion);
    }

    public void modificar(String id, String nuevaDescripcion) throws Exception {
        service().modificarCategoria(id, nuevaDescripcion);
    }

    public void borrar(String id) throws Exception {
        service().borrarCategoria(id);
    }

    public void generarReportePDF(List<Categoria> categorias, String rutaSalida) throws IOException {
        List<Object[]> filas = new ArrayList<>();
        for (Categoria c : categorias) {
            filas.add(new Object[]{c.getId(), c.getDescripcion()});
        }
        pdfService.generarReporte("Listado de Categorías", new String[]{"Id", "Descripción"}, filas, rutaSalida);
    }

    /** Utilidad de rutas que todavía usa RecursoController (versión con XML). */
    static String rutaHermana(String rutaArchivo, String nombre) {
        File padre = new File(rutaArchivo).getParentFile();
        return padre == null ? nombre : new File(padre, nombre).getPath();
    }

    private IService service() {
        return service != null ? service : ServicioRemoto.get();
    }
}
