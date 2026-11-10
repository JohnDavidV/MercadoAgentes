package mercado.util;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utilidades para mostrar precios en pesos colombianos y la hora en los logs.
 *
 * @author John David Vásquez Serna
 */
public final class Formato {

    private static final Locale COLOMBIA = new Locale("es", "CO");

    private Formato() {
    }

    public static String pesos(double valor) {
        NumberFormat nf = NumberFormat.getIntegerInstance(COLOMBIA);
        return "$ " + nf.format(Math.round(valor));
    }

    /** Quita tildes para la consola, que en Windows no siempre muestra bien UTF-8. */
    public static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public static String hora() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }
}
