package ru.yandex.market.checkout.checkouter.tasks.eventinspector.yql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.curator.shaded.com.google.common.base.Throwables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@TestComponent
public class YQLMockConfigurer {

    @Autowired
    private WireMockServer yqlMock;
    @Value("${market.checkout.yt.token}")
    private String yqlToken;

    private ObjectMapper mapper = new ObjectMapper();

    public void configure(YQLMockParameters parameters) {
        mockCreate(parameters);
        mockStatus(parameters);
        mockResultData(parameters);

        yqlMock.stubFor(WireMock.any(WireMock.anyUrl())
                .withHeader(HttpHeaders.AUTHORIZATION, WireMock.notMatching("OAuth " + yqlToken))
                .willReturn(new ResponseDefinitionBuilder().withStatus(401))
        );
    }

    private void mockCreate(YQLMockParameters parameters) {
        String body = writeValueAsJson(parameters.getCreateResponse());

        yqlMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/operations"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withBody(body)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
    }

    private void mockStatus(YQLMockParameters parameters) {
        String body = writeValueAsJson(parameters.getStatusResponse());

        yqlMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/operations/" +
                parameters.getCreateResponse().getId()))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withBody(body)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
    }

    private void mockResultData(YQLMockParameters parameters) {
        String body = writeValueAsJson(parameters.getOperationData());

        yqlMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/operations/" +
                parameters.getCreateResponse().getId() + "/results_data"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withBody(body)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
    }

    private <T> String writeValueAsJson(T value) {
        String body;
        try {
            body = mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw Throwables.propagate(e);
        }
        return body;
    }
}
