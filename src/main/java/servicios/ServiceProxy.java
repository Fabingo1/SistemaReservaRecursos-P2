package servicios;

import controladores.ResultadoReserva;
import modelo.*;
import protocolo.Peticion;
import protocolo.Protocolo;
import protocolo.Respuesta;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static protocolo.Protocolo.*;

// Patrón Proxy: el Frontend usa esta clase como si fuera el Service del Backend (implementa la
// misma IService), pero cada método solo arma una Peticion, la manda por el socket y espera la
// Respuesta. Los controllers no saben que hay una red de por medio.
// El Frontend NO tiene ninguna referencia a la base de datos: todo pasa por aquí.
public class ServiceProxy implements IService {

    private static final int TIEMPO_CONEXION_MS = 5000;

    private final String host;
    private final int puerto;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final String idSesion;

    private final Object candadoAlertas = new Object();
    private Socket socketAlertas;

    /** Se conecta al Backend al crearse; lanza IOException si no está corriendo. */
    public ServiceProxy(String host, int puerto) throws IOException {
        this.host = host;
        this.puerto = puerto;
        this.socket = new Socket();
        socket.connect(new InetSocketAddress(host, puerto), TIEMPO_CONEXION_MS);

        // Mismo orden que en el servidor: primero el de salida (con flush), luego el de entrada
        this.out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());

        out.writeObject(Protocolo.CANAL_PETICIONES);
        out.flush();
        try {
            this.idSesion = (String) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Respuesta inválida del servidor.", e);
        }
    }

    /** synchronized: un solo pedido a la vez por el socket, para no mezclar respuestas. */
    private synchronized Object enviar(String operacion, Object... parametros) throws Exception {
        Respuesta respuesta;
        try {
            out.writeObject(new Peticion(operacion, parametros));
            out.reset();
            out.flush();
            respuesta = (Respuesta) in.readObject();
        } catch (IOException e) {
            throw new IOException("Se perdió la conexión con el servidor. " + e.getMessage(), e);
        }
        if (!respuesta.isExito()) {
            throw new Exception(respuesta.getError());
        }
        return respuesta.getDatos();
    }

    // ---------- Alertas (segundo socket, fuera de IService) ----------

    /**
     * Abre el canal de alertas y llama a alRecibir por cada reserva que creen OTROS usuarios.
     * Ojo: alRecibir se ejecuta en un hilo aparte, no en el de Swing.
     */
    public void escucharAlertas(Consumer<Reserva> alRecibir) throws IOException {
        detenerAlertas();

        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, puerto), TIEMPO_CONEXION_MS);
        ObjectOutputStream salida = new ObjectOutputStream(s.getOutputStream());
        salida.flush();
        ObjectInputStream entrada = new ObjectInputStream(s.getInputStream());
        salida.writeObject(Protocolo.CANAL_ALERTAS);
        salida.writeObject(idSesion); // así el servidor sabe de qué sesión es este canal
        salida.flush();

        synchronized (candadoAlertas) {
            socketAlertas = s;
        }

        Thread escucha = new Thread(() -> {
            try {
                while (true) {
                    Object recibido = entrada.readObject();
                    if (recibido instanceof Reserva) {
                        alRecibir.accept((Reserva) recibido);
                    }
                }
            } catch (Exception e) {
                // Se cerró el canal (logout, ventana cerrada o servidor caído): el hilo termina
            }
        }, "escucha-alertas");
        escucha.setDaemon(true);
        escucha.start();
    }

    public void detenerAlertas() {
        synchronized (candadoAlertas) {
            if (socketAlertas != null) {
                try {
                    socketAlertas.close();
                } catch (IOException ignored) {
                }
                socketAlertas = null;
            }
        }
    }

    /** Cierra todo (al salir de la aplicación). */
    public void cerrar() {
        detenerAlertas();
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    // ---------- IService ----------

    // Login
    @Override
    public Usuario login(String id, String clave) throws Exception {
        return (Usuario) enviar(LOGIN, id, clave);
    }

    @Override
    public void cambiarClave(String id, String claveActual, String claveNueva) throws Exception {
        enviar(CAMBIAR_CLAVE, id, claveActual, claveNueva);
    }

    @Override
    public void logout() throws Exception {
        detenerAlertas();
        enviar(LOGOUT);
    }

    // Funcionarios
    @Override
    @SuppressWarnings("unchecked")
    public List<Funcionario> listarFuncionarios() throws Exception {
        return (List<Funcionario>) enviar(LISTAR_FUNCIONARIOS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Funcionario> buscarFuncionarios(String texto) throws Exception {
        return (List<Funcionario>) enviar(BUSCAR_FUNCIONARIOS, texto);
    }

    @Override
    public Funcionario crearFuncionario(String id, String nombre, String telefono) throws Exception {
        return (Funcionario) enviar(CREAR_FUNCIONARIO, id, nombre, telefono);
    }

    @Override
    public void modificarFuncionario(String id, String nombre, String telefono) throws Exception {
        enviar(MODIFICAR_FUNCIONARIO, id, nombre, telefono);
    }

    @Override
    public void borrarFuncionario(String id) throws Exception {
        enviar(BORRAR_FUNCIONARIO, id);
    }

    // Categorías
    @Override
    @SuppressWarnings("unchecked")
    public List<Categoria> listarCategorias() throws Exception {
        return (List<Categoria>) enviar(LISTAR_CATEGORIAS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Categoria> buscarCategorias(String texto) throws Exception {
        return (List<Categoria>) enviar(BUSCAR_CATEGORIAS, texto);
    }

    @Override
    public Categoria crearCategoria(String descripcion) throws Exception {
        return (Categoria) enviar(CREAR_CATEGORIA, descripcion);
    }

    @Override
    public void modificarCategoria(String id, String descripcion) throws Exception {
        enviar(MODIFICAR_CATEGORIA, id, descripcion);
    }

    @Override
    public void borrarCategoria(String id) throws Exception {
        enviar(BORRAR_CATEGORIA, id);
    }

    // Recursos
    @Override
    @SuppressWarnings("unchecked")
    public List<Recurso> buscarRecursos(Categoria categoria, String texto) throws Exception {
        return (List<Recurso>) enviar(BUSCAR_RECURSOS, categoria, texto);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Recurso> listarRecursos() throws Exception {
        return (List<Recurso>) enviar(LISTAR_RECURSOS);
    }

    @Override
    public Recurso crearRecurso(String id, Categoria categoria, String descripcion) throws Exception {
        return (Recurso) enviar(CREAR_RECURSO, id, categoria, descripcion);
    }

    @Override
    public void modificarRecurso(String id, Categoria categoria, String descripcion) throws Exception {
        enviar(MODIFICAR_RECURSO, id, categoria, descripcion);
    }

    @Override
    public void borrarRecurso(String id) throws Exception {
        enviar(BORRAR_RECURSO, id);
    }

    // Reservas
    @Override
    public ResultadoReserva crearReserva(Funcionario funcionario, String actividad, Date fecha,
                                         Date horaInicio, Date horaFin, List<Categoria> categorias) throws Exception {
        return (ResultadoReserva) enviar(CREAR_RESERVA, funcionario, actividad, fecha, horaInicio, horaFin, categorias);
    }

    @Override
    public void cancelarReserva(String idReserva, Funcionario funcionario) throws Exception {
        enviar(CANCELAR_RESERVA, idReserva, funcionario);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Reserva> obtenerReservasDe(Funcionario funcionario) throws Exception {
        return (List<Reserva>) enviar(OBTENER_RESERVAS_DE, funcionario);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> extraerDatosReserva(String frase) throws Exception {
        return (Map<String, String>) enviar(EXTRAER_DATOS_RESERVA, frase);
    }

    // Calendarización y Actividades
    @Override
    @SuppressWarnings("unchecked")
    public List<Reserva> obtenerReservasEntre(Date desde, Date hasta) throws Exception {
        return (List<Reserva>) enviar(OBTENER_RESERVAS_ENTRE, desde, hasta);
    }

    // Estadísticas
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Integer> estadisticasRecursos(Date desde, Date hasta) throws Exception {
        return (Map<String, Integer>) enviar(ESTADISTICAS_RECURSOS, desde, hasta);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Integer> estadisticasActividades(Date desde, Date hasta) throws Exception {
        return (Map<String, Integer>) enviar(ESTADISTICAS_ACTIVIDADES, desde, hasta);
    }
}
