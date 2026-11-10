package mercado.util;

import java.util.ArrayList;
import java.util.List;
import mercado.modelo.Producto;

/**
 * Catálogos de ejemplo para cada tienda. Los precios y el stock son distintos
 * a propósito, para que la negociación tenga sentido.
 *
 * @author John David Vásquez Serna
 */
public final class Catalogos {

    private Catalogos() {
    }

    public static List<Producto> crear(String tienda) {
        List<Producto> lista = new ArrayList<Producto>();
        if ("norte".equalsIgnoreCase(tienda)) {
            lista.add(new Producto("Portatil", 2850000, 3));
            lista.add(new Producto("Monitor", 690000, 5));
            lista.add(new Producto("Mouse", 45000, 20));
            lista.add(new Producto("Teclado", 120000, 8));
        } else if ("centro".equalsIgnoreCase(tienda)) {
            lista.add(new Producto("Portatil", 2790000, 1));
            lista.add(new Producto("Monitor", 720000, 10));
            lista.add(new Producto("Mouse", 39000, 6));
            lista.add(new Producto("Audifonos", 180000, 4));
        } else if ("sur".equalsIgnoreCase(tienda)) {
            lista.add(new Producto("Monitor", 650000, 2));
            lista.add(new Producto("Teclado", 135000, 15));
            lista.add(new Producto("Audifonos", 165000, 10));
            lista.add(new Producto("Mouse", 52000, 30));
        }
        return lista;
    }
}
