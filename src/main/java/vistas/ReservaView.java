package vistas;

import controladores.ReservaController;
import controladores.ResultadoReserva;
import modelo.Categoria;
import modelo.Funcionario;
import modelo.Reserva;
import servicios.LLMService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

// Las validaciones de negocio están en ReservaController; aquí solo se leen los campos y se muestran mensajes.
public class ReservaView extends JPanel implements Refrescable {
    private final ReservaController controller;
    private final Funcionario funcionarioActual;

    private JTextField txtFrase;
    private JButton btnExtraer;
    private JTextField txtActividad;
    private JSpinner spnFecha;
    private JSpinner spnHoraInicio;
    private JSpinner spnHoraFin;
    private JList<Categoria> listCategorias;
    private DefaultListModel<Categoria> modeloCategorias;

    private JTable tablaReservas;
    private DefaultTableModel modeloTabla;
    private List<Reserva> reservasMostradas = new ArrayList<>();

    public ReservaView(Funcionario funcionarioActual) {
        this.controller = new ReservaController();
        this.funcionarioActual = funcionarioActual;

        setLayout(new BorderLayout(10, 10));
        add(construirPanelFormulario(), BorderLayout.NORTH);
        add(construirPanelTabla(), BorderLayout.CENTER);

        limpiarFormulario();
        cargarMisReservas();
    }

    // ------------------------------------------------------------------
    // Interfaz
    // ------------------------------------------------------------------
    private JPanel construirPanelFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Nueva reserva"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Frase:"), gbc);
        txtFrase = new JTextField();
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1;
        panel.add(txtFrase, gbc);
        btnExtraer = new JButton("Extraer (IA)");
        gbc.gridx = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(btnExtraer, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Actividad:"), gbc);
        txtActividad = new JTextField();
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1;
        panel.add(txtActividad, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(new JLabel("Fecha:"), gbc);
        spnFecha = new JSpinner(new SpinnerDateModel());
        spnFecha.setEditor(new JSpinner.DateEditor(spnFecha, "dd/MM/yyyy"));
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(spnFecha, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("Hora inicio:"), gbc);
        spnHoraInicio = new JSpinner(new SpinnerDateModel());
        spnHoraInicio.setEditor(new JSpinner.DateEditor(spnHoraInicio, "HH:mm"));
        gbc.gridx = 3; gbc.weightx = 1;
        panel.add(spnHoraInicio, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panel.add(new JLabel("Hora fin:"), gbc);
        spnHoraFin = new JSpinner(new SpinnerDateModel());
        spnHoraFin.setEditor(new JSpinner.DateEditor(spnHoraFin, "HH:mm"));
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(spnHoraFin, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        panel.add(new JLabel("Categorías:"), gbc);
        modeloCategorias = new DefaultListModel<>();
        listCategorias = new JList<>(modeloCategorias);
        listCategorias.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listCategorias.setVisibleRowCount(4);
        listCategorias.setToolTipText("Ctrl + clic para seleccionar varias");
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1; gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(listCategorias), gbc);
        recargarCategorias();

        JPanel panelBotones = new JPanel();
        JButton btnReservar = new JButton("Reservar");
        JButton btnCancelar = new JButton("Cancelar reserva seleccionada");
        JButton btnLimpiar = new JButton("Limpiar");
        panelBotones.add(btnReservar);
        panelBotones.add(btnCancelar);
        panelBotones.add(btnLimpiar);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.NONE;
        panel.add(panelBotones, gbc);

        btnReservar.addActionListener(e -> reservar());
        btnCancelar.addActionListener(e -> cancelarSeleccionada());
        btnExtraer.addActionListener(e -> extraerConIA());
        btnLimpiar.addActionListener(e -> limpiarFormulario());

        return panel;
    }

    private JPanel construirPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Mis reservas"));

        String[] columnas = {"Id", "Actividad", "Fecha", "Horario", "Recursos", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaReservas = new JTable(modeloTabla);
        tablaReservas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaReservas.setRowHeight(28);
        tablaReservas.setShowGrid(true);
        tablaReservas.setGridColor(new Color(90, 94, 100));
        tablaReservas.setIntercellSpacing(new Dimension(1, 1));
        tablaReservas.getTableHeader().setBackground(new Color(66, 133, 200));
        tablaReservas.getTableHeader().setForeground(Color.WHITE);
        tablaReservas.setDefaultRenderer(Object.class, new EstadoCellRenderer());
        panel.add(new JScrollPane(tablaReservas), BorderLayout.CENTER);

        JButton btnImprimir = new JButton("Imprimir (PDF)");
        btnImprimir.addActionListener(e -> generarPdf());
        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> {
            recargarCategorias();
            cargarMisReservas();
        });
        JPanel panelBoton = new JPanel();
        panelBoton.add(btnRefrescar);
        panelBoton.add(btnImprimir);
        panel.add(panelBoton, BorderLayout.SOUTH);

        return panel;
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    private void reservar() {
        String actividad = txtActividad.getText();
        List<Categoria> seleccionadas = listCategorias.getSelectedValuesList();
        Date fecha = (Date) spnFecha.getValue();
        Date horaInicio = (Date) spnHoraInicio.getValue();
        Date horaFin = (Date) spnHoraFin.getValue();

        try {
            ResultadoReserva resultado = controller.crearReserva(
                    funcionarioActual, actividad, fecha, horaInicio, horaFin, seleccionadas);

            if (resultado.isExitosa()) {
                JOptionPane.showMessageDialog(this,
                        "Reserva creada: " + resultado.getReserva().getId()
                                + "\nRecursos asignados: " + controller.textoRecursos(resultado.getReserva()),
                        "Reserva exitosa", JOptionPane.INFORMATION_MESSAGE);
                limpiarFormulario();
                cargarMisReservas();
            } else {
                // No se limpia: el funcionario puede cambiar datos y reintentar.
                StringBuilder mensaje = new StringBuilder("No hubo disponibilidad para:\n");
                for (Categoria c : resultado.getCategoriasNoDisponibles()) {
                    mensaje.append("- ").append(c.getDescripcion()).append("\n");
                }
                mensaje.append("\nPuede modificar la reserva e intentar de nuevo.");
                JOptionPane.showMessageDialog(this, mensaje.toString(), "Sin disponibilidad",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Datos inválidos", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar la reserva: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelarSeleccionada() {
        int fila = tablaReservas.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una reserva de la tabla primero.");
            return;
        }

        String id = (String) modeloTabla.getValueAt(fila, 0);
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Cancelar la reserva " + id + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            controller.cancelarReserva(id, funcionarioActual);
            JOptionPane.showMessageDialog(this, "Reserva " + id + " cancelada. Sus recursos quedaron libres.");
            cargarMisReservas();
        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo cancelar", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al cancelar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void extraerConIA() {
        String frase = txtFrase.getText().trim();
        if (frase.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribí una frase describiendo la reserva primero.");
            return;
        }

        btnExtraer.setEnabled(false);
        btnExtraer.setText("Extrayendo...");

        List<String> nombresCategorias = new ArrayList<>();
        for (int i = 0; i < modeloCategorias.size(); i++) {
            nombresCategorias.add(modeloCategorias.get(i).getDescripcion());
        }

        new SwingWorker<Map<String, String>, Void>() {
            @Override
            protected Map<String, String> doInBackground() throws Exception {
                LLMService llmService = new LLMService();
                return llmService.extraerDatos(frase, nombresCategorias);
            }

            @Override
            protected void done() {
                btnExtraer.setEnabled(true);
                btnExtraer.setText("Extraer (IA)");
                try {
                    aplicarDatosExtraidos(get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ReservaView.this,
                            "No se pudo extraer la información: " + ex.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void aplicarDatosExtraidos(Map<String, String> datos) {
        if (!datos.get("actividad").isEmpty()) {
            txtActividad.setText(datos.get("actividad"));
        }

        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm");

        try {
            if (!datos.get("fecha").isEmpty()) {
                spnFecha.setValue(sdfFecha.parse(datos.get("fecha")));
            }
            if (!datos.get("horaInicio").isEmpty()) {
                spnHoraInicio.setValue(sdfHora.parse(datos.get("horaInicio")));
            }
            if (!datos.get("horaFin").isEmpty()) {
                spnHoraFin.setValue(sdfHora.parse(datos.get("horaFin")));
            }
        } catch (ParseException ex) {
            JOptionPane.showMessageDialog(this, "La IA devolvió una fecha u hora en formato inesperado.");
        }

        seleccionarCategoriasPorTexto(datos.get("categorias"));
    }

    private void seleccionarCategoriasPorTexto(String categoriasTexto) {
        if (categoriasTexto == null || categoriasTexto.isBlank()) {
            return;
        }

        String[] palabrasClave = categoriasTexto.toLowerCase().split(",\\s*");
        List<Integer> indicesAMarcar = new ArrayList<>();

        for (int i = 0; i < modeloCategorias.size(); i++) {
            String descripcion = modeloCategorias.get(i).getDescripcion().toLowerCase();
            for (String palabra : palabrasClave) {
                if (descripcion.equals(palabra.trim())) {
                    indicesAMarcar.add(i);
                    break;
                }
            }
        }

        listCategorias.setSelectedIndices(indicesAMarcar.stream().mapToInt(Integer::intValue).toArray());
    }

    private void generarPdf() {
        if (reservasMostradas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay reservas para imprimir.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File("mis_reservas.pdf"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String ruta = selector.getSelectedFile().getAbsolutePath();
        if (!ruta.toLowerCase().endsWith(".pdf")) {
            ruta += ".pdf";
        }
        try {
            String nombre = funcionarioActual.getNombre() != null ? funcionarioActual.getNombre() : funcionarioActual.getId();
            controller.generarReportePDF(reservasMostradas, "Reservas de " + nombre, ruta);
            JOptionPane.showMessageDialog(this, "Reporte generado en:\n" + ruta,
                    "PDF generado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al generar el PDF: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        txtFrase.setText("");
        txtActividad.setText("");
        // Valores por defecto razonables: hoy, próxima hora en punto, y una hora después.
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        spnFecha.setValue(cal.getTime());
        spnHoraInicio.setValue(cal.getTime());
        cal.add(Calendar.HOUR_OF_DAY, 1);
        spnHoraFin.setValue(cal.getTime());
        listCategorias.clearSelection();
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    @Override
    public void refrescar() {
        List<Categoria> seleccion = listCategorias.getSelectedValuesList();
        recargarCategorias();
        // Volver a marcar las categorías que estaban seleccionadas
        for (int i = 0; i < modeloCategorias.size(); i++) {
            for (Categoria c : seleccion) {
                if (c.getId().equals(modeloCategorias.get(i).getId())) {
                    listCategorias.addSelectionInterval(i, i);
                }
            }
        }
        cargarMisReservas();
    }

    private void recargarCategorias() {
        modeloCategorias.clear();
        try {
            for (Categoria c : controller.obtenerCategoriasDisponibles()) {
                modeloCategorias.addElement(c);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las categorías: " + e.getMessage());
        }
    }

    private void cargarMisReservas() {
        try {
            reservasMostradas = controller.obtenerReservasDe(funcionarioActual);
            modeloTabla.setRowCount(0);

            SimpleDateFormat sdfFecha = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm");

            for (Reserva r : reservasMostradas) {
                String horario = sdfHora.format(r.getHoraInicio()) + " - " + sdfHora.format(r.getHoraFin());
                modeloTabla.addRow(new Object[]{
                        r.getId(), r.getActividad(), sdfFecha.format(r.getFecha()),
                        horario, controller.textoRecursos(r), r.getEstado()
                });
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar sus reservas: " + e.getMessage());
        }
    }

    private static class EstadoCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                if (column == 5 && "ACTIVA".equals(String.valueOf(value))) {
                    c.setForeground(new Color(102, 187, 106));
                } else if (column == 5 && "CANCELADA".equals(String.valueOf(value))) {
                    c.setForeground(new Color(158, 158, 158));
                } else {
                    c.setForeground(table.getForeground());
                }
            }
            return c;
        }
    }
}