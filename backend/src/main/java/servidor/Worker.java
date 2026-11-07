package servidor;

import modelo.ResultadoReserva;
import modelo.Administrador;
import modelo.Categoria;
import modelo.Funcionario;
import modelo.Reserva;
import modelo.Usuario;
import protocolo.Peticion;
import protocolo.Respuesta;
import servicios.IService;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static protocolo.Protocolo.*;

// Un hilo por Frontend conectado. Lee cada Peticion, llama al IService del Backend y contesta
// con una Respuesta. Además recuerda QUIÉN hizo login en esta conexión: las operaciones de
// reservas usan ese usuario y no el que mande el cliente, así un Frontend no puede hacerse
// pasar por otro funcionario.
public class Worker extends Thread {

    private final Servidor servidor;
    private final IService service;
    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final String idSesion = UUID.randomUUID().toString();

    private volatile Usuario usuario; // null = todavía no hizo login (o ya hizo logout)

    // Canal de alertas: lo escribe el hilo de OTRO cliente cuando crea una reserva, por eso va con candado
    private final Object candadoAlertas = new Object();
    private Socket socketAlertas;
    private ObjectOutputStream outAlertas;

    Worker(Servidor servidor, IService service, Socket socket, ObjectInputStream in, ObjectOutputStream out) {
        super("cliente-" + socket.getRemoteSocketAddress());
        this.servidor = servidor;
        this.service = service;
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    String getIdSesion() {
        return idSesion;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object recibido = in.readObject();
                if (!(recibido instanceof Peticion)) {
                    continue;
                }
                Peticion peticion = (Peticion) recibido;

                Respuesta respuesta;
                Reserva paraAvisar = null;
                try {
                    Object datos = atender(peticion);
                    respuesta = Respuesta.ok(datos);
                    if (CREAR_RESERVA.equals(peticion.getOperacion()) && datos instanceof ResultadoReserva
                            && ((ResultadoReserva) datos).isExitosa()) {
                        paraAvisar = ((ResultadoReserva) datos).getReserva();
                    }
                } catch (IllegalArgumentException e) {
                    respuesta = Respuesta.error(mensajeDe(e), Respuesta.TipoError.VALIDACION);
                } catch (IllegalStateException e) {
                    respuesta = Respuesta.error(mensajeDe(e), Respuesta.TipoError.ESTADO);
                } catch (Exception e) {
                    respuesta = Respuesta.error(mensajeDe(e), Respuesta.TipoError.TECNICO);
                }

                enviar(respuesta);
                if (paraAvisar != null) {
                    servidor.notificarNuevaReserva(this, paraAvisar);
                }
            }
        } catch (EOFException | SocketException e) {
            // El Frontend se cerró o se cayó la red: es la forma normal de terminar
        } catch (Exception e) {
            System.err.println("Error con el cliente " + getName() + ": " + e.getMessage());
        } finally {
            cerrar();
            servidor.retirar(this);
        }
    }

    /** Reparte cada operación al IService. Devuelve lo que se manda al Frontend. */
    @SuppressWarnings("unchecked")
    private Object atender(Peticion p) throws Exception {
        Object[] a = p.getParametros();
        String operacion = p.getOperacion();

        // Operaciones permitidas sin sesión
        switch (operacion) {
            case LOGIN: {
                Usuario u = service.login((String) a[0], (String) a[1]);
                if (u != null) {
                    usuario = u;
                }
                return u;
            }
            case CAMBIAR_CLAVE:
                // Se permite desde la pantalla de login: la clave actual es la que prueba la identidad
                service.cambiarClave((String) a[0], (String) a[1], (String) a[2]);
                return null;
            case LOGOUT:
                usuario = null;
                cerrarAlertas();
                service.logout();
                return null;
            default:
                break;
        }

        requerirSesion();

        switch (operacion) {
            // Funcionarios (solo administrador)
            case LISTAR_FUNCIONARIOS:
                requerirAdministrador();
                return service.listarFuncionarios();
            case BUSCAR_FUNCIONARIOS:
                requerirAdministrador();
                return service.buscarFuncionarios((String) a[0]);
            case CREAR_FUNCIONARIO:
                requerirAdministrador();
                return service.crearFuncionario((String) a[0], (String) a[1], (String) a[2]);
            case MODIFICAR_FUNCIONARIO:
                requerirAdministrador();
                service.modificarFuncionario((String) a[0], (String) a[1], (String) a[2]);
                return null;
            case BORRAR_FUNCIONARIO:
                requerirAdministrador();
                service.borrarFuncionario((String) a[0]);
                return null;

            // Categorías: leerlas lo necesita también el funcionario (formulario de reservas)
            case LISTAR_CATEGORIAS:
                return service.listarCategorias();
            case BUSCAR_CATEGORIAS:
                return service.buscarCategorias((String) a[0]);
            case CREAR_CATEGORIA:
                requerirAdministrador();
                return service.crearCategoria((String) a[0]);
            case MODIFICAR_CATEGORIA:
                requerirAdministrador();
                service.modificarCategoria((String) a[0], (String) a[1]);
                return null;
            case BORRAR_CATEGORIA:
                requerirAdministrador();
                service.borrarCategoria((String) a[0]);
                return null;

            // Recursos
            case BUSCAR_RECURSOS:
                return service.buscarRecursos((Categoria) a[0], (String) a[1]);
            case LISTAR_RECURSOS:
                return service.listarRecursos();
            case CREAR_RECURSO:
                requerirAdministrador();
                return service.crearRecurso((String) a[0], (Categoria) a[1], (String) a[2]);
            case MODIFICAR_RECURSO:
                requerirAdministrador();
                service.modificarRecurso((String) a[0], (Categoria) a[1], (String) a[2]);
                return null;
            case BORRAR_RECURSO:
                requerirAdministrador();
                service.borrarRecurso((String) a[0]);
                return null;

            // Reservas (solo funcionario). El funcionario es SIEMPRE el de la sesión, se ignora el que mande el cliente.
            case CREAR_RESERVA:
                return service.crearReserva(requerirFuncionario(), (String) a[1], (Date) a[2],
                        (Date) a[3], (Date) a[4], (List<Categoria>) a[5]);
            case CANCELAR_RESERVA:
                service.cancelarReserva((String) a[0], requerirFuncionario());
                return null;
            case OBTENER_RESERVAS_DE:
                return service.obtenerReservasDe(requerirFuncionario());
            case EXTRAER_DATOS_RESERVA:
                return service.extraerDatosReserva((String) a[0]);

            // Calendarización, Actividades y Estadísticas (ambos roles)
            case OBTENER_RESERVAS_ENTRE:
                return service.obtenerReservasEntre((Date) a[0], (Date) a[1]);
            case ESTADISTICAS_RECURSOS:
                return service.estadisticasRecursos((Date) a[0], (Date) a[1]);
            case ESTADISTICAS_ACTIVIDADES:
                return service.estadisticasActividades((Date) a[0], (Date) a[1]);

            default:
                throw new IllegalArgumentException("Operación desconocida: " + operacion);
        }
    }

    private void requerirSesion() {
        if (usuario == null) {
            throw new IllegalStateException("Debe iniciar sesión primero.");
        }
    }

    private void requerirAdministrador() {
        if (!(usuario instanceof Administrador)) {
            throw new IllegalStateException("Esta operación solo la puede hacer un administrador.");
        }
    }

    private Funcionario requerirFuncionario() {
        if (!(usuario instanceof Funcionario)) {
            throw new IllegalStateException("Esta operación solo la puede hacer un funcionario.");
        }
        return (Funcionario) usuario;
    }

    private String mensajeDe(Exception e) {
        String mensaje = e.getMessage();
        if (e instanceof SQLException) {
            return "Error de base de datos: " + mensaje;
        }
        return (mensaje == null || mensaje.isBlank()) ? e.getClass().getSimpleName() : mensaje;
    }

    private void enviar(Respuesta respuesta) throws IOException {
        out.writeObject(respuesta);
        out.reset(); // sin esto, ObjectOutputStream reenvía copias viejas de objetos ya enviados
        out.flush();
    }

    // ---------- Alertas ----------

    void conectarAlertas(Socket socket, ObjectOutputStream salida) {
        synchronized (candadoAlertas) {
            cerrarAlertas();
            this.socketAlertas = socket;
            this.outAlertas = salida;
        }
    }

    boolean recibeAlertasDe(String idCreador) {
        Usuario actual = usuario;
        synchronized (candadoAlertas) {
            return actual != null && outAlertas != null && !Objects.equals(actual.getId(), idCreador);
        }
    }

    void enviarAlerta(Reserva reserva) {
        synchronized (candadoAlertas) {
            if (outAlertas == null) {
                return;
            }
            try {
                outAlertas.writeObject(reserva);
                outAlertas.reset();
                outAlertas.flush();
            } catch (IOException e) {
                cerrarAlertas(); // ese Frontend ya no escucha
            }
        }
    }

    private void cerrarAlertas() {
        synchronized (candadoAlertas) {
            if (socketAlertas != null) {
                try {
                    socketAlertas.close();
                } catch (IOException ignored) {
                }
            }
            socketAlertas = null;
            outAlertas = null;
        }
    }

    void cerrar() {
        cerrarAlertas();
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
