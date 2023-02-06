package ru.yandex.market.logistic.pechkin.client;

import org.junit.jupiter.api.Test;
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;


public class PechkinHttpClientTest extends AbstractClientTest{


    @Test
    void test() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/message"))
            .withHeader("X-Ya-Service-Ticket", equalTo("100"))
            .willReturn(aResponse().withStatus(200)));

        MessageDto messageDto = new MessageDto();
        messageDto.setSender("me");
        messageDto.setMessage("hello");
        messageDto.setChannel("me");

        final PechkinHttpClient pechkinHttpClient =
            new PechkinHttpClient("http://127.0.0.1:1111", () -> "100");
        pechkinHttpClient.sendMessage(messageDto);

        wireMockServer.verify(postRequestedFor(urlMatching("/message"))
            .withRequestBody(equalTo(objectMapper.writeValueAsString(messageDto)))
            .withHeader("X-Ya-Service-Ticket", equalTo("100")));
    }
}
