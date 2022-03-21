package link.apcodes.pproxy;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class Client {
    public static void main(String[] args) {
        WebClient.create("http://localhost:9000")
                .get().uri("/")
                // .exchangeToMono(c -> Mono.just(new AppController.Thing(c)))
                .exchangeToMono(c -> Mono.just(c))
                .subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.boundedElastic())
                .flatMap(thing -> {
                    System.out.println("flat mapping");
                    List<byte[]> block = thing.bodyToFlux(byte[].class).collectList().block();
                    return Mono.empty();
                    // return thing.body().map(dataBuffer -> {
                    //     String s = new String(dataBuffer.asByteBuffer().array(), StandardCharsets.UTF_8);
                    //     System.out.println("got a new string: " + s);
                    //     return s;
                    // })
                    //         .collectList();
                })
                .block();
    }
}
