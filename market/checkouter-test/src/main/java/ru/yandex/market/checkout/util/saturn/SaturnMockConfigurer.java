package ru.yandex.market.checkout.util.saturn;

import java.io.IOException;
import java.nio.charset.Charset;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.checkouter.saturn.ScoringRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@TestComponent
public class SaturnMockConfigurer {

    @Autowired
    private WireMockServer saturnMock;

    private static String getStringBodyFromFile(String fileName, Object... args) {
        try {
            String response = IOUtils.toString(
                    SaturnMockConfigurer.class.getResourceAsStream(fileName),
                    Charset.defaultCharset());
            return String.format(response, args);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void mockScoring(ScoringRequest request) {
        MappingBuilder builder = post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.request_id", equalTo(request.getRequestId())))
                .withRequestBody(matchingJsonPath("$.puid", equalTo(request.getPuid().toString())))
                .withRequestBody(matchingJsonPath("$.service", equalTo(request.getService())))
                .withRequestBody(matchingJsonPath("$.basket.total_sum",
                        equalTo(request.getBasket().getTotalSum().toString())))
                .withRequestBody(matchingJsonPath("$.basket.credit_sum",
                        equalTo(request.getBasket().getCreditSum().toString())))
                .willReturn(ok()
                        .withBody(getStringBodyFromFile("scoringResponse.json", request.getPuid().toString()))
                        .withHeader("Content-Type", "application/json"));

        saturnMock.stubFor(builder);
    }

    public void mockScoring(ScoringRequest request, Double score) {
        MappingBuilder builder = post(urlEqualTo("/"))
                .willReturn(ok()
                        .withBody(getStringBodyFromFile(
                                "scoringResponseWithScore.json",
                                request.getPuid().toString(),
                                score
                        ))
                        .withHeader("Content-Type", "application/json"));

        saturnMock.stubFor(builder);
    }
}
