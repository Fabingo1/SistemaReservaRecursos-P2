package vistas;

import controladores.LoginController;

import javax.swing.*;
import java.awt.*;

// El id llega ya escrito desde LoginView, por lo que aquí solo se piden las claves.
public class CambiarClaveDialog extends JDialog {

    private final LoginController controller;
    private final String id;

    private JPasswordField txtClaveActual;
    private JPasswordField txtClaveNueva;
    private JPasswordField txtClaveNuevaConfirmar;

    public CambiarClaveDialog(Frame propietario, LoginController controller, String id) {
        super(propietario, "Cambiar Clave", true);
        this.controller = controller;
        this.id = id;

        setSize(340, 220);
        setLocationRelativeTo(propietario);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Clave Actual:"), gbc);
        txtClaveActual = new JPasswordField(14);
        gbc.gridx = 1;
        panel.add(txtClaveActual, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Clave Nueva:"), gbc);
        txtClaveNueva = new JPasswordField(14);
        gbc.gridx = 1;
        panel.add(txtClaveNueva, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Confirmar Clave Nueva:"), gbc);
        txtClaveNuevaConfirmar = new JPasswordField(14);
        gbc.gridx = 1;
        panel.add(txtClaveNuevaConfirmar, gbc);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton btnAceptar = new JButton("Aceptar");
        JButton btnCancelar = new JButton("Cancelar");
        panelBotones.add(btnAceptar);
        panelBotones.add(btnCancelar);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(panelBotones, gbc);

        btnAceptar.addActionListener(e -> intentarCambiarClave());
        btnCancelar.addActionListener(e -> dispose());

        setContentPane(panel);
    }

    private void intentarCambiarClave() {
        String claveActual = new String(txtClaveActual.getPassword());
        String claveNueva = new String(txtClaveNueva.getPassword());
        String confirmacion = new String(txtClaveNuevaConfirmar.getPassword());

        if (claveActual.isBlank() || claveNueva.isBlank() || confirmacion.isBlank()) {
            JOptionPane.showMessageDialog(this, "Debe llenar todos los campos.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!claveNueva.equals(confirmacion)) {
            JOptionPane.showMessageDialog(this, "La clave nueva y su confirmación no coinciden.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            controller.cambiarClave(id, claveActual, claveNueva);
            JOptionPane.showMessageDialog(this, "Clave actualizada correctamente.",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            // Las validaciones (clave actual, largo mínimo, etc.) las hace el Backend y llegan como mensaje
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
