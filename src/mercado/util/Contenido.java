package mercado.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Formato sencillo para el contenido de los mensajes ACL: pares clave=valor
 * separados por punto y coma, por ejemplo "producto=Mouse;cantidad=2".
 * Para un proyecto más grande lo ideal sería usar una ontología de JADE.
 *
 * @author John David Vásquez Serna
 */
public final class Contenido {

    private final StringBuilder sb = new StringBuilder();

    private Contenido() {
    }

    public static Contenido crear() {
        return new Contenido();
    }

    public Contenido con(String clave, Object valor) {
        if (sb.length() > 0) {
            sb.append(';');
        }
        sb.append(clave).append('=').append(valor);
        return this;
    }

    public String texto() {
        return sb.toString();
    }

    public static Map<String, String> leer(String texto) {
        Map<String, String> datos = new LinkedHashMap<String, String>();
        if (texto == null) {
            return datos;
        }
        for (String par : texto.split(";")) {
            int i = par.indexOf('=');
            if (i > 0) {
                datos.put(par.substring(0, i).trim(), par.substring(i + 1).trim());
            }
        }
        return datos;
    }
}
