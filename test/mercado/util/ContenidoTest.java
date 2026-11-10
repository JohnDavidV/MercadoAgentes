package mercado.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ContenidoTest {

    @Test
    void creaYLeePares() {
        String texto = Contenido.crear().con("producto", "Mouse").con("cantidad", 3).texto();
        assertEquals("producto=Mouse;cantidad=3", texto);
        Map<String, String> datos = Contenido.leer(texto);
        assertEquals("Mouse", datos.get("producto"));
        assertEquals("3", datos.get("cantidad"));
    }

    @Test
    void ignoraTextoVacioOMalFormado() {
        assertTrue(Contenido.leer(null).isEmpty());
        assertTrue(Contenido.leer("sin-igual;=vacio").isEmpty());
    }

    @Test
    void formatoDePesos() {
        assertEquals("$ 2.790.000", Formato.pesos(2790000));
    }

    @Test
    void quitaTildes() {
        assertEquals("Portatil electronico", Formato.sinTildes("Portátil electrónico"));
    }
}
