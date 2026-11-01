package servidor;

import controladores.ResultadoReserva;
import modelo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import servicios.IService;
import servicios.IServiceFalso;
import servicios.ServiceProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// Integración: servidor real con sockets e hilos + ServiceProxy real, en la misma máquina.
// El Service del Backend es uno falso, así la prueba no necesita Oracle.
class ServidorIT {

    private Servidor servidor;
    private int puerto;
    private final List<ServiceProxy> clientes = new ArrayList<>();
    private volatile Funcionario funcionarioQueLlegoAlService;

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
                if (id.startsWith("func")) {
                    Funcionario f = new Funcionario();
                    f.setId(id);
                    f.setNombre("Nombre de " + id);
                    return f;
                }
                return null;
            }

            @Override
            public List<Categoria> listarCategorias() {
                Categoria c = new Categoria();
                c.setId("CAT-001");
                c.setDescripcion("Sala");
                return List.of(c);
            }

            @Override
            public Categoria crearCategoria(String descripcion) {
                throw new IllegalArgumentException("Ya existe una categoría con la descripción \"" + descripcion + "\".");
            }

            @Override
            public ResultadoReserva crearReserva(Funcionario funcionario, String actividad, Date fecha,
                                                 Date horaInicio, Date horaFin, List<Categoria> categorias) {
                funcionarioQueLlegoAlService = funcionario;
                Reserva r = new Reserva();
                r.setId("RES-000001");
                r.setActividad(actividad);
                r.setFecha(fecha);
                r.setHoraInicio(horaInicio);
                r.setHoraFin(horaFin);
                r.setFuncionario(funcionario);
                return ResultadoReserva.exito(r);
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

    @Test
    void login_porSocket_devuelveElUsuarioDelBackend() throws Exception {
        Usuario u = conectar().login("func1", "func1");
        assertInstanceOf(Funcionario.class, u);
        assertEquals("Nombre de func1", ((Funcionario) u).getNombre());
    }

    @Test
    void login_claveIncorrecta_devuelveNull() throws Exception {
        assertNull(conectar().login("func1", "mala"));
    }

    @Test
    void operacionSinLogin_esRechazadaPorElServidor() throws Exception {
        ServiceProxy proxy = conectar();
        Exception e = assertThrows(Exception.class, proxy::listarCategorias);
        assertTrue(e.getMessage().contains("iniciar sesión"));
    }

    @Test
    void errorDelService_llegaAlFrontendConSuMensaje() throws Exception {
        ServiceProxy admin = conectarComo("admin1");
        Exception e = assertThrows(Exception.class, () -> admin.crearCategoria("Sala"));
        assertEquals("Ya existe una categoría con la descripción \"Sala\".", e.getMessage());
    }

    @Test
    void funcionario_noPuedeHacerOperacionesDeAdministrador() throws Exception {
        ServiceProxy func = conectarComo("func1");
        assertEquals(1, func.listarCategorias().size()); // leer sí puede
        Exception e = assertThrows(Exception.class, () -> func.crearCategoria("Otra"));
        assertTrue(e.getMessage().contains("administrador"));
    }

    @Test
    void crearReserva_usaElFuncionarioDeLaSesionYNoElQueMandaElCliente() throws Exception {
        ServiceProxy func1 = conectarComo("func1");
        Funcionario impostor = new Funcionario();
        impostor.setId("func2");

        func1.crearReserva(impostor, "Reunión", new Date(), new Date(), new Date(), List.of());

        assertEquals("func1", funcionarioQueLlegoAlService.getId());
    }

    @Test
    void reservaNueva_llegaComoAlertaALosDemasPeroNoAlQueLaCreo() throws Exception {
        ServiceProxy creador = conectarComo("func1");
        ServiceProxy otroFuncionario = conectarComo("func2");
        ServiceProxy administrador = conectarComo("admin1");

        List<Reserva> recibidasCreador = new CopyOnWriteArrayList<>();
        List<Reserva> recibidasOtros = new CopyOnWriteArrayList<>();
        CountDownLatch llegaron = new CountDownLatch(2);

        creador.escucharAlertas(recibidasCreador::add);
        otroFuncionario.escucharAlertas(r -> { recibidasOtros.add(r); llegaron.countDown(); });
        administrador.escucharAlertas(r -> { recibidasOtros.add(r); llegaron.countDown(); });
        Thread.sleep(300); // da tiempo a que el servidor registre los canales de alertas

        creador.crearReserva(null, "Capacitación", new Date(), new Date(), new Date(), List.of());

        assertTrue(llegaron.await(3, TimeUnit.SECONDS), "Las alertas no llegaron a los otros clientes");
        assertEquals("Capacitación", recibidasOtros.get(0).getActividad());
        assertEquals("func1", recibidasOtros.get(0).getFuncionario().getId());
        Thread.sleep(300);
        assertTrue(recibidasCreador.isEmpty(), "El que creó la reserva no debe recibir su propia alerta");
    }

    @Test
    void despuesDelLogout_noSeRecibenMasAlertas() throws Exception {
        ServiceProxy creador = conectarComo("func1");
        ServiceProxy otro = conectarComo("func2");
        List<Reserva> recibidas = new CopyOnWriteArrayList<>();
        otro.escucharAlertas(recibidas::add);
        Thread.sleep(300);

        otro.logout();
        creador.crearReserva(null, "Reunión", new Date(), new Date(), new Date(), List.of());
        Thread.sleep(500);

        assertTrue(recibidas.isEmpty());
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
