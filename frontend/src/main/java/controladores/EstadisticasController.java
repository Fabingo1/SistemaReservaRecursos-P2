package controladores;

import servicios.IService;
import servicios.PDFService;
import servicios.ServicioRemoto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

// Frontend: los totales (por categoría de recurso y por semana) los calcula el Backend en
// ServicioEstadisticas; aquí solo se piden por el IService. El PDF se genera aquí, en la máquina
// del usuario, con los datos que ya tiene en pantalla.
public class EstadisticasController {

    private final IService service; // null = usar ServicioRemoto
    private final PDFService pdfService = new PDFService();

    public EstadisticasController() {
        this(null);
    }

    /** Para pruebas con un IService falso. */
    public EstadisticasController(IService service) {
        this.service = service;
    }

    /** Reservas por categoría de recurso, de mayor a menor. */
    public Map<String, Integer> estadisticasRecursos(Date desde, Date hasta) throws Exception {
        return service().estadisticasRecursos(desde, hasta);
    }

    /** Reservas por semana; las semanas sin actividades vienen con 0. */
    public Map<String, Integer> estadisticasActividades(Date desde, Date hasta) throws Exception {
        return service().estadisticasActividades(desde, hasta);
    }

    public void generarReportePDF(String titulo, String nombreColumna, Map<String, Integer> datos,
                                  String rutaSalida) throws IOException {
        List<Object[]> filas = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, Integer> e : datos.entrySet()) {
            filas.add(new Object[]{e.getKey(), e.getValue()});
            total += e.getValue();
        }
        filas.add(new Object[]{"TOTAL", total});
        pdfService.generarReporte(titulo, new String[]{nombreColumna, "Cantidad"}, filas, rutaSalida);
    }

    private IService service() {
        return service != null ? service : ServicioRemoto.get();
    }
}
