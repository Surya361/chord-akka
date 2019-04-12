package chordHashing;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.cli.*;

import java.time.Duration;


public class ChordMain {

     static CommandLine parseFlags(String[] args){
        Options options = new Options();
        Option port = new Option("l", "listen", true, "listen port");
        port.setRequired(true);
        options.addOption(port);

         Option ip = new Option("ip", "ip", true, "listen ip");
         ip.setRequired(true);
         options.addOption(ip);

        Option seedNode = new Option("sip", "seedIp", true, "seedNode ip");
        options.addOption(seedNode);

         Option seedPort = new Option("sp", "seedPort", true, "seedNode port");
         options.addOption(seedPort);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            return cmd;
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }
        return null;

    }




    public static void main(String[] args) {
        CommandLine cmd = parseFlags(args);
        Node currNode = new Node(cmd.getOptionValue("ip" ), cmd.getOptionValue("l"));

        Config config = ConfigFactory.parseString(
                "akka.remote.netty.tcp.port=" + cmd.getOptionValue("l")).withFallback(
                ConfigFactory.load());

        ActorSystem system = ActorSystem.create("chord", config);
        ActorRef nodeActorref;
        if (cmd.getOptionValue("sip") == null){
            nodeActorref = system.actorOf(ChordInitActor.props("","",true, currNode,160), "nodeManager");
        } else {
            nodeActorref = system.actorOf(ChordInitActor.props(cmd.getOptionValue("sip"),cmd.getOptionValue("sp"),true, currNode,160), "nodeManager");
        }
        system.scheduler()
                .schedule(
                        Duration.ZERO, Duration.ofMillis(10000), nodeActorref, new ChordMessages.Remainder(), system.dispatcher(), null);


    }
}
