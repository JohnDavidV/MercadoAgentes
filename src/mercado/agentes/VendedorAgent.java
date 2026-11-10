package mercado.agentes;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SSContractNetResponder;
import jade.proto.SSResponderDispatcher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import mercado.gui.VendedorGui;
import mercado.modelo.Oferta;
import mercado.modelo.Pedido;
import mercado.modelo.Producto;
import mercado.util.Catalogos;
import mercado.util.Formato;

/**
 * Agente vendedor. Se registra en las páginas amarillas (DF) y participa en el
 * protocolo FIPA Contract Net: responde a cada CFP con una propuesta o con un
 * rechazo y, si su propuesta gana, confirma la venta descontando el stock.
 *
 * Argumento opcional: nombre del catálogo (norte, centro o sur).
 *
 * @author John David Vásquez Serna
 */
public class VendedorAgent extends Agent {

    public static final String TIPO_SERVICIO = "mercado-venta";

    private final Map<String, Producto> catalogo = new LinkedHashMap<String, Producto>();
    private VendedorGui gui;
    private int ventas = 0;
    private double ingresos = 0;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        String tienda = (args != null && args.length > 0) ? args[0].toString() : "norte";
        for (Producto p : Catalogos.crear(tienda)) {
            catalogo.put(p.getNombre().toLowerCase(), p);
        }

        registrarEnDF();

        gui = VendedorGui.crear(this);
        log("Tienda abierta con " + catalogo.size() + " productos");

        MessageTemplate plantilla = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));
        // El dispatcher crea un responder por cada CFP, así la tienda puede
        // negociar con varios compradores al mismo tiempo
        addBehaviour(new SSResponderDispatcher(this, plantilla) {
            @Override
            protected Behaviour createResponder(ACLMessage cfp) {
                return new ResponderContractNet(myAgent, cfp);
            }
        });
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            // si la plataforma se está cerrando el DF puede no estar disponible
        }
        if (gui != null) {
            gui.cerrar();
        }
        System.out.println(getLocalName() + " cerró con " + ventas + " ventas");
    }

    private void registrarEnDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(TIPO_SERVICIO);
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " no se pudo registrar en el DF: " + e.getMessage());
            doDelete();
        }
    }

    /**
     * Estrategia de precio: descuento por volumen y un descuento extra si
     * hay mucho inventario del producto.
     */
    Oferta calcularOferta(Producto p, int cantidad) {
        int descuento = 0;
        if (cantidad >= 10) {
            descuento = 10;
        } else if (cantidad >= 5) {
            descuento = 5;
        }
        if (p.getStock() >= 15) {
            descuento += 3;
        }
        double precio = p.getPrecio() * (100 - descuento) / 100.0;
        return new Oferta(precio, cantidad, descuento);
    }

    // Comportamiento del protocolo Contract Net del lado del participante (una sesión)
    private class ResponderContractNet extends SSContractNetResponder {

        ResponderContractNet(Agent a, ACLMessage cfp) {
            super(a, cfp);
        }

        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, NotUnderstoodException {
            Pedido pedido;
            try {
                pedido = Pedido.desdeContenido(cfp.getContent());
            } catch (RuntimeException e) {
                throw new NotUnderstoodException("formato-invalido");
            }
            String comprador = cfp.getSender().getLocalName();
            Producto p = catalogo.get(pedido.getProducto().toLowerCase());

            if (p == null) {
                log(comprador + " pide " + pedido.getProducto() + ": no lo vendo");
                throw new RefuseException("producto-no-disponible");
            }
            if (p.getStock() < pedido.getCantidad()) {
                log(comprador + " pide " + pedido.getCantidad() + " " + p.getNombre()
                        + ": solo hay " + p.getStock());
                throw new RefuseException("stock-insuficiente");
            }

            Oferta oferta = calcularOferta(p, pedido.getCantidad());
            ACLMessage propuesta = cfp.createReply();
            propuesta.setPerformative(ACLMessage.PROPOSE);
            propuesta.setContent(oferta.aContenido());
            log("Propuesta a " + comprador + ": " + pedido.getCantidad() + " " + p.getNombre()
                    + " a " + Formato.pesos(oferta.getPrecioUnitario())
                    + (oferta.getDescuento() > 0 ? " (-" + oferta.getDescuento() + " %)" : ""));
            return propuesta;
        }

        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept)
                throws FailureException {
            Pedido pedido = Pedido.desdeContenido(cfp.getContent());
            Oferta oferta = Oferta.desdeContenido(propose.getContent());
            Producto p = catalogo.get(pedido.getProducto().toLowerCase());
            String comprador = accept.getSender().getLocalName();

            // Entre la propuesta y la aceptación otro comprador pudo llevarse el stock
            boolean vendido;
            synchronized (catalogo) {
                vendido = p != null && p.descontar(pedido.getCantidad());
            }
            if (!vendido) {
                log("No pude cumplir a " + comprador + ", se acabó el stock de " + pedido.getProducto());
                throw new FailureException("stock-agotado");
            }
            ventas++;
            ingresos += oferta.getTotal();
            log("VENTA a " + comprador + ": " + pedido.getCantidad() + " " + p.getNombre()
                    + " por " + Formato.pesos(oferta.getTotal()));
            actualizarGui();

            ACLMessage inform = accept.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            inform.setContent(oferta.aContenido());
            return inform;
        }

        @Override
        protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
            log(reject.getSender().getLocalName() + " rechazó mi propuesta");
        }
    }

    // Métodos que usa la interfaz gráfica

    /** Copia del catálogo para mostrarla en la tabla: nombre, precio y stock. */
    public List<Object[]> filasCatalogo() {
        List<Object[]> filas = new ArrayList<Object[]>();
        synchronized (catalogo) {
            for (Producto p : catalogo.values()) {
                filas.add(new Object[]{p.getNombre(), Formato.pesos(p.getPrecio()), p.getStock()});
            }
        }
        return filas;
    }

    public int getVentas() {
        return ventas;
    }

    public double getIngresos() {
        return ingresos;
    }

    /** Lo llama la interfaz al agregar o reponer un producto. */
    public void agregarProducto(final String nombre, final double precio, final int cantidad) {
        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                Producto p;
                synchronized (catalogo) {
                    p = catalogo.get(nombre.toLowerCase());
                    if (p == null) {
                        p = new Producto(nombre, precio, cantidad);
                        catalogo.put(nombre.toLowerCase(), p);
                    } else {
                        p.setPrecio(precio);
                        p.agregarStock(cantidad);
                    }
                }
                log(p.getNombre() + ": precio " + Formato.pesos(precio) + ", stock " + p.getStock());
                actualizarGui();
            }
        });
    }

    void log(final String texto) {
        System.out.println("[" + getLocalName() + "] " + Formato.sinTildes(texto));
        if (gui != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    gui.agregarLog("[" + Formato.hora() + "] " + texto);
                }
            });
        }
    }

    private void actualizarGui() {
        if (gui != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    gui.refrescar();
                }
            });
        }
    }
}
