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

    /**
     *
     */
    final class Remainder implements Serializable{}


}
