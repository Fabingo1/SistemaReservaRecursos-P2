package vistas;


import controladores.CategoriaController;
import modelo.Categoria;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

public class CategoriaView extends JPanel implements Refrescable {

    private final CategoriaController controller;

    private JTextField txtBusqueda;
    private JButton btnBuscar;
    private JButton btnImprimir;

    private JTextField txtId;
    private JTextField txtDescripcion;
    private JButton btnGuardar;
    private JButton btnBorrar;
    private JButton btnLimpiar;

    private JTable tablaListado;
    private DefaultTableModel modeloTabla;

    private String idSeleccionado;

    public CategoriaView() {
        controller = new CategoriaController();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(crearPanelBusqueda(), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(10, 10));
        centro.add(crearPanelFormulario(), BorderLayout.NORTH);
        centro.add(crearPanelListado(), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        cargarTabla(obtenerTodasOMostrarError());
    }

    // ---------- Construcción de paneles ----------

    private JPanel crearPanelBusqueda() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Búsqueda"));

        panel.add(new JLabel("Descripción:"));
        txtBusqueda = new JTextField(20);
        panel.add(txtBusqueda);

        btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> buscar());
        panel.add(btnBuscar);

        btnImprimir = new JButton("Imprimir");
        btnImprimir.addActionListener(e -> imprimir());
        panel.add(btnImprimir);

        return panel;
    }

    private JPanel crearPanelFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Categoría"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("ID:"), c);
        c.gridx = 1;
        txtId = new JTextField(15);
        txtId.setEditable(false); // el id es autogenerado, nunca se escribe a mano
        panel.add(txtId, c);

        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("Descripción:"), c);
        c.gridx = 1;
        txtDescripcion = new JTextField(25);
        panel.add(txtDescripcion, c);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> guardar());
        botones.add(btnGuardar);

        btnBorrar = new JButton("Borrar");
        btnBorrar.addActionListener(e -> borrar());
        botones.add(btnBorrar);

        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> limpiar());
        botones.add(btnLimpiar);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        panel.add(botones, c);

        return panel;
    }

    private JScrollPane crearPanelListado() {
        modeloTabla = new DefaultTableModel(new Object[]{"Id", "Descripción"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // la tabla es solo de consulta; se edita por el formulario
            }
        };
        tablaListado = new JTable(modeloTabla);
        tablaListado.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaListado.setShowGrid(true);
        tablaListado.setGridColor(new Color(90, 94, 100));
        tablaListado.setIntercellSpacing(new Dimension(1, 1));
        tablaListado.getTableHeader().setBackground(new Color(66, 133, 200));
        tablaListado.getTableHeader().setForeground(Color.WHITE);
        tablaListado.getSelectionModel().addListSelectionListener(this::alSeleccionarFila);

        JScrollPane scroll = new JScrollPane(tablaListado);
        scroll.setBorder(BorderFactory.createTitledBorder("Listado"));
        return scroll;
    }

    // ---------- Acciones ----------

    @Override
    public void refrescar() {
        buscar();
    }

    private void buscar() {
        try {
            List<Categoria> resultado = controller.buscarPorDescripcion(txtBusqueda.getText());
            cargarTabla(resultado);
        } catch (Exception ex) {
            mostrarError("Error al buscar categorías", ex);
        }
    }

    private void guardar() {
        try {
            String descripcion = txtDescripcion.getText();
            if (idSeleccionado == null) {
                controller.crear(descripcion);
            } else {
                controller.modificar(idSeleccionado, descripcion);
            }
            limpiar();
            buscar(); // refresca el listado respetando el filtro de búsqueda actual
        } catch (Exception ex) {
            // Las validaciones las hace el Backend y llegan como mensaje
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo guardar", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void borrar() {
        if (idSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una categoría de la lista para borrar.");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Seguro que desea borrar la categoría " + idSeleccionado + "?",
                "Confirmar borrado", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            controller.borrar(idSeleccionado);
            limpiar();
            buscar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo borrar", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void limpiar() {
        idSeleccionado = null;
        txtId.setText("");
        txtDescripcion.setText("");
        tablaListado.clearSelection();
    }

    private void imprimir() {
        try {
            List<Categoria> categorias = controller.buscarPorDescripcion(txtBusqueda.getText());
            if (categorias.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay categorías para incluir en el reporte.");
                return;
            }

            JFileChooser selector = new JFileChooser();
            selector.setSelectedFile(new File("categorias.pdf"));
            if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            String ruta = selector.getSelectedFile().getAbsolutePath();
            if (!ruta.toLowerCase().endsWith(".pdf")) {
                ruta += ".pdf";
            }

            controller.generarReportePDF(categorias, ruta);
            JOptionPane.showMessageDialog(this, "Reporte generado en:\n" + ruta);
        } catch (Exception ex) {
            mostrarError("Error al generar el reporte PDF", ex);
        }
    }

    private void alSeleccionarFila(ListSelectionEvent evento) {
        if (evento.getValueIsAdjusting()) {
            return;
        }
        int fila = tablaListado.getSelectedRow();
        if (fila < 0) {
            return;
        }
        idSeleccionado = (String) modeloTabla.getValueAt(fila, 0);
        txtId.setText(idSeleccionado);
        txtDescripcion.setText((String) modeloTabla.getValueAt(fila, 1));
    }

    // ---------- Utilidades ----------

    private void cargarTabla(List<Categoria> categorias) {
        modeloTabla.setRowCount(0);
        for (Categoria c : categorias) {
            modeloTabla.addRow(new Object[]{c.getId(), c.getDescripcion()});
        }
    }

    private List<Categoria> obtenerTodasOMostrarError() {
        try {
            return controller.listar();
        } catch (Exception ex) {
            mostrarError("Error al cargar categorías", ex);
            return List.of();
        }
    }

    private void mostrarError(String titulo, Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), titulo, JOptionPane.ERROR_MESSAGE);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Prueba Categorías");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(700, 550);
            frame.add(new CategoriaView());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}