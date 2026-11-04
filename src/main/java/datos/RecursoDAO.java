package datos;

import modelo.Categoria;
import modelo.Recurso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Acceso a la tabla recurso. Las validaciones de negocio quedan en ServicioRecursos.
public class RecursoDAO extends BaseDAO {

    // Siempre se trae la categoría completa con un JOIN: la vista necesita su descripción, no solo el id.
    private static final String SELECT_BASE =
            "SELECT r.id, r.descripcion, r.categoria_id, c.descripcion AS categoria_descripcion "
                    + "FROM recurso r JOIN categoria c ON c.id = r.categoria_id ";

    private static final String LISTAR = SELECT_BASE + "ORDER BY r.id";
    private static final String BUSCAR_ID = SELECT_BASE + "WHERE r.id = ?";

    private static final String INSERTAR =
            "INSERT INTO recurso (id, descripcion, categoria_id) VALUES (?, ?, ?)";
    private static final String MODIFICAR =
            "UPDATE recurso SET descripcion = ?, categoria_id = ? WHERE id = ?";
    // detalle_reserva guarda una copia de la descripción (igual que categoria_descripcion); se actualiza igual
    private static final String MODIFICAR_EN_RESERVAS =
            "UPDATE detalle_reserva SET recurso_descripcion = ? WHERE recurso_id = ?";
    private static final String BORRAR =
            "DELETE FROM recurso WHERE id = ?";

    // recurso_id en detalle_reserva NO tiene FK (ver schema.sql): hay que revisar esto a mano antes de borrar
    private static final String TIENE_RESERVAS_FUTURAS =
            "SELECT COUNT(*) FROM detalle_reserva d JOIN reserva r ON r.id = d.reserva_id "
                    + "WHERE d.recurso_id = ? AND r.estado = 'ACTIVA' AND r.fin > SYSDATE";

    public List<Recurso> listar() throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(LISTAR);
             ResultSet rs = ps.executeQuery()) {
            return leerLista(rs);
        }
    }

    /**
     * Filtra por categoría y/o por texto de descripción; cualquiera de los dos puede venir null
     * para no aplicar ese filtro. El SQL se arma con placeholders (nunca concatenando el valor),
     * así que sigue siendo seguro contra inyección aunque la cantidad de condiciones varíe.
     */
    public List<Recurso> buscar(String idCategoria, String texto) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1 = 1 ");
        List<Object> parametros = new ArrayList<>();

        if (idCategoria != null) {
            sql.append("AND r.categoria_id = ? ");
            parametros.add(idCategoria);
        }
        if (texto != null && !texto.isBlank()) {
            sql.append("AND LOWER(r.descripcion) LIKE ? ");
            parametros.add("%" + texto.trim().toLowerCase() + "%");
        }
        sql.append("ORDER BY r.id");

        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, (String) parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return leerLista(rs);
            }
        }
    }

    /** Null si no existe. */
    public Recurso buscarPorId(String id) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(BUSCAR_ID)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public void insertar(Recurso recurso) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(INSERTAR)) {
            ps.setString(1, recurso.getId());
            ps.setString(2, recurso.getDescripcion());
            ps.setString(3, recurso.getCategoria().getId());
            ps.executeUpdate();
        }
    }

    /* Actualiza el recurso y la copia guardada en las reservas, todo o nada. False si no existe. */
    public boolean modificar(String id, String idCategoria, String descripcion) throws SQLException {
        try (Connection c = conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(MODIFICAR);
                 PreparedStatement psRes = c.prepareStatement(MODIFICAR_EN_RESERVAS)) {
                ps.setString(1, descripcion);
                ps.setString(2, idCategoria);
                ps.setString(3, id);
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

    /** True si el recurso está asignado a alguna reserva ACTIVA que todavía no ha terminado. */
    public boolean tieneReservasFuturas(String idRecurso) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(TIENE_RESERVAS_FUTURAS)) {
            ps.setString(1, idRecurso);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private List<Recurso> leerLista(ResultSet rs) throws SQLException {
        List<Recurso> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(leer(rs));
        }
        return lista;
    }

    private Recurso leer(ResultSet rs) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setId(rs.getString("categoria_id"));
        categoria.setDescripcion(rs.getString("categoria_descripcion"));

        Recurso recurso = new Recurso();
        recurso.setId(rs.getString("id"));
        recurso.setDescripcion(rs.getString("descripcion"));
        recurso.setCategoria(categoria);
        return recurso;
    }
}
