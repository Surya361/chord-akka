package chordHashing;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.math.BigInteger;
import java.util.HashMap;

/** Chord Actor class extends
 *  akka abstract actor
 */
public class ChordActor extends AbstractActor {

    private final BigInteger id;
    private BigInteger Predecessor;
    private HashMap<BigInteger, Node> idNodeMap =new HashMap<>();
    private HashMap<BigInteger, ActorSelection> idActorRefMap = new HashMap<>();
    private FingerEntry[] fingerTable;


    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    /**
     * @param id --> id of the Node
     * @param i --> generates ith entry of the fingertable
     * @param successor --> sucessor fot the entry
     * @param bitlen --> bit length of the hash
     * @return --> returns fingertable entry object
     */
    private FingerEntry getFingertableEntry(BigInteger id, int i, BigInteger successor, int bitlen){
        BigInteger two = BigInteger.valueOf(2);
        BigInteger max_value = two.pow(bitlen);
        BigInteger start = id.add(two.pow(i)).mod(max_value);
        BigInteger end = id.add(two.pow(i+1)).mod(max_value);
        return new FingerEntry(start,end, successor);

    }

    /**
     * @return The first entry of the fingertable, i.e. successor of the node
     */
    private BigInteger Sucessor(){
        return fingerTable[0].id;
    }

    private void updateNodeCache(Node node){
        idNodeMap.put(node.id, node);
    }

    private ChordActor(Node successor, BigInteger id, int bitlen){
        this.id = id;
        fingerTable = new FingerEntry[bitlen];
        fingerTable[0] = getFingertableEntry(this.id, 0, successor.id, bitlen);
        updateNodeCache(successor);
    }

    public static Props props(Node Sucessor, BigInteger id, int bitlen){
        return Props.create(ChordActor.class, () -> new ChordActor(Sucessor, id, bitlen));
    }


    private boolean greaterthan(BigInteger a, BigInteger b, boolean equal){
        if (a.compareTo(b) == 1 || (equal && a.compareTo(b) == 0)) {
            return true;
        }
        return false;
    }

    private boolean lessthan(BigInteger a, BigInteger b, boolean equal){
        if (a.compareTo(b) == -1 || (equal && a.compareTo(b) == 0)) {
            return true;
        }
        return false;
    }

    private boolean between(BigInteger a, BigInteger b, BigInteger id){
        if(lessthan(a, b, false)){
            if(greaterthan(id, a, true) && lessthan(id, b, false)){
                return true;
            }
        } else{
            if(greaterthan(id, a, true) || lessthan(id, b, false)){
                return true;
            }
        }
        return false;
    }

    private Node getNodeById(BigInteger id){
        return idNodeMap.get(id);
    }

    private ActorSelection getActorById(BigInteger id){
        if(idActorRefMap.get(id) == null){
            idActorRefMap.put(id,getContext().actorSelection(getNodeById(id).ActorPath()));
        }
        return  idActorRefMap.get(id);
    }

    private void rpcFindSuccessor(ChordMessages.FindSuccessor getsucc){
        if(between(this.id, this.Sucessor(), getsucc.id)){
            sender().tell(new ChordMessages.SuccessorResp(getsucc.id, getNodeById(this.Sucessor())), self());
            return;
        }
        for (int i =0; i < this.fingerTable.length; i++){
            if(between(fingerTable[i].start, fingerTable[i].end, getsucc.id)){
                getActorById(getsucc.id).tell(getsucc, sender());
                return;
            }
        }
        getActorById(this.Sucessor()).tell(getsucc, sender());
    }

    private void rpcNotify(ChordMessages.Notify noti){
        if(this.Predecessor == null || between(this.Predecessor, this.id, noti.Nodeid.id)){
            this.Predecessor = noti.Nodeid.id;
        }
    }

    private void rpcStabilize(ChordMessages.Stabilize msg){
        sender().tell(new ChordMessages.StabilizeResp(getNodeById(this.Predecessor), this.id), self());
    }

    private void rpcStabilizeResp(ChordMessages.StabilizeResp msg){
        if(this.id.equals(msg.id)){
            return;
        }else {
            if(between(this.id, this.Sucessor(), msg.id)){
                this.fingerTable[0].id = msg.id;
            }
        }
    }

    private void sync(ChordMessages.Remainder rm){
        //Notify
        getActorById(this.Sucessor()).tell(new ChordMessages.Notify(getNodeById(this.id)), self());

        //Stabilize
        getActorById(this.Sucessor()).tell(new ChordMessages.Stabilize(), self());

    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(ChordMessages.FindSuccessor.class,
                        this::rpcFindSuccessor
                )

                .match(ChordMessages.Notify.class,
                        this::rpcNotify
                )

                .match(ChordMessages.Stabilize.class,
                        this::rpcStabilize
                )

                .match(ChordNodeMessages.NotifyAck.class, notifyAck -> {
                    log.info("Notify Acked");
                })

                .match(ChordMessages.StabilizeResp.class,
                        this::rpcStabilizeResp
                )

                .match(ChordMessages.Remainder.class,
                        this::sync

                )


                .build();
    }


}
