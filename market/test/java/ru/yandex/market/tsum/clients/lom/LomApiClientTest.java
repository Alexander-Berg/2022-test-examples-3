package ru.yandex.market.tsum.clients.lom;

import java.time.Instant;
import java.util.List;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;

public class LomApiClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    LomApiClient lomApiClient = new LomApiClient(new NettyHttpClientContext(new HttpClientConfig()), 0, "");

    @Test
    public void getOrderTracksByOrderId() {
        mockStub();
        lomApiClient.setBaseUrl("http://localhost:" + wireMockRule.port());
        List<OrderTrack> tracks = lomApiClient.getOrderTracks(
            1L,
            ImmutableList.of("123")
        );
        assertEquals(tracks.size(), 1);
        assertEquals(tracks.get(0), getOrderTrack());
    }

    @Test
    public void getShootingOrdersProcessingStatus() {
        mockStub();
        lomApiClient.setBaseUrl("http://localhost:" + wireMockRule.port());
        ShootingOrdersProcessingStatus status = lomApiClient.getShootingOrdersProcessingStatus(
            Instant.parse("2020-08-01T01:00:00Z"),
            Instant.parse("2020-08-01T11:00:00Z"),
            1L, 10L);
        assertEquals(2, status.getTotalOrders());
        assertEquals(1, status.getProcessedOrders());
    }

    private void mockStub() {
        wireMockRule.stubFor(put(urlEqualTo("/orders/tracks/search"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(equalToJson("{ \"externalIds\" : [ \"123\" ], \"platformClientId\" : 1 }"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("[ { \"externalId\": \"123\", \"trackerId\": 1001 } ]")));
        wireMockRule.stubFor(put(urlEqualTo("/shooting/orders-processing-status"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(equalToJson("{" +
                "  \"orderCreatedFrom\" : 1596243600000, " +
                "  \"orderCreatedTo\" : 1596279600000, " +
                "  \"uidRangeLowerBound\":1, " +
                "  \"uidRangeUpperBound\":10 " +
                "}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{ \"totalOrders\": 2, \"processedOrders\": 1 }")));
    }

    private OrderTrack getOrderTrack() {
        OrderTrack track = new OrderTrack();
        track.setExternalId("123");
        track.setTrackerId(1001L);
        return track;
    }
}
