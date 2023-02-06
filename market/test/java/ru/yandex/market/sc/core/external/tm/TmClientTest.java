package ru.yandex.market.sc.core.external.tm;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.external.tm.model.PutScStateRequest;
import ru.yandex.market.sc.core.external.tm.model.TmBox;
import ru.yandex.market.sc.core.external.tm.model.TmCenterType;
import ru.yandex.market.sc.core.external.tm.model.TmPallet;
import ru.yandex.market.tpl.common.util.TplObjectMappers;

import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.verify.VerificationTimes.once;

class TmClientTest {

    private final OffsetDateTime now = OffsetDateTime.of(2021, 5, 31, 23, 21, 10, 0, ZoneOffset.ofHours(3));

    private ClientAndServer mockServer;

    @BeforeEach
    void init() {
        try {
            mockServer = startClientAndServer(SocketUtils.findAvailableTcpPort());
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AfterEach
    public void stopMockServer() {
        mockServer.stop();
    }

    @Test
    void putScState() {
        SortingCenter sortingCenter = mock(SortingCenter.class);
        when(sortingCenter.getYandexId()).thenReturn("134");
        var body = PutScStateRequest.ofPallets(List.of(
                new TmPallet("XDOC-1", 100, "10", now, Collections.emptyList(), TmCenterType.DISTRIBUTION_CENTER),
                new TmPallet("XDOC-4", 200, null, null, List.of(
                        new TmBox("XDOC-2", "11", now),
                        new TmBox("XDOC-3", "12", now)
                ), TmCenterType.DISTRIBUTION_CENTER)
        ));

        HttpRequest request = request()
                .withMethod(HttpMethod.PUT.name())
                .withPath("/distribution_center/134/state")
                .withBody(json("{"
                        + "  \"pallets\": ["
                        + "    {"
                        + "      \"id\": \"XDOC-1\","
                        + "      \"target_point_id\": 100,"
                        + "      \"inbound_id\": \"10\","
                        + "      \"inbound_datetime\": \"2021-05-31T23:21:10+03:00\","
                        + "      \"boxes\": []"
                        + "    },"
                        + "    {"
                        + "      \"id\": \"XDOC-4\","
                        + "      \"target_point_id\": 200,"
                        + "      \"boxes\": ["
                        + "        {"
                        + "          \"id\": \"XDOC-2\","
                        + "          \"inbound_id\": \"11\","
                        + "          \"inbound_datetime\": \"2021-05-31T23:21:10+03:00\""
                        + "        },"
                        + "        {"
                        + "          \"id\": \"XDOC-3\","
                        + "          \"inbound_id\": \"12\","
                        + "          \"inbound_datetime\": \"2021-05-31T23:21:10+03:00\""
                        + "        }"
                        + "      ]"
                        + "    }"
                        + "  ]"
                        + "}"
                ));
        mockServer.when(request).respond(HttpResponse.response().withStatusCode(OK_200));
        var url = String.format("http://%s:%d/", mockServer.remoteAddress().getHostName(), mockServer.getLocalPort());
        var stringMessageConverter = new StringHttpMessageConverter();
        stringMessageConverter.setSupportedMediaTypes(List.of(MediaType.ALL));
        var tmClient = new TmClient(url, new RestTemplate(
                List.of(stringMessageConverter, new MappingJackson2HttpMessageConverter(
                        TplObjectMappers.baseObjectMapper())))
        );
        tmClient.putScState(sortingCenter, body);
        mockServer.verify(request, once());
    }
}
