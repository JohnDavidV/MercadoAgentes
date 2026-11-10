package mercado.modelo;

/**
 * Cómo calcula una tienda el precio que ofrece: descuento por volumen
 * (5 % desde 5 unidades, 10 % desde 10) y 3 % extra si tiene mucho
 * inventario del producto, para rotarlo.
 *
 * @author John David Vásquez Serna
 */
public final class EstrategiaPrecios {

    public static final int VOLUMEN_MEDIO = 5;
    public static final int VOLUMEN_ALTO = 10;
    public static final int STOCK_ALTO = 15;

    private EstrategiaPrecios() {
    }

    public static int descuento(int cantidad, int stock) {
        int descuento = 0;
        if (cantidad >= VOLUMEN_ALTO) {
            descuento = 10;
        } else if (cantidad >= VOLUMEN_MEDIO) {
            descuento = 5;
        }
        if (stock >= STOCK_ALTO) {
            descuento += 3;
        }
        return descuento;
    }

    public static Oferta calcular(Producto p, int cantidad) {
        int descuento = descuento(cantidad, p.getStock());
        double precio = p.getPrecio() * (100 - descuento) / 100.0;
        return new Oferta(precio, cantidad, descuento);
    }
}
