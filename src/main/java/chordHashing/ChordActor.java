package chordHashing;

import akka.actor.AbstractActor;
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

    private BigInteger Predecessor = null;
    private HashMap<BigInteger, Node> idNodeMap =new HashMap<>();
    private HashMap<BigInteger, ActorSelection> idActorRefMap = new HashMap<>();
    private FingerEntry[] fingerTable;
    private final Node currentNode;


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

    private ChordActor(Node successor, int bitlen, Node currentNode){
        this.currentNode = currentNode;
        fingerTable = new FingerEntry[bitlen];
        fingerTable[0] = getFingertableEntry(this.currentNode.id, 0, successor.id, bitlen);
        updateNodeCache(successor);
        updateNodeCache(currentNode);
    }

    public static Props props(Node Sucessor, int bitlen, Node currentNode){
        return Props.create(ChordActor.class, () -> new ChordActor(Sucessor, bitlen, currentNode));
    }


    private Node getNodeById(BigInteger id){
        if( idNodeMap.get(id) != null){
            return idNodeMap.get(id);
        } else {
            log.info("cannot find Node Object for node id:"+ id.toString());
            return null;
        }
    }

    private ActorSelection getActorById(BigInteger id){
        if(idActorRefMap.get(id) == null){
            idActorRefMap.put(id,getContext().actorSelection(getNodeById(id).ActorPath()));
        }
        return  idActorRefMap.get(id);
    }

    private void rpcFindSuccessor(ChordMessages.FindSuccessor getsucc){
        if(BigIntUtils.between(this.currentNode.id, this.Sucessor(), getsucc.id)){
            sender().tell(new ChordMessages.SuccessorResp(getsucc.id, getNodeById(this.Sucessor())), self());
            return;
        }
        for (int i =0; i < this.fingerTable.length; i++){
            if(fingerTable[i] != null){
                if(BigIntUtils.between(fingerTable[i].start, fingerTable[i].end, getsucc.id)){
                    getActorById(getsucc.id).tell(getsucc, sender());
                    return;
                }
            }

        }
        getActorById(this.Sucessor()).tell(getsucc, sender());
    }

    private void rpcNotify(ChordMessages.Notify noti){
        log.info("Notify from:"+ noti.Nodeid.toJson());
        if(this.Predecessor == null || ( this.currentNode.id != noti.Nodeid.id && BigIntUtils.between(this.Predecessor, this.currentNode.id, noti.Nodeid.id))  ){ //A hack need to look at it
            this.Predecessor = new BigInteger(noti.Nodeid.id.toString());
            updateNodeCache(noti.Nodeid);
        }
    }

    private void rpcStabilize(ChordMessages.Stabilize msg){
        sender().tell(new ChordMessages.StabilizeResp(getNodeById(this.Predecessor), this.currentNode.id), self());
    }

    private void rpcStabilizeResp(ChordMessages.StabilizeResp msg){
        log.info("new stabilize between: " + msg.Predecessor.id.toString());
        if(this.currentNode.id.equals(msg.Predecessor.id)){
            return;
        }else {
            log.info("new Node: "+ msg.Predecessor.id.toString()+" between: this: "+ this.currentNode.id.toString()+"  Successor: "+this.Sucessor().toString());
            if(BigIntUtils.between(this.currentNode.id, this.Sucessor(), msg.Predecessor.id)){
                this.fingerTable[0].id = msg.Predecessor.id;
                updateNodeCache(msg.Predecessor);
            }
        }
    }

    private void sync(ChordMessages.Remainder rm){
        //Notify
        getActorById(this.Sucessor()).tell(new ChordMessages.Notify(getNodeById(this.currentNode.id)), self());

        //Stabilize
        getActorById(this.Sucessor()).tell(new ChordMessages.Stabilize(), self());

    }

    private void sendNodeInfo(ChordMessages.NodeInfo req){
        sender().tell(new ChordMessages.NodeInfoResp(currentNode, getNodeById(this.Sucessor()), getNodeById(this.Predecessor)), self());
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

                .match(ChordMessages.NotifyAck.class, notifyAck -> {
                    log.info("Notify Acked");
                })

                .match(ChordMessages.StabilizeResp.class,
                        this::rpcStabilizeResp
                )

                .match(ChordMessages.Remainder.class,
                        this::sync
                )

                .match(ChordMessages.NodeInfo.class,
                        this::sendNodeInfo
                )


                .build();
    }


}
