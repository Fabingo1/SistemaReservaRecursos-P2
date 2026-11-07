package logica;

import datos.AdministradorDAO;
import datos.FuncionarioDAO;
import modelo.Administrador;
import modelo.Funcionario;
import modelo.Usuario;

import java.sql.SQLException;

// Login y cambio de clave en el Backend. Nunca devuelve la clave al Frontend.
public class ServicioLogin {

    private static final int LARGO_MINIMO_CLAVE = 4;
    private static final int LARGO_MAXIMO_CLAVE = 50; // VARCHAR2(50) en schema.sql

    private final AdministradorDAO administradorDAO;
    private final FuncionarioDAO funcionarioDAO;

    public ServicioLogin() {
        this(new AdministradorDAO(), new FuncionarioDAO());
    }

    /** Para pruebas con DAOs falsos. */
    public ServicioLogin(AdministradorDAO administradorDAO, FuncionarioDAO funcionarioDAO) {
        this.administradorDAO = administradorDAO;
        this.funcionarioDAO = funcionarioDAO;
    }

    /** Devuelve el usuario SIN clave, o null si el id no existe o la clave no coincide. */
    public Usuario login(String id, String clave) throws SQLException {
        if (id == null || id.isBlank() || clave == null || clave.isEmpty()) {
            return null;
        }
        String idLimpio = id.trim();

        Administrador admin = administradorDAO.buscarPorId(idLimpio);
        if (admin != null && clave.equals(admin.getClave())) {
            Administrador copia = new Administrador();
            copia.setId(admin.getId());
            return copia;
        }

        Funcionario funcionario = funcionarioDAO.buscarPorId(idLimpio);
        if (funcionario != null && clave.equals(funcionario.getClave())) {
            Funcionario copia = new Funcionario();
            copia.setId(funcionario.getId());
            copia.setNombre(funcionario.getNombre());
            copia.setTelefono(funcionario.getTelefono());
            return copia;
        }
        return null;
    }

    public void cambiarClave(String id, String claveActual, String claveNueva) throws SQLException {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el id del usuario.");
        }
        validarClaveNueva(claveActual, claveNueva);
        String idLimpio = id.trim();

        Administrador admin = administradorDAO.buscarPorId(idLimpio);
        if (admin != null) {
            validarClaveActual(admin, claveActual);
            admin.cambiarClave(claveNueva); // regla del modelo: no vacía
            administradorDAO.cambiarClave(idLimpio, admin.getClave());
            return;
        }

        Funcionario funcionario = funcionarioDAO.buscarPorId(idLimpio);
        if (funcionario != null) {
            validarClaveActual(funcionario, claveActual);
            funcionario.cambiarClave(claveNueva);
            funcionarioDAO.cambiarClave(idLimpio, funcionario.getClave());
            return;
        }

        throw new IllegalArgumentException("No se encontró un usuario con ese id.");
    }

    void validarClaveNueva(String claveActual, String claveNueva) {
        if (claveNueva == null || claveNueva.isBlank()) {
            throw new IllegalArgumentException("La clave nueva no puede estar vacía.");
        }
        if (claveNueva.length() < LARGO_MINIMO_CLAVE) {
            throw new IllegalArgumentException("La clave nueva debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres.");
        }
        if (claveNueva.length() > LARGO_MAXIMO_CLAVE) {
            throw new IllegalArgumentException("La clave nueva no puede tener más de " + LARGO_MAXIMO_CLAVE + " caracteres.");
        }
        if (claveNueva.equals(claveActual)) {
            throw new IllegalArgumentException("La clave nueva debe ser distinta de la actual.");
        }
    }

    private void validarClaveActual(Usuario usuario, String claveActual) {
        if (claveActual == null || !claveActual.equals(usuario.getClave())) {
            throw new IllegalArgumentException("La clave actual no es correcta.");
        }
    }
}
