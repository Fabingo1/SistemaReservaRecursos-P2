package datos;

import modelo.Administrador;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdministradorDAO extends BaseDAO {

    private static final String BUSCAR_ID =
            "SELECT id, clave FROM administrador WHERE id = ?";
    private static final String CAMBIAR_CLAVE =
            "UPDATE administrador SET clave = ? WHERE id = ?";

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

    /** Devuelve false si no existe. */
    public boolean cambiarClave(String id, String claveNueva) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(CAMBIAR_CLAVE)) {
            ps.setString(1, claveNueva);
            ps.setString(2, id);
            return ps.executeUpdate() > 0;
        }
    }
}
