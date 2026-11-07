package servicios;

import modelo.ResultadoReserva;
import modelo.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

// IService de mentira para pruebas: cada prueba sobreescribe solo los métodos que necesita.
// Así se prueban controllers y el servidor de sockets sin Oracle.
public class IServiceFalso implements IService {

    private static UnsupportedOperationException noUsado() {
        return new UnsupportedOperationException("No usado en esta prueba");
    }

    @Override public Usuario login(String id, String clave) throws Exception { throw noUsado(); }
    @Override public void cambiarClave(String id, String claveActual, String claveNueva) throws Exception { throw noUsado(); }
    @Override public void logout() throws Exception { }

    @Override public List<Funcionario> listarFuncionarios() throws Exception { throw noUsado(); }
    @Override public List<Funcionario> buscarFuncionarios(String texto) throws Exception { throw noUsado(); }
    @Override public Funcionario crearFuncionario(String id, String nombre, String telefono) throws Exception { throw noUsado(); }
    @Override public void modificarFuncionario(String id, String nombre, String telefono) throws Exception { throw noUsado(); }
    @Override public void borrarFuncionario(String id) throws Exception { throw noUsado(); }

    @Override public List<Categoria> listarCategorias() throws Exception { throw noUsado(); }
    @Override public List<Categoria> buscarCategorias(String texto) throws Exception { throw noUsado(); }
    @Override public Categoria crearCategoria(String descripcion) throws Exception { throw noUsado(); }
    @Override public void modificarCategoria(String id, String descripcion) throws Exception { throw noUsado(); }
    @Override public void borrarCategoria(String id) throws Exception { throw noUsado(); }

    @Override public List<Recurso> buscarRecursos(Categoria categoria, String texto) throws Exception { throw noUsado(); }
    @Override public List<Recurso> listarRecursos() throws Exception { throw noUsado(); }
    @Override public Recurso crearRecurso(String id, Categoria categoria, String descripcion) throws Exception { throw noUsado(); }
    @Override public void modificarRecurso(String id, Categoria categoria, String descripcion) throws Exception { throw noUsado(); }
    @Override public void borrarRecurso(String id) throws Exception { throw noUsado(); }

    @Override
    public ResultadoReserva crearReserva(Funcionario funcionario, String actividad, Date fecha, Date horaInicio,
                                         Date horaFin, List<Categoria> categorias) throws Exception { throw noUsado(); }
    @Override public void cancelarReserva(String idReserva, Funcionario funcionario) throws Exception { throw noUsado(); }
    @Override public List<Reserva> obtenerReservasDe(Funcionario funcionario) throws Exception { throw noUsado(); }
    @Override public Map<String, String> extraerDatosReserva(String frase) throws Exception { throw noUsado(); }

    @Override public List<Reserva> obtenerReservasEntre(Date desde, Date hasta) throws Exception { throw noUsado(); }
    @Override public Map<String, Integer> estadisticasRecursos(Date desde, Date hasta) throws Exception { throw noUsado(); }
    @Override public Map<String, Integer> estadisticasActividades(Date desde, Date hasta) throws Exception { throw noUsado(); }
}
