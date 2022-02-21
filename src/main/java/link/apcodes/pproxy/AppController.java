package link.apcodes.pproxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/**")
    public Mono<ResponseEntity<byte[]>> get(ServerWebExchange exchange) {
        return webClient.get().uri(exchange.getRequest().getPath().toString()).exchangeToMono(this::toEntity);
    }

    private Mono<ResponseEntity<byte[]>> toEntity(ClientResponse clientResponse) {
        return clientResponse.toEntity(byte[].class);
    }
}
