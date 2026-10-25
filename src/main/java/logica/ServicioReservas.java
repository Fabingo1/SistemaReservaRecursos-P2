package logica;

import controladores.ResultadoReserva;
import datos.FuncionarioDAO;
import datos.ReservaDAO;
import modelo.*;
import servicios.LLMService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServicioReservas {

    // Compartido por todos los clientes: evita que dos tomen el mismo recurso o id
    private static final Object CANDADO_RESERVAS = new Object();

    private final ReservaDAO reservaDAO;
    private final FuncionarioDAO funcionarioDAO;
    private final LLMService llmService;

    public ServicioReservas() {
        this(new ReservaDAO(), new FuncionarioDAO(), new LLMService());
    }

    /** Para pruebas con DAOs falsos. */
    public ServicioReservas(ReservaDAO reservaDAO, FuncionarioDAO funcionarioDAO, LLMService llmService) {
        this.reservaDAO = reservaDAO;
        this.funcionarioDAO = funcionarioDAO;
        this.llmService = llmService;
    }

    public List<Reserva> obtenerReservasDe(Funcionario funcionario) throws SQLException {
        if (funcionario == null || funcionario.getId() == null) {
            throw new IllegalArgumentException("Debe haber un funcionario autenticado.");
        }
        return reservaDAO.listarDeFuncionario(funcionario.getId());
    }

    public ResultadoReserva crearReserva(Funcionario funcionario, String actividad, Date fecha,
                                         Date horaInicio, Date horaFin,
                                         List<Categoria> categoriasSolicitadas) throws SQLException {
        validarDatos(funcionario, actividad, fecha, horaInicio, horaFin, categoriasSolicitadas);
        List<Categoria> categoriasUnicas = quitarRepetidas(categoriasSolicitadas);

        // Datos del funcionario desde la BD, no del cliente
        Funcionario delBackend = funcionarioDAO.buscarPorId(funcionario.getId());
        if (delBackend == null) {
            throw new IllegalArgumentException("El funcionario " + funcionario.getId() + " no existe.");
        }

        Date inicio = Reserva.combinar(fecha, horaInicio);
        Date fin = Reserva.combinar(fecha, horaFin);

        synchronized (CANDADO_RESERVAS) {
            List<DetalleReserva> detalles = new ArrayList<>();
            List<Categoria> categoriasNoDisponibles = new ArrayList<>();

            for (Categoria categoria : categoriasUnicas) {
                Recurso disponible = reservaDAO.buscarRecursoDisponible(categoria, inicio, fin);
                if (disponible != null) {
                    DetalleReserva detalle = new DetalleReserva();
                    detalle.setCategoriaSolicitada(categoria);
                    detalle.setRecursoAsignado(disponible);
                    detalles.add(detalle);
                } else {
                    categoriasNoDisponibles.add(categoria);
                }
            }

            if (!categoriasNoDisponibles.isEmpty()) {
                return ResultadoReserva.fallo(categoriasNoDisponibles);
            }

            Reserva nueva = new Reserva();
            nueva.setId(String.format("RES-%06d", reservaDAO.maxNumeroId() + 1));
            nueva.setActividad(actividad.trim());
            nueva.setFecha(fecha);
            nueva.setHoraInicio(horaInicio);
            nueva.setHoraFin(horaFin);
            nueva.setFuncionario(copiaSinClave(delBackend));
            nueva.setDetalles(detalles);

            reservaDAO.insertar(nueva);
            return ResultadoReserva.exito(nueva);
        }
    }

    /** funcionario null = administrador (no revisa dueño). */
    public void cancelarReserva(String idReserva, Funcionario funcionario) throws SQLException {
        Reserva reserva = reservaDAO.buscarPorId(idReserva);

        if (reserva == null) {
            throw new IllegalArgumentException("No existe una reserva con id " + idReserva);
        }
        if (funcionario != null && !reserva.perteneceA(funcionario)) {
            throw new IllegalArgumentException("Solo puede cancelar sus propias reservas.");
        }
        if (!reserva.estaActiva()) {
            throw new IllegalStateException("La reserva " + idReserva + " ya está cancelada.");
        }
        if (reserva.yaPaso()) {
            throw new IllegalStateException("No se puede cancelar una reserva que ya pasó.");
        }
        reservaDAO.cancelar(idReserva);
    }

    public Map<String, String> extraerDatos(String frase, List<Categoria> categorias)
            throws IOException, InterruptedException {
        if (frase == null || frase.isBlank()) {
            throw new IllegalArgumentException("Escriba una frase describiendo la reserva.");
        }
        List<String> nombres = new ArrayList<>();
        for (Categoria c : categorias) {
            nombres.add(c.getDescripcion());
        }
        return llmService.extraerDatos(frase.trim(), nombres);
    }

    void validarDatos(Funcionario funcionario, String actividad, Date fecha,
                      Date horaInicio, Date horaFin, List<Categoria> categorias) {
        if (funcionario == null || funcionario.getId() == null) {
            throw new IllegalArgumentException("Debe haber un funcionario autenticado para reservar.");
        }
        if (actividad == null || actividad.isBlank()) {
            throw new IllegalArgumentException("La descripción de la actividad no puede estar vacía.");
        }
        if (fecha == null || horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException("Debe indicar la fecha, la hora de inicio y la hora de fin.");
        }
        if (Reserva.minutosDelDia(horaFin) <= Reserva.minutosDelDia(horaInicio)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (Reserva.combinar(fecha, horaInicio).before(new Date())) {
            throw new IllegalArgumentException("No se puede reservar en una fecha u hora que ya pasó.");
        }
        if (categorias == null || categorias.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una categoría de recurso.");
        }
    }

    private List<Categoria> quitarRepetidas(List<Categoria> categorias) {
        Set<String> vistos = new HashSet<>();
        List<Categoria> unicas = new ArrayList<>();
        for (Categoria c : categorias) {
            if (c != null && c.getId() != null && vistos.add(c.getId())) {
                unicas.add(c);
            }
        }
        if (unicas.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una categoría de recurso válida.");
        }
        return unicas;
    }

    private Funcionario copiaSinClave(Funcionario f) {
        Funcionario copia = new Funcionario();
        copia.setId(f.getId());
        copia.setNombre(f.getNombre());
        copia.setTelefono(f.getTelefono());
        return copia;
    }
}