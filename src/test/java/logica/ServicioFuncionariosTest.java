package logica;

import datos.AdministradorDAO;
import datos.FuncionarioDAO;
import datos.ReservaDAO;
import modelo.Administrador;
import modelo.Funcionario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Unitarios: DAOs falsos en memoria, no necesita Oracle
class ServicioFuncionariosTest {

    private Map<String, Funcionario> funcionarios;
    private Map<String, Administrador> admins;
    private List<String> conReservasFuturas;
    private ServicioFuncionarios servicio;

    @BeforeEach
    void setUp() {
        funcionarios = new HashMap<>();
        admins = new HashMap<>();
        conReservasFuturas = new ArrayList<>();

        FuncionarioDAO funcionarioDAO = new FuncionarioDAO() {
            @Override
            public Funcionario buscarPorId(String id) {
                return funcionarios.get(id);
            }

            @Override
            public void insertar(Funcionario f) {
                funcionarios.put(f.getId(), copia(f));
            }

            @Override
            public boolean modificar(String id, String nombre, String telefono) {
                Funcionario f = funcionarios.get(id);
                if (f == null) return false;
                f.setNombre(nombre);
                f.setTelefono(telefono);
                return true;
            }

            @Override
            public boolean borrar(String id) {
                return funcionarios.remove(id) != null;
            }
        };
        AdministradorDAO administradorDAO = new AdministradorDAO() {
            @Override
            public Administrador buscarPorId(String id) {
                return admins.get(id);
            }
        };
        ReservaDAO reservaDAO = new ReservaDAO() {
            @Override
            public boolean tieneReservasFuturas(String funcionarioId) {
                return conReservasFuturas.contains(funcionarioId);
            }
        };

        servicio = new ServicioFuncionarios(funcionarioDAO, administradorDAO, reservaDAO);
    }

    //crear

    @Test
    void crear_datosValidos_loGuardaConClaveIgualAlIdYLoDevuelveSinClave() throws Exception {
        Funcionario creado = servicio.crear(" func9 ", " Ana Mora ", "8888-1234");

        assertEquals("func9", creado.getId());
        assertEquals("Ana Mora", creado.getNombre());
        assertNull(creado.getClave());
        assertEquals("func9", funcionarios.get("func9").getClave());
    }

    @Test
    void crear_idRepetido_lanzaExcepcion() {
        funcionarios.put("func1", funcionario("func1"));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("func1", "Otro", "8888-8888"));
    }

    @Test
    void crear_idDeAdministrador_lanzaExcepcion() {
        Administrador a = new Administrador();
        a.setId("admin1");
        admins.put("admin1", a);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("admin1", "Ana", "8888-8888"));
    }

    @Test
    void crear_idConEspacios_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("func 9", "Ana", "8888-8888"));
    }

    @Test
    void crear_telefonoConLetras_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("func9", "Ana", "8888-abcd"));
    }

    @Test
    void crear_telefonoCorto_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("func9", "Ana", "8888"));
    }

    @Test
    void crear_nombreVacio_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("func9", "  ", "8888-8888"));
    }

    //modificar

    @Test
    void modificar_existente_actualizaDatos() throws Exception {
        funcionarios.put("func1", funcionario("func1"));

        servicio.modificar("func1", "Juan Nuevo", "8777-0000");

        assertEquals("Juan Nuevo", funcionarios.get("func1").getNombre());
        assertEquals("8777-0000", funcionarios.get("func1").getTelefono());
    }

    @Test
    void modificar_inexistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.modificar("nadie", "Ana", "8888-8888"));
    }

    //borrar

    @Test
    void borrar_sinReservasFuturas_loElimina() throws Exception {
        funcionarios.put("func1", funcionario("func1"));

        servicio.borrar("func1");

        assertFalse(funcionarios.containsKey("func1"));
    }

    @Test
    void borrar_conReservasFuturas_lanzaExcepcionYNoBorra() {
        funcionarios.put("func1", funcionario("func1"));
        conReservasFuturas.add("func1");

        assertThrows(IllegalArgumentException.class, () -> servicio.borrar("func1"));
        assertTrue(funcionarios.containsKey("func1"));
    }

    @Test
    void borrar_inexistente_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.borrar("nadie"));
    }

    //utilidades

    private static Funcionario funcionario(String id) {
        Funcionario f = new Funcionario();
        f.setId(id);
        f.setClave(id);
        f.setNombre("Juan Pérez");
        f.setTelefono("8888-8888");
        return f;
    }

    private static Funcionario copia(Funcionario f) {
        Funcionario c = funcionario(f.getId());
        c.setClave(f.getClave());
        c.setNombre(f.getNombre());
        c.setTelefono(f.getTelefono());
        return c;
    }
}