package logica;

import datos.AdministradorDAO;
import datos.FuncionarioDAO;
import modelo.Administrador;
import modelo.Funcionario;
import modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Unitarios: DAOs falsos en memoria, no necesita Oracle
class ServicioLoginTest {

    private Map<String, String> clavesAdmins;
    private Map<String, Funcionario> funcionarios;
    private ServicioLogin servicio;

    @BeforeEach
    void setUp() {
        clavesAdmins = new HashMap<>();
        funcionarios = new HashMap<>();
        clavesAdmins.put("admin1", "admin1");

        Funcionario f = new Funcionario();
        f.setId("func1");
        f.setClave("func1");
        f.setNombre("Juan Pérez");
        f.setTelefono("8888-8888");
        funcionarios.put("func1", f);

        AdministradorDAO adminDAO = new AdministradorDAO() {
            @Override
            public Administrador buscarPorId(String id) {
                if (!clavesAdmins.containsKey(id)) return null;
                Administrador a = new Administrador();
                a.setId(id);
                a.setClave(clavesAdmins.get(id));
                return a;
            }

            @Override
            public boolean cambiarClave(String id, String claveNueva) {
                return clavesAdmins.replace(id, claveNueva) != null;
            }
        };
        FuncionarioDAO funcionarioDAO = new FuncionarioDAO() {
            @Override
            public Funcionario buscarPorId(String id) {
                Funcionario guardado = funcionarios.get(id);
                if (guardado == null) return null;
                Funcionario copia = new Funcionario();
                copia.setId(guardado.getId());
                copia.setClave(guardado.getClave());
                copia.setNombre(guardado.getNombre());
                copia.setTelefono(guardado.getTelefono());
                return copia;
            }

            @Override
            public boolean cambiarClave(String id, String claveNueva) {
                Funcionario guardado = funcionarios.get(id);
                if (guardado == null) return false;
                guardado.setClave(claveNueva);
                return true;
            }
        };
        servicio = new ServicioLogin(adminDAO, funcionarioDAO);
    }

    @Test
    void login_administradorCorrecto_devuelveAdministradorSinClave() throws Exception {
        Usuario u = servicio.login("admin1", "admin1");
        assertInstanceOf(Administrador.class, u);
        assertEquals("admin1", u.getId());
        assertNull(u.getClave());
    }

    @Test
    void login_funcionarioCorrecto_devuelveFuncionarioConNombreYSinClave() throws Exception {
        Usuario u = servicio.login(" func1 ", "func1");
        assertInstanceOf(Funcionario.class, u);
        assertEquals("Juan Pérez", ((Funcionario) u).getNombre());
        assertNull(u.getClave());
    }

    @Test
    void login_claveIncorrectaOUsuarioInexistente_devuelveNull() throws Exception {
        assertNull(servicio.login("func1", "otra"));
        assertNull(servicio.login("nadie", "x"));
        assertNull(servicio.login("", "x"));
        assertNull(servicio.login("func1", null));
    }

    @Test
    void cambiarClave_funcionario_permiteIngresarConLaNueva() throws Exception {
        servicio.cambiarClave("func1", "func1", "nueva123");
        assertNull(servicio.login("func1", "func1"));
        assertNotNull(servicio.login("func1", "nueva123"));
    }

    @Test
    void cambiarClave_administrador_laActualiza() throws Exception {
        servicio.cambiarClave("admin1", "admin1", "secreta");
        assertEquals("secreta", clavesAdmins.get("admin1"));
    }

    @Test
    void cambiarClave_claveActualIncorrecta_lanzaExcepcionYNoCambia() {
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarClave("func1", "mala", "nueva123"));
        assertEquals("func1", funcionarios.get("func1").getClave());
    }

    @Test
    void cambiarClave_nuevaIgualCortaOVacia_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarClave("admin1", "admin1", "admin1"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarClave("admin1", "admin1", "abc"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarClave("admin1", "admin1", "   "));
    }

    @Test
    void cambiarClave_usuarioInexistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarClave("nadie", "x", "nueva123"));
    }
}
