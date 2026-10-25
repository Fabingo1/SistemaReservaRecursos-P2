package logica;

import datos.AdministradorDAO;
import datos.FuncionarioDAO;
import datos.ReservaDAO;
import modelo.Funcionario;

import java.sql.SQLException;
import java.util.List;

// El id lo asigna el administrador. La clave inicial queda igual al id
public class ServicioFuncionarios {

    private final FuncionarioDAO funcionarioDAO;
    private final AdministradorDAO administradorDAO;
    private final ReservaDAO reservaDAO;

    public ServicioFuncionarios() {
        this(new FuncionarioDAO(), new AdministradorDAO(), new ReservaDAO());
    }

    /** Para pruebas con DAOs falsos. */
    public ServicioFuncionarios(FuncionarioDAO funcionarioDAO, AdministradorDAO administradorDAO,
                                ReservaDAO reservaDAO) {
        this.funcionarioDAO = funcionarioDAO;
        this.administradorDAO = administradorDAO;
        this.reservaDAO = reservaDAO;
    }

    public List<Funcionario> listar() throws SQLException {
        return funcionarioDAO.listar();
    }

    public List<Funcionario> buscar(String texto) throws SQLException {
        if (texto == null || texto.isBlank()) {
            return listar();
        }
        return funcionarioDAO.buscar(texto);
    }

    public Funcionario crear(String id, String nombre, String telefono) throws SQLException {
        validarDatos(id, nombre, telefono);
        String idLimpio = id.trim();

        if (funcionarioDAO.buscarPorId(idLimpio) != null) {
            throw new IllegalArgumentException("Ya existe un funcionario con id " + idLimpio);
        }
        if (administradorDAO.buscarPorId(idLimpio) != null) {
            throw new IllegalArgumentException(
                    "El id " + idLimpio + " ya está en uso por un administrador; el login se confundiría.");
        }

        Funcionario nuevo = new Funcionario();
        nuevo.setId(idLimpio);
        nuevo.setClave(idLimpio);
        nuevo.setNombre(nombre.trim());
        nuevo.setTelefono(telefono.trim());
        funcionarioDAO.insertar(nuevo);

        nuevo.setClave(null); // no se envía al Frontend
        return nuevo;
    }

    public void modificar(String id, String nombre, String telefono) throws SQLException {
        validarDatos(id, nombre, telefono);
        if (!funcionarioDAO.modificar(id.trim(), nombre.trim(), telefono.trim())) {
            throw new IllegalArgumentException("No existe un funcionario con id " + id);
        }
    }

    public void borrar(String id) throws SQLException {
        if (reservaDAO.tieneReservasFuturas(id)) {
            throw new IllegalArgumentException("No se puede borrar al funcionario " + id
                    + ": tiene reservas activas pendientes. Deben cancelarse primero.");
        }
        if (!funcionarioDAO.borrar(id)) {
            throw new IllegalArgumentException("No existe un funcionario con id " + id);
        }
    }

    void validarDatos(String id, String nombre, String telefono) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del funcionario no puede estar vacío.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del funcionario no puede estar vacío.");
        }
        if (telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException("El teléfono del funcionario no puede estar vacío.");
        }
        if (id.trim().contains(" ")) {
            throw new IllegalArgumentException("El id del funcionario no puede contener espacios.");
        }
        // Teléfono: solo dígitos, espacios, guiones, paréntesis o +, con al menos 8 dígitos
        String soloDigitos = telefono.replaceAll("[^0-9]", "");
        if (!telefono.trim().matches("[0-9+()\\-\\s]+") || soloDigitos.length() < 8) {
            throw new IllegalArgumentException("El teléfono no es válido (ej. 8888-8888).");
        }
    }
}