package controladores;

import modelo.Reserva;
import servicios.IService;
import servicios.PDFService;
import servicios.ServicioRemoto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// Frontend: las reservas de la semana las pide el Backend por el IService (solo ACTIVAS, ya filtradas
// por rango de fechas). Calcular la semana (lunes a domingo) y qué actividad ocupa cada celda se hace
// aquí, en el cliente. El PDF también se genera aquí, en la máquina del usuario.
public class ActividadesController {

    private final IService service; // null = usar ServicioRemoto
    private final PDFService pdfService = new PDFService();

    public ActividadesController() {
        this(null);
    }


    public ActividadesController(IService service) {
        this.service = service;
    }

    public Date obtenerLunesDeSemana(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setTime(fecha);

        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        limpiarHora(cal);
        return cal.getTime();
    }

    public List<Date> obtenerDiasDeSemana(Date fechaReferencia) {
        List<Date> dias = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(obtenerLunesDeSemana(fechaReferencia));
        for (int i = 0; i < 7; i++) {
            dias.add(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dias;
    }

    /** Reservas activas de la semana (lunes a domingo, ambos incluidos) de la fecha de referencia. */
    public List<Reserva> obtenerReservasSemana(Date fechaReferencia) throws Exception {
        List<Date> dias = obtenerDiasDeSemana(fechaReferencia);
        Date lunes = dias.get(0);
        Date domingo = dias.get(6);
        return service().obtenerReservasEntre(lunes, domingo);
    }

    public boolean ocurreEn(Reserva r, Date dia, int hora) {
        return r.ocupaHora(dia, hora);
    }

    public String descripcionCelda(Reserva r) {
        String funcionario = r.getFuncionario() != null ? r.getFuncionario().getNombre() : "N/D";
        return r.getActividad() + " (" + funcionario + ")";
    }

    public void generarReportePDF(String titulo, String[] columnas, List<Object[]> filas, String rutaSalida)
            throws IOException {
        pdfService.generarReporte(titulo, columnas, filas, rutaSalida);
    }

    private void limpiarHora(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private IService service() {
        return service != null ? service : ServicioRemoto.get();
    }
}
