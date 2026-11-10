package mercado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EstrategiaPreciosTest {

    @Test
    void sinDescuentoParaPocasUnidades() {
        Oferta o = EstrategiaPrecios.calcular(new Producto("Monitor", 700000, 5), 2);
        assertEquals(0, o.getDescuento());
        assertEquals(700000, o.getPrecioUnitario(), 0.001);
        assertEquals(1400000, o.getTotal(), 0.001);
    }

    @Test
    void descuentoPorVolumen() {
        assertEquals(5, EstrategiaPrecios.descuento(5, 10));
        assertEquals(5, EstrategiaPrecios.descuento(9, 10));
        assertEquals(10, EstrategiaPrecios.descuento(10, 14));
    }

    @Test
    void descuentoExtraConMuchoInventario() {
        assertEquals(3, EstrategiaPrecios.descuento(1, 15));
        assertEquals(13, EstrategiaPrecios.descuento(12, 20));
    }

    @Test
    void precioConDescuentoAplicado() {
        // 12 mouse con 20 en stock: 10 % por volumen + 3 % por inventario
        Oferta o = EstrategiaPrecios.calcular(new Producto("Mouse", 45000, 20), 12);
        assertEquals(13, o.getDescuento());
        assertEquals(39150, o.getPrecioUnitario(), 0.001);
        assertEquals(469800, o.getTotal(), 0.001);
    }
}
