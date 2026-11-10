package mercado.modelo;

import java.util.Map;
import mercado.util.Contenido;

/**
 * Propuesta que un vendedor le hace al comprador. Viaja en el mensaje PROPOSE.
 *
 * @author John David Vásquez Serna
 */
public class Oferta {

    private final double precioUnitario;
    private final int cantidad;
    private final int descuento;

    public Oferta(double precioUnitario, int cantidad, int descuento) {
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.descuento = descuento;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public int getCantidad() {
        return cantidad;
    }

    /** Porcentaje de descuento que aplicó el vendedor. */
    public int getDescuento() {
        return descuento;
    }

    public double getTotal() {
        return precioUnitario * cantidad;
    }

    public String aContenido() {
        return Contenido.crear()
                .con("precioUnitario", precioUnitario)
                .con("cantidad", cantidad)
                .con("descuento", descuento)
                .texto();
    }

    public static Oferta desdeContenido(String texto) {
        Map<String, String> datos = Contenido.leer(texto);
        return new Oferta(Double.parseDouble(datos.get("precioUnitario")),
                Integer.parseInt(datos.get("cantidad")),
                Integer.parseInt(datos.get("descuento")));
    }
}
