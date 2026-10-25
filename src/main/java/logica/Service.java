package logica;

import controladores.ResultadoReserva;
import modelo.*;
import servicios.IService;

import java.util.Date;
import java.util.List;
import java.util.Map;

// IService del Backend: reparte cada operación al servicio de su área.
public class Service implements IService {

    private final ServicioFuncionarios funcionarios = new ServicioFuncionarios();
    private final ServicioReservas reservas = new ServicioReservas();

    // Login
    @Override
    public Usuario login(String id, String clave) throws Exception {
        throw new UnsupportedOperationException("Pendiente: login");
    }

    @Override
    public void cambiarClave(String id, String claveActual, String claveNueva) throws Exception {
        throw new UnsupportedOperationException("Pendiente: cambiarClave");
    }

    @Override
    public void logout() throws Exception {
        throw new UnsupportedOperationException("Pendiente: logout");
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
        throw new UnsupportedOperationException("Pendiente: listarCategorias");
    }

    @Override
    public List<Categoria> buscarCategorias(String texto) throws Exception {
        throw new UnsupportedOperationException("Pendiente: buscarCategorias");
    }

    @Override
    public Categoria crearCategoria(String descripcion) throws Exception {
        throw new UnsupportedOperationException("Pendiente: crearCategoria");
    }

    @Override
    public void modificarCategoria(String id, String descripcion) throws Exception {
        throw new UnsupportedOperationException("Pendiente: modificarCategoria");
    }

    @Override
    public void borrarCategoria(String id) throws Exception {
        throw new UnsupportedOperationException("Pendiente: borrarCategoria");
    }

    // Recursos
    @Override
    public List<Recurso> buscarRecursos(Categoria categoria, String texto) throws Exception {
        throw new UnsupportedOperationException("Pendiente: buscarRecursos");
    }

    @Override
    public List<Recurso> listarRecursos() throws Exception {
        throw new UnsupportedOperationException("Pendiente: listarRecursos");
    }

    @Override
    public Recurso crearRecurso(String id, Categoria categoria, String descripcion) throws Exception {
        throw new UnsupportedOperationException("Pendiente: crearRecurso");
    }

    @Override
    public void modificarRecurso(String id, Categoria categoria, String descripcion) throws Exception {
        throw new UnsupportedOperationException("Pendiente: modificarRecurso");
    }

    @Override
    public void borrarRecurso(String id) throws Exception {
        throw new UnsupportedOperationException("Pendiente: borrarRecurso");
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
        throw new UnsupportedOperationException("Pendiente: obtenerReservasEntre");
    }

    @Override
    public Map<String, Integer> estadisticasRecursos(Date desde, Date hasta) throws Exception {
        throw new UnsupportedOperationException("Pendiente: estadisticasRecursos");
    }

    @Override
    public Map<String, Integer> estadisticasActividades(Date desde, Date hasta) throws Exception {
        throw new UnsupportedOperationException("Pendiente: estadisticasActividades");
    }
}