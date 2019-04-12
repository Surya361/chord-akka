package chordHashing;

import java.math.BigInteger;

/**Finger Entry contains the intervals along
 *  with the id of the successor
 */
public class FingerEntry {
    BigInteger start;
    BigInteger end;
    BigInteger id;

    public FingerEntry(BigInteger start, BigInteger end, BigInteger id){
        this.start = start;
        this.end = end;
        this.id = id;
    }
}
