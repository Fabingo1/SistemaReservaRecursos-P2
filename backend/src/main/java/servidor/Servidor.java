package servidor;

import modelo.Reserva;
import protocolo.Protocolo;
import servicios.IService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

// Backend: escucha en un puerto, crea un Worker (hilo) por cada Frontend conectado y
// reparte las alertas de reservas nuevas a los demás clientes.
//
// Cada Frontend abre DOS sockets:
//   1) PETICIONES: petición/respuesta. Al conectarse recibe un id de sesión.
//   2) ALERTAS: manda ese id de sesión y desde ahí solo recibe reservas nuevas de otros.
// Van separados para que el Proxy nunca lea una alerta cuando esperaba una respuesta.
public class Servidor {

    private final int puerto;
    private final IService service;
    private final List<Worker> workers = new ArrayList<>();

    private ServerSocket serverSocket;
    private volatile boolean activo;

    /** puerto 0 = el sistema elige uno libre (lo usan las pruebas). */
    public Servidor(int puerto, IService service) {
        this.puerto = puerto;
        this.service = service;
    }

    /** Abre el puerto y atiende clientes en este mismo hilo (bloquea hasta detener()). */
    public void iniciar() throws IOException {
        abrir();
        System.out.println("Servidor escuchando en el puerto " + serverSocket.getLocalPort());
        cicloAceptar();
    }

    /** Abre el puerto y atiende clientes en un hilo aparte. Devuelve el puerto real. */
    public int iniciarEnSegundoPlano() throws IOException {
        abrir();
        Thread hilo = new Thread(this::cicloAceptar, "servidor-aceptar");
        hilo.setDaemon(true);
        hilo.start();
        return serverSocket.getLocalPort();
    }

    public void detener() {
        activo = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        for (Worker w : copiaWorkers()) {
            w.cerrar();
        }
    }

    private void abrir() throws IOException {
        serverSocket = new ServerSocket(puerto);
        activo = true;
    }

    private void cicloAceptar() {
        while (activo) {
            try {
                Socket socket = serverSocket.accept();
                // El saludo inicial se hace en otro hilo para que un cliente lento no frene a los demás
                new Thread(() -> atenderConexion(socket), "servidor-saludo").start();
            } catch (SocketException e) {
                if (activo) {
                    System.err.println("Error en el servidor: " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Error aceptando un cliente: " + e.getMessage());
            }
        }
    }

    private void atenderConexion(Socket socket) {
        try {
            // Primero el ObjectOutputStream (y flush) en ambos lados; si no, los dos se quedan esperando
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            Object canal = in.readObject();
            if (Protocolo.CANAL_PETICIONES.equals(canal)) {
                Worker worker = new Worker(this, service, socket, in, out);
                synchronized (workers) {
                    workers.add(worker);
                }
                out.writeObject(worker.getIdSesion());
                out.flush();
                worker.start();
                System.out.println("Cliente conectado: " + socket.getRemoteSocketAddress()
                        + " (clientes activos: " + cantidadClientes() + ")");
            } else if (Protocolo.CANAL_ALERTAS.equals(canal)) {
                Object idSesion = in.readObject();
                Worker worker = buscarWorker(idSesion);
                if (worker == null) {
                    socket.close();
                } else {
                    worker.conectarAlertas(socket, out);
                }
            } else {
                socket.close();
            }
        } catch (Exception e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Avisa la reserva nueva a todos los clientes con sesión iniciada, menos al que la creó
     * (y menos a otras ventanas del mismo usuario): el enunciado pide las de "otros usuarios".
     */
    void notificarNuevaReserva(Worker origen, Reserva reserva) {
        String creador = reserva.getFuncionario() != null ? reserva.getFuncionario().getId() : null;
        for (Worker w : copiaWorkers()) {
            if (w != origen && w.recibeAlertasDe(creador)) {
                w.enviarAlerta(reserva);
            }
        }
    }

    void retirar(Worker worker) {
        synchronized (workers) {
            workers.remove(worker);
        }
        System.out.println("Cliente desconectado (clientes activos: " + cantidadClientes() + ")");
    }

    int cantidadClientes() {
        synchronized (workers) {
            return workers.size();
        }
    }

    private Worker buscarWorker(Object idSesion) {
        for (Worker w : copiaWorkers()) {
            if (w.getIdSesion().equals(idSesion)) {
                return w;
            }
        }
        return null;
    }

    private List<Worker> copiaWorkers() {
        synchronized (workers) {
            return new ArrayList<>(workers);
        }
    }
}
