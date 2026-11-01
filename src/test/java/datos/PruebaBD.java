package datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Solo para las pruebas de integración. Todo lo de prueba usa ids "IT-...", así no toca el seed.
class PruebaBD extends BaseDAO {

    /** False si Oracle está apagado o falta config.properties. */
    static boolean hayConexion() {
        try (Connection c = new PruebaBD().conectar()) {
            return c.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    void ejecutar(String sql, String... parametros) throws SQLException {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setString(i + 1, parametros[i]);
            }
            ps.executeUpdate();
        }
    }

    /** Borra los datos de prueba. El detalle se va solo (ON DELETE CASCADE). */
    void limpiar() throws SQLException {
        ejecutar("DELETE FROM reserva WHERE id LIKE 'IT-%' OR funcionario_id LIKE 'IT-%'");
        ejecutar("DELETE FROM recurso WHERE id LIKE 'IT-%'");
        ejecutar("DELETE FROM categoria WHERE id LIKE 'IT-%'");
        ejecutar("DELETE FROM funcionario WHERE id LIKE 'IT-%'");
    }
}