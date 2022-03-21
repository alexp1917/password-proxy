package link.apcodes.pproxy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.time.Duration;
import java.util.Objects;

@SpringBootTest
public class AppControllerTest {
    WebClient webClient = WebClient.builder().filter(new ExchangeFilterFunction() {
        @Override
        public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
            return null;
        }
    }).build();

    int port;
    DisposableServer disposableServer;

    @BeforeEach
    void setUp() {
        HttpServer httpServer = HttpServer.create().handle((HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) -> {
            // if (2 > 1) {
            //     NettyOutbound abc = httpServerResponse.sendString(Mono.just("abc"));
            //     return abc;
            // }
            final int count = Integer.parseInt(Objects.requireNonNullElse(httpServerRequest.param("count"), "50"));
            final int delay = Integer.parseInt(Objects.requireNonNullElse(httpServerRequest.param("delay"), "1000"));
            return httpServerResponse.sendString(Flux.create(emitter -> {
                System.out.println("created flux");
                emitter.onRequest(consumer -> {
                    Flux.interval(Duration.ofMillis(delay)).take(count).map(iteration -> {
                        emitter.next("abc" + iteration);
                        return iteration;
                    }).last().subscribe(s -> {
                        System.out.println("done");
                        emitter.complete();
                    });
                });
            }));
        });
        System.out.println("binding");
        disposableServer = httpServer.bindNow();
        System.out.println("bound");
        port = disposableServer.port();
    }

    @AfterEach
    void tearDown() {
        disposableServer.dispose();
        disposableServer.onDispose().block();
    }

    @Test
    void test() {
        long t0 = System.currentTimeMillis();
        // String block = webClient.get().uri("http://localhost:" + port + "/abc").retrieve().bodyToMono(String.class).block();
        Mono<ResponseEntity<Flux<DataBuffer>>> responseEntityMono = webClient.get().uri("http://localhost:" + port + "/abc").exchangeToMono(c -> c.toEntity(new ParameterizedTypeReference<Flux<DataBuffer>>() {
        }));
        long t1 = System.currentTimeMillis();
        System.out.println("elapsed: " + (t1 - t0));
        ResponseEntity<Flux<DataBuffer>> block = responseEntityMono.block();
        long t2 = System.currentTimeMillis();
        System.out.println("elapsed: " + (t2 - t0));
        System.out.println("okay");
        // System.out.println("got back: " + block +" after: " + (t1 - t0));
    }
}
