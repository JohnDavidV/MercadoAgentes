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
import mercado.agentes.VendedorAgent;
import mercado.util.Formato;

/**
 * Ventana de una tienda: muestra el catálogo, las ventas y lo que va haciendo
 * el agente. También permite agregar o reponer productos.
 *
 * @author John David Vásquez Serna
 */
public final class VendedorGui extends JFrame {

    private final VendedorAgent agente;
    private final DefaultTableModel modelo;
    private final JTextArea log = new JTextArea();
    private final JLabel resumen = new JLabel();
    private final JTextField txtNombre = new JTextField(9);
    private final JTextField txtPrecio = new JTextField(7);
    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(5, 1, 999, 1));

    private VendedorGui(VendedorAgent agente) {
        super("Tienda " + agente.getLocalName());
        this.agente = agente;

        modelo = new DefaultTableModel(new Object[]{"Producto", "Precio", "Stock"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int col) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(22);
        tabla.setFillsViewportHeight(true);

        JLabel titulo = new JLabel(agente.getLocalName());
        Estilo.titulo(titulo);
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
        cabecera.add(titulo, BorderLayout.WEST);
        cabecera.add(resumen, BorderLayout.EAST);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        form.add(new JLabel("Producto"));
        form.add(txtNombre);
        form.add(new JLabel("Precio"));
        form.add(txtPrecio);
        form.add(new JLabel("Unidades"));
        form.add(spCantidad);
        JButton btnAgregar = new JButton("Agregar / reponer");
        form.add(btnAgregar);
        btnAgregar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                agregar();
            }
        });

        JPanel panelCatalogo = new JPanel(new BorderLayout());
        panelCatalogo.setBorder(Estilo.borde("Catálogo"));
        panelCatalogo.add(new JScrollPane(tabla), BorderLayout.CENTER);

        log.setEditable(false);
        log.setFont(Estilo.LOG);
        JScrollPane scrollLog = new JScrollPane(log);
        scrollLog.setBorder(Estilo.borde("Actividad del agente"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelCatalogo, scrollLog);
        split.setResizeWeight(0.5);
        split.setDividerLocation(430);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(cabecera, BorderLayout.NORTH);
        raiz.add(split, BorderLayout.CENTER);
        raiz.add(form, BorderLayout.SOUTH);
        setContentPane(raiz);

        // Cerrar la ventana elimina al agente de la plataforma
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                VendedorGui.this.agente.doDelete();
            }
        });

        setPreferredSize(new Dimension(930, 280));
        pack();
        int[] pos = Estilo.posicionVendedor();
        setLocation(pos[0], pos[1]);
        refrescar();
    }

    private void agregar() {
        String nombre = txtNombre.getText().trim();
        double precio;
        try {
            precio = Double.parseDouble(txtPrecio.getText().trim().replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Escribe un precio válido, por ejemplo 45000");
            return;
        }
        if (nombre.isEmpty() || precio <= 0) {
            JOptionPane.showMessageDialog(this, "Falta el nombre o el precio del producto");
            return;
        }
        agente.agregarProducto(nombre, precio, (Integer) spCantidad.getValue());
        txtNombre.setText("");
        txtPrecio.setText("");
    }

    public void refrescar() {
        modelo.setRowCount(0);
        for (Object[] fila : agente.filasCatalogo()) {
            modelo.addRow(fila);
        }
        resumen.setText("Ventas: " + agente.getVentas() + "   Ingresos: " + Formato.pesos(agente.getIngresos()));
    }

    public void agregarLog(String texto) {
        log.append(texto + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    /**
     * Crea y muestra la ventana en el hilo de Swing. Los agentes arrancan en
     * paralelo y Swing no es seguro entre hilos, por eso se usa invokeAndWait.
     */
    public static VendedorGui crear(final VendedorAgent agente) {
        final VendedorGui[] ventana = new VendedorGui[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    ventana[0] = new VendedorGui(agente);
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
