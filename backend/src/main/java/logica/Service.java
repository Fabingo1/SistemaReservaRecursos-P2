package logica;

import modelo.ResultadoReserva;
import modelo.*;
import servicios.IService;

import java.util.Date;
import java.util.List;
import java.util.Map;

// IService del Backend: reparte cada operación al servicio de su área.
public class Service implements IService {

    private final ServicioFuncionarios funcionarios = new ServicioFuncionarios();
    private final ServicioReservas reservas = new ServicioReservas();
    private final ServicioLogin login = new ServicioLogin();
    private final ServicioCategorias categorias = new ServicioCategorias();
    private final ServicioRecursos recursos = new ServicioRecursos();
    private final ServicioConsultaReservas consultaReservas = new ServicioConsultaReservas();
    private final ServicioEstadisticas estadisticas = new ServicioEstadisticas();

    // Login
    @Override
    public Usuario login(String id, String clave) throws Exception {
        return login.login(id, clave);
    }

    @Override
    public void cambiarClave(String id, String claveActual, String claveNueva) throws Exception {
        login.cambiarClave(id, claveActual, claveNueva);
    }

    @Override
    public void logout() throws Exception {
        // Nada que hacer aquí: la sesión la recuerda el hilo del servidor (Worker) y él la limpia
    }

    // Funcionarios
    @Override
    public List<Funcionario> listarFuncionarios() throws Exception {
        return funcionarios.listar();
    }

    @Override
    public List<Funcionario> buscarFuncionarios(String texto) throws Exception {
        return funcionarios.buscar(texto);
    }

    @Override
    public Funcionario crearFuncionario(String id, String nombre, String telefono) throws Exception {
        return funcionarios.crear(id, nombre, telefono);
    }

    @Override
    public void modificarFuncionario(String id, String nombre, String telefono) throws Exception {
        funcionarios.modificar(id, nombre, telefono);
    }

    @Override
    public void borrarFuncionario(String id) throws Exception {
        funcionarios.borrar(id);
    }

    // Categorías
    @Override
    public List<Categoria> listarCategorias() throws Exception {
        return categorias.listar();
    }

    @Override
    public List<Categoria> buscarCategorias(String texto) throws Exception {
        return categorias.buscar(texto);
    }

    @Override
    public Categoria crearCategoria(String descripcion) throws Exception {
        return categorias.crear(descripcion);
    }

    @Override
    public void modificarCategoria(String id, String descripcion) throws Exception {
        categorias.modificar(id, descripcion);
    }

    @Override
    public void borrarCategoria(String id) throws Exception {
        categorias.borrar(id);
    }

    // Recursos
    @Override
    public List<Recurso> buscarRecursos(Categoria categoria, String texto) throws Exception {
        return recursos.buscar(categoria, texto);
    }

    @Override
    public List<Recurso> listarRecursos() throws Exception {
        return recursos.listar();
    }

    @Override
    public Recurso crearRecurso(String id, Categoria categoria, String descripcion) throws Exception {
        return recursos.crear(id, categoria, descripcion);
    }

    @Override
    public void modificarRecurso(String id, Categoria categoria, String descripcion) throws Exception {
        recursos.modificar(id, categoria, descripcion);
    }

    @Override
    public void borrarRecurso(String id) throws Exception {
        recursos.borrar(id);
    }

    // Reservas
    @Override
    public ResultadoReserva crearReserva(Funcionario funcionario, String actividad, Date fecha,
                                         Date horaInicio, Date horaFin, List<Categoria> categorias) throws Exception {
        return reservas.crearReserva(funcionario, actividad, fecha, horaInicio, horaFin, categorias);
    }

    @Override
    public void cancelarReserva(String idReserva, Funcionario funcionario) throws Exception {
        reservas.cancelarReserva(idReserva, funcionario);
    }

    @Override
    public List<Reserva> obtenerReservasDe(Funcionario funcionario) throws Exception {
        return reservas.obtenerReservasDe(funcionario);
    }

    @Override
    public Map<String, String> extraerDatosReserva(String frase) throws Exception {
        return reservas.extraerDatos(frase, listarCategorias());
    }

    // Calendarización, Actividades y Estadísticas
    @Override
    public List<Reserva> obtenerReservasEntre(Date desde, Date hasta) throws Exception {
        return consultaReservas.obtenerReservasEntre(desde, hasta);
    }

    @Override
    public Map<String, Integer> estadisticasRecursos(Date desde, Date hasta) throws Exception {
        return estadisticas.estadisticasRecursos(desde, hasta);
    }

    @Override
    public Map<String, Integer> estadisticasActividades(Date desde, Date hasta) throws Exception {
        return estadisticas.estadisticasActividades(desde, hasta);
    }
}