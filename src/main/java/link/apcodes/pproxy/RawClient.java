package link.apcodes.pproxy;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

public class RawClient {
    public static void main(String[] args) {
        HttpClient httpClient = HttpClient.create().baseUrl("http://localhost:9000");
        Mono<HttpClientResponse> response = httpClient.request(HttpMethod.GET)
                .send(Mono.empty())
                .response((HttpClientResponse r, ByteBufFlux b) -> {
                    return b
                            .map(byteBuf -> {
                                System.out.println("got a new byteBuf");
                                System.out.println(read(byteBuf));
                                return byteBuf;
                            })
                            .then(Mono.just(r))
                            ;
                }).last();

        HttpClientResponse httpClientResponse = response.block();
        HttpHeaders headers = httpClientResponse.responseHeaders();
        HttpResponseStatus status = httpClientResponse.status();
        System.out.println("httpClientResponse: " + httpClientResponse);
        System.out.println("headers: " + headers);
        System.out.println("status: " + status);
    }

    private static String read(ByteBuf byteBuf) {
        int i = byteBuf.readableBytes();
        byte[] bytes = new byte[i];
        byteBuf.readBytes(bytes);
        return new String(bytes);
    }
}
