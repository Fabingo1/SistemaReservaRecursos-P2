package servidor;

import modelo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import servicios.IService;
import servicios.IServiceFalso;
import servicios.ServiceProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Integración: servidor real con sockets e hilos + ServiceProxy real, en la misma máquina.
// Cubre las operaciones de Recursos, Calendarización/Actividades (reservas por rango) y Estadísticas:
// que los objetos viajen completos por el socket, que se respeten los roles y que los errores lleguen
// con su mensaje. El Service del Backend es uno falso, así la prueba no necesita Oracle.
class ServidorConsultasIT {

    private Servidor servidor;
    private int puerto;
    private final List<ServiceProxy> clientes = new ArrayList<>();

    // Lo que llegó al Service del Backend, para comprobar que los parámetros viajaron bien
    private volatile String idRecursoRecibido;
    private volatile Categoria categoriaRecibida;
    private volatile String textoRecibido;
    private volatile String descripcionRecibida;
    private volatile List<Date> fechasRecibidas = new ArrayList<>();

    @BeforeEach
    void arrancarServidor() throws IOException {
        IService serviceFalso = new IServiceFalso() {
            @Override
            public Usuario login(String id, String clave) {
                if (!id.equals(clave)) return null; // en esta prueba la clave es igual al id
                if (id.startsWith("admin")) {
                    Administrador a = new Administrador();
                    a.setId(id);
                    return a;
                }
                Funcionario f = new Funcionario();
                f.setId(id);
                f.setNombre("Nombre de " + id);
                return f;
            }

            @Override
            public List<Recurso> listarRecursos() {
                return List.of(recurso("SALA-1", "Sala 1 primer piso"), recurso("PRY-01", "Proyector Epson"));
            }

            @Override
            public List<Recurso> buscarRecursos(Categoria categoria, String texto) {
                categoriaRecibida = categoria;
                textoRecibido = texto;
                return List.of(recurso("SALA-1", "Sala 1 primer piso"));
            }

            @Override
            public Recurso crearRecurso(String id, Categoria categoria, String descripcion) {
                if (id.equals("R-REPETIDO")) {
                    throw new IllegalArgumentException("Ya existe un recurso con id/activo " + id);
                }
                idRecursoRecibido = id;
                categoriaRecibida = categoria;
                descripcionRecibida = descripcion;
                Recurso r = new Recurso();
                r.setId(id);
                r.setCategoria(categoria);
                r.setDescripcion(descripcion);
                return r;
            }

            @Override
            public void modificarRecurso(String id, Categoria categoria, String descripcion) {
                idRecursoRecibido = id;
                categoriaRecibida = categoria;
                descripcionRecibida = descripcion;
            }

            @Override
            public void borrarRecurso(String id) {
                if (id.equals("R-CON-RESERVAS")) {
                    throw new IllegalArgumentException("No se puede borrar el recurso " + id
                            + ": tiene reservas activas pendientes. Deben cancelarse primero.");
                }
                idRecursoRecibido = id;
            }

            @Override
            public List<Reserva> obtenerReservasEntre(Date desde, Date hasta) {
                fechasRecibidas = List.of(desde, hasta);
                Reserva r = new Reserva();
                r.setId("RES-000001");
                r.setActividad("Sesión de Junta Directiva");
                r.setFecha(desde);
                r.setHoraInicio(desde);
                r.setHoraFin(hasta);
                Funcionario f = new Funcionario();
                f.setId("func1");
                f.setNombre("Juan Pérez");
                r.setFuncionario(f);
                DetalleReserva d = new DetalleReserva();
                d.setCategoriaSolicitada(categoria("CAT-001", "Sala"));
                d.setRecursoAsignado(recurso("SALA-1", "Sala 1 primer piso"));
                r.getDetalles().add(d);
                return List.of(r);
            }

            @Override
            public Map<String, Integer> estadisticasRecursos(Date desde, Date hasta) {
                fechasRecibidas = List.of(desde, hasta);
                Map<String, Integer> m = new LinkedHashMap<>();
                m.put("Sala", 5);
                m.put("Proyector", 3);
                m.put("Laptop", 1);
                return m;
            }

            @Override
            public Map<String, Integer> estadisticasActividades(Date desde, Date hasta) {
                throw new IllegalArgumentException("La fecha 'Desde' no puede ser posterior a 'Hasta'.");
            }
        };
        servidor = new Servidor(0, serviceFalso); // 0 = cualquier puerto libre
        puerto = servidor.iniciarEnSegundoPlano();
    }

    @AfterEach
    void detenerServidor() {
        for (ServiceProxy p : clientes) {
            p.cerrar();
        }
        servidor.detener();
    }

    // ---------- Recursos ----------

    @Test
    void recursos_funcionarioYAdministrador_puedenListarYBuscar() throws Exception {
        ServiceProxy func = conectarComo("func1");
        ServiceProxy admin = conectarComo("admin1");

        assertEquals(2, func.listarRecursos().size());
        assertEquals("Sala 1 primer piso", admin.listarRecursos().get(0).getDescripcion());

        List<Recurso> encontrados = func.buscarRecursos(categoria("CAT-001", "Sala"), "sala");
        assertEquals(1, encontrados.size());
        assertEquals("CAT-001", categoriaRecibida.getId());
        assertEquals("sala", textoRecibido);
    }

    @Test
    void recursos_buscarSinFiltros_mandaNullAlBackend() throws Exception {
        conectarComo("func1").buscarRecursos(null, null);

        assertNull(categoriaRecibida);
        assertNull(textoRecibido);
    }

    @Test
    void recursos_elAdministradorCrea_yLosParametrosLleganCompletos() throws Exception {
        ServiceProxy admin = conectarComo("admin1");

        Recurso creado = admin.crearRecurso("PRY-02", categoria("CAT-003", "Proyector"), "Proyector Sony");

        assertEquals("PRY-02", creado.getId());
        assertEquals("CAT-003", creado.getCategoria().getId());
        assertEquals("PRY-02", idRecursoRecibido);
        assertEquals("CAT-003", categoriaRecibida.getId());
        assertEquals("Proyector Sony", descripcionRecibida);
    }

    @Test
    void recursos_funcionarioNoPuedeCrearModificarNiBorrar() throws Exception {
        ServiceProxy func = conectarComo("func1");
        Categoria sala = categoria("CAT-001", "Sala");

        assertTrue(assertThrows(Exception.class, () -> func.crearRecurso("R1", sala, "x"))
                .getMessage().contains("administrador"));
        assertTrue(assertThrows(Exception.class, () -> func.modificarRecurso("R1", sala, "x"))
                .getMessage().contains("administrador"));
        assertTrue(assertThrows(Exception.class, () -> func.borrarRecurso("R1"))
                .getMessage().contains("administrador"));
        assertNull(idRecursoRecibido, "ninguna de las tres debió llegar al Service");
    }

    @Test
    void recursos_erroresDelBackend_llegaAlFrontendConSuMensaje() throws Exception {
        ServiceProxy admin = conectarComo("admin1");
        Categoria sala = categoria("CAT-001", "Sala");

        Exception duplicado = assertThrows(Exception.class, () -> admin.crearRecurso("R-REPETIDO", sala, "x"));
        assertEquals("Ya existe un recurso con id/activo R-REPETIDO", duplicado.getMessage());

        Exception conReservas = assertThrows(Exception.class, () -> admin.borrarRecurso("R-CON-RESERVAS"));
        assertTrue(conReservas.getMessage().contains("reservas activas pendientes"));
    }

    @Test
    void recursos_modificarYBorrarComoAdministrador_llegaAlService() throws Exception {
        ServiceProxy admin = conectarComo("admin1");

        admin.modificarRecurso("SALA-1", categoria("CAT-002", "Laptop"), "Nueva descripción");
        assertEquals("SALA-1", idRecursoRecibido);
        assertEquals("CAT-002", categoriaRecibida.getId());
        assertEquals("Nueva descripción", descripcionRecibida);

        admin.borrarRecurso("PRY-01");
        assertEquals("PRY-01", idRecursoRecibido);
    }

    // ---------- Calendarización / Actividades ----------

    @Test
    void reservasEntre_ambosRoles_recibenLasReservasConSusDetalles() throws Exception {
        Date desde = fecha(2026, 9, 7, 9);
        Date hasta = fecha(2026, 9, 7, 11);

        for (String id : List.of("func1", "admin1")) {
            List<Reserva> reservas = conectarComo(id).obtenerReservasEntre(desde, hasta);

            assertEquals(1, reservas.size());
            Reserva r = reservas.get(0);
            assertEquals("Sesión de Junta Directiva", r.getActividad());
            assertEquals("Juan Pérez", r.getFuncionario().getNombre());
            assertEquals(1, r.getDetalles().size());
            assertEquals("SALA-1", r.getDetalles().get(0).getRecursoAsignado().getId());
            assertTrue(r.ocupaHora(desde, 9), "la regla de la matriz funciona con la reserva recibida");
        }
        assertEquals(List.of(desde, hasta), fechasRecibidas);
    }

    // ---------- Estadísticas ----------

    @Test
    void estadisticasRecursos_ambosRoles_recibenElMapaEnElMismoOrden() throws Exception {
        for (String id : List.of("func1", "admin1")) {
            Map<String, Integer> r = conectarComo(id).estadisticasRecursos(fecha(2026, 9, 1, 0), fecha(2026, 9, 30, 0));

            assertEquals(List.of("Sala", "Proyector", "Laptop"), new ArrayList<>(r.keySet()),
                    "el orden de mayor a menor debe sobrevivir al socket");
            assertEquals(Integer.valueOf(5), r.get("Sala"));
        }
    }

    @Test
    void estadisticasActividades_errorDeValidacion_llegaConSuMensaje() throws Exception {
        ServiceProxy func = conectarComo("func1");

        Exception e = assertThrows(Exception.class,
                () -> func.estadisticasActividades(fecha(2026, 9, 30, 0), fecha(2026, 9, 1, 0)));
        assertEquals("La fecha 'Desde' no puede ser posterior a 'Hasta'.", e.getMessage());
    }

    // ---------- Sesión ----------

    @Test
    void sinLogin_ningunaDeEstasOperacionesSePermite() throws Exception {
        ServiceProxy proxy = conectar();
        Date hoy = new Date();

        assertTrue(assertThrows(Exception.class, proxy::listarRecursos).getMessage().contains("iniciar sesión"));
        assertTrue(assertThrows(Exception.class, () -> proxy.obtenerReservasEntre(hoy, hoy))
                .getMessage().contains("iniciar sesión"));
        assertTrue(assertThrows(Exception.class, () -> proxy.estadisticasRecursos(hoy, hoy))
                .getMessage().contains("iniciar sesión"));
    }

    // ---------- Utilidades ----------

    private static Categoria categoria(String id, String descripcion) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setDescripcion(descripcion);
        return c;
    }

    private static Recurso recurso(String id, String descripcion) {
        Recurso r = new Recurso();
        r.setId(id);
        r.setDescripcion(descripcion);
        r.setCategoria(categoria("CAT-001", "Sala"));
        return r;
    }

    private static Date fecha(int anio, int mes, int dia, int hora) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(anio, mes - 1, dia, hora, 0);
        return cal.getTime();
    }

    private ServiceProxy conectar() throws IOException {
        ServiceProxy proxy = new ServiceProxy("localhost", puerto);
        clientes.add(proxy);
        return proxy;
    }

    private ServiceProxy conectarComo(String id) throws Exception {
        ServiceProxy proxy = conectar();
        assertNotNull(proxy.login(id, id));
        return proxy;
    }
}
