package controladores;

import modelo.*;
import servicios.IService;
import servicios.PDFService;
import servicios.ServicioRemoto;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

// Frontend: las validaciones y la disponibilidad están en el Backend (ServicioReservas).
public class ReservaController {

    private final IService service; // null = usar ServicioRemoto
    private final PDFService pdfService = new PDFService();

    public ReservaController() {
        this(null);
    }

    /** Para pruebas con un IService falso. */
    public ReservaController(IService service) {
        this.service = service;
    }

    public List<Categoria> obtenerCategoriasDisponibles() throws Exception {
        return service().listarCategorias();
    }

    public List<Reserva> obtenerReservasDe(Funcionario funcionario) throws Exception {
        return service().obtenerReservasDe(funcionario);
    }

    public ResultadoReserva crearReserva(Funcionario funcionario, String actividad, Date fecha,
                                         Date horaInicio, Date horaFin,
                                         List<Categoria> categoriasSolicitadas) throws Exception {
        return service().crearReserva(funcionario, actividad, fecha, horaInicio, horaFin, categoriasSolicitadas);
    }

    public void cancelarReserva(String idReserva, Funcionario funcionario) throws Exception {
        service().cancelarReserva(idReserva, funcionario);
    }

    /** La IA corre en el Backend. */
    public Map<String, String> extraerDatos(String frase) throws Exception {
        return service().extraerDatosReserva(frase);
    }

    public void generarReportePDF(List<Reserva> reservas, String titulo, String rutaSalida) throws IOException {
        SimpleDateFormat sdfFecha = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm");
        List<Object[]> filas = new ArrayList<>();
        for (Reserva r : reservas) {
            filas.add(new Object[]{
                    r.getId(),
                    r.getActividad(),
                    r.getFecha() == null ? "" : sdfFecha.format(r.getFecha()),
                    sdfHora.format(r.getHoraInicio()) + "-" + sdfHora.format(r.getHoraFin()),
                    textoRecursos(r),
                    r.getEstado()
            });
        }
        pdfService.generarReporte(titulo,
                new String[]{"Id", "Actividad", "Fecha", "Horario", "Recursos", "Estado"},
                filas, rutaSalida);
    }

    public String textoRecursos(Reserva r) {
        StringBuilder sb = new StringBuilder();
        for (DetalleReserva d : r.getDetalles()) {
            if (d.getRecursoAsignado() == null) continue;
            if (sb.length() > 0) sb.append(", ");
            String desc = d.getRecursoAsignado().getDescripcion();
            sb.append(desc != null ? desc : d.getRecursoAsignado().getId());
        }
        return sb.toString();
    }

    private IService service() {
        return service != null ? service : ServicioRemoto.get();
    }
}