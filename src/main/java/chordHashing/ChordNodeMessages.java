package chordHashing;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.Base64;

public interface ChordNodeMessages {

     enum Type {
        PREDECESSOR, SUCCESSOR;
    }

    final class Changeprednfinger implements  Serializable {
         final long id;
         final long pred;
         public Changeprednfinger(long id, long pred){
             this.id = id;
             this.pred = pred;
         }
    }

    final class GetSuccessor implements Serializable {
        final long id;
        public GetSuccessor(long id) {
            this.id = id;
        }

        @Override
        public String toString(){
            return Base64.getEncoder().encodeToString(Long.toBinaryString(this.id).getBytes());
        }
    }

    final class Updatefingertable implements Serializable {
         final long id;
         final long pred;
         public Updatefingertable(long id, long pred){
             this.id = id;
             this.pred = pred;
         }
    }

    final class GetPredecessor implements Serializable {
        final long id;
        public GetPredecessor(long id) {
            this.id = id;
        }

        @Override
        public String toString(){
            return Base64.getEncoder().encodeToString(Long.toBinaryString(this.id).getBytes());
        }
    }

    final class RequestResponse implements Serializable {
        final long id;
        Type requestype;
        final long resolvedid;
        public RequestResponse(long id, long resolvedid, Type requestype){
            this.id = id;
            this.requestype = requestype;
            this.resolvedid = resolvedid;
        }
    }

    final class Stabilize implements Serializable {}

    final class StabilizeResp implements Serializable{
         final long predecessorid, id;
         final  ActorRef predActorref;
         public StabilizeResp(long predecessorid, ActorRef predActorref, long id){
             this.predecessorid = predecessorid;
             this.predActorref = predActorref;
             this.id = id;
         }
    }

    final class Notify implements Serializable{}

    final class NotifyAck implements Serializable{}

    final class FixFingers implements Serializable{
         final long lowerend, upperend, id;
         public FixFingers(long lowerend, long upperend, long id){
             this.lowerend = lowerend;
             this.upperend = upperend;
             this.id = id;
         }
    }

    final class Remainder implements Serializable{}

    final class NewNode implements Serializable {
         final long id;
         final ActorRef self;
         public NewNode(long id, ActorRef self){
             this.id = id;
             this.self = self;
         }
    }

    final class SendPred implements Serializable{}

    final class updatePred implements  Serializable{
         final long id;
         final ActorRef predActorred;

         public updatePred(long id, ActorRef predActorred){
             this.id = id;
             this.predActorred = predActorred;
         }
    }

    final class SucessorResponce implements Serializable {
        final long id;
        final long successorFor;
        public SucessorResponce(long id, long successorFor){
            this.id = id;
            this.successorFor = successorFor;
        }
    }

}