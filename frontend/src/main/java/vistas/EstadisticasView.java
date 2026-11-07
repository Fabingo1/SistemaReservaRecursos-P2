package vistas;

import controladores.EstadisticasController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class EstadisticasView extends JPanel {

    private final EstadisticasController controller;

    private JSpinner spinnerDesdeRecursos;
    private JSpinner spinnerHastaRecursos;
    private DefaultTableModel modeloTablaRecursos;
    private BarChartPanel chartRecursos;
    private Map<String, Integer> datosRecursosActuales = new LinkedHashMap<>();

    private JSpinner spinnerDesdeActividades;
    private JSpinner spinnerHastaActividades;
    private DefaultTableModel modeloTablaActividades;
    private BarChartPanel chartActividades;
    private Map<String, Integer> datosActividadesActuales = new LinkedHashMap<>();

    public EstadisticasView() {
        this.controller = new EstadisticasController();

        // Dos columnas (Recursos | Actividades) visibles a la vez, en vez de pestañas.
        setLayout(new GridLayout(1, 2, 10, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(crearPanelRecursos());
        add(crearPanelActividadesSemana());
    }

    // ---------------------------------------------------------------
    // RECURSOS
    // ---------------------------------------------------------------

    private JPanel crearPanelRecursos() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Recursos"));

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        Calendar haceUnMes = Calendar.getInstance();
        haceUnMes.add(Calendar.MONTH, -1);

        Calendar enUnMes = Calendar.getInstance();
        enUnMes.add(Calendar.MONTH, 1);
        spinnerDesdeRecursos = crearSpinnerFecha(haceUnMes.getTime());
        spinnerHastaRecursos = crearSpinnerFecha(enUnMes.getTime());

        controles.add(new JLabel("Desde:"));
        controles.add(spinnerDesdeRecursos);
        controles.add(new JLabel("Hasta:"));
        controles.add(spinnerHastaRecursos);

        JButton btnConsultar = new JButton("Consultar");
        btnConsultar.addActionListener(e -> consultarRecursos());
        controles.add(btnConsultar);

        JButton btnPdf = new JButton("Generar PDF");
        btnPdf.addActionListener(e -> generarPdfRecursos());
        controles.add(btnPdf);

        panel.add(controles, BorderLayout.NORTH);

        modeloTablaRecursos = new DefaultTableModel(new String[]{"Categoría", "Cantidad"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabla = new JTable(modeloTablaRecursos);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setPreferredSize(new Dimension(260, 300));

        chartRecursos = new BarChartPanel();
        chartRecursos.setBorder(BorderFactory.createTitledBorder("Reservas por categoría de recurso"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollTabla, chartRecursos);
        split.setResizeWeight(0.3);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    private void consultarRecursos() {
        Date desde = (Date) spinnerDesdeRecursos.getValue();
        Date hasta = (Date) spinnerHastaRecursos.getValue();
        if (desde.after(hasta)) {
            JOptionPane.showMessageDialog(this, "La fecha 'Desde' no puede ser posterior a 'Hasta'.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            datosRecursosActuales = controller.estadisticasRecursos(desde, hasta);

            modeloTablaRecursos.setRowCount(0);
            for (Map.Entry<String, Integer> e : datosRecursosActuales.entrySet()) {
                modeloTablaRecursos.addRow(new Object[]{e.getKey(), e.getValue()});
            }
            chartRecursos.setDatos(datosRecursosActuales);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al leer las reservas: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generarPdfRecursos() {
        if (datosRecursosActuales.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Primero realice una consulta.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String ruta = elegirRutaPdf("estadisticas_recursos.pdf");
            if (ruta == null) return;
            controller.generarReportePDF("Estadisticas de recursos reservados ("
                    + rango(spinnerDesdeRecursos, spinnerHastaRecursos) + ")",
                    "Categoria", datosRecursosActuales, ruta);
            JOptionPane.showMessageDialog(this, "Reporte generado en: " + ruta,
                    "PDF generado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al generar el PDF: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------
    // ACTIVIDADES
    // ---------------------------------------------------------------

    private JPanel crearPanelActividadesSemana() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Actividades"));

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        Calendar haceUnMes = Calendar.getInstance();
        haceUnMes.add(Calendar.MONTH, -1);
        Calendar enUnMes = Calendar.getInstance();
        enUnMes.add(Calendar.MONTH, 1);

        spinnerDesdeActividades = crearSpinnerFecha(haceUnMes.getTime());
        spinnerHastaActividades = crearSpinnerFecha(enUnMes.getTime());

        controles.add(new JLabel("Desde:"));
        controles.add(spinnerDesdeActividades);
        controles.add(new JLabel("Hasta:"));
        controles.add(spinnerHastaActividades);

        JButton btnConsultar = new JButton("Consultar");
        btnConsultar.addActionListener(e -> consultarActividades());
        controles.add(btnConsultar);

        JButton btnPdf = new JButton("Generar PDF");
        btnPdf.addActionListener(e -> generarPdfActividades());
        controles.add(btnPdf);

        panel.add(controles, BorderLayout.NORTH);

        modeloTablaActividades = new DefaultTableModel(new String[]{"Semana", "Cantidad"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabla = new JTable(modeloTablaActividades);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setPreferredSize(new Dimension(260, 300));

        chartActividades = new BarChartPanel();
        chartActividades.setBorder(BorderFactory.createTitledBorder("Actividades programadas por semana"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollTabla, chartActividades);
        split.setResizeWeight(0.3);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    private void consultarActividades() {
        Date desde = (Date) spinnerDesdeActividades.getValue();
        Date hasta = (Date) spinnerHastaActividades.getValue();
        if (desde.after(hasta)) {
            JOptionPane.showMessageDialog(this, "La fecha 'Desde' no puede ser posterior a 'Hasta'.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            datosActividadesActuales = controller.estadisticasActividades(desde, hasta);

            modeloTablaActividades.setRowCount(0);
            for (Map.Entry<String, Integer> e : datosActividadesActuales.entrySet()) {
                modeloTablaActividades.addRow(new Object[]{e.getKey(), e.getValue()});
            }
            chartActividades.setDatos(datosActividadesActuales);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al leer las reservas: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generarPdfActividades() {
        if (datosActividadesActuales.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Primero realice una consulta.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String ruta = elegirRutaPdf("estadisticas_actividades.pdf");
            if (ruta == null) return;
            controller.generarReportePDF("Estadisticas de actividades por semana ("
                    + rango(spinnerDesdeActividades, spinnerHastaActividades) + ")",
                    "Semana", datosActividadesActuales, ruta);
            JOptionPane.showMessageDialog(this, "Reporte generado en: " + ruta,
                    "PDF generado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al generar el PDF: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------

    private String elegirRutaPdf(String nombreSugerido) {
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File(nombreSugerido));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        String ruta = selector.getSelectedFile().getAbsolutePath();
        return ruta.toLowerCase().endsWith(".pdf") ? ruta : ruta + ".pdf";
    }

    private String rango(JSpinner desde, JSpinner hasta) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy");
        return f.format((Date) desde.getValue()) + " a " + f.format((Date) hasta.getValue());
    }

    private JSpinner crearSpinnerFecha(Date valorInicial) {
        SpinnerDateModel modelo = new SpinnerDateModel();
        modelo.setValue(valorInicial);
        JSpinner spinner = new JSpinner(modelo);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        return spinner;
    }

    // Java2D en vez de una librería de gráficos externa.
    private static class BarChartPanel extends JPanel {
        private Map<String, Integer> datos = new LinkedHashMap<>();

        BarChartPanel() {
            setPreferredSize(new Dimension(420, 300));
        }

        void setDatos(Map<String, Integer> datos) {
            this.datos = datos;
            repaint();
        }

        /** "07/09/26 - 13/09/26" -> "07/09/26"; textos largos se recortan. */
        private static String etiquetaCorta(String etiqueta) {
            int guion = etiqueta.indexOf(" - ");
            String corta = guion > 0 ? etiqueta.substring(0, guion) : etiqueta;
            return corta.length() > 16 ? corta.substring(0, 15) + "…" : corta;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color colorTexto = UIManager.getColor("Label.foreground");
            Color colorEjes = UIManager.getColor("Label.disabledForeground");

            int ancho = getWidth();
            int alto = getHeight();
            int margenInferior = 70;
            int margenSuperior = 30;
            int margenLateral = 20;

            if (datos == null || datos.isEmpty()) {
                g2.setColor(colorTexto);
                g2.drawString("Sin datos para el período seleccionado.", margenLateral, alto / 2);
                return;
            }

            int max = 1;
            for (int v : datos.values()) {
                max = Math.max(max, v);
            }

            int numBarras = datos.size();
            double espacio = (ancho - 2.0 * margenLateral) / numBarras;
            int anchoBarra = (int) Math.max(4, espacio * 0.65);
            int alturaDisponible = alto - margenSuperior - margenInferior;
            int saltoEtiquetas = (int) Math.max(1, Math.ceil(18 / espacio));

            int i = 0;
            for (Map.Entry<String, Integer> entry : datos.entrySet()) {
                int valor = entry.getValue();
                int x = (int) (margenLateral + i * espacio + (espacio - anchoBarra) / 2);
                int alturaBarra = (int) Math.round((valor / (double) max) * alturaDisponible);
                int y = alto - margenInferior - alturaBarra;

                g2.setColor(new Color(66, 133, 200));
                g2.fillRect(x, y, anchoBarra, alturaBarra);

                g2.setColor(colorTexto);
                String textoValor = String.valueOf(valor);
                int anchoValor = g2.getFontMetrics().stringWidth(textoValor);
                g2.drawString(textoValor, x + (anchoBarra - anchoValor) / 2, Math.max(12, y - 4));

                if (i % saltoEtiquetas == 0) {
                    Graphics2D g2r = (Graphics2D) g2.create();
                    g2r.setColor(colorTexto);
                    g2r.translate(x + anchoBarra / 2.0 - 3, alto - margenInferior + 12);
                    g2r.rotate(Math.toRadians(40));
                    g2r.drawString(etiquetaCorta(entry.getKey()), 0, 0);
                    g2r.dispose();
                }
                i++;
            }

            g2.setColor(colorEjes);
            g2.drawLine(margenLateral, alto - margenInferior, ancho - margenLateral, alto - margenInferior);
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Estadísticas - Prueba");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 560);
            frame.setLocationRelativeTo(null);
            frame.add(new EstadisticasView());
            frame.setVisible(true);
        });
    }
}
