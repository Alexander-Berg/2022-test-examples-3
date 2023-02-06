package ru.yandex.market.checkout.checkouter.tasks.eventinspector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * @author sergeykoles
 * Created on: 14.05.18
 */
@TestComponent
public class YtHttpApiMockConfigurer {
    private static final String AUTH_HEADER = "OAuth SOME_YT_TOKEN";
    private static final String NOT_FOUND = "{\n" +
            "  \"code\": 500,\n" +
            "  \"message\": \"Error resolving path //logs/market-cpa-orders-events-log/1d/1017-11-20/@\",\n" +
            "  \"attributes\": {\n" +
            "    \"fid\": 18441885735445750276,\n" +
            "    \"method\": \"Get\",\n" +
            "    \"tid\": 2944829260570049228,\n" +
            "    \"datetime\": \"2018-05-14T15:19:52.780590Z\",\n" +
            "    \"pid\": 420620,\n" +
            "    \"host\": \"m03-sas.hahn.yt.yandex.net\"\n" +
            "  },\n" +
            "  \"inner_errors\": [\n" +
            "    {\n" +
            "      \"code\": 500,\n" +
            "      \"message\": \"Node //home/logfeller/logs/market-cpa-orders-events-log/1d " +
            "has no child with key \\\"1017-11-20\\\"\",\n" +
            "      \"attributes\": {\n" +
            "        \"fid\": 18441885735445750276,\n" +
            "        \"tid\": 2944829260570049228,\n" +
            "        \"datetime\": \"2018-05-14T15:19:52.780568Z\",\n" +
            "        \"pid\": 420620,\n" +
            "        \"host\": \"m03-sas.hahn.yt.yandex.net\"\n" +
            "      },\n" +
            "      \"inner_errors\": []\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    @Autowired
    private WireMockServer ytHttpApiMock;

    public void configure(YtHttpApiMockParameters ytHttpApiMockParameters) {
        stubGet(ytHttpApiMockParameters);
        ytHttpApiMock.stubFor(WireMock.any(WireMock.anyUrl())
                .withHeader(HttpHeaders.AUTHORIZATION, WireMock.notMatching(AUTH_HEADER))
                .willReturn(new ResponseDefinitionBuilder()
                        .withStatus(401)));
    }

    private void stubGet(YtHttpApiMockParameters ytHttpApiMockParameters) {
        ytHttpApiMock.stubFor(get(anyUrl())
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(AUTH_HEADER))
                .willReturn(new ResponseDefinitionBuilder()
                        .withStatus(400)
                        .withBody(NOT_FOUND)
                )
        );
        ytHttpApiMockParameters.getGetPathToResponse().forEach(
                (k, v) -> ytHttpApiMock.stubFor(get(urlPathEqualTo("/get"))
                        .withQueryParam("path", equalTo(k))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo(AUTH_HEADER))
                        .willReturn(new ResponseDefinitionBuilder()
                                .withStatus(200)
                                .withHeader("Content-type",
                                        MediaType.APPLICATION_JSON_UTF8_VALUE)
                                .withBody(v.get())
                        )
                )
        );
    }
}
