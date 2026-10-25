package controladores;

import modelo.Funcionario;
import servicios.IService;
import servicios.PDFService;
import servicios.ServicioRemoto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Frontend: las validaciones están en el Backend (ServicioFuncionarios); aquí solo se llama al IService.
public class FuncionarioController {

    private final IService service; // null = usar ServicioRemoto
    private final PDFService pdfService = new PDFService();

    public FuncionarioController() {
        this(null);
    }

    /** Para pruebas con un IService falso. */
    public FuncionarioController(IService service) {
        this.service = service;
    }

    public List<Funcionario> listar() throws Exception {
        return service().listarFuncionarios();
    }

    public List<Funcionario> buscar(String texto) throws Exception {
        return service().buscarFuncionarios(texto);
    }

    public Funcionario crear(String id, String nombre, String telefono) throws Exception {
        return service().crearFuncionario(id, nombre, telefono);
    }

    public void modificar(String id, String nombre, String telefono) throws Exception {
        service().modificarFuncionario(id, nombre, telefono);
    }

    public void borrar(String id) throws Exception {
        service().borrarFuncionario(id);
    }

    public void generarReportePDF(List<Funcionario> funcionarios, String rutaSalida) throws IOException {
        List<Object[]> filas = new ArrayList<>();
        for (Funcionario f : funcionarios) {
            filas.add(new Object[]{f.getId(), f.getNombre(), f.getTelefono()});
        }
        pdfService.generarReporte("Listado de Funcionarios", new String[]{"Id", "Nombre", "Teléfono"}, filas, rutaSalida);
    }

    private IService service() {
        return service != null ? service : ServicioRemoto.get();
    }
}