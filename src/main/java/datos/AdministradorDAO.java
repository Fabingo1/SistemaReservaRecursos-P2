package datos;

import modelo.Administrador;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdministradorDAO extends BaseDAO {

    private static final String BUSCAR_ID =
            "SELECT id, clave FROM administrador WHERE id = ?";

    /** Con clave. Null si no existe. */
    public Administrador buscarPorId(String id) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(BUSCAR_ID)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Administrador a = new Administrador();
                a.setId(rs.getString("id"));
                a.setClave(rs.getString("clave"));
                return a;
            }
        }
    }
}