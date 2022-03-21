package link.apcodes.pproxy;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.function.BiFunction;

public class Main {
    public static void main(String[] args) {
        HttpServer httpServer = HttpServer.create().route((HttpServerRoutes httpServerRoutes) -> {
            System.out.println("routing configured");
            httpServerRoutes.get("/**", new BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>() {
                @Override
                public Publisher<Void> apply(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
                    return httpServerResponse.sendString(Flux.just("a", "b", "c", "\n").delayElements(Duration.ofMillis(1000)));
                }
            });
        });

        Mono<? extends DisposableServer> bind = httpServer.bindAddress(() -> new InetSocketAddress(9000)).bind();
        DisposableServer disposableServer = bind.block();
        Objects.requireNonNull(disposableServer);

        int port = disposableServer.port();


        String addr = "http://localhost:" + port;
        System.out.println(addr);
        // String block = WebClient.create(addr).get().uri("/").retrieve().bodyToMono(String.class).block();
        // System.out.println();
        Runtime.getRuntime().addShutdownHook(new Thread(disposableServer::dispose));
        try {
            Thread.sleep(10000000L);
        } catch (Throwable t) {
            System.out.println(t);
        }
    }
}
