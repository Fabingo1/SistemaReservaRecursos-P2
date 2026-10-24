package datos;

import modelo.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Acceso a las tablas reserva y detalle_reserva. Las validaciones de negocio quedan en el Service.
public class ReservaDAO extends BaseDAO {

    // Reserva + detalles en una sola consulta; se agrupan en Java por id
    private static final String SELECT_BASE =
            "SELECT r.id, r.actividad, r.inicio, r.fin, r.estado, "
                    + "r.funcionario_id, r.funcionario_nombre, r.funcionario_telefono, "
                    + "d.categoria_id, d.categoria_descripcion, d.recurso_id, d.recurso_descripcion "
                    + "FROM reserva r LEFT JOIN detalle_reserva d ON d.reserva_id = r.id ";

    private static final String POR_ID = SELECT_BASE + "WHERE r.id = ?";
    private static final String DE_FUNCIONARIO = SELECT_BASE
            + "WHERE r.funcionario_id = ? ORDER BY r.inicio DESC, r.id";
    private static final String ACTIVAS_ENTRE = SELECT_BASE
            + "WHERE r.estado = 'ACTIVA' AND r.inicio >= ? AND r.inicio < ? ORDER BY r.inicio, r.id";

    private static final String INSERTAR_RESERVA =
            "INSERT INTO reserva (id, actividad, inicio, fin, estado, "
                    + "funcionario_id, funcionario_nombre, funcionario_telefono) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERTAR_DETALLE =
            "INSERT INTO detalle_reserva (reserva_id, categoria_id, categoria_descripcion, "
                    + "recurso_id, recurso_descripcion) VALUES (?, ?, ?, ?, ?)";
    private static final String CANCELAR =
            "UPDATE reserva SET estado = 'CANCELADA' WHERE id = ?";

    // Primer recurso de la categoría sin reservas activas que se traslapen con [inicio, fin)
    private static final String RECURSO_LIBRE =
            "SELECT rec.id, rec.descripcion FROM recurso rec "
                    + "WHERE rec.categoria_id = ? AND NOT EXISTS ("
                    + "  SELECT 1 FROM detalle_reserva d JOIN reserva r ON r.id = d.reserva_id "
                    + "  WHERE d.recurso_id = rec.id AND r.estado = 'ACTIVA' "
                    + "  AND r.inicio < ? AND ? < r.fin) "
                    + "ORDER BY rec.id";

    private static final String MAX_NUMERO_ID =
            "SELECT MAX(TO_NUMBER(SUBSTR(id, 5))) FROM reserva WHERE REGEXP_LIKE(id, '^RES-[0-9]+$')";

    private static final String FUTURAS_DE_FUNCIONARIO =
            "SELECT COUNT(*) FROM reserva WHERE funcionario_id = ? AND estado = 'ACTIVA' AND fin > SYSDATE";

    //Devuelve null si no existe
    public Reserva buscarPorId(String id) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(POR_ID)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reserva> lista = leerLista(rs);
                return lista.isEmpty() ? null : lista.get(0);
            }
        }
    }

    // Todas las reservas del funcionario (activas y canceladas), las más recientes primero
    public List<Reserva> listarDeFuncionario(String funcionarioId) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(DE_FUNCIONARIO)) {
            ps.setString(1, funcionarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return leerLista(rs);
            }
        }
    }

    //Reservas activas que empiezan entre desde y hasta (días completos, ambos incluidos).
    public List<Reserva> listarActivasEntre(Date desde, Date hasta) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(ACTIVAS_ENTRE)) {
            ps.setTimestamp(1, aTimestamp(inicioDelDia(desde, 0)));
            ps.setTimestamp(2, aTimestamp(inicioDelDia(hasta, 1)));
            try (ResultSet rs = ps.executeQuery()) {
                return leerLista(rs);
            }
        }
    }

    public void insertar(Reserva r) throws SQLException {
        try (Connection c = conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(INSERTAR_RESERVA);
                 PreparedStatement psDet = c.prepareStatement(INSERTAR_DETALLE)) {
                Funcionario f = r.getFuncionario();
                ps.setString(1, r.getId());
                ps.setString(2, r.getActividad());
                ps.setTimestamp(3, aTimestamp(r.calcularMomentoInicio()));
                ps.setTimestamp(4, aTimestamp(r.calcularMomentoFin()));
                ps.setString(5, r.getEstado().name());
                ps.setString(6, f.getId());
                ps.setString(7, f.getNombre());
                ps.setString(8, f.getTelefono());
                ps.executeUpdate();

                for (DetalleReserva d : r.getDetalles()) {
                    psDet.setString(1, r.getId());
                    psDet.setString(2, d.getCategoriaSolicitada().getId());
                    psDet.setString(3, d.getCategoriaSolicitada().getDescripcion());
                    psDet.setString(4, d.getRecursoAsignado().getId());
                    psDet.setString(5, d.getRecursoAsignado().getDescripcion());
                    psDet.addBatch();
                }
                psDet.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public boolean cancelar(String id) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(CANCELAR)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    //Primer recurso libre de la categoría en ese horario, o null si no hay
    public Recurso buscarRecursoDisponible(Categoria categoria, Date inicio, Date fin) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(RECURSO_LIBRE)) {
            ps.setString(1, categoria.getId());
            ps.setTimestamp(2, aTimestamp(fin));
            ps.setTimestamp(3, aTimestamp(inicio));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Recurso recurso = new Recurso();
                recurso.setId(rs.getString("id"));
                recurso.setDescripcion(rs.getString("descripcion"));
                recurso.setCategoria(categoria);
                return recurso;
            }
        }
    }

    //numero más alto usado en ids RES-000001; 0 si no hay
    public int maxNumeroId() throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(MAX_NUMERO_ID);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean tieneReservasFuturas(String funcionarioId) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(FUTURAS_DE_FUNCIONARIO)) {
            ps.setString(1, funcionarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // lee una fila por detalle; se agrupan por id de reserva
    private List<Reserva> leerLista(ResultSet rs) throws SQLException {
        Map<String, Reserva> porId = new LinkedHashMap<>();
        while (rs.next()) {
            String id = rs.getString("id");
            Reserva r = porId.get(id);
            if (r == null) {
                r = leerReserva(rs);
                porId.put(id, r);
            }
            if (rs.getString("recurso_id") != null) {
                r.getDetalles().add(leerDetalle(rs));
            }
        }
        return new ArrayList<>(porId.values());
    }

    private Reserva leerReserva(ResultSet rs) throws SQLException {
        Date inicio = new Date(rs.getTimestamp("inicio").getTime());
        Date fin = new Date(rs.getTimestamp("fin").getTime());

        Funcionario f = new Funcionario();
        f.setId(rs.getString("funcionario_id"));
        f.setNombre(rs.getString("funcionario_nombre"));
        f.setTelefono(rs.getString("funcionario_telefono"));

        Reserva r = new Reserva();
        r.setId(rs.getString("id"));
        r.setActividad(rs.getString("actividad"));
        r.setFecha(inicio);      // el modelo toma el día de fecha
        r.setHoraInicio(inicio); // y la hora de horaInicio / horaFin
        r.setHoraFin(fin);
        r.setEstado(EstadoReserva.valueOf(rs.getString("estado")));
        r.setFuncionario(f);
        return r;
    }

    private DetalleReserva leerDetalle(ResultSet rs) throws SQLException {
        Categoria cat = new Categoria();
        cat.setId(rs.getString("categoria_id"));
        cat.setDescripcion(rs.getString("categoria_descripcion"));

        Recurso rec = new Recurso();
        rec.setId(rs.getString("recurso_id"));
        rec.setDescripcion(rs.getString("recurso_descripcion"));
        rec.setCategoria(cat);

        DetalleReserva d = new DetalleReserva();
        d.setCategoriaSolicitada(cat);
        d.setRecursoAsignado(rec);
        return d;
    }

    private static Timestamp aTimestamp(Date fecha) {
        return new Timestamp(fecha.getTime());
    }

    private static Date inicioDelDia(Date fecha, int diasExtra) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fecha);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, diasExtra);
        return cal.getTime();
    }
}