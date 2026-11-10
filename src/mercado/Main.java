package mercado;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;
import mercado.agentes.CompradorAgent;
import mercado.agentes.VendedorAgent;
import mercado.gui.Estilo;

/**
 * Punto de entrada. Arranca JADE desde código (sin usar jade.Boot), crea el
 * Main-Container con el RMA y lanza tres tiendas y dos compradores.
 *
 * Uso: java -cp "lib/jade.jar;build/classes" mercado.Main [puerto]
 *
 * @author John David Vásquez Serna
 */
public class Main {

    public static void main(String[] args) throws StaleProxyException {
        Estilo.aplicarLookAndFeel();

        String puerto = args.length > 0 ? args[0] : "1099";

        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        Profile perfil = new ProfileImpl();
        perfil.setParameter(Profile.MAIN_PORT, puerto);
        perfil.setParameter(Profile.LOCAL_PORT, puerto);
        perfil.setParameter(Profile.GUI, "true");
        perfil.setParameter(Profile.PLATFORM_ID, "Mercado");

        AgentContainer main = rt.createMainContainer(perfil);
        if (main == null) {
            System.err.println("No se pudo crear la plataforma. Revisa que el puerto " + puerto
                    + " no esté ocupado por otra plataforma JADE.");
            System.exit(1);
        }

        main.createNewAgent("TiendaNorte", VendedorAgent.class.getName(), new Object[]{"norte"}).start();
        main.createNewAgent("TiendaCentro", VendedorAgent.class.getName(), new Object[]{"centro"}).start();
        main.createNewAgent("TiendaSur", VendedorAgent.class.getName(), new Object[]{"sur"}).start();

        main.createNewAgent("Ana", CompradorAgent.class.getName(), null).start();
        main.createNewAgent("Luis", CompradorAgent.class.getName(), null).start();
    }
}
