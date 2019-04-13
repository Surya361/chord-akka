package chordHashing;

import java.io.Serializable;
import java.math.BigInteger;

public interface ChordMessages {


    /** Message Object class to find successor
     *  of key/node id
     */
    final class FindSuccessor implements Serializable {
        final BigInteger id;
        public FindSuccessor(BigInteger id){
            this.id = id;
        }
    }


    /** Message Object class for successor Response
     *  Keyid --> the key id quried for (Request is async)
     *  Nodeid --> Node object of the successor actor
     */
    final class SuccessorResp implements Serializable{
        final BigInteger Keyid;
        final Node Nodeid;
        public SuccessorResp(BigInteger keyid, Node Nodeid){
            this.Keyid = keyid;
            this.Nodeid = Nodeid;
        }
        public String toJson(){
            return "{" +
                    "Requestid: " + this.Keyid.toString()  +
                    "}";
        }
    }

    /** Message Object class to notify
     *  to notify the successor to update its predecessor
     */
    final class Notify implements Serializable{
        final Node Nodeid;
        public Notify(Node Nodeid){
            this.Nodeid = Nodeid;
        }

    }

    /** Message Object to request
     *  stabilization to the successor
     */
    final class Stabilize implements Serializable{}


    /** Message Object class to
     *  Reply to a stabilization request
     */
    final class StabilizeResp implements Serializable{
        final Node Predecessor;
        final BigInteger id;
        public StabilizeResp(Node Predecessor, BigInteger id){
            this.Predecessor = Predecessor;
            this.id = id;
        }
    }

    /** Sends a Remainder to do a periodic synchronization
     *
     */
    final class Remainder implements Serializable{}

    /** Acknowldeges the Notify
     *
     */
    final class NotifyAck implements Serializable{}


    /**
     * Requests nodeinfo
     */
    final class NodeInfo implements Serializable{}


    final class NodeInfoResp implements Serializable{
        final Node current;
        final Node successor;
        final Node predecessor;
        public NodeInfoResp(Node current, Node successor, Node predecessor){
            this.current = current;
            this.predecessor = predecessor;
            this.successor = successor;
        }

        public String toJson(){
            return "{" +
                    "current: " + current.toJson()+ ", "+
                    "successor: " + successor.toJson()+", "+
                    "predecessor: " + predecessor.toJson() +", "+
                    "}";
        }
    }

}
