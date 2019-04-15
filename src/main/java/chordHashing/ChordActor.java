package chordHashing;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.math.BigInteger;
import java.util.Arrays;
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
    private final int bitlen;
    private static int fix_fingers = 0;


    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    /**
     * @param id --> id of the Node
     * @param i --> generates ith entry of the fingertable
     * @param successor --> sucessor fot the entry
     * @param bitlen --> bit length of the hash
     * @return --> returns fingertable entry object
     */
    private FingerEntry getFingertableEntry(BigInteger id, int i, Node successor, int bitlen){
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
        return fingerTable[0].succ.id;
    }

    private void updateNodeCache(Node node){
        idNodeMap.put(node.id, node);
    }

    private ChordActor(Node successor, int bitlen, Node currentNode){
        this.currentNode = currentNode;
        this.bitlen = bitlen;
        fingerTable = new FingerEntry[bitlen];
        fingerTable[0] = getFingertableEntry(this.currentNode.id, 0, successor, bitlen);
        updateNodeCache(successor);
        updateNodeCache(currentNode);
        initBareFingerTable();
    }

    public void initBareFingerTable(){
        for(int i =1; i< this.fingerTable.length; i++){
            fingerTable[i] = getFingertableEntry(this.currentNode.id, i , null, this.bitlen);
        }
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

    private ActorSelection getActorById(Node node){
        if(idActorRefMap.get(node.id) == null){
            idActorRefMap.put(node.id,getContext().actorSelection(getNodeById(node.id).ActorPath()));
        }
        return  idActorRefMap.get(node.id);
    }

    private void rpcFindSuccessor(ChordMessages.FindSuccessor getsucc){

        if(BigIntUtils.between(this.currentNode.id, this.Sucessor(), getsucc.id)){
            sender().tell(new ChordMessages.SuccessorResp(getsucc.id, getNodeById(this.Sucessor()), getsucc.trace), self());
            return;
        }
        if(getsucc.tty == 0){
            sender().tell(new ChordMessages.SuccessorResp(getsucc.id, getNodeById(this.Sucessor()), getsucc.trace), self());
            return;
        }
        for (int i =fingerTable.length-1; i >= 0; i--){
            if(fingerTable[i].succ != null){
                if(BigIntUtils.between(this.currentNode.id, getsucc.id, fingerTable[i].succ.id, true)){
                    getActorById(fingerTable[i].succ).tell(new ChordMessages.FindSuccessor(getsucc.id, getsucc.trace+", "+ this.currentNode.id.toString(), getsucc.tty-1), sender());
                    return;
                }
            }

        }
        getActorById(this.Sucessor()).tell(new ChordMessages.FindSuccessor(getsucc.id, getsucc.trace+", "+ this.currentNode.id.toString(),getsucc.tty-1), sender());
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
                this.fingerTable[0] = getFingertableEntry(this.currentNode.id,0, msg.Predecessor, this.bitlen);
                updateNodeCache(msg.Predecessor);
            }
        }
    }

    private void sync(ChordMessages.Remainder rm){
        //Notify
        getActorById(this.Sucessor()).tell(new ChordMessages.Notify(getNodeById(this.currentNode.id)), self());

        //Stabilize
        getActorById(this.Sucessor()).tell(new ChordMessages.Stabilize(), self());

        //FixFingers
        for(int i=0; i < 10; i++){
            self().tell(new ChordMessages.FindSuccessor(fingerTable[this.fix_fingers].start), self());
            this.fix_fingers = (this.fix_fingers + 1) % bitlen;
            if(this.fix_fingers == 0){
                this.fix_fingers = 1;
            }
        }


    }

    private void sendNodeInfo(ChordMessages.NodeInfo req){
        sender().tell(new ChordMessages.NodeInfoResp(currentNode, getNodeById(this.Sucessor()), getNodeById(this.Predecessor)), self());
    }

    private void rpcFindPredecessor(ChordMessages.FindPredecessor fp){
        if(BigIntUtils.between(this.currentNode.id, this.Sucessor(), fp.id)){
            sender().tell(new ChordMessages.PredecessorResp(fp.id, this.currentNode), self());
            return;
        }
        for (int i =this.fingerTable.length-1; i >= 0; i--){
            if(fingerTable[i] != null){
                if(BigIntUtils.between(this.currentNode.id, fp.id, fingerTable[i].succ.id, true)){
                    getActorById(fingerTable[i].succ).tell(fp, sender());
                    return;
                }
            }
        }
        getActorById(this.Sucessor()).tell(fp, sender());
    }

    private void rpcAskFingerTable(ChordMessages.AskFingerTable ask){
        sender().tell(new ChordMessages.RespondFingerTable(this.fingerTable), self());
    }

    private FingerEntry updateFingerTablebyRow(FingerEntry cmp, int i){
        FingerEntry newFingerEntry = getFingertableEntry(this.currentNode.id, i, null, this.bitlen);
        if(cmp.succ != null && BigIntUtils.between(cmp.start, cmp.succ.id, newFingerEntry.start)){
            newFingerEntry = new FingerEntry(newFingerEntry.start, newFingerEntry.end, cmp.succ);
        }
        return newFingerEntry;
    }

    private void rpcRespondFingerTable(ChordMessages.RespondFingerTable fingerTable){
       // int fill =1;
        int totalUpdated = 0;
        //FingerEntry cmpRow = this.fingerTable[0];

        for (int i =0; i < fingerTable.FingerTable.size() && totalUpdated < this.fingerTable.length; i++){
            FingerEntry cmpRow = i == 0? this.fingerTable[0]  : fingerTable.FingerTable.get(i);
          //  log.info("first for");
            for (int j = 1; j< this.fingerTable.length; j++){
                FingerEntry newFinger = updateFingerTablebyRow(cmpRow, j);
                //log.info("second for");
                if (newFinger.succ != null && this.fingerTable[j].succ == null) {
                    updateNodeCache(fingerTable.FingerTable.get(i).succ);
                    totalUpdated++;
                //    log.info("updating");
                    this.fingerTable[j] = newFinger;
                }
            }
        }
        /*for (; fill < this.fingerTable.length; fill++){
            FingerEntry newFinger = updateFingerTablebyRow(cmpRow, fill);
            if (newFinger.succ != null ){
                updateNodeCache(cmpRow.succ);
                this.fingerTable[fill] = newFinger;
            } else {
                if (fingercmp < fingerTable.FingerTable.size()){
                    cmpRow = fingerTable.FingerTable.get(fingercmp++);
                    fill--;
                } else {
                    break;
                }
            }
        }
        while(fill < this.fingerTable.length){
            log.info("asking sucessor to help with fingertable id:"+ fill + "  lenght: "+ this.fingerTable.length +" for id:"+this.fingerTable[fill]);
            getActorById(this.Sucessor()).tell(new ChordMessages.FindSuccessor(this.fingerTable[fill].start), self());
            fill++;
        }*/

        for(int i =0; i< this.fingerTable.length; i++){
            if(this.fingerTable[i].succ == null){
                log.info("asking sucessor to help with fingertable id:"+ i + "  lenght: "+ this.fingerTable.length +" for id:"+this.fingerTable[i]);
                getActorById(this.Sucessor()).tell(new ChordMessages.FindSuccessor(this.fingerTable[i].start), self());
            }
        }
    }

    private void rpcSuccessorResponse(ChordMessages.SuccessorResp succResp){
        for(int i=1; i < this.fingerTable.length; i++){
            if(this.fingerTable[i].start.equals(succResp.Keyid)){
                this.fingerTable[i] = new FingerEntry(this.fingerTable[i].start,this.fingerTable[i].end, succResp.Nodeid);
            }
        }
        updateNodeCache(succResp.Nodeid);
    }

    private void rpcUpdateFingerEntry(ChordMessages.UpdateFingerEntry updatemsg){
        log.info("Received a updated for finger table entry\n Updated: "+ updatemsg.node.toJson() +"\n" +"Previous: " + this.fingerTable[updatemsg.i].toJson() );
        if(this.fingerTable[updatemsg.i].succ != null && BigIntUtils.between(this.Sucessor(), this.fingerTable[updatemsg.i].succ.id, updatemsg.node.id) && !updatemsg.node.id.equals(this.fingerTable[updatemsg.i].succ)){
                this.fingerTable[updatemsg.i] = new FingerEntry(fingerTable[updatemsg.i].start, fingerTable[updatemsg.i].end, updatemsg.node);
                if(this.Predecessor != null){
                    if (getNodeById(this.Predecessor) != null){
                        getActorById(this.Predecessor).tell(updatemsg, self());
                    }
                }
                updateNodeCache(updatemsg.node);
        }

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
                .match(ChordMessages.FindPredecessor.class,
                        this::rpcFindPredecessor
                )

                .match(ChordMessages.AskFingerTable.class,
                        this::rpcAskFingerTable
                )

                .match(ChordMessages.RespondFingerTable.class,
                        this::rpcRespondFingerTable
                )

                .match(ChordMessages.SuccessorResp.class,
                        this::rpcSuccessorResponse
                )
                .match(ChordMessages.UpdateFingerEntry.class,
                        this::rpcUpdateFingerEntry
                )
                .match(ChordMessages.DebugInfo.class, debugInfo -> {
                    sender().tell(new ChordMessages.RespondFingerTable(this.fingerTable), self());
                })

                .build();
    }


}
