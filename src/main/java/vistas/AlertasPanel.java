package vistas;

import modelo.DetalleReserva;
import modelo.Reserva;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

// Área de Alertas (funcionalidad 9): siempre visible a la derecha de MainView, independiente
// de las pestañas. Muestra, solo de forma informativa, las reservas que OTROS usuarios crean
// durante esta sesión. "Ver" abre el detalle; "Descartar" la quita de la lista SIN avisar al Backend.
public class AlertasPanel extends JPanel {

    private static final int COL_ACTIVIDAD = 0;
    private static final int COL_VER = 1;
    private static final int COL_DESCARTAR = 2;
    private static final String ICONO_VER = "📂";
    private static final String ICONO_DESCARTAR = "⛔";

    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final List<Reserva> alertas = new ArrayList<>(); // misma posición que las filas de la tabla

    public AlertasPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Alertas"));
        setPreferredSize(new Dimension(300, 0));

        modelo = new DefaultTableModel(new Object[]{"Actividad", "Ver", "Desc."}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(26);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setBackground(new Color(66, 133, 200));
        tabla.getTableHeader().setForeground(Color.WHITE);

        // Columnas de íconos angostas y centradas
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        for (int col : new int[]{COL_VER, COL_DESCARTAR}) {
            tabla.getColumnModel().getColumn(col).setCellRenderer(centrado);
            tabla.getColumnModel().getColumn(col).setMaxWidth(45);
            tabla.getColumnModel().getColumn(col).setPreferredWidth(45);
        }

        // Clic en el ícono "Ver" o "Descartar"; doble clic en la actividad también abre el detalle
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tabla.rowAtPoint(e.getPoint());
                int columna = tabla.columnAtPoint(e.getPoint());
                if (fila < 0) {
                    return;
                }
                if (columna == COL_VER || (columna == COL_ACTIVIDAD && e.getClickCount() == 2)) {
                    ver(fila);
                } else if (columna == COL_DESCARTAR) {
                    descartar(fila);
                }
            }
        });
        tabla.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel nota = new JLabel("<html>Reservas que otros usuarios crean durante esta sesión.</html>");
        nota.setFont(nota.getFont().deriveFont(11f));
        add(nota, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /** Lo llama el hilo que escucha el socket de alertas: pasa el trabajo al hilo de Swing. */
    public void agregarDesdeOtroHilo(Reserva reserva) {
        SwingUtilities.invokeLater(() -> agregar(reserva));
    }

    /** Debe llamarse en el hilo de Swing. La más reciente queda de primera. */
    public void agregar(Reserva reserva) {
        alertas.add(0, reserva);
        modelo.insertRow(0, new Object[]{textoActividad(reserva), ICONO_VER, ICONO_DESCARTAR});
    }

    public int cantidadAlertas() {
        return alertas.size();
    }

    private void ver(int fila) {
        Reserva r = alertas.get(fila);
        SimpleDateFormat sdfFecha = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm");

        JPanel detalle = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 10);
        c.anchor = GridBagConstraints.NORTHWEST;

        agregarFila(detalle, c, 0, "Encargado:", new JLabel(nombreFuncionario(r)));
        agregarFila(detalle, c, 1, "Fecha:", new JLabel(r.getFecha() == null ? "" : sdfFecha.format(r.getFecha())));
        agregarFila(detalle, c, 2, "Desde:", new JLabel(r.getHoraInicio() == null ? "" : sdfHora.format(r.getHoraInicio())));
        agregarFila(detalle, c, 3, "Hasta:", new JLabel(r.getHoraFin() == null ? "" : sdfHora.format(r.getHoraFin())));

        DefaultListModel<String> recursos = new DefaultListModel<>();
        for (DetalleReserva d : r.getDetalles()) {
            if (d.getRecursoAsignado() != null) {
                String desc = d.getRecursoAsignado().getDescripcion();
                recursos.addElement(desc != null ? desc : d.getRecursoAsignado().getId());
            }
        }
        JScrollPane listaRecursos = new JScrollPane(new JList<>(recursos));
        listaRecursos.setPreferredSize(new Dimension(200, 90));
        agregarFila(detalle, c, 4, "Recursos:", listaRecursos);

        JOptionPane.showMessageDialog(this, detalle, r.getActividad(), JOptionPane.PLAIN_MESSAGE);
    }

    private void descartar(int fila) {
        Reserva r = alertas.get(fila);
        int opcion = JOptionPane.showConfirmDialog(this, r.getActividad(), "¿Descartar?",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opcion == JOptionPane.YES_OPTION) {
            // Solo se quita de esta lista: NO se manda nada al Backend (la reserva sigue existiendo)
            alertas.remove(fila);
            modelo.removeRow(fila);
        }
    }

    private void agregarFila(JPanel panel, GridBagConstraints c, int fila, String etiqueta, JComponent valor) {
        c.gridy = fila;
        c.gridx = 0;
        panel.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        panel.add(valor, c);
    }

    private String textoActividad(Reserva r) {
        return nombreFuncionario(r) + " - " + r.getActividad();
    }

    private String nombreFuncionario(Reserva r) {
        if (r.getFuncionario() == null) {
            return "?";
        }
        String nombre = r.getFuncionario().getNombre();
        return nombre != null ? nombre : r.getFuncionario().getId();
    }
}
