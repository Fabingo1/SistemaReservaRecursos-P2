package logica;

import datos.ReservaDAO;
import modelo.DetalleReserva;
import modelo.Reserva;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Estadísticas en el Backend: la base de datos entrega las reservas ACTIVAS del rango y aquí se agrupan.
// El Frontend solo recibe los totales ya calculados (Map etiqueta -> cantidad) para tabla, gráfico y PDF.
public class ServicioEstadisticas {

    private final ReservaDAO reservaDAO;

    public ServicioEstadisticas() {
        this(new ReservaDAO());
    }

    /** Para pruebas con un DAO falso. */
    public ServicioEstadisticas(ReservaDAO reservaDAO) {
        this.reservaDAO = reservaDAO;
    }

    /**
     * Cantidad de reservas por categoría de recurso solicitada, de mayor a menor.
     * Si dos categorías distintas tuvieran la misma descripción, se agrega su id para distinguirlas.
     */
    public Map<String, Integer> estadisticasRecursos(Date desde, Date hasta) throws SQLException {
        validarRango(desde, hasta);

        Map<String, Integer> conteoPorId = new LinkedHashMap<>();
        Map<String, String> descripcionPorId = new LinkedHashMap<>();
        for (Reserva r : reservaDAO.listarActivasEntre(desde, hasta)) {
            for (DetalleReserva d : r.getDetalles()) {
                if (d.getCategoriaSolicitada() == null) continue;
                String id = d.getCategoriaSolicitada().getId();
                conteoPorId.merge(id, 1, Integer::sum);
                descripcionPorId.putIfAbsent(id, d.getCategoriaSolicitada().getDescripcion());
            }
        }

        Map<String, Integer> resultado = new LinkedHashMap<>();
        conteoPorId.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()) // estable: empates en orden de aparición
                .forEach(e -> {
                    String etiqueta = descripcionPorId.get(e.getKey());
                    if (resultado.containsKey(etiqueta)) {
                        etiqueta = etiqueta + " (" + e.getKey() + ")";
                    }
                    resultado.put(etiqueta, e.getValue());
                });
        return resultado;
    }

    /** Cantidad de reservas por semana (lunes a domingo). Las semanas sin actividades aparecen con 0. */
    public Map<String, Integer> estadisticasActividades(Date desde, Date hasta) throws SQLException {
        validarRango(desde, hasta);

        // 1. Todas las semanas del período, con conteo 0
        Map<Date, Integer> conteoPorLunes = new LinkedHashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(lunesDe(desde));
        Date limite = finDelDia(hasta);
        while (!cal.getTime().after(limite)) {
            conteoPorLunes.put(cal.getTime(), 0);
            cal.add(Calendar.DAY_OF_MONTH, 7);
        }

        // 2. Sumar las actividades de cada semana
        List<Reserva> reservas = reservaDAO.listarActivasEntre(desde, hasta);
        for (Reserva r : reservas) {
            if (r.getFecha() == null) continue;
            conteoPorLunes.merge(lunesDe(r.getFecha()), 1, Integer::sum);
        }

        Map<String, Integer> resultado = new LinkedHashMap<>();
        for (Map.Entry<Date, Integer> entrada : conteoPorLunes.entrySet()) {
            resultado.put(etiquetaSemana(entrada.getKey()), entrada.getValue());
        }
        return resultado;
    }

    private void validarRango(Date desde, Date hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Debe indicar las fechas 'desde' y 'hasta'.");
        }
        if (truncarADia(desde).after(truncarADia(hasta))) {
            throw new IllegalArgumentException("La fecha 'Desde' no puede ser posterior a 'Hasta'.");
        }
    }

    private Date lunesDe(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(truncarADia(fecha));
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        return cal.getTime();
    }

    private String etiquetaSemana(Date lunes) {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yy");
        Calendar cal = Calendar.getInstance();
        cal.setTime(lunes);
        cal.add(Calendar.DAY_OF_MONTH, 6);
        return formato.format(lunes) + " - " + formato.format(cal.getTime());
    }

    private Date truncarADia(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date finDelDia(Date fecha) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }
}
