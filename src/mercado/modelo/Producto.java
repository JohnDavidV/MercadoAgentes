package mercado.modelo;

/**
 * Producto del catálogo de un vendedor.
 *
 * @author John David Vásquez Serna
 */
public class Producto {

    private final String nombre;
    private double precio;
    private int stock;

    public Producto(String nombre, double precio, int stock) {
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getStock() {
        return stock;
    }

    public void agregarStock(int cantidad) {
        stock += cantidad;
    }

    /**
     * Descuenta unidades del inventario si hay suficientes.
     *
     * @return true si se pudo descontar
     */
    public boolean descontar(int cantidad) {
        if (cantidad <= 0 || cantidad > stock) {
            return false;
        }
        stock -= cantidad;
        return true;
    }
}
