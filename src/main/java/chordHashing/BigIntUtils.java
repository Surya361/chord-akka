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

    static boolean between(BigInteger a , BigInteger b , BigInteger id, boolean strict){
        if(lessthan(a, b, false)){
            if(greaterthan(id, a, false) && lessthan(id, b, false)){
                return true;
            }
        } else{
            if(greaterthan(id, a, false) || lessthan(id, b, false)){
                return true;
            }
        }
        return false;
    }

    static BigInteger gobackpred(BigInteger id, int i, int bitlen){
        BigInteger two = BigInteger.valueOf(2);
        BigInteger max_value = two.pow(bitlen);
        BigInteger start = id.subtract(two.pow(i));
        if(start.signum() < 0){
            return start.add(max_value);
        } else {
            return start;
        }
    }


}
