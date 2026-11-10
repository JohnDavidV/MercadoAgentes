package mercado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** El contenido de los mensajes ACL debe poder ir y volver sin perder datos. */
class MensajesTest {

    @Test
    void pedidoIdaYVuelta() {
        Pedido p = new Pedido("Monitor", 2, 700000);
        Pedido copia = Pedido.desdeContenido(p.aContenido());
        assertEquals("Monitor", copia.getProducto());
        assertEquals(2, copia.getCantidad());
        assertEquals(700000, copia.getPrecioMaximo(), 0.001);
    }

    @Test
    void ofertaIdaYVuelta() {
        Oferta o = new Oferta(39150, 12, 13);
        Oferta copia = Oferta.desdeContenido(o.aContenido());
        assertEquals(39150, copia.getPrecioUnitario(), 0.001);
        assertEquals(12, copia.getCantidad());
        assertEquals(13, copia.getDescuento());
    }
}
