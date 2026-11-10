package mercado.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import mercado.agentes.CompradorAgent;
import mercado.modelo.Pedido;
import mercado.util.Formato;

/**
 * Ventana del comprador: formulario del pedido, ofertas recibidas en la
 * negociación actual, historial de compras y actividad del agente.
 *
 * @author John David Vásquez Serna
 */
public final class CompradorGui extends JFrame {

    private static final String[] PRODUCTOS = {"Portatil", "Monitor", "Mouse", "Teclado", "Audifonos", "Tablet"};

    private final CompradorAgent agente;
    private final JComboBox<String> cbProducto = new JComboBox<String>(PRODUCTOS);
    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JTextField txtPrecioMax = new JTextField("700000", 8);
    private final JButton btnComprar = new JButton("Comprar");
    private final DefaultTableModel ofertas = modelo("Tienda", "Precio unitario", "Descuento", "Total", "Estado");
    private final DefaultTableModel compras = modelo("Hora", "Producto", "Cantidad", "Tienda", "Total");
    private final JLabel lblGastado = new JLabel();
    private final JTextArea log = new JTextArea();

    private CompradorGui(CompradorAgent agente) {
        super("Comprador " + agente.getLocalName());
        this.agente = agente;
        cbProducto.setEditable(true);
        cbProducto.setSelectedItem("Monitor");

        JLabel titulo = new JLabel(agente.getLocalName());
        Estilo.titulo(titulo);
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
        cabecera.add(titulo, BorderLayout.WEST);
        cabecera.add(lblGastado, BorderLayout.EAST);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        form.setBorder(Estilo.borde("Nuevo pedido"));
        form.add(new JLabel("Producto"));
        form.add(cbProducto);
        form.add(new JLabel("Cantidad"));
        form.add(spCantidad);
        form.add(new JLabel("Precio máximo c/u"));
        form.add(txtPrecioMax);
        form.add(btnComprar);
        btnComprar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                comprar();
            }
        });

        JTable tablaOfertas = tabla(ofertas);
        tablaOfertas.getColumnModel().getColumn(4).setCellRenderer(new Estilo.EstadoRenderer());
        tablaOfertas.getColumnModel().getColumn(4).setPreferredWidth(230);
        JScrollPane spOfertas = new JScrollPane(tablaOfertas);
        spOfertas.setBorder(Estilo.borde("Ofertas de la negociación actual"));

        JScrollPane spCompras = new JScrollPane(tabla(compras));
        spCompras.setBorder(Estilo.borde("Historial de compras"));

        log.setEditable(false);
        log.setFont(Estilo.LOG);
        log.setRows(6);
        JScrollPane spLog = new JScrollPane(log);
        spLog.setBorder(Estilo.borde("Actividad del agente"));

        JSplitPane tablas = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spOfertas, spCompras);
        tablas.setResizeWeight(0.5);
        JSplitPane centro = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablas, spLog);
        centro.setResizeWeight(0.65);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(cabecera, BorderLayout.NORTH);
        norte.add(form, BorderLayout.CENTER);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(norte, BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        setContentPane(raiz);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                CompradorGui.this.agente.doDelete();
            }
        });

        actualizarGastado();
        setPreferredSize(new Dimension(700, 600));
        pack();
        int[] pos = Estilo.posicionComprador();
        setLocation(pos[0], pos[1]);
    }

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int col) {
                return false;
            }
        };
    }

    private static JTable tabla(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(22);
        t.setFillsViewportHeight(true);
        return t;
    }

    private void comprar() {
        String producto = String.valueOf(cbProducto.getSelectedItem()).trim();
        double precioMax;
        try {
            precioMax = Double.parseDouble(txtPrecioMax.getText().trim().replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Escribe un precio máximo válido, por ejemplo 700000");
            return;
        }
        if (producto.isEmpty() || precioMax <= 0) {
            JOptionPane.showMessageDialog(this, "Falta el producto o el precio máximo");
            return;
        }
        btnComprar.setEnabled(false);
        agente.comprar(new Pedido(producto, (Integer) spCantidad.getValue(), precioMax));
    }

    public void agregarLog(String texto) {
        log.append(texto + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    public void limpiarOfertas() {
        ofertas.setRowCount(0);
    }

    public void agregarOferta(String tienda, String precio, String descuento, String total, String estado) {
        ofertas.addRow(new Object[]{tienda, precio, descuento, total, estado});
    }

    public void marcarOferta(String tienda, String estado) {
        for (int i = 0; i < ofertas.getRowCount(); i++) {
            if (tienda.equals(ofertas.getValueAt(i, 0))) {
                ofertas.setValueAt(estado, i, 4);
            }
        }
    }

    public void agregarCompra(String hora, String producto, int cantidad, String tienda, String total) {
        compras.addRow(new Object[]{hora, producto, cantidad, tienda, total});
        actualizarGastado();
    }

    public void negociacionTerminada() {
        btnComprar.setEnabled(true);
    }

    private void actualizarGastado() {
        lblGastado.setText("Total gastado: " + Formato.pesos(agente.getGastado()));
    }

    /**
     * Crea y muestra la ventana en el hilo de Swing. Los agentes arrancan en
     * paralelo y Swing no es seguro entre hilos, por eso se usa invokeAndWait.
     */
    public static CompradorGui crear(final CompradorAgent agente) {
        final CompradorGui[] ventana = new CompradorGui[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    ventana[0] = new CompradorGui(agente);
                    ventana[0].setVisible(true);
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear la ventana de " + agente.getLocalName(), e);
        }
        return ventana[0];
    }

    public void cerrar() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dispose();
            }
        });
    }
}
