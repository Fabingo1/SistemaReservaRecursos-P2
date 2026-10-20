package servicios;

import controladores.ResultadoReserva;
import modelo.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

// Contrato Frontend-Backend: Service (Backend) y ServiceProxy (Frontend) la implementan.
public interface IService {

    // Login
    /** Devuelve el usuario sin la clave, o null si no coincide. */
    Usuario login(String id, String clave) throws Exception;
    void cambiarClave(String id, String claveActual, String claveNueva) throws Exception;
    void logout() throws Exception;

    // Funcionarios
    List<Funcionario> listarFuncionarios() throws Exception;
    List<Funcionario> buscarFuncionarios(String texto) throws Exception;
    Funcionario crearFuncionario(String id, String nombre, String telefono) throws Exception;
    void modificarFuncionario(String id, String nombre, String telefono) throws Exception;
    void borrarFuncionario(String id) throws Exception;

    // Categorías
    List<Categoria> listarCategorias() throws Exception;
    List<Categoria> buscarCategorias(String texto) throws Exception;
    Categoria crearCategoria(String descripcion) throws Exception;
    void modificarCategoria(String id, String descripcion) throws Exception;
    void borrarCategoria(String id) throws Exception;

    // Recursos
    /** categoria o texto en null = sin ese filtro. */
    List<Recurso> buscarRecursos(Categoria categoria, String texto) throws Exception;
    List<Recurso> listarRecursos() throws Exception;
    Recurso crearRecurso(String id, Categoria categoria, String descripcion) throws Exception;
    void modificarRecurso(String id, Categoria categoria, String descripcion) throws Exception;
    void borrarRecurso(String id) throws Exception;

    // Reservas
    ResultadoReserva crearReserva(Funcionario funcionario, String actividad, Date fecha,
                                  Date horaInicio, Date horaFin, List<Categoria> categorias) throws Exception;
    void cancelarReserva(String idReserva, Funcionario funcionario) throws Exception;
    List<Reserva> obtenerReservasDe(Funcionario funcionario) throws Exception;
    Map<String, String> extraerDatosReserva(String frase) throws Exception;

    // Calendarización y Actividades
    List<Reserva> obtenerReservasEntre(Date desde, Date hasta) throws Exception;

    // Estadísticas
    Map<String, Integer> estadisticasRecursos(Date desde, Date hasta) throws Exception;
    Map<String, Integer> estadisticasActividades(Date desde, Date hasta) throws Exception;
}