package chordHashing;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * gets necessary info to initialize the node and creates the ChordActor,
 * creates the http-server and exposes the endpoint for clients to query
 */
public class ChordInitActor extends AbstractActor {
    final Node seedNode;
    final Node currentNode;
    final int bitlen;
    ActorRef nodeActor;

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);


    private ChordInitActor(String seedIp, String seedPort, boolean clusterInit, Node currentNode, int bitlen){
        this.currentNode = currentNode;
        this.bitlen = bitlen;
        this.nodeActor = null;
        if (clusterInit){
            this.seedNode = null;
            log.info("cluster init");
            //if clusterInit the currentNode is its successor
           this.nodeActor = context().actorOf(ChordActor.props(this.currentNode, bitlen, currentNode),"node");
           RestApiServer.startServer(context().system(), this.nodeActor, currentNode.ip, Integer.parseInt(currentNode.port)+10000);
        } else{
            this.seedNode = new Node(seedIp, seedPort);
            init();
        }
    }

    public static Props  props(String seedIp, String seedPort, boolean clusterInit, Node currentNode, int bitlen){
        return Props.create(ChordInitActor.class, () -> new ChordInitActor(seedIp, seedPort, clusterInit, currentNode, bitlen));
    }

    private void init(){
        ActorSelection selection = getContext().actorSelection(this.seedNode.ActorPath());
        selection.tell(new ChordMessages.FindSuccessor(this.seedNode.id), self());
    }

    private void nodejoin(ChordMessages.SuccessorResp successorResp){
        if (this.nodeActor == null) {
            log.info("Successor: "+ successorResp.Nodeid.toJson());
            this.nodeActor = context().actorOf(ChordActor.props(successorResp.Nodeid, bitlen, currentNode),"node");
            RestApiServer.startServer(context().system(), this.nodeActor, currentNode.ip, Integer.parseInt(currentNode.port)+10000);
        }
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(ChordMessages.SuccessorResp.class,
                        this::nodejoin
                )

                .match(ChordMessages.Remainder.class, remainder -> {
                    if (this.nodeActor != null){
                        this.nodeActor.tell(new ChordMessages.Remainder(), self());
                    } else {
                        log.info("Node not yet initialized, running init again");
                        init();
                    }
                })

                .build();
    }
}
