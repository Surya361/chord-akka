package chordHashing;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Arrays;
import java.util.HashMap;

public class NodeActorManager extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    final long id;
    final String port;
    int bitlen;
    long predecessor, successor, fingerTable[], idmap[];
    ActorRef succ, pred, child;
    boolean initialized;
    HashMap<String, ActorRef> idActorMap = new HashMap<String, ActorRef>();

    public NodeActorManager(long id, String port, int bitlen, boolean init){
        this.id = id;
        this.fingerTable = new long[bitlen];
        this.idmap = new long[bitlen];
        for (int i=0; i< bitlen; i++) {
            idmap[i] = (this.id + (long) Math.pow(2,i)) % ((long) Math.pow(2,bitlen));
        }
        if (init){
            this.predecessor = id;
            this.successor = id;
            Arrays.fill(fingerTable, id);
            child = context().actorOf(ChordNodeActor.props(this.id, this.predecessor, this.successor, this.idActorMap, this.fingerTable,this.bitlen, this.idmap), "node");
            this.port = port;
            this.initialized = true;
        } else {
            this.predecessor = -1;
            this.successor = -1;
            this.port = port;
            this.bitlen = bitlen;
            this.initialized = false;
            Arrays.fill(fingerTable, -1);
            init();
        }

    }

    public static Props props(long id, String port, int bitlen, boolean init){
        return Props.create(NodeActorManager.class, ()-> new NodeActorManager(id, port, bitlen, init));
    }

    void init(){
        log.info(this.port);
        ActorSelection selection = context().actorSelection("akka.tcp://chordHashing@127.0.0.1:"+ this.port+"/user/nodeManager/node");
         selection.tell(new ChordNodeMessages.GetSuccessor(this.id), self());
    }

    boolean predecessorofa(long a, long b, long num ){
        if (a == Math.min(a,b)){
            return (num > b || num < a && num > 0);
        } else {
            return (num >b && num < a);
        }

    }

    void populatefingertable(){
        for(int i=0;i < fingerTable.length; i++){
            //idmap[i] = (id + (long) Math.pow(2,i) )% ((long) Math.pow(2,32));
            if(predecessorofa(this.id, this.predecessor, idmap[i])){
                fingerTable[i] = this.id;
            } else {
                ActorSelection selection =  context().actorSelection ("akka.tcp://chordHashing@127.0.0.1:"+ this.port+"/user/nodeManager/node");
                selection.tell(new ChordNodeMessages.GetSuccessor(idmap[i]), self());
            }
        }

    }

    int search(long idmap[], long id){
        for (int i=0; i< idmap.length; i++){
            if(idmap[i] == id){
                return i;
            }
        }
        return -1;
    }

    public boolean checkfingertable(){
        for (int i =0; i< fingerTable.length; i++){
            if (fingerTable[i] == -1){
                return false;
            }
        }
        return true;
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(ChordNodeMessages.SucessorResponce.class, successorResp -> {
                    if (successorResp.successorFor == this.id){
                        this.successor = successorResp.id;
                        idActorMap.put(Long.toString(this.successor), sender());
                        log.info("asking for predecessor");
                        sender().tell(new ChordNodeMessages.SendPred(), self());

                    }
                    if (search(idmap, successorResp.successorFor) != -1){
                        fingerTable[search(idmap, successorResp.successorFor)] = successorResp.id;
                        idActorMap.put(Long.toString(successorResp.id), sender());
                    }
                })
                .match(ChordNodeMessages.updatePred.class, updatepred -> {
                    log.info("updating pred");
                    this.predecessor = updatepred.id;
                    idActorMap.put(Long.toString(this.predecessor), sender());
                    populatefingertable();
                })
                .match(ChordNodeMessages.Remainder.class, i -> {
                    if (child == null) {
                        log.info("State is: Myid:"+ this.id+"\n successor:"+ this.successor+"\n predecessor:"+ this.predecessor+ "\ninitialized:"+ this.initialized+ "\n fingertable:"+Arrays.toString(fingerTable)+ "\n idmaptable"+ Arrays.toString(idmap));
                    }
                    if(this.successor == -1){
                        init();
                    } else{
                        if(!checkfingertable()){
                            populatefingertable();
                        } else{
                            if(this.id != this.successor && child == null){
                                child = context().actorOf(ChordNodeActor.props(this.id, this.predecessor, this.successor, this.idActorMap, this.fingerTable,this.bitlen, this.idmap),"node");
                                this.initialized = true;
                            }


                        }
                        if (child != null){
                            log.info("Informing the child");
                            child.tell(new ChordNodeMessages.Remainder(), self());
                        }

                    }
                })
                .match(ChordNodeMessages.Remainder.class, remainder -> {
                    child.tell(remainder, self());
                })
                .build();
    }


}
