package datos;

import modelo.Funcionario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Acceso a la tabla funcionario. Las validaciones de negocio quedan en el Service.
public class FuncionarioDAO extends BaseDAO {

    private static final String LISTAR =
            "SELECT id, nombre, telefono FROM funcionario ORDER BY id";
    private static final String BUSCAR =
            "SELECT id, nombre, telefono FROM funcionario "
                    + "WHERE LOWER(id) LIKE ? OR LOWER(nombre) LIKE ? ORDER BY id";
    private static final String BUSCAR_ID =
            "SELECT id, clave, nombre, telefono FROM funcionario WHERE id = ?";
    private static final String INSERTAR =
            "INSERT INTO funcionario (id, clave, nombre, telefono) VALUES (?, ?, ?, ?)";
    private static final String MODIFICAR =
            "UPDATE funcionario SET nombre = ?, telefono = ? WHERE id = ?";
    private static final String MODIFICAR_EN_RESERVAS =
            "UPDATE reserva SET funcionario_nombre = ?, funcionario_telefono = ? WHERE funcionario_id = ?";
    private static final String BORRAR =
            "DELETE FROM funcionario WHERE id = ?";

    /** Sin clave: es para mostrar en tablas. */
    public List<Funcionario> listar() throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(LISTAR);
             ResultSet rs = ps.executeQuery()) {
            return leerLista(rs);
        }
    }

    /** Busca por id o nombre, sin importar mayúsculas. Sin clave. */
    public List<Funcionario> buscar(String texto) throws SQLException {
        String patron = "%" + texto.trim().toLowerCase() + "%";
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(BUSCAR)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                return leerLista(rs);
            }
        }
    }

    /** Con clave (la usa el login). Devuelve null si no existe. */
    public Funcionario buscarPorId(String id) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(BUSCAR_ID)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Funcionario f = leer(rs);
                f.setClave(rs.getString("clave"));
                return f;
            }
        }
    }

    public void insertar(Funcionario f) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(INSERTAR)) {
            ps.setString(1, f.getId());
            ps.setString(2, f.getClave());
            ps.setString(3, f.getNombre());
            ps.setString(4, f.getTelefono());
            ps.executeUpdate();
        }
    }

    /** Actualiza el funcionario y la copia guardada en sus reservas, todo o nada. Devuelve false si no existe. */
    public boolean modificar(String id, String nombre, String telefono) throws SQLException {
        try (Connection c = conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(MODIFICAR);
                 PreparedStatement psRes = c.prepareStatement(MODIFICAR_EN_RESERVAS)) {
                ps.setString(1, nombre);
                ps.setString(2, telefono);
                ps.setString(3, id);
                if (ps.executeUpdate() == 0) {
                    c.rollback();
                    return false;
                }
                psRes.setString(1, nombre);
                psRes.setString(2, telefono);
                psRes.setString(3, id);
                psRes.executeUpdate();
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Devuelve false si no existía. */
    public boolean borrar(String id) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(BORRAR)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private List<Funcionario> leerLista(ResultSet rs) throws SQLException {
        List<Funcionario> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(leer(rs));
        }
        return lista;
    }

    private Funcionario leer(ResultSet rs) throws SQLException {
        Funcionario f = new Funcionario();
        f.setId(rs.getString("id"));
        f.setNombre(rs.getString("nombre"));
        f.setTelefono(rs.getString("telefono"));
        return f;
    }
}