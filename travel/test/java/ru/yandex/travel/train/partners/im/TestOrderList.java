package ru.yandex.travel.train.partners.im;

import java.time.Duration;
import java.time.LocalDateTime;
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
import ru.yandex.travel.train.partners.im.model.orderlist.ImShortOrderInfo;
import ru.yandex.travel.train.partners.im.model.orderlist.ImShortOrderItem;
import ru.yandex.travel.train.partners.im.model.orderlist.ImShortOrderItemBlank;
import ru.yandex.travel.train.partners.im.model.orderlist.OrderListRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

public class TestOrderList {
    private static final Logger logger = LoggerFactory.getLogger(TestOrderList.class);
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
                String.format("http://localhost:%d/", wireMockRule.port()), "ya", "***");
    }

    @Test
    public void testOrderList() {
        stubFor(post(anyUrl()).willReturn(aResponse()
                .withBody(TestResources.readResource("OrderListResponse.json"))));

        var response = client.orderList(new OrderListRequest(LocalDateTime.of(2019, 11, 15, 0, 0)));

        verify(postRequestedFor(urlPathEqualTo("/Order/V1/Info/OrderList")).withRequestBody(equalToJson(
                TestResources.readResource("OrderListRequest.json")
        )));

        assertThat(response.getOrders().size()).isEqualTo(2);
        ImShortOrderInfo firstOrder = response.getOrders().get(0);
        assertThat(firstOrder.getOrderId()).isNotZero();
        assertThat(firstOrder.getOrderItems().size()).isEqualTo(2);
        ImShortOrderItem firstOrderItem = firstOrder.getOrderItems().get(0);
        assertThat(firstOrderItem.getOrderItemId()).isNotZero();
        assertThat(firstOrderItem.getPreviousOrderItemId()).isNotNull();
        assertThat(firstOrderItem.getConfirmDateTime()).isNotNull();
        assertThat(firstOrderItem.getIsExternallyLoaded()).isTrue();
        assertThat(firstOrderItem.getOrderItemBlanks().size()).isEqualTo(1);
        ImShortOrderItemBlank firstBlank = firstOrderItem.getOrderItemBlanks().get(0);
        assertThat(firstBlank.getOrderItemBlankId()).isNotZero();
        assertThat(firstBlank.getPreviousOrderItemBlankId()).isNotNull();
        assertThat(firstBlank.getAmount()).isPositive();

        assertThat(response.getOrders().get(1).getOrderId()).isNotZero();
    }
}
