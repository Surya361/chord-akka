package chordHashing;


import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Class contains node identifies
 *  ip, port (NOTE: use ActorSelection instead of ActorRef for remote actors)
 */
public class Node implements Serializable {
    final BigInteger id;
    final String ip;
    final String port;

    public Node(String ip, String port){
        this.ip = ip;
        this.port = port;
        try{
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            byte[] messageDigest = sha1.digest((ip+":"+port).getBytes());
            this.id = new BigInteger(1, messageDigest);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String toJson(){
        return "{" +
                "id: " + this.id.toString() + "," +
                "ip: " + this.ip +  "," +
                "port: " + this.port + "," +
                "ActorPath : " + this.ActorPath() + "," +
                "}";
    }



    public String ActorPath(){
        return "akka.tcp://chord@"+this.ip+":"+this.port+"/user/nodeManager/node";
    }

}
