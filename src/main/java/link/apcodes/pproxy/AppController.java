package link.apcodes.pproxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RequestMapping("/")
@Controller
public class AppController {
    final WebClient webClient;

    public AppController(AppProperties appProperties) {
        webClient = WebClient.builder().baseUrl(appProperties.getUrl()).build();
    }

    @RequestMapping("/**")
    public Mono<ResponseEntity<byte[]>> request(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        if (method == null) {
            return Mono.error(new RuntimeException("unknown method: " + exchange.getRequest().getMethodValue()));
        }

        return webClient.method(method).uri(exchange.getRequest().getPath().toString()).exchangeToMono(this::toEntity);
    }

    private Mono<ResponseEntity<byte[]>> toEntity(ClientResponse clientResponse) {
        return clientResponse.toEntity(byte[].class);
    }
}
