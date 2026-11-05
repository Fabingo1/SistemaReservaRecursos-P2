package vistas;

import controladores.CalendarizacionController;
import modelo.Categoria;
import modelo.Recurso;
import modelo.Reserva;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Matriz: filas = horas del día, columnas = recursos de la categoría seleccionada.
public class CalendarizacionView extends JPanel implements Refrescable {

    private static final int HORA_INICIO = 0;
    private static final int HORA_FIN = 24; // exclusivo
    private static final int HORA_SCROLL_INICIAL = 7;

    private final CalendarizacionController controller;

    private JComboBox<Categoria> cbCategoria;
    private JSpinner spnFecha;
    private JTable tablaMatriz;
    private DefaultTableModel modeloTabla;

    public CalendarizacionView() {
        controller = new CalendarizacionController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(crearPanelFiltros(), BorderLayout.NORTH);

        modeloTabla = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaMatriz = new JTable(modeloTabla);
        tablaMatriz.setRowHeight(30);
        tablaMatriz.setShowGrid(true);
        tablaMatriz.setGridColor(new Color(90, 94, 100));
        tablaMatriz.setIntercellSpacing(new Dimension(1, 1));
        tablaMatriz.getTableHeader().setReorderingAllowed(false);
        tablaMatriz.getTableHeader().setBackground(new Color(66, 133, 200));
        tablaMatriz.getTableHeader().setForeground(Color.WHITE);
        tablaMatriz.setDefaultRenderer(Object.class, new CeldaReservaRenderer());
        add(new JScrollPane(tablaMatriz), BorderLayout.CENTER);

        cargarCategorias();
    }

    private JPanel crearPanelFiltros() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Filtros"));

        panel.add(new JLabel("Fecha:"));
        spnFecha = new JSpinner(new SpinnerDateModel());
        spnFecha.setEditor(new JSpinner.DateEditor(spnFecha, "dd/MM/yyyy"));
        spnFecha.setValue(new Date());
        panel.add(spnFecha);

        panel.add(new JLabel("Categoría:"));
        cbCategoria = new JComboBox<>();
        cbCategoria.setPreferredSize(new Dimension(200, 25));
        panel.add(cbCategoria);

        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> generarMatriz());
        panel.add(btnBuscar);

        JButton btnRefrescar = new JButton("Recargar categorías");
        btnRefrescar.addActionListener(e -> cargarCategorias());
        panel.add(btnRefrescar);

        JButton btnPdf = new JButton("Generar PDF");
        btnPdf.addActionListener(e -> generarPdf());
        panel.add(btnPdf);

        return panel;
    }


    @Override
    public void refrescar() {
        Categoria actual = (Categoria) cbCategoria.getSelectedItem();
        cargarCategorias();
        if (actual != null) {
            for (int i = 0; i < cbCategoria.getItemCount(); i++) {
                if (cbCategoria.getItemAt(i).getId().equals(actual.getId())) {
                    cbCategoria.setSelectedIndex(i);
                    break;
                }
            }
        }
        if (modeloTabla.getColumnCount() > 0) {
            generarMatriz();
        }
    }

    private void cargarCategorias() {
        cbCategoria.removeAllItems();
        try {
            for (Categoria c : controller.obtenerCategorias()) {
                cbCategoria.addItem(c);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generarMatriz() {
        Categoria categoria = (Categoria) cbCategoria.getSelectedItem();
        if (categoria == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una categoría.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Date fecha = (Date) spnFecha.getValue();

        try {
            List<Recurso> recursos = controller.obtenerRecursosPorCategoria(categoria);
            List<Reserva> reservasDelDia = controller.obtenerReservasDelDia(fecha);

            // Columnas: "Hora" + un recurso por columna
            String[] columnas = new String[recursos.size() + 1];
            columnas[0] = "Hora";
            for (int i = 0; i < recursos.size(); i++) {
                columnas[i + 1] = recursos.get(i).getDescripcion();
            }

            Object[][] datos = new Object[HORA_FIN - HORA_INICIO][recursos.size() + 1];
            for (int fila = 0; fila < datos.length; fila++) {
                int hora = HORA_INICIO + fila;
                datos[fila][0] = String.format("%02d:00", hora);

                for (int col = 0; col < recursos.size(); col++) {
                    Reserva reserva = controller.buscarReservaEnCelda(reservasDelDia, recursos.get(col), hora);
                    datos[fila][col + 1] = reserva != null ? controller.descripcionCelda(reserva) : "";
                }
            }

            modeloTabla.setDataVector(datos, columnas);

            SwingUtilities.invokeLater(() -> {
                Rectangle celda = tablaMatriz.getCellRect(HORA_SCROLL_INICIAL - HORA_INICIO, 0, true);
                celda.height = tablaMatriz.getVisibleRect().height;
                tablaMatriz.scrollRectToVisible(celda);
            });

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al leer los datos: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generarPdf() {
        if (modeloTabla.getColumnCount() == 0 || modeloTabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Primero presione 'Buscar' para cargar la matriz.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File("calendarizacion.pdf"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String ruta = selector.getSelectedFile().getAbsolutePath();
        if (!ruta.toLowerCase().endsWith(".pdf")) {
            ruta += ".pdf";
        }

        try {
            int numColumnas = modeloTabla.getColumnCount();
            String[] columnas = new String[numColumnas];
            for (int c = 0; c < numColumnas; c++) {
                columnas[c] = modeloTabla.getColumnName(c);
            }

            List<Object[]> filas = new ArrayList<>();
            for (int f = 0; f < modeloTabla.getRowCount(); f++) {
                Object[] fila = new Object[numColumnas];
                for (int c = 0; c < numColumnas; c++) {
                    Object valor = modeloTabla.getValueAt(f, c);
                    fila[c] = valor == null ? "" : valor.toString();
                }
                filas.add(fila);
            }

            Categoria categoria = (Categoria) cbCategoria.getSelectedItem();
            String titulo = "Calendarizacion de recursos - "
                    + new SimpleDateFormat("dd/MM/yyyy").format((Date) spnFecha.getValue())
                    + (categoria != null ? " - " + categoria.getDescripcion() : "");

            controller.generarReportePDF(titulo, columnas, filas, ruta);

            JOptionPane.showMessageDialog(this, "Reporte generado en:\n" + ruta,
                    "PDF generado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al generar el PDF: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class CeldaReservaRenderer extends DefaultTableCellRenderer {
        private static final Color COLOR_RESERVADO = new Color(255, 224, 178);
        private static final Color TEXTO_RESERVADO = Color.BLACK;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                boolean ocupada = column > 0 && value != null && !value.toString().isEmpty();
                if (ocupada) {
                    c.setBackground(COLOR_RESERVADO);
                    c.setForeground(TEXTO_RESERVADO);
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
            }
            if (value != null) {
                setToolTipText(value.toString().isEmpty() ? null : value.toString());
            }
            return c;
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Prueba Calendarización");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 550);
            frame.add(new CalendarizacionView());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
