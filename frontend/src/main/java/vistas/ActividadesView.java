package vistas;

import controladores.ActividadesController;
import modelo.Reserva;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class ActividadesView extends JPanel implements Refrescable {

    private static final String[] NOMBRES_DIAS =
            {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final SimpleDateFormat FORMATO_ENCABEZADO = new SimpleDateFormat("dd/MM");

    // Scroll automático a las 7:00 para que se vea el horario laboral.
    private static final int HORA_INICIO = 0;
    private static final int HORA_FIN = 24; // exclusivo
    private static final int HORA_SCROLL_INICIAL = 7;
    private static final int ALTO_POR_ACTIVIDAD = 48;

    private final ActividadesController controller;

    private JSpinner spinnerFecha;
    private JTable tablaActividades;
    private DefaultTableModel modeloTabla;
    private JLabel lblRangoSemana;

    private List<Reserva> reservasSemanaActual = new ArrayList<>();
    private Date lunesActual;

    public ActividadesView() {
        this.controller = new ActividadesController();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(crearPanelSuperior(), BorderLayout.NORTH);
        add(crearPanelTabla(), BorderLayout.CENTER);

        cargarSemana();
    }

    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Programación de actividades"));

        panel.add(new JLabel("Semana de:"));

        spinnerFecha = new JSpinner(new SpinnerDateModel());
        spinnerFecha.setEditor(new JSpinner.DateEditor(spinnerFecha, "dd/MM/yyyy"));
        panel.add(spinnerFecha);

        JButton btnVer = new JButton("Ver semana");
        btnVer.addActionListener(e -> cargarSemana());
        panel.add(btnVer);

        JButton btnHoy = new JButton("Semana actual");
        btnHoy.addActionListener(e -> {
            spinnerFecha.setValue(new Date());
            cargarSemana();
        });
        panel.add(btnHoy);

        JButton btnPdf = new JButton("Generar PDF");
        btnPdf.addActionListener(e -> generarPdf());
        panel.add(btnPdf);

        lblRangoSemana = new JLabel(" ");
        panel.add(lblRangoSemana);

        return panel;
    }

    private JScrollPane crearPanelTabla() {
        String[] columnas = new String[8];
        columnas[0] = "Hora";
        System.arraycopy(NOMBRES_DIAS, 0, columnas, 1, 7);

        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaActividades = new JTable(modeloTabla);
        tablaActividades.setRowHeight(45);
        tablaActividades.setShowGrid(true);
        tablaActividades.setGridColor(new Color(90, 94, 100));
        tablaActividades.setIntercellSpacing(new Dimension(1, 1));
        tablaActividades.getTableHeader().setReorderingAllowed(false);
        tablaActividades.getTableHeader().setBackground(new Color(66, 133, 200));
        tablaActividades.getTableHeader().setForeground(Color.WHITE);
        tablaActividades.setDefaultRenderer(Object.class, new CeldaActividadRenderer());

        JScrollPane scroll = new JScrollPane(tablaActividades);
        scroll.setPreferredSize(new Dimension(700, 420));
        return scroll;
    }

    @Override
    public void refrescar() {
        cargarSemana();
    }

    private void cargarSemana() {
        Date fechaSeleccionada = (Date) spinnerFecha.getValue();
        this.lunesActual = controller.obtenerLunesDeSemana(fechaSeleccionada);
        List<Date> dias = controller.obtenerDiasDeSemana(fechaSeleccionada);

        try {
            reservasSemanaActual = controller.obtenerReservasSemana(fechaSeleccionada);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al leer las reservas: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            reservasSemanaActual = new ArrayList<>();
        }

        // Encabezados con el nombre del día y su fecha (ej. "Lunes 07/09")
        String[] columnas = new String[8];
        columnas[0] = "Hora";
        for (int d = 0; d < 7; d++) {
            columnas[d + 1] = NOMBRES_DIAS[d] + " " + FORMATO_ENCABEZADO.format(dias.get(d));
        }
        modeloTabla.setColumnIdentifiers(columnas);

        modeloTabla.setRowCount(0);
        for (int hora = HORA_INICIO; hora < HORA_FIN; hora++) {
            Object[] fila = new Object[8];
            fila[0] = String.format("%02d:00", hora);
            int maxActividades = 1;
            for (int d = 0; d < 7; d++) {
                fila[d + 1] = construirTextoCelda(dias.get(d), hora);
                maxActividades = Math.max(maxActividades, contarActividades(dias.get(d), hora));
            }
            modeloTabla.addRow(fila);
            // Si en una misma hora hay varias actividades, la fila crece para que todas se vean
            tablaActividades.setRowHeight(modeloTabla.getRowCount() - 1, ALTO_POR_ACTIVIDAD * maxActividades);
        }

        Calendar calDomingo = Calendar.getInstance();
        calDomingo.setTime(lunesActual);
        calDomingo.add(Calendar.DAY_OF_MONTH, 6);
        lblRangoSemana.setText(FORMATO_ENCABEZADO.format(lunesActual) + " - "
                + FORMATO_ENCABEZADO.format(calDomingo.getTime()));

        // Mostrar desde las 7:00 sin ocultar las demás horas
        SwingUtilities.invokeLater(() -> {
            Rectangle celda = tablaActividades.getCellRect(HORA_SCROLL_INICIAL - HORA_INICIO, 0, true);
            celda.height = tablaActividades.getVisibleRect().height;
            tablaActividades.scrollRectToVisible(celda);
        });
    }

    private int contarActividades(Date dia, int hora) {
        int n = 0;
        for (Reserva r : reservasSemanaActual) {
            if (controller.ocurreEn(r, dia, hora)) n++;
        }
        return n;
    }

    private String construirTextoCelda(Date dia, int hora) {
        StringBuilder sb = new StringBuilder();
        for (Reserva r : reservasSemanaActual) {
            if (controller.ocurreEn(r, dia, hora)) {
                if (sb.length() > 0) sb.append("<br>");
                sb.append(controller.descripcionCelda(r));
            }
        }
        return sb.length() == 0 ? "" : "<html>" + sb + "</html>";
    }

    private void generarPdf() {
        try {
            String[] columnas = new String[8];
            for (int j = 0; j < 8; j++) {
                columnas[j] = modeloTabla.getColumnName(j);
            }

            List<Object[]> filas = new ArrayList<>();
            for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                Object[] fila = new Object[8];
                for (int j = 0; j < 8; j++) {
                    Object valor = modeloTabla.getValueAt(i, j);
                    fila[j] = valor == null ? "" : valor.toString()
                            .replace("<html>", "").replace("</html>", "").replace("<br>", " / ");
                }
                filas.add(fila);
            }

            JFileChooser selector = new JFileChooser();
            selector.setSelectedFile(new java.io.File("actividades_semana.pdf"));
            if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            String ruta = selector.getSelectedFile().getAbsolutePath();
            if (!ruta.toLowerCase().endsWith(".pdf")) {
                ruta += ".pdf";
            }
            controller.generarReportePDF("Programacion de actividades - Semana " + lblRangoSemana.getText(),
                    columnas, filas, ruta);

            JOptionPane.showMessageDialog(this, "Reporte generado en: " + ruta,
                    "PDF generado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al generar el PDF: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private static class CeldaActividadRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setVerticalAlignment(SwingConstants.TOP);
            return c;
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Actividades - Prueba");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(950, 650);
            frame.setLocationRelativeTo(null);
            frame.add(new ActividadesView());
            frame.setVisible(true);
        });
    }
}