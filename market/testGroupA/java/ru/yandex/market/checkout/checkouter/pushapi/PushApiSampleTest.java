package ru.yandex.market.checkout.checkouter.pushapi;

import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.common.report.model.FeedOfferId;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class PushApiSampleTest extends AbstractServicesTestBase {

    @Autowired
    private WireMockServer pushApiMock;
    @Autowired
    private PushApi pushApi;

    @Test
    public void testPushApi() {
        pushApiMock.givenThat(post(urlPathEqualTo("/shops/242102/order/accept"))
                .willReturn(new ResponseDefinitionBuilder().withBody(
                        "<order id=\"230845234234\" accepted=\"true\"/>"
                ).withHeader("Content-Type", "application/xml")));

        OrderItem item = new OrderItem();
        item.setFeedOfferId(new FeedOfferId("1", 383182L));

        Order order = OrderProvider.getBlueOrder();
        order.setItems(Collections.singletonList(item));

        OrderResponse response = pushApi.orderAccept(242102L, order, ApiSettings.PRODUCTION, "asdasd");
        Assertions.assertEquals("230845234234", response.getId());
        Assertions.assertEquals(true, response.isAccepted());
    }

    @Test
    public void testShipmentDateForDsbs() {
        pushApiMock.givenThat(post(urlPathEqualTo("/shops/242102/order/accept"))
                .willReturn(new ResponseDefinitionBuilder().withBody(
                        "<order id=\"230845234234\" accepted=\"true\" shipment-date=\"23-02-2021\"/>"
                ).withHeader("Content-Type", "application/xml")));

        Order order = OrderProvider.getColorOrder(Color.WHITE);

        OrderResponse response = pushApi.orderAccept(242102L, order, ApiSettings.PRODUCTION, "asdasd");
        Assertions.assertEquals("230845234234", response.getId());
        Assertions.assertEquals(true, response.isAccepted());
        Assertions.assertNotNull(response.getShipmentDate());
    }
}
