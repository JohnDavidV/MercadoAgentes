package mercado.agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.SwingUtilities;
import mercado.gui.CompradorGui;
import mercado.modelo.Oferta;
import mercado.modelo.Pedido;
import mercado.util.Formato;

/**
 * Agente comprador. Busca vendedores en el DF, les envía un CFP con el pedido
 * y elige la oferta más barata que no supere su precio máximo (FIPA Contract
 * Net del lado del iniciador). Si la tienda ganadora falla al confirmar,
 * vuelve a negociar sin esa tienda.
 *
 * @author John David Vásquez Serna
 */
public class CompradorAgent extends Agent {

    private static final long TIEMPO_RESPUESTA = 3000;
    private static final int MAX_INTENTOS = 3;

    private CompradorGui gui;
    private double gastado = 0;

    @Override
    protected void setup() {
        gui = CompradorGui.crear(this);
        log("Listo para comprar. Escribe un pedido y presiona Comprar.");
    }

    @Override
    protected void takeDown() {
        if (gui != null) {
            gui.cerrar();
        }
        System.out.println(getLocalName() + " cerró, gastó " + Formato.pesos(gastado));
    }

    /** Lo llama la interfaz gráfica cuando el usuario presiona Comprar. */
    public void comprar(final Pedido pedido) {
        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                negociar(pedido, new HashSet<String>(), 1);
            }
        });
    }

    private void negociar(Pedido pedido, Set<String> excluidos, int intento) {
        List<AID> vendedores = buscarVendedores(excluidos);
        if (vendedores.isEmpty()) {
            log("No hay vendedores disponibles en el DF.");
            terminar();
            return;
        }

        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        for (AID v : vendedores) {
            cfp.addReceiver(v);
        }
        cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        cfp.setReplyByDate(new Date(System.currentTimeMillis() + TIEMPO_RESPUESTA));
        cfp.setContent(pedido.aContenido());

        log("Intento " + intento + ": CFP a " + vendedores.size() + " tiendas, "
                + pedido.getCantidad() + " " + pedido.getProducto() + " hasta "
                + Formato.pesos(pedido.getPrecioMaximo()) + " c/u");
        limpiarOfertas();
        addBehaviour(new Negociacion(this, cfp, pedido, excluidos, intento));
    }

    /** Busca en las páginas amarillas a todos los que ofrecen el servicio de venta. */
    private List<AID> buscarVendedores(Set<String> excluidos) {
        List<AID> lista = new ArrayList<AID>();
        DFAgentDescription plantilla = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(VendedorAgent.TIPO_SERVICIO);
        plantilla.addServices(sd);
        try {
            for (DFAgentDescription d : DFService.search(this, plantilla)) {
                if (!excluidos.contains(d.getName().getLocalName())) {
                    lista.add(d.getName());
                }
            }
        } catch (FIPAException e) {
            log("Error consultando el DF: " + e.getMessage());
        }
        return lista;
    }

    // Comportamiento del protocolo Contract Net del lado del iniciador
    private class Negociacion extends ContractNetInitiator {

        private final Pedido pedido;
        private final Set<String> excluidos;
        private final int intento;
        private String ganador;

        Negociacion(Agent a, ACLMessage cfp, Pedido pedido, Set<String> excluidos, int intento) {
            super(a, cfp);
            this.pedido = pedido;
            this.excluidos = excluidos;
            this.intento = intento;
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected void handlePropose(ACLMessage propose, Vector acceptances) {
            Oferta o = Oferta.desdeContenido(propose.getContent());
            String estado = o.getPrecioUnitario() <= pedido.getPrecioMaximo() ? "En evaluación" : "Muy cara";
            agregarOferta(propose.getSender().getLocalName(), o, estado);
        }

        @Override
        protected void handleRefuse(ACLMessage refuse) {
            agregarRechazo(refuse.getSender().getLocalName(), motivo(refuse.getContent()));
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            ACLMessage mejor = null;
            double mejorPrecio = Double.MAX_VALUE;
            int propuestas = 0;

            for (Object o : responses) {
                ACLMessage r = (ACLMessage) o;
                if (r.getPerformative() == ACLMessage.PROPOSE) {
                    propuestas++;
                    double precio = Oferta.desdeContenido(r.getContent()).getPrecioUnitario();
                    if (precio <= pedido.getPrecioMaximo() && precio < mejorPrecio) {
                        mejorPrecio = precio;
                        mejor = r;
                    }
                }
            }

            // A todas las propuestas se les responde: aceptar la mejor y rechazar el resto
            for (Object o : responses) {
                ACLMessage r = (ACLMessage) o;
                if (r.getPerformative() != ACLMessage.PROPOSE) {
                    continue;
                }
                ACLMessage respuesta = r.createReply();
                if (r == mejor) {
                    respuesta.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    ganador = r.getSender().getLocalName();
                    marcarOferta(ganador, "Aceptada");
                } else {
                    respuesta.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    marcarOferta(r.getSender().getLocalName(), "Rechazada");
                }
                acceptances.add(respuesta);
            }

            if (mejor == null) {
                log(propuestas == 0
                        ? "Ninguna tienda envió propuesta para este pedido."
                        : "Ninguna propuesta cumple el precio máximo.");
                terminar();
            } else {
                log("Mejor oferta: " + ganador + " a " + Formato.pesos(mejorPrecio) + " c/u");
            }
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            Oferta o = Oferta.desdeContenido(inform.getContent());
            gastado += o.getTotal();
            log("COMPRA CONFIRMADA por " + ganador + ": " + Formato.pesos(o.getTotal()));
            marcarOferta(ganador, "Comprada");
            agregarCompra(pedido, ganador, o);
            terminar();
        }

        @Override
        protected void handleFailure(ACLMessage failure) {
            if (failure.getSender().equals(myAgent.getAMS())) {
                // el AMS avisa cuando un receptor no existe
                return;
            }
            String tienda = failure.getSender().getLocalName();
            marcarOferta(tienda, "Falló");
            log(tienda + " no pudo cumplir (" + motivo(failure.getContent()) + ")");
            if (intento < MAX_INTENTOS) {
                excluidos.add(tienda);
                log("Vuelvo a negociar sin " + tienda + "...");
                negociar(pedido, excluidos, intento + 1);
            } else {
                log("Se agotaron los intentos, no se pudo comprar.");
                terminar();
            }
        }
    }

    private static String motivo(String contenido) {
        if (contenido == null) {
            return "sin motivo";
        }
        // JADE envía el motivo con el formato ((motivo)) o "motivo"
        return contenido.replaceAll("[()\"]", "").replace('-', ' ').trim();
    }

    public double getGastado() {
        return gastado;
    }

    // Métodos para actualizar la interfaz desde el hilo del agente

    void log(final String texto) {
        System.out.println("[" + getLocalName() + "] " + Formato.sinTildes(texto));
        enGui(new Runnable() {
            @Override
            public void run() {
                gui.agregarLog("[" + Formato.hora() + "] " + texto);
            }
        });
    }

    private void limpiarOfertas() {
        enGui(new Runnable() {
            @Override
            public void run() {
                gui.limpiarOfertas();
            }
        });
    }

    private void agregarOferta(final String tienda, final Oferta o, final String estado) {
        enGui(new Runnable() {
            @Override
            public void run() {
                gui.agregarOferta(tienda, Formato.pesos(o.getPrecioUnitario()),
                        o.getDescuento() + " %", Formato.pesos(o.getTotal()), estado);
            }
        });
    }

    private void agregarRechazo(final String tienda, final String motivo) {
        enGui(new Runnable() {
            @Override
            public void run() {
                gui.agregarOferta(tienda, "", "", "", "No ofertó: " + motivo);
            }
        });
    }

    private void marcarOferta(final String tienda, final String estado) {
        enGui(new Runnable() {
            @Override
            public void run() {
                gui.marcarOferta(tienda, estado);
            }
        });
    }

    private void agregarCompra(final Pedido p, final String tienda, final Oferta o) {
        enGui(new Runnable() {
            @Override
            public void run() {
                gui.agregarCompra(Formato.hora(), p.getProducto(), p.getCantidad(), tienda,
                        Formato.pesos(o.getTotal()));
            }
        });
    }

    private void terminar() {
        enGui(new Runnable() {
            @Override
            public void run() {
                gui.negociacionTerminada();
            }
        });
    }

    private void enGui(Runnable r) {
        if (gui != null) {
            SwingUtilities.invokeLater(r);
        }
    }
}
