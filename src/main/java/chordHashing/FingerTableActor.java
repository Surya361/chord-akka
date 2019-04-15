package chordHashing;

import akka.actor.AbstractActor;

import java.math.BigInteger;

/**
 *  This actor maintains the fingerTable
 */
public class FingerTableActor extends AbstractActor {

    FingerEntry[] fingerTable;
    Node currentNode;
    int bitlen;

    private FingerEntry getFingertableEntry(BigInteger id, int i, Node successor, int bitlen){
        BigInteger two = BigInteger.valueOf(2);
        BigInteger max_value = two.pow(bitlen);
        BigInteger start = id.add(two.pow(i)).mod(max_value);
        BigInteger end = id.add(two.pow(i+1)).mod(max_value);
        return new FingerEntry(start,end, successor);

    }


    private void rpcAskFingerTable(ChordMessages.AskFingerTable ask){
        sender().tell(new ChordMessages.RespondFingerTable(this.fingerTable), self());
    }

    private FingerEntry updateFingerTablebyRow(FingerEntry cmp, int i){
        FingerEntry newFingerEntry = getFingertableEntry(this.currentNode.id, i, null, this.bitlen);
        if(BigIntUtils.between(cmp.start, cmp.succ.id, newFingerEntry.start)){
            newFingerEntry = new FingerEntry(newFingerEntry.start, newFingerEntry.end, cmp.succ);
        }
        return newFingerEntry;
    }

    private void rpcSuccessorResponce(ChordMessages.SuccessorResp succResp){
        for(int i=0; i < this.fingerTable.length; i++){
            if(this.fingerTable[i].start == succResp.Keyid){
                this.fingerTable[i] = new FingerEntry(this.fingerTable[i].start,this.fingerTable[i].end, succResp.Nodeid);
            }
        }
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()

                .build();
    }
}
