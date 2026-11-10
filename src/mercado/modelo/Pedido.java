package mercado.modelo;

import java.util.Map;
import mercado.util.Contenido;

/**
 * Lo que el comprador quiere: producto, cantidad y precio máximo por unidad.
 * Viaja en el contenido del mensaje CFP.
 *
 * @author John David Vásquez Serna
 */
public class Pedido {

    private final String producto;
    private final int cantidad;
    private final double precioMaximo;

    public Pedido(String producto, int cantidad, double precioMaximo) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioMaximo = precioMaximo;
    }

    public String getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecioMaximo() {
        return precioMaximo;
    }

    public String aContenido() {
        return Contenido.crear()
                .con("producto", producto)
                .con("cantidad", cantidad)
                .con("precioMaximo", precioMaximo)
                .texto();
    }

    public static Pedido desdeContenido(String texto) {
        Map<String, String> datos = Contenido.leer(texto);
        return new Pedido(datos.get("producto"),
                Integer.parseInt(datos.get("cantidad")),
                Double.parseDouble(datos.get("precioMaximo")));
    }
}
