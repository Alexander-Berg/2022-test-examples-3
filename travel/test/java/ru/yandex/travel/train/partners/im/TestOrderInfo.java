package ru.yandex.travel.train.partners.im;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.opentracing.mock.MockTracer;
import org.asynchttpclient.Dsl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.testing.misc.TestResources;
import ru.yandex.travel.train.partners.im.model.ImBlankStatus;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

public class TestOrderInfo {
    private static final Logger logger = LoggerFactory.getLogger(TestOrderInfo.class);
    private ImClient client;

    @Rule
    public WireMockRule wireMockRule
            = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        var testClient = Dsl.asyncHttpClient(Dsl.config()
                .setThreadPoolName("imAhcPool")
                .setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build())
                .build());
        var clientWrapper = new AsyncHttpClientWrapper(
                testClient, logger, "testDestination", new MockTracer(),
                Arrays.stream(DefaultImClient.Method.values()).map(Enum::name).collect(Collectors.toSet())
        );
        client = new DefaultImClient(clientWrapper, "pos", Duration.ofSeconds(10),
                String.format("http://localhost:%d/", wireMockRule.port()),"ya", "***");
    }

    @Test
    public void testOrderInfo(){
        stubFor(post(anyUrl()).willReturn(aResponse()
                .withBody(TestResources.readResource("OrderInfoResponse.json"))));

        var response = client.orderInfo(1234567);

        verify(postRequestedFor(urlPathEqualTo("/Order/V1/Info/OrderInfo")).withRequestBody(equalToJson(
                TestResources.readResource("OrderInfoRequest.json")
        )));

        assertThat(response.getOrderItems().size()).isEqualTo(7);
        var buyRailwayItem = response.findBuyRailwayItems().get(0);
        assertThat(buyRailwayItem.getSimpleOperationStatus()).isEqualTo(ImOperationStatus.OK);
        assertThat(buyRailwayItem.getOrderItemId()).isEqualTo(34984186);
        assertThat(buyRailwayItem.getReservationNumber()).isNotNull();
        assertThat(buyRailwayItem.getOrderItemBlanks().size()).isEqualTo(2);
        assertThat(buyRailwayItem.getOrderItemBlanks().get(0).getBlankStatus()).isEqualTo(ImBlankStatus.REFUNDED);
        assertThat(buyRailwayItem.getOrderItemBlanks().get(1).getBlankStatus()).isEqualTo(ImBlankStatus.REFUNDED);
        assertThat(buyRailwayItem.getOrderItemCustomers().size()).isEqualTo(2);
        assertThat(buyRailwayItem.getIsExternallyLoaded()).isNotNull();

        var refundRailwayItems = response.findRefundRailwayItems();
        assertThat(refundRailwayItems.size()).isEqualTo(2);

        assertThat(refundRailwayItems.get(0).getSimpleOperationStatus()).isEqualTo(ImOperationStatus.OK);
        assertThat(refundRailwayItems.get(0).getOrderItemId()).isEqualTo(34984792);
        assertThat(refundRailwayItems.get(0).getAgentReferenceId()).isNotNull();
        assertThat(refundRailwayItems.get(0).getPreviousOrderItemId()).isEqualTo(buyRailwayItem.getOrderItemId());
        assertThat(refundRailwayItems.get(0).getOrderItemBlanks().size()).isEqualTo(1);
        assertThat(refundRailwayItems.get(0).getOrderItemBlanks().get(0).getBlankStatus()).isEqualTo(ImBlankStatus.NO_REMOTE_CHECK_IN);
        assertThat(refundRailwayItems.get(0).getOrderItemCustomers().size()).isEqualTo(1);
        assertThat(refundRailwayItems.get(0).getIsExternallyLoaded()).isNotNull();

        assertThat(refundRailwayItems.get(1).getSimpleOperationStatus()).isEqualTo(ImOperationStatus.OK);
        assertThat(refundRailwayItems.get(1).getOrderItemId()).isEqualTo(34984872);
        assertThat(refundRailwayItems.get(1).getAgentReferenceId()).isNotNull();
        assertThat(refundRailwayItems.get(1).getPreviousOrderItemId()).isEqualTo(buyRailwayItem.getOrderItemId());
        assertThat(refundRailwayItems.get(1).getOrderItemBlanks().size()).isEqualTo(1);
        assertThat(refundRailwayItems.get(1).getOrderItemBlanks().get(0).getBlankStatus()).isEqualTo(ImBlankStatus.NO_REMOTE_CHECK_IN);
        assertThat(refundRailwayItems.get(1).getOrderItemCustomers().size()).isEqualTo(1);
        assertThat(refundRailwayItems.get(1).getIsExternallyLoaded()).isNotNull();
    }
}
