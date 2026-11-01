import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import protocolo.Configuracion;
import servicios.GenerarDatosPrueba;
import servicios.ServiceProxy;
import servicios.ServicioRemoto;
import vistas.LoginView;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Collections;

// Punto de entrada del FRONTEND. El Backend se arranca aparte con servidor.ServidorMain.
public class Main {
    public static void main(String[] args) {
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#4285C8"));
        UIManager.put("TitledBorder.titleColor", new Color(0x4285C8));
        FlatDarkLaf.setup();

        // Si es la primera ejecución (data/ vacío, p. ej. recién clonado el repo),
        // se crean datos de prueba para poder ingresar: admin1/admin1, func1/func1.
        try {
            if (GenerarDatosPrueba.generarSiNoExisten("data")) {
                System.out.println("Primera ejecución: se generaron datos de prueba en data/");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "No se pudieron crear los datos iniciales: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Al iniciar, el Frontend se conecta con el Backend (host y puerto en config.properties)
        String host = Configuracion.get("servidor.host", "localhost");
        int puerto = Configuracion.getInt("servidor.puerto", 5000);
        try {
            ServiceProxy proxy = new ServiceProxy(host, puerto);
            ServicioRemoto.set(proxy);
            Runtime.getRuntime().addShutdownHook(new Thread(proxy::cerrar));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo conectar con el servidor en " + host + ":" + puerto + ".\n"
                            + "Verifique que el Backend (servidor.ServidorMain) esté corriendo y que\n"
                            + "servidor.host / servidor.puerto en config.properties sean correctos.\n\n"
                            + "Detalle: " + e.getMessage(),
                    "Sin conexión con el servidor", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            LoginView login = new LoginView();
            login.setVisible(true);
        });
    }
}