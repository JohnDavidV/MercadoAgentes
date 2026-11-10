package mercado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProductoTest {

    @Test
    void descontarConStockSuficiente() {
        Producto p = new Producto("Teclado", 120000, 8);
        assertTrue(p.descontar(3));
        assertEquals(5, p.getStock());
    }

    @Test
    void noDescuentaSiNoAlcanza() {
        Producto p = new Producto("Portatil", 2790000, 1);
        assertFalse(p.descontar(2));
        assertEquals(1, p.getStock());
    }

    @Test
    void noAceptaCantidadesInvalidas() {
        Producto p = new Producto("Mouse", 45000, 10);
        assertFalse(p.descontar(0));
        assertFalse(p.descontar(-1));
        assertEquals(10, p.getStock());
    }
}
