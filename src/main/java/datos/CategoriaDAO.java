package datos;

import modelo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Acceso a la tabla categoria. Las validaciones de negocio quedan en ServicioCategorias.
public class CategoriaDAO extends BaseDAO {

    private static final String LISTAR =
            "SELECT id, descripcion FROM categoria ORDER BY id";
    private static final String BUSCAR =
            "SELECT id, descripcion FROM categoria WHERE LOWER(descripcion) LIKE ? ORDER BY id";
    private static final String BUSCAR_ID =
            "SELECT id, descripcion FROM categoria WHERE id = ?";
    private static final String INSERTAR =
            "INSERT INTO categoria (id, descripcion) VALUES (?, ?)";
    private static final String MODIFICAR =
            "UPDATE categoria SET descripcion = ? WHERE id = ?";
    // detalle_reserva guarda una copia de la descripción: se actualiza igual que en el #1
    private static final String MODIFICAR_EN_RESERVAS =
            "UPDATE detalle_reserva SET categoria_descripcion = ? WHERE categoria_id = ?";
    private static final String BORRAR =
            "DELETE FROM categoria WHERE id = ?";
    private static final String CONTAR_RECURSOS =
            "SELECT COUNT(*) FROM recurso WHERE categoria_id = ?";

    public List<Categoria> listar() throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(LISTAR);
             ResultSet rs = ps.executeQuery()) {
            return leerLista(rs);
        }
    }

    /** Busca por descripción, parcial y sin importar mayúsculas. */
    public List<Categoria> buscar(String texto) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(BUSCAR)) {
            ps.setString(1, "%" + texto.trim().toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return leerLista(rs);
            }
        }
    }

    /** Null si no existe. */
    public Categoria buscarPorId(String id) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(BUSCAR_ID)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public void insertar(Categoria categoria) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(INSERTAR)) {
            ps.setString(1, categoria.getId());
            ps.setString(2, categoria.getDescripcion());
            ps.executeUpdate();
        }
    }

    /** Actualiza la categoría y la copia guardada en las reservas, todo o nada. False si no existe. */
    public boolean modificar(String id, String descripcion) throws SQLException {
        try (Connection c = conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(MODIFICAR);
                 PreparedStatement psRes = c.prepareStatement(MODIFICAR_EN_RESERVAS)) {
                ps.setString(1, descripcion);
                ps.setString(2, id);
                if (ps.executeUpdate() == 0) {
                    c.rollback();
                    return false;
                }
                psRes.setString(1, descripcion);
                psRes.setString(2, id);
                psRes.executeUpdate();
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** False si no existía. */
    public boolean borrar(String id) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(BORRAR)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int contarRecursos(String idCategoria) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(CONTAR_RECURSOS)) {
            ps.setString(1, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private List<Categoria> leerLista(ResultSet rs) throws SQLException {
        List<Categoria> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(leer(rs));
        }
        return lista;
    }

    private Categoria leer(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getString("id"));
        c.setDescripcion(rs.getString("descripcion"));
        return c;
    }
}
