package chordHashing;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

public class RestApiServer extends AllDirectives {

    private final ApiRoutes apiRoutes;

    public RestApiServer(ActorSystem system, ActorRef chordActor){
        apiRoutes = new ApiRoutes(system, chordActor);
    }

    public static void startServer(ActorSystem system, ActorRef chordActor, String host, int port){
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        RestApiServer server = new RestApiServer(system, chordActor);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = server.createRoute().flow(system, materializer);
        http.bindAndHandle(routeFlow, ConnectHttp.toHost(host, port), materializer);

        System.out.println("Server online at http://"+host+":"+port+"/");

    }


    protected Route createRoute() {
        return apiRoutes.routes();
    }

}
