package chordHashing;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

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
           this.nodeActor = context().actorOf(ChordActor.props(this.currentNode, currentNode.id, bitlen),"node");
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
        this.nodeActor = context().actorOf(ChordActor.props(successorResp.Nodeid, currentNode.id, bitlen),"node");
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
