package chordHashing;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.komamitsu.failuredetector.PhiAccuralFailureDetector;

public class LiveActor extends AbstractActor {
    PhiAccuralFailureDetector failureDetector = new PhiAccuralFailureDetector.Builder().build();
    private Node successor;
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private ActorSelection getActorById(Node node){
        return getContext().actorSelection(node.LiveActorPath());
    }

    private void rpcSucessorChange(ChordMessages.SuccessorChange sc){
        if(this.successor == null || !this.successor.id.equals(sc.newNode.id)){
            this.successor = sc.newNode;
            this.failureDetector = new PhiAccuralFailureDetector.Builder().build();
        }else {
            if(failureDetector.phi() > 100.00){
                log.info("Node: "+ this.successor.toJson() +" Failed phi value:"+ failureDetector.phi());
                sender().tell(new ChordMessages.SuccessorFailure(this.successor), self());
            } else {
                log.info("Node: "+ this.successor.toJson() +" Live phi value: "+ failureDetector.phi());
            }
        }
        getActorById(this.successor).tell(new ChordMessages.Ping(), self());
    }

    private void rpcPong(ChordMessages.Pong pong){
        this.failureDetector.heartbeat();
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(ChordMessages.Ping.class, ping -> {
                    sender().tell(new ChordMessages.Pong(), self());
                })
                .match(ChordMessages.Pong.class,
                        this::rpcPong
                )
                .match(ChordMessages.SuccessorChange.class,
                        this::rpcSucessorChange
                )
                .build();
    }


}
