package controladores;

import modelo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import servicios.GestorXML;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EstadisticasControllerTest {

    private EstadisticasController controller;
    private String rutaReservas;
    private Categoria sala;
    private Categoria laptop;

    @BeforeEach
    void preparar(@TempDir Path carpeta) {
        controller = new EstadisticasController(carpeta.toString());
        rutaReservas = carpeta.resolve("reservas.xml").toString();

        sala = new Categoria();
        sala.setId("CAT-001");
        sala.setDescripcion("Sala");
        laptop = new Categoria();
        laptop.setId("CAT-002");
        laptop.setDescripcion("Laptop");
    }

    private static Date fecha(int anio, int mes, int dia) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(anio, mes - 1, dia);
        return cal.getTime();
    }

    private Reserva reserva(Date dia, Categoria... categorias) {
        Reserva r = new Reserva();
        r.setFecha(dia);
        r.setHoraInicio(dia);
        r.setHoraFin(dia);
        for (Categoria c : categorias) {
            DetalleReserva d = new DetalleReserva();
            d.setCategoriaSolicitada(c);
            r.getDetalles().add(d);
        }
        return r;
    }

    @Test
    void estadisticasRecursos_cuentaPorCategoriaEIgnoraCanceladasYFueraDeRango() throws IOException {
        Reserva cancelada = reserva(fecha(2026, 9, 8), sala);
        cancelada.cancelar();
        List<Reserva> reservas = new ArrayList<>(List.of(
                reserva(fecha(2026, 9, 7), sala, laptop),
                reserva(fecha(2026, 9, 9), sala),
                cancelada,
                reserva(fecha(2026, 10, 1), laptop)));
        new GestorXML().guardarDatos(reservas, rutaReservas);

        Map<String, Integer> r = controller.estadisticasRecursos(fecha(2026, 9, 1), fecha(2026, 9, 30));

        assertEquals(Integer.valueOf(2), r.get("Sala"));
        assertEquals(Integer.valueOf(1), r.get("Laptop"));
        assertEquals("Sala", r.keySet().iterator().next(), "ordenado de mayor a menor");
    }

    @Test
    void estadisticasActividades_incluyeSemanasSinActividades() throws IOException {
        // Semana 07/09 tiene 2, semana 14/09 tiene 0, semana 21/09 tiene 1
        List<Reserva> reservas = new ArrayList<>(List.of(
                reserva(fecha(2026, 9, 7)),
                reserva(fecha(2026, 9, 13)),
                reserva(fecha(2026, 9, 23))));
        new GestorXML().guardarDatos(reservas, rutaReservas);

        Map<String, Integer> r = controller.estadisticasActividades(fecha(2026, 9, 7), fecha(2026, 9, 27));

        assertEquals(3, r.size());
        List<Integer> valores = new ArrayList<>(r.values());
        assertEquals(List.of(2, 0, 1), valores);
        assertEquals("07/09/26 - 13/09/26", r.keySet().iterator().next());
    }

    @Test
    void rangoInvertido_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.estadisticasActividades(fecha(2026, 9, 30), fecha(2026, 9, 1)));
    }
}
