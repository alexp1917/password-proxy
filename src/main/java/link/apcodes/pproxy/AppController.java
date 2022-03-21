package link.apcodes.pproxy;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.HttpOperations;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static io.netty.handler.codec.http.HttpMethod.valueOf;

@Slf4j
@RequestMapping("/")
@Controller
public class AppController {
    final WebClient webClient;
    final HttpClient client;

    public AppController(AppProperties appProperties) {
        webClient = WebClient.builder().baseUrl(appProperties.getUrl()).build();
        client = HttpClient.create().baseUrl(appProperties.getUrl());
    }

    @RequestMapping("/**")
    public Mono<ResponseEntity<Flux<byte[]>>> forward(ServerHttpRequest exchange) {
        HttpHeaders headers = exchange.getHeaders();
        HttpClient.RequestSender requestSender = client
                .headers(h -> headers.forEach(h::add))
                .request(valueOf(exchange.getMethodValue()))
                .uri(exchange.getPath().toString());
        // requestSender.response(((httpClientResponse, byteBufFlux) -> {
        //     return byteBufFlux
        //             .map(byteBuf -> {
        //                 byte[] b = new byte[byteBuf.readableBytes()];
        //                 byteBuf.readBytes(b);
        //                 System.out.println("got a new byte array: " + new String(b));
        //                 return b;
        //             })
        //             .then(Mono.just(httpClientResponse));
        // })).subscribe();
        Mono<HttpClientResponse> response = requestSender.send(Flux.empty()).response();
        // response.cache();
        var fl = response
                .map(HttpOperations.class::cast)
                .map(HttpOperations::receive)
                .flatMapMany(ByteBufFlux::asByteArray);
        // Mono<ResponseEntity<Flux<byte[]>>> a = requestSender.response((res, byteBufFlux) -> {
        //     // if (res instanceof HttpOperations) {
        //     //     ByteBufFlux receive = ((HttpOperations<?, ?>) res).receive();
        //     // }
        //     // Flux<byte[]> f = Flux.just(new byte[]{'d'}, new byte[]{'e'}, new byte[]{'f'}, new byte[]{'\n'}).delayElements(Duration.ofMillis(1000));
        //     Flux<byte[]> f =
        //             // Mono.delay(Duration.ofMillis(500))
        //             //         .then()
        //             //         .flatMapMany(__ -> byteBufFlux.map(AppController::bbToString))
        //                     byteBufFlux
        //                             // .subscribeOn(Schedulers.boundedElastic())
        //                             // .publishOn(Schedulers.boundedElastic())
        //                             .asByteArray()
        //                             .switchIfEmpty(Mono.just("missing".getBytes(StandardCharsets.UTF_8)))
        //                             .onErrorResume(e -> Mono.just("error".getBytes(StandardCharsets.UTF_8)))
        //                             .doOnNext(b -> System.out.println("got a new byte array: " + new String(b)));
        //     System.out.println("creating outgoing headers");
        //     MultiValueMap<String, String> h = new LinkedMultiValueMap<>();
        //     res.responseHeaders().entries().forEach(e -> h.add(e.getKey(), e.getValue()));
        //     ResponseEntity<Flux<byte[]>> entity = new ResponseEntity<>(f, h, res.status().code());
        //     System.out.println("entity: " + entity);
        //     return Mono.just(entity);
        // })
        //         .doOnNext(s -> System.out.println("NEXT: " + s))
        //         .next();
        // System.out.println("a: " + a);
        // a.doOnEach(fluxResponseEntity -> System.out.println("emitted next: " + fluxResponseEntity));
        // return a;
        // {
        //     Flux<byte[]> f = Flux.just(new byte[]{'d'}, new byte[]{'e'}, new byte[]{'f'}, new byte[]{'\n'}).delayElements(Duration.ofMillis(1000));
        //     MultiValueMap<String, String> h = new LinkedMultiValueMap<>();
        //     return Mono.just(new ResponseEntity<>(f, h, 200));
        // }
        {
            MultiValueMap<String, String> h = new LinkedMultiValueMap<>();
            return Mono.just(new ResponseEntity<>(fl, h, 200));
        }
    }

    // public Mono<ResponseEntity<Flux<DataBuffer>>> request(ServerWebExchange exchange) {
    public Mono<Void> request(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        if (method == null) {
            // return Mono.error(new RuntimeException("unknown method: " + exchange.getRequest().getMethodValue()));
            throw new RuntimeException("unknown method: " + exchange.getRequest().getMethodValue());
        }

        ServerHttpResponse response = exchange.getResponse();
        System.out.println("server response is a " + response.getClass().getName());

        return webClient.method(method)
                .uri(exchange.getRequest().getPath().toString())
                .headers(h -> h.putAll(exchange.getRequest().getHeaders()))
                // .exchangeToMono(c -> c.toEntity(byte[].class))
                // .retrieve().toEntityFlux(DataBuffer.class)
                .exchangeToMono(c -> Mono.just(new Thing(c)))
                .flatMap((Thing thing) -> {
                    // response.getHeaders().clear();
                    response.getHeaders().putAll(thing.httpHeaders());
                    response.setStatusCode(thing.httpStatus());
                    return response.writeWith(thing.body());
                })
                .map(f -> f)
                ;
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class Thing {
        HttpStatus httpStatus;
        HttpHeaders httpHeaders;
        Flux<DataBuffer> body;

        public Thing(ClientResponse clientResponse) {
            this.httpStatus(clientResponse.statusCode());
            this.httpHeaders(clientResponse.headers().asHttpHeaders());
            this.body(clientResponse.bodyToFlux(DataBuffer.class));
        }
    }
}
