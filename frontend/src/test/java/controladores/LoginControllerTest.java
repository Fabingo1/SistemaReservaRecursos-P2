package controladores;

import modelo.Funcionario;
import modelo.Usuario;
import org.junit.jupiter.api.Test;
import servicios.IServiceFalso;

import static org.junit.jupiter.api.Assertions.*;

// Frontend: el controller solo delega en el IService (aquí uno falso, sin red ni BD)
class LoginControllerTest {

    @Test
    void autenticar_quitaEspaciosDelIdYDevuelveLoQueDiceElServicio() throws Exception {
        String[] idRecibido = new String[1];
        LoginController controller = new LoginController(new IServiceFalso() {
            @Override
            public Usuario login(String id, String clave) {
                idRecibido[0] = id;
                Funcionario f = new Funcionario();
                f.setId(id);
                return f;
            }
        });

        Usuario u = controller.autenticar("  func1 ", "func1");

        assertEquals("func1", idRecibido[0]);
        assertInstanceOf(Funcionario.class, u);
    }

    @Test
    void autenticar_datosVacios_devuelveNullSinLlamarAlServidor() throws Exception {
        LoginController controller = new LoginController(new IServiceFalso()); // login lanzaría excepción
        assertNull(controller.autenticar("", "x"));
        assertNull(controller.autenticar("func1", ""));
    }

    @Test
    void cambiarClave_errorDelServidor_llegaConSuMensaje() {
        LoginController controller = new LoginController(new IServiceFalso() {
            @Override
            public void cambiarClave(String id, String claveActual, String claveNueva) throws Exception {
                throw new Exception("La clave actual no es correcta.");
            }
        });

        Exception e = assertThrows(Exception.class, () -> controller.cambiarClave("func1", "mala", "nueva123"));
        assertEquals("La clave actual no es correcta.", e.getMessage());
    }
}
