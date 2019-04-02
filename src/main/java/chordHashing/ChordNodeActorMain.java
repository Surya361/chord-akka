package chordHashing;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;


public class ChordNodeActorMain {
    public static void main(String[] args) {
        Config config = ConfigFactory.parseString(
                "akka.remote.netty.tcp.port=" + args[0]).withFallback(
                ConfigFactory.load());

        // Create an Akka system
        ActorSystem system = ActorSystem.create("chordHashing", config);
        long id;
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(args[0].getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
             id = Math.abs(no.intValue());

        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        ActorRef nodeActorref;
        if (args[2].equals("true") ) {
             nodeActorref = system.actorOf(ChordNodeActor.props(Math.abs(id), 32, true, args[1] ), "node");
        } else {
            nodeActorref = system.actorOf(ChordNodeActor.props(Math.abs(id), 32, false, args[1] ), "node");
        }


      //  ActorSelection selection =
               // system.actorSelection("akka.tcp://chordHashing@127.0.0.1:"+ args[1]+"/user/node");
       // selection.tell(new ChordNodeMessages.GetSuccessor(id), nodeActorref);

        system.scheduler()
                .schedule(
                        Duration.ZERO, Duration.ofMillis(5000), nodeActorref, new ChordNodeMessages.Remainder(), system.dispatcher(), null);

        /*try{
            Thread.sleep(10000);
        }
        catch (InterruptedException e){
            throw new RuntimeException(e);
        }

        system.actorOf(ChordNodeActor.props(Math.abs(10000), 64), "node");*/
    }

}
