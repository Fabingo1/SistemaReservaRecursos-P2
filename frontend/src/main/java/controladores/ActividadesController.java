package controladores;

import modelo.Reserva;
import servicios.GestorXML;
import servicios.PDFService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ActividadesController {

    private final String rutaReservas;
    private final GestorXML gestorXML;
    private final PDFService pdfService;

    public ActividadesController() {
        this("data");
    }

    public ActividadesController(String carpetaDatos) {
        this.rutaReservas = new File(carpetaDatos, "reservas.xml").getPath();
        this.gestorXML = new GestorXML();
        this.pdfService = new PDFService();
    }

    public List<Reserva> cargarReservasActivas() throws IOException {
        List<Reserva> todas = gestorXML.cargarDatos(rutaReservas);
        List<Reserva> activas = new ArrayList<>();
        for (Reserva r : todas) {
            if (r.estaActiva()) {
                activas.add(r);
            }
        }
        return activas;
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

    public List<Reserva> obtenerReservasSemana(Date fechaReferencia) throws IOException {
        Date lunes = obtenerLunesDeSemana(fechaReferencia);
        Calendar calFin = Calendar.getInstance();
        calFin.setTime(lunes);
        calFin.add(Calendar.DAY_OF_MONTH, 7);
        Date finSemanaExclusivo = calFin.getTime();

        List<Reserva> resultado = new ArrayList<>();
        for (Reserva r : cargarReservasActivas()) {
            if (r.getFecha() == null) continue;
            Date dia = truncarADia(r.getFecha());
            if (!dia.before(lunes) && dia.before(finSemanaExclusivo)) {
                resultado.add(r);
            }
        }
        return resultado;
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

    private Date truncarADia(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        limpiarHora(cal);
        return cal.getTime();
    }

    private void limpiarHora(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }
}
