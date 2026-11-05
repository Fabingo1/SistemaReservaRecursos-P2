package controladores;

import modelo.Categoria;
import modelo.DetalleReserva;
import modelo.Recurso;
import modelo.Reserva;
import servicios.IService;
import servicios.PDFService;
import servicios.ServicioRemoto;

import java.io.IOException;
import java.util.Date;
import java.util.List;

// Frontend: los datos vienen del Backend por el IService. La lógica de la matriz (qué reserva ocupa
// cada celda) se calcula aquí, en el cliente, con las reservas del día que ya recibió. El PDF también
// se genera aquí, en la máquina del usuario.
public class CalendarizacionController {

    private final IService service; // null = usar ServicioRemoto
    private final PDFService pdfService = new PDFService();

    public CalendarizacionController() {
        this(null);
    }

    /** Para pruebas con un IService falso. */
    public CalendarizacionController(IService service) {
        this.service = service;
    }

    public List<Categoria> obtenerCategorias() throws Exception {
        return service().listarCategorias();
    }

    public List<Recurso> obtenerRecursosPorCategoria(Categoria categoria) throws Exception {
        return service().buscarRecursos(categoria, null);
    }

    /** El Backend solo devuelve reservas ACTIVAS; un solo día = misma fecha como desde y hasta. */
    public List<Reserva> obtenerReservasDelDia(Date fecha) throws Exception {
        return service().obtenerReservasEntre(fecha, fecha);
    }

    public Reserva buscarReservaEnCelda(List<Reserva> reservasDelDia, Recurso recurso, int hora) {
        for (Reserva r : reservasDelDia) {
            for (DetalleReserva d : r.getDetalles()) {
                if (d.getRecursoAsignado() != null
                        && d.getRecursoAsignado().getId().equals(recurso.getId())
                        && r.ocupaHora(r.getFecha(), hora)) {
                    return r;
                }
            }
        }
        return null;
    }

    public String descripcionCelda(Reserva r) {
        String nombre = r.getFuncionario() != null ? r.getFuncionario().getNombre() : "N/D";
        return r.getActividad() + " (" + nombre + ")";
    }

    public void generarReportePDF(String titulo, String[] columnas, List<Object[]> filas, String rutaSalida)
            throws IOException {
        pdfService.generarReporte(titulo, columnas, filas, rutaSalida);
    }

    private IService service() {
        return service != null ? service : ServicioRemoto.get();
    }
}
