package chordHashing;

import java.math.BigInteger;

public class BigIntUtils {
     static boolean greaterthan(BigInteger a, BigInteger b, boolean equal){
        if (a.compareTo(b) == 1 || (equal && a.compareTo(b) == 0)) {
            return true;
        }
        return false;
    }

     static boolean lessthan(BigInteger a, BigInteger b, boolean equal){
        if (a.compareTo(b) == -1 || (equal && a.compareTo(b) == 0)) {
            return true;
        }
        return false;
    }

     static boolean between(BigInteger a, BigInteger b, BigInteger id){
        if(lessthan(a, b, false)){
            if(greaterthan(id, a, true) && lessthan(id, b, false)){
                return true;
            }
        } else{
            if(greaterthan(id, a, true) || lessthan(id, b, false)){
                return true;
            }
        }
        return false;
    }


}
