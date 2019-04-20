package chordHashing;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;


public class ApiRoutes extends AllDirectives {

    final private ActorRef chordActor;
    final private LoggingAdapter log;

    public ApiRoutes(ActorSystem system, ActorRef chordActor){
        this.chordActor = chordActor;
        this.log = Logging.getLogger(system, this);
    }
    Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));

    public Route routes(){
        return route(
                pathPrefix("key", () ->
                        route(
                                path(PathMatchers.segment(), id -> route(
                                        getNode(id)
                                        )

                                )
                              )
                ),
                pathPrefix("nodeinfo", () ->
                        route(
                                getNodeinfo()
                        )
                ),
                pathPrefix("debug", () ->
                        route(
                                getDebuginfo()
                        )
                ),
                pathPrefix("redundancyinfo", () ->
                        route(
                                getRedundancyInfo()
                        )
                )
        );
    }

    private Route getNode(String id){
        return  get( () -> {
            Future<Object> getSucessor = Patterns.ask(chordActor, new ChordMessages.FindSuccessor(new BigInteger(id)),
                    timeout);
            CompletionStage<ChordMessages.SuccessorResp> succNode = FutureConverters.toJava(getSucessor)
                    .thenApply(ChordMessages.SuccessorResp.class::cast);
            return onSuccess(() -> succNode,
                    nodeInfo -> complete(StatusCodes.OK, nodeInfo.toJson()));
        });

    }

    private Route getNodeinfo() {
        return get(() -> {
            Future<Object> getNodeInfo = Patterns.ask(chordActor, new ChordMessages.NodeInfo(),
                    timeout);
            CompletionStage<ChordMessages.NodeInfoResp> node = FutureConverters.toJava(getNodeInfo)
                    .thenApply(ChordMessages.NodeInfoResp.class::cast);
            return onSuccess(() -> node,
                    nodeInfo -> complete(StatusCodes.OK, nodeInfo.toJson()));
        });

    }
    private Route getDebuginfo() {
        return get(() -> {
            Future<Object> getNodeInfo = Patterns.ask(chordActor, new ChordMessages.DebugInfo(),
                    timeout);
            CompletionStage<ChordMessages.RespondFingerTable> node = FutureConverters.toJava(getNodeInfo)
                    .thenApply(ChordMessages.RespondFingerTable.class::cast);
            return onSuccess(() -> node,
                    nodeInfo -> complete(StatusCodes.OK, nodeInfo.toJson()));
        });

    }
    private Route getRedundancyInfo() {
        return get(() -> {
            Future<Object> getNodeInfo = Patterns.ask(chordActor, new ChordMessages.RedundancyInfo(),
                    timeout);
            CompletionStage<ChordMessages.RespRedundancyInfo> node = FutureConverters.toJava(getNodeInfo)
                    .thenApply(ChordMessages.RespRedundancyInfo.class::cast);
            return onSuccess(() -> node,
                    nodeInfo -> complete(StatusCodes.OK, nodeInfo.toJson()));
        });

    }
}
