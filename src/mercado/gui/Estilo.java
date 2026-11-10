package mercado.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Colores, fuentes y ubicación de las ventanas para que todas se vean iguales.
 *
 * @author John David Vásquez Serna
 */
public final class Estilo {

    public static final Color AZUL = new Color(0, 51, 102);
    public static final Color VERDE = new Color(46, 125, 50);
    public static final Color ROJO = new Color(198, 40, 40);
    public static final Color NARANJA = new Color(239, 108, 0);
    public static final Color GRIS = new Color(110, 110, 110);
    public static final Font TITULO = new Font("SansSerif", Font.BOLD, 15);
    public static final Font LOG = new Font("Monospaced", Font.PLAIN, 12);

    private static int vendedores = 0;
    private static int compradores = 0;

    private Estilo() {
    }

    /** Usa Nimbus si está disponible, para que se vea igual en Windows y Linux. */
    public static void aplicarLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception e) {
            // si falla se queda el look and feel por defecto
        }
    }

    public static Border borde(String titulo) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(titulo),
                BorderFactory.createEmptyBorder(4, 6, 6, 6));
    }

    public static void titulo(JLabel label) {
        label.setFont(TITULO);
        label.setForeground(AZUL);
    }

    /** Posición en cascada: vendedores a la izquierda, compradores a la derecha. */
    public static synchronized int[] posicionVendedor() {
        int i = vendedores++;
        return new int[]{10, 10 + i * 290};
    }

    public static synchronized int[] posicionComprador() {
        int i = compradores++;
        return new int[]{950 + i * 40, 10 + i * 40};
    }

    /** Colorea la columna de estado de la tabla de ofertas. */
    public static class EstadoRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                boolean foco, int fila, int col) {
            JComponent c = (JComponent) super.getTableCellRendererComponent(t, valor, sel, foco, fila, col);
            String texto = valor == null ? "" : valor.toString();
            if (!sel) {
                if (texto.startsWith("Comprada") || texto.startsWith("Aceptada")) {
                    c.setForeground(VERDE);
                } else if (texto.startsWith("Falló") || texto.startsWith("No ofertó")) {
                    c.setForeground(ROJO);
                } else if (texto.startsWith("Muy cara") || texto.startsWith("Rechazada")) {
                    c.setForeground(NARANJA);
                } else {
                    c.setForeground(GRIS);
                }
            }
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            return c;
        }
    }
}
