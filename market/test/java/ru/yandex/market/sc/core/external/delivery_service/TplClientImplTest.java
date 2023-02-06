package ru.yandex.market.sc.core.external.delivery_service;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.sc.core.external.delivery_service.model.TplCouriers;
import ru.yandex.market.tpl.api.model.order.partner.ScDamagedOrderList;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static ru.yandex.market.sc.core.external.delivery_service.TplClientImpl.METHOD_GET_DAMAGED_ORDERS_COUNT;

/**
 * @author valter
 */
class TplClientImplTest {

    private static final String URL = "https://tpl-int.tst.vs.market.yandex.net";

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final TplClient tplClient = new TplClientImpl(URL, restTemplate);

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
    void getCouriersByScAndTokenAndDate() {
        tplClient.getCouriers(123L, "token", LocalDate.of(2021, 4, 21));
        String expectedUrl = URL + "/internal/sc/123?date=2021-04-21&scToken=token";
        verify(restTemplate).getForObject(eq(expectedUrl), eq(TplCouriers.class));
    }

    @Test
    void getDamagedOrdersByDateIntervalHasCorrectUri() {
        tplClient.getDamagedOrders(Instant.ofEpochMilli(0L), Instant.ofEpochMilli(10_000L));
        String expectedUrl = URL + "/internal/sc/damagedOrders" +
                "?damagedFrom=1970-01-01T00:00:00Z&damagedTo=1970-01-01T00:00:10Z";
        verify(restTemplate).getForObject(eq(expectedUrl), eq(ScDamagedOrderList.class));
    }

    @Test
    void getDamagedOrdersByIdsHasCorrectUri() {
        tplClient.getDamagedOrders(List.of("o1", "o2", "o3"));
        String expectedUrl = URL + "/internal/sc/damagedOrders?oid=o1&oid=o2&oid=o3";
        verify(restTemplate).getForObject(eq(expectedUrl), eq(ScDamagedOrderList.class));
    }

    @Test
    void getDamagedOrdersByIdsUriTooLongRequestByPart() {
        List<String> externalOrderIds = Collections.nCopies(1500, "123456789");
        HttpRequest request = request()
                .withMethod(HttpMethod.GET.name())
                .withPath("/internal/sc/damagedOrders")
                .withQueryStringParameters(Map.of("oid", externalOrderIds.subList(1, METHOD_GET_DAMAGED_ORDERS_COUNT)));
        HttpResponse response = response()
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("{\"orders\": [{\"externalId\": \"0\", \"isDamaged\": true}]}");
        mockServer.when(request).respond(response);

        String url = String.format("http://%s:%d/", mockServer.remoteAddress().getHostName(),
                mockServer.getLocalPort());
        TplClient tplClient = new TplClientImpl(url, new RestTemplate());
        tplClient.getDamagedOrders(externalOrderIds);
        mockServer.verify(request, VerificationTimes.exactly(3));
    }

    @Test
    @SneakyThrows
    void sendCourierBatchReady() {
        String batchRegisterId = "tpl_10000000123";
        tplClient.sendCourierBatchReady(batchRegisterId);
        String expectedUrl = URL + "/api/batchRegistry/" + batchRegisterId + "/update";
        verify(restTemplate).postForEntity(eq(new URI(expectedUrl)), eq(HttpEntity.EMPTY), eq(Void.class));
    }
}
