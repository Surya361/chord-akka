package chordHashing;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import chordHashing.ChordNodeMessages.updatePred;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Arrays;

public class ChordNodeActor extends AbstractActorWithTimers {


    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);



    HashMap<String, ActorRef> idActorMap = new HashMap<String, ActorRef>();
    long successor, predecessor, fingerTable[], idmap[];
    final long id;
    ActorRef mediator;
    boolean initialized, fingertableInit;

    public static final class Notify implements Serializable {
        final long id;
        public Notify(long id){
            this. id = id;
        }
    }
    public static final class Stabilize implements Serializable {}
    public static final class FixFingers implements Serializable {}


   /* public ChordNodeActor(long id, int bitlen, boolean initialized, String port){
        this.id = id;
        this.predecessor = (initialized) ? id : -1 ;
        this.successor = id;
        this.fingerTable = new long[bitlen];
        this.idmap = new long[bitlen];
        this.initialized = initialized;
        this.fingertableInit = false;
        if (initialized) {
            Arrays.fill(fingerTable, this.id);
        } else {
            Arrays.fill(fingerTable, -1);
        }

        for (int i=0; i< bitlen; i++) {
            idmap[i] = (this.id + (long) Math.pow(2,i)) % ((long) Math.pow(2,bitlen));
            //log.info(Long.toString(idmap[i]));
        }
        idActorMap.put(Long.toString(id), context().self());

    }*/

    public ChordNodeActor(long id, long predecessor, long successor, HashMap<String, ActorRef> idActorMap , long[] fingerTable, int bitlen, long[] idmap){
        this.id = id;
        this.predecessor = predecessor;
        this.successor = successor;
        this.fingerTable = fingerTable;
        this.idActorMap = idActorMap;
        this.idmap = idmap;
        if(this.id != this.successor){
            idActorMap.get(Long.toString(this.successor)).tell(new ChordNodeMessages.Changeprednfinger(this.id, this.predecessor), self() );
        }else {
            idActorMap.put(Long.toString(this.id), self());
        }

    }

    public static Props props(long serverNum, long predecessor, long successor, HashMap<String, ActorRef> idActorMap , long[] fingerTable, int bitlen, long[] idmap){
        return Props.create(ChordNodeActor.class, ()-> new ChordNodeActor(serverNum, predecessor, successor, idActorMap, fingerTable, bitlen, idmap));
    }

    public int Searchfor(long j){
        for (int i =0; i< idmap.length; i++){
            if (idmap[i] == j){
                return i;
            }
        }
        return -1;
    }
    public void updatefingertable(long lower, long uppper, long id){
        for(int i=0; i< fingerTable.length; i++){
            log.info(Long.toString(idmap[i])+"   ;   " +Long.toString(lower)+"    ;   "+uppper, id);
            if (predecessorofa(lower, uppper, idmap[i])){
                fingerTable[i] = id;
            }
        }
    }

      boolean predecessorofa(long a, long b, long num ){
        if (a == Math.min(a,b)){
            return (num > b || num < a && num > 0);
        } else {
            return (num >b && num < a);
        }

    }
    @Override
    public void preStart(){
        log.info("Starting the Actor");
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(ChordNodeMessages.SucessorResponce.class, successorResp -> {
                   log.info("Sucessor Response for: "+ successorResp.successorFor+" is "+ successorResp.id);
                })

                .match(ChordNodeMessages.Changeprednfinger.class, changeprednfinger -> {
                    log.info("\n\n\n\nupdating predecessor to "+ changeprednfinger.id+" as requested by a new node\n\n\n\\n");
                    this.predecessor = changeprednfinger.id;
                    updatefingertable(changeprednfinger.id, changeprednfinger.pred, changeprednfinger.id);
                    for (String name : this.idActorMap.keySet()){
                        if(idActorMap.get(name) != null && name != Long.toString(changeprednfinger.id)){
                            log.debug("Sending update fingertable to"+ name);
                            idActorMap.get(name).tell(new ChordNodeMessages.Updatefingertable(changeprednfinger.id, changeprednfinger.pred), self());
                        }
                    }

                })
                .match(ChordNodeMessages.Updatefingertable.class, updatefingertable -> {
                    updatefingertable(updatefingertable.id, updatefingertable.pred, updatefingertable.id);
                })

                .match(ChordNodeMessages.SendPred.class, sendPred -> {
                    log.debug("Request to know my predecessor");
                    sender().tell(new updatePred(this.predecessor, idActorMap.get(Long.toString(this.predecessor))), self());
                })

                .match(ChordNodeMessages.GetSuccessor.class, getSucessor -> {
                    if (predecessorofa(this.id, this.predecessor, getSucessor.id) || successor == predecessor) {
                        log.debug("Sending response");
                        sender().tell(new ChordNodeMessages.SucessorResponce(this.id, getSucessor.id), self());
                    } else{
                        long id = -1;
                        for(int i=0; i< fingerTable.length; i++){
                            if(idmap[i] < getSucessor.id && id < idmap[i]){
                                id = idmap[i];
                            }
                            ActorRef forwardto = idActorMap.get(Long.toString(id));
                            if (forwardto == null){
                                idActorMap.get(Long.toString(this.successor)).tell(getSucessor, sender());
                            }else{
                                forwardto.tell(getSucessor, sender());
                            }

                    }
                    }
                })
                .match(ChordNodeMessages.Remainder.class, i -> {
                    log.info("\n Self: "+self().path()+"\n  State is: Myid:"+ this.id+"\n successor:"+ this.successor+"\n predecessor:"+ this.predecessor+ "\ninitialized:"+ this.initialized+ "\n fingertable:"+Arrays.toString(fingerTable)+ "\n idmaptable"+ Arrays.toString(idmap));
                    if(this.id != this.predecessor){
                        ActorRef sucessorActorref = idActorMap.get(Long.toString(this.successor));

                        sucessorActorref.tell(new Stabilize(), self());
                        if(this.id != this.successor){
                            sucessorActorref.tell(new Notify(this.id), self());
                        }
                    }

                })
                .match(Notify.class, notify -> {
                    log.info("\n\n\n\n\nUpdating predecessors because of notify from"+ notify.id+"\n\n\n\n\n");
                    this.predecessor = notify.id;
                    idActorMap.put(Long.toString(notify.id), sender());
                    sender().tell(new ChordNodeMessages.NotifyAck(), self());
                })

                .match(Stabilize.class, i -> {
                    log.info("\n\n\n\n\nSending Response with predecessorid"+ this.predecessor);
                    sender().tell(new ChordNodeMessages.StabilizeResp(this.predecessor, idActorMap.get(Long.toString(this.predecessor)), this.id), self());
                })

                .match(ChordNodeMessages.NotifyAck.class, notifyAck -> { log.info("Notify acked");})

                .match(ChordNodeMessages.StabilizeResp.class, i -> {
                   log.info("\n\n\n\n\n\nStabilize from:"+ i.id +";  "+i.predecessorid);
                    if (i.predecessorid != this.id && this.successor == i.id){
                        if(i.predecessorid == this.predecessor && this.successor != this.id){
                            log.info("ingonring Stabilize suspecting stale pointing give it sometime");
                        }else {
                            this.successor = i.predecessorid;
                            idActorMap.put(Long.toString(i.predecessorid), i.predActorref);
                        }

                    }
                })
                .matchAny(o -> {log.info("UNKNOW MESSAGE from "+ sender().path());})
                .build();
    }


}
