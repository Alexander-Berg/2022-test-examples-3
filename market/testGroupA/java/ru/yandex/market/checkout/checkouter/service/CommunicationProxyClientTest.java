package ru.yandex.market.checkout.checkouter.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.service.communicationproxy.CommunicationProxyClient;
import ru.yandex.market.checkout.checkouter.service.communicationproxy.api.CallResolution;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CommunicationProxyClientTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer communicationProxyMock;
    @Autowired
    private CommunicationProxyClient communicationProxyClient;

    @AfterEach
    public void resetMocks() {
        communicationProxyMock.resetAll();
    }

    @Test
    public void shouldParseResponseCorrectly() {
        communicationProxyMock.stubFor(
                post(urlPathEqualTo("/calls"))
                        .willReturn(okJson("{\n" +
                                "    \"pageNum\": 0,\n" +
                                "    \"pageSize\": 1,\n" +
                                "    \"totalElements\": 1,\n" +
                                "    \"totalPages\": 1,\n" +
                                "    \"calls\": [\n" +
                                "        {\n" +
                                "            \"resolution\": \"INVALID_NUMBER\",\n" +
                                "            \"started\": \"2022-04-12T15:02:24.382+03:00\",\n" +
                                "            \"dialStarted\": \"2022-04-12T15:02:24.382+03:00\",\n" +
                                "            \"ended\": \"2022-04-12T15:02:24.382+03:00\"\n" +
                                "        },\n" +
                                "        {\n" +
                                "           \"resolution\": \"INVALID_NUMBER\",\n" +
                                "           \"started\": \"2022-04-12T15:02:24.382+03:00\",\n" +
                                "           \"dialStarted\": \"2022-04-12T15:02:24.382+03:00\",\n" +
                                "           \"ended\": \"2022-04-12T15:02:24.382+03:00\"\n" +
                                "       }\n" +
                                "    ]\n" +
                                "}"))
        );

        var resp = communicationProxyClient.calls(1L, Set.of(CallResolution.INVALID_NUMBER), 1, 10);
        assertNotNull(resp);
        assertEquals(resp.getPageNum(), 0);
        assertEquals(resp.getPageSize(), 1);
        assertEquals(resp.getTotalElements(), 1);
        assertEquals(resp.getTotalPages(), 1);
        assertNotNull(resp.getCalls());
        assertEquals(resp.getCalls().size(), 2);
        var call = resp.getCalls().get(0);
        assertEquals(CallResolution.INVALID_NUMBER, call.getResolution());
        assertEquals(ZonedDateTime.parse("2022-04-12T15:02:24.382+03:00",
                                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))
                        .withZoneSameInstant(ZoneId.of("UTC")),
                call.getStarted());
        assertEquals(ZonedDateTime.parse("2022-04-12T15:02:24.382+03:00",
                                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))
                        .withZoneSameInstant(ZoneId.of("UTC")),
                call.getDialStarted());
        assertEquals(ZonedDateTime.parse("2022-04-12T15:02:24.382+03:00",
                                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))
                        .withZoneSameInstant(ZoneId.of("UTC")),
                call.getEnded());
    }
}
