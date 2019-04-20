package chordHashing;


import java.io.Serializable;
import java.math.BigInteger;
import com.google.common.collect.ImmutableList;

public interface ChordMessages {


    /** Message Object class to find successor
     *  of key/node id
     */
    final class FindSuccessor implements Serializable {
        final BigInteger id;
        final String trace;
        final int tty;
        public FindSuccessor(BigInteger id){
            this.id = id;
            this.trace = "";
            this.tty=10;
        }
        public FindSuccessor(BigInteger id, String trace, int tty){
            this.id = id;
            this.trace = trace;
            this.tty=tty;
        }
    }


    /** Message Object class for successor Response
     *  Keyid --> the key id quried for (Request is async)
     *  Nodeid --> Node object of the successor actor
     */
    final class SuccessorResp implements Serializable{
        final BigInteger Keyid;
        final Node Nodeid;
        final String trace;
        public SuccessorResp(BigInteger keyid, Node Nodeid){
            this.Keyid = keyid;
            this.Nodeid = Nodeid;
            this.trace = "";
        }
        public SuccessorResp(BigInteger keyid, Node Nodeid, String trace){
            this.Keyid = keyid;
            this.Nodeid = Nodeid;
            this.trace = trace;
        }
        public String toJson(){
            return "{" +
                    "Requestid: " + this.Keyid.toString()  +
                    "Node: " + this.Nodeid.toJson() +
                    "trace: " + this.trace +
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
        final ImmutableList<Node> nextSucessors;
        public StabilizeResp(Node Predecessor, BigInteger id){
            this.Predecessor = Predecessor;
            this.id = id;
            this.nextSucessors = null;
        }

        public StabilizeResp(Node Predecessor, BigInteger id, Node[] successorList){
            this.Predecessor = Predecessor;
            this.id = id;

            for(int i=0; i< successorList.length; i++){
                if (successorList[i] ==  null){
                    successorList[i] = new Node("0.0.0.0","0");
                }
            }
            this.nextSucessors = ImmutableList.copyOf(successorList);
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


    /**
     * NodeInfoResponse Object that contains
     * information about the current node, its predecessor and successor
     */
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
                    "predecessor: " + predecessor.toJson() +
                    "}";
        }
    }

    /**
     * Request to FindPredecessor Object
     */
    final class FindPredecessor implements Serializable{
        final BigInteger id;
        public FindPredecessor(BigInteger id){
            this. id = id;
        }
    }


    /** Message Object class for successor Response
     *  Keyid --> the key id queried for (Request is async)
     *  Nodeid --> Node object of the predecessor actor
     */
    final class PredecessorResp implements Serializable{
        final BigInteger Keyid;
        final Node Nodeid;
        public PredecessorResp(BigInteger keyid, Node Nodeid){
            this.Keyid = keyid;
            this.Nodeid = Nodeid;
        }
        public String toJson(){
            return "{" +
                    "Requestid: " + this.Keyid.toString()  +
                    "Node: " + this.Nodeid.toJson() +
                    "}";
        }
    }

    final class AskFingerTable implements Serializable{}

    final class RespondFingerTable implements Serializable {
        final ImmutableList<FingerEntry> FingerTable;

        public RespondFingerTable(FingerEntry[] fingerTable) {
            FingerTable = ImmutableList.copyOf(fingerTable);
        }

        public String toJson() {
            String Json = "{";
            for (int i = 0; i < this.FingerTable.size(); i++) {
                Json = Json + "\n" + FingerTable.get(i).toJson();
            }
            return Json + "}";

        }
    }

    final class UpdateFingerEntry implements Serializable{
        final Node node;
        final int i;
        public UpdateFingerEntry(Node node, int i){
            this.i = i;
            this.node = node;
        }
    }

    final class DebugInfo implements Serializable{ }


    final class RedundancyInfo implements Serializable{}

    final class RespRedundancyInfo implements Serializable{
        final ImmutableList<Node> nextSucessors;
        public RespRedundancyInfo(Node[] nextsucessor) {
            for(int i=0; i< nextsucessor.length; i++){
                if (nextsucessor[i] ==  null){
                    nextsucessor[i] = new Node("0.0.0.0","0");
                }
            }
            this.nextSucessors = ImmutableList.copyOf(nextsucessor);
        }
        public String toJson() {
            String Json = "{";
            for (int i = 0; i < this.nextSucessors.size(); i++) {
                Json = Json + "\n" + nextSucessors.get(i).toJson();
            }
            return Json + "}";

        }
    }

    final class Ping implements Serializable{}
    final class Pong implements Serializable{}

    final class SuccessorChange implements Serializable{
        final Node newNode;
        public SuccessorChange(Node newNode){
            this.newNode = newNode;
        }
    }

    final class SuccessorFailure implements Serializable{
        final Node failedNode;
        public SuccessorFailure(Node failedNode){
            this.failedNode = failedNode;
        }
    }

    final class PredecessorFailedNotify implements Serializable{
        final Node newPred;
        final Node pred;
        public PredecessorFailedNotify(Node newPred, Node pred){
            this.newPred = newPred;
            this.pred = pred;
        }
    }

}
