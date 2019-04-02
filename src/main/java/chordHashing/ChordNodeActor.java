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
    final String port;


    public static final class Notify implements Serializable {
        final long id;
        public Notify(long id){
            this. id = id;
        }
    }
    public static final class Stabilize implements Serializable {}
    public static final class FixFingers implements Serializable {}


    public ChordNodeActor(long id, int bitlen, boolean initialized, String port){
        this.id = id;
        this.predecessor = (initialized) ? id : -1 ;
        this.successor = id;
        this.fingerTable = new long[bitlen];
        this.idmap = new long[bitlen];
        this.initialized = initialized;
        this.port = port;
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

    }

    public static Props props(long serverNum, int bitlen, boolean initialized, String port){
        return Props.create(ChordNodeActor.class, ()-> new ChordNodeActor(serverNum, bitlen, initialized, port));
    }

    public int Searchfor(long j){
        for (int i =0; i< idmap.length; i++){
            if (idmap[i] == j){
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

    public void updatefingertable(long lower, long uppper, long id){
        for(int i=0; i< fingerTable.length; i++){
            log.info(Long.toString(idmap[i])+"   ;   " +Long.toString(lower)+"    ;   "+uppper, id);
            if (predecessorofa(lower, uppper, idmap[i])){
                fingerTable[i] = id;
            }
        }
    }

    long getActortosend(long peerid){
        log.info("Searchinf for "+ peerid);
        for(int i=0; i< fingerTable.length; i++){
            if (idmap[i]> peerid && idmap[(i+1) % fingerTable.length]  < peerid){
                log.info( Arrays.toString(fingerTable));
                return fingerTable[i];
            }
        }
        return -1;
    }

      boolean predecessorofa(long a, long b, long num ){
        if (a == Math.min(a,b)){
            return (num > b || num < a && num > 0);
        } else {
            return (num >b && num < a);
        }

    }
    void init(){
        log.info("length of the finger table is"+Integer.toString( fingerTable.length));
        for(int i=0;i < fingerTable.length; i++){
            idmap[i] = (id + (long) Math.pow(2,i) )% ((long) Math.pow(2,32));
            if(predecessorofa(this.id, this.predecessor, idmap[i])){
                fingerTable[i] = this.id;
            } else {
               ActorSelection selection =  context().actorSelection ("akka.tcp://chordHashing@127.0.0.1"+ this.port+"/user/node");
                selection.tell(new ChordNodeMessages.GetSuccessor(idmap[i]), self());
            }
        }
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(ChordNodeMessages.SucessorResponce.class, successorResp -> {
                    if (successorResp.successorFor == this.id){
                    log.info("Learned about successor: "+ this.successor);
                    this.successor = successorResp.id;
                    idActorMap.put(Long.toString(successorResp.id), sender());
                    log.info("asking for predecessor");
                    sender().tell(new ChordNodeMessages.SendPred(), self());

                    }
                })

                .match(ChordNodeMessages.Changeprednfinger.class, changeprednfinger -> {
                    log.info("updating predecessor");
                    this.predecessor = changeprednfinger.id;
                    updatefingertable(changeprednfinger.id, changeprednfinger.pred, changeprednfinger.id);
                })

                .match(ChordNodeMessages.SendPred.class, sendPred -> {
                    log.info("Request to know my predecessor");
                    sender().tell(new updatePred(this.predecessor, idActorMap.get(Long.toString(this.predecessor))), self());
                })
                .match(ChordNodeMessages.GetSuccessor.class, getSucessor -> {
                    log.info(" successor request for: "+ this.successor);
                    if (predecessorofa(this.id, this.predecessor, getSucessor.id) || successor == predecessor){
                        sender().tell(new ChordNodeMessages.SucessorResponce(this.id, getSucessor.id), self());
                        log.info(" successor response sent  for: "+ this.successor+" at "+ sender().path());
                    } else {
                        log.info(idActorMap.keySet().toString());
                        long peerid = getActortosend(getSucessor.id);
                        log.info("needs to forward it to"+ peerid + "this id is :"+ this.id+ " ;predecessor:"+ this.predecessor+"; ;sucessor: "+ this.successor+ "; id:"+getSucessor.id);
                        idActorMap.get(Long.toString(peerid)).tell(getSucessor, sender());
                    }
                })
                .match(ChordNodeMessages.Remainder.class, i -> {
                    log.info("State is: Myid:"+ this.id+"\n successor:"+ this.successor+"\n predecessor:"+ this.predecessor+ "\ninitialized:"+ this.initialized+ "\n fingertable:"+Arrays.toString(fingerTable)+ "\n idmaptable"+ Arrays.toString(idmap));
                    if(this.initialized && (this.successor != this.id)) {
                        ActorRef sucessorActorref = idActorMap.get(Long.toString(this.successor));
                        sucessorActorref.tell(new Notify(this.id), self());
                        sucessorActorref.tell(new Stabilize(), self());
                    } else{
                        if (this.predecessor != -1 && this.successor != this.id && fingertableInit){
                                this.initialized = true;

                        } else {
                           ActorSelection selection = context().actorSelection ("akka.tcp://chordHashing@127.0.0.1:"+ this.port+"/user/node");
                            selection.tell(new ChordNodeMessages.GetSuccessor(this.id), self());
                            if (checkfingertable()){
                                this.fingertableInit = true;
                            } else{
                                init();
                            }
                        }
                    }
                })
                .match(Notify.class, notify -> {
                    if( this.successor == this.id){
                        this.predecessor = notify.id;
                    }
                    idActorMap.put(Long.toString(notify.id), sender());
                    sender().tell(new ChordNodeMessages.NotifyAck(), self());
                })

                .match(Stabilize.class, i -> {
                    sender().tell(new ChordNodeMessages.StabilizeResp(this.predecessor, idActorMap.get(Long.toString(this.predecessor))), self());
                })

                .match(ChordNodeMessages.NotifyAck.class, notifyAck -> { log.info("Notify acked");})

                .match(ChordNodeMessages.StabilizeResp.class, i -> {
                    if (i.predecessorid != this.id){
                        this.successor = i.predecessorid;
                        idActorMap.put(Long.toString(i.predecessorid), i.predActorref);
                    }
                    if (checkfingertable() && !this.initialized){
                        updatefingertable(this.predecessor,this.id -1, this.id);
                        this.initialized = true;
                    }
                })

                .match(updatePred.class, updatepred -> {
                    log.info("updating pred");
                    this.predecessor = updatepred.id;
                    idActorMap.put(Long.toString(this.predecessor),updatepred.predActorred);
                    sender().tell(new ChordNodeMessages.Changeprednfinger(this.id, this.predecessor), self());
                    init();
                })
                .matchAny(o -> {log.info("UNKNOW MESSAGE from "+ sender().path());})
                .build();
    }


}
