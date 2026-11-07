package controladores;

import org.junit.jupiter.api.Test;
import servicios.IServiceFalso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Frontend: el controller solo delega en el IService (aquí uno falso, sin red ni BD).
// El conteo por categoría y por semana vive en ServicioEstadisticas y se prueba en ServicioEstadisticasTest.
class EstadisticasControllerTest {

    private static Date fecha(int anio, int mes, int dia) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(anio, mes - 1, dia);
        return cal.getTime();
    }

    @Test
    void estadisticasRecursos_devuelveLoQueCalculaElBackendYLeMandaElRango() throws Exception {
        List<Date> recibidas = new ArrayList<>();
        Map<String, Integer> esperado = new LinkedHashMap<>();
        esperado.put("Sala", 2);
        esperado.put("Laptop", 1);

        EstadisticasController controller = new EstadisticasController(new IServiceFalso() {
            @Override
            public Map<String, Integer> estadisticasRecursos(Date desde, Date hasta) {
                recibidas.add(desde);
                recibidas.add(hasta);
                return esperado;
            }
        });

        Map<String, Integer> r = controller.estadisticasRecursos(fecha(2026, 9, 1), fecha(2026, 9, 30));

        assertSame(esperado, r);
        assertEquals(List.of(fecha(2026, 9, 1), fecha(2026, 9, 30)), recibidas);
    }

    @Test
    void estadisticasActividades_devuelveLoQueCalculaElBackendYLeMandaElRango() throws Exception {
        List<Date> recibidas = new ArrayList<>();
        Map<String, Integer> esperado = new LinkedHashMap<>();
        esperado.put("07/09/26 - 13/09/26", 2);

        EstadisticasController controller = new EstadisticasController(new IServiceFalso() {
            @Override
            public Map<String, Integer> estadisticasActividades(Date desde, Date hasta) {
                recibidas.add(desde);
                recibidas.add(hasta);
                return esperado;
            }
        });

        Map<String, Integer> r = controller.estadisticasActividades(fecha(2026, 9, 7), fecha(2026, 9, 13));

        assertSame(esperado, r);
        assertEquals(List.of(fecha(2026, 9, 7), fecha(2026, 9, 13)), recibidas);
    }

    @Test
    void rangoInvertido_errorDelBackend_llegaConSuMensaje() {
        EstadisticasController controller = new EstadisticasController(new IServiceFalso() {
            @Override
            public Map<String, Integer> estadisticasActividades(Date desde, Date hasta) throws Exception {
                throw new Exception("La fecha 'Desde' no puede ser posterior a 'Hasta'.");
            }
        });

        Exception e = assertThrows(Exception.class,
                () -> controller.estadisticasActividades(fecha(2026, 9, 30), fecha(2026, 9, 1)));
        assertEquals("La fecha 'Desde' no puede ser posterior a 'Hasta'.", e.getMessage());
    }
}
