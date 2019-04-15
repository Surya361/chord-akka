package chordHashing;

import java.io.Serializable;
import java.math.BigInteger;

/**Finger Entry contains the intervals along
 *  with the id of the successor
 */
public class FingerEntry implements Serializable {
    final BigInteger start;
    final BigInteger end;
    final Node succ;

    public FingerEntry(BigInteger start, BigInteger end, Node succ){
        this.start = start;
        this.end = end;
        this.succ = succ;
    }

    public String toJson(){
        if(this.succ != null){
            return "[ "+ this.start.toString() +" , "+ this.end.toString() +" , "+this.succ.id.toString()+" ]";
        }
        return "[ "+ this.start.toString() +" , "+ this.end.toString() +" , "+"  Null  "+" ]";
    }
}
