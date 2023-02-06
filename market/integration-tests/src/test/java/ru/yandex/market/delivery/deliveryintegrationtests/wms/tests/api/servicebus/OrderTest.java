package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.servicebus;

import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ApiClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ApiOrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;

import java.util.List;

@DisplayName("API: Order")
@Epic("API Tests")
@Slf4j
public class OrderTest {

    private final ServiceBus serviceBus = new ServiceBus();
    private final ApiClient apiClient = new ApiClient();

    private final Order SHIPPED_ORDER = new Order(1555688309453L, "0000000908");
    private final Order TWO_BOXES_ORDER = new Order(1632230708L, "0000177164");
    private final Order THREE_BOXES_ORDER = new Order(1632250637L, "0000177317");

    private final Item MULTIPLACE_ITEM = Item.builder()
            .sku("AUTO_MNOGOBOX")
            .vendorId(1559)
            .article("AUTO_MNOGOBOX")
            .quantity(1)
            .build();

    private final Item COMMON_ITEM = Item.builder()
            .sku("AUTO_FOR_ORDER")
            .vendorId(1559)
            .article("AUTO_FOR_ORDER")
            .quantity(1)
            .build();

    @Test
    @DisplayName("createOrderViaServiceBus")
    public void createOrderTestViaServiceBus() {
        log.info("Testing createOrder via ServiceBus");

        serviceBus.createOrder(
                UniqueId.get(),
                List.of(COMMON_ITEM),
                DateUtil.tomorrowDateTime()
        );
    }

    @Test
    @DisplayName("createMultiOrderViaServiceBus: Заказ на многоместный товар")
    public void createMultiOrderTestViaServiceBus() {
        log.info("Testing createMultiOrder via ServiceBus");

        Order orderViaServicebus = serviceBus.createOrder(
                UniqueId.get(),
                List.of(MULTIPLACE_ITEM),
                DateUtil.tomorrowDateTime()
        );

        ApiSteps.Order().verifyOrder(orderViaServicebus,
                "wms/servicebus/createOrder.check.via.getOrder.response.xml");
    }

    @Test
    @DisplayName("createExistingOrder")
    public void createExistingOrder() {
        log.info("Testing createExistingOrder");

        Order initialOrder = serviceBus.createOrder(
                UniqueId.get(),
                List.of(COMMON_ITEM),
                DateUtil.tomorrowDateTime()
        );

        Order recreatedOrder = serviceBus.createOrder(
                initialOrder.getYandexId(),
                List.of(COMMON_ITEM),
                DateUtil.tomorrowDateTime()
        );

        ApiSteps.Order().verifyFulfillmentIdOfExistingOrder(recreatedOrder);
    }

    @Test
    @DisplayName("cancelOrderViaServiceBus")
    public void cancelOrderViaServiceBusTest() {
        log.info("Testing cancelOrder via ServiceBus");

        Order order = serviceBus.createOrder(
                UniqueId.get(),
                List.of(COMMON_ITEM),
                DateUtil.tomorrowDateTime()
        );

        ApiSteps.Order().cancelOrder(order);
        ApiSteps.Order().verifyOrderStatus(order, ApiOrderStatus.SERVICE_CENTER_CANCELED);
    }

    @Test
    @DisplayName("getOrderViaServiceBus")
    public void getOrderTestViaServiceBus() {
        log.info("Testing getOrder via ServiceBus");

        ApiSteps.Order().getOrder(TWO_BOXES_ORDER)
                .body("root.response.order.places.place.find " +
                                "{it.placeId.partnerId == 'P000018111'}.placeId.partnerId",
                        Matchers.is("P000018111"))
                .body("root.response.order.places.place.find " +
                                "{it.placeId.partnerId == 'P000018111'}.placeId.fulfillmentId",
                        Matchers.is("P000018111"))
                .body("root.response.order.places.place.find " +
                                "{it.placeId.partnerId == 'P000018102'}.placeId.partnerId",
                        Matchers.is("P000018102"))
                .body("root.response.order.places.place.find " +
                                "{it.placeId.partnerId == 'P000018102'}.placeId.fulfillmentId",
                        Matchers.is("P000018102"));
    }

    @Test
    @DisplayName("getOrderViaServiceBusWithBoxSizes")
    public void getOrderTestViaServiceBusWithBoxSizes() {
        log.info("Testing getOrder via ServiceBus with carton sizes");
        String box76 = "root.response.order.places.place.find {it.placeId.partnerId == 'P000018112'}";
        String box77 = "root.response.order.places.place.find {it.placeId.partnerId == 'P000018114'}";
        String box78 = "root.response.order.places.place.find {it.placeId.partnerId == 'P000018113'}";
        ApiSteps.Order().getOrder(THREE_BOXES_ORDER)
                .body(box76 + ".placeId.partnerId", Matchers.is("P000018112"))
                .body(box76 + ".placeId.fulfillmentId", Matchers.is("P000018112"))
                .body(box76 + ".korobyte.width.toInteger()", Matchers.is(40))
                .body(box76 + ".korobyte.height.toInteger()", Matchers.is(40))
                .body(box76 + ".korobyte.length.toInteger()", Matchers.is(60))
                .body(box76 + ".korobyte.weightGross.toDouble()", Matchers.closeTo(2.5, 10e-2))

                .body(box77 + ".placeId.partnerId", Matchers.is("P000018114"))
                .body(box77 + ".placeId.fulfillmentId", Matchers.is("P000018114"))
                .body(box77 + ".korobyte.width.toInteger()", Matchers.is(16))
                .body(box77 + ".korobyte.height.toInteger()", Matchers.is(10))
                .body(box77 + ".korobyte.length.toInteger()", Matchers.is(21))
                .body(box77 + ".korobyte.weightGross.toDouble()", Matchers.closeTo(2.5, 10e-2))

                .body(box78 + ".placeId.partnerId", Matchers.is("P000018113"))
                .body(box78 + ".placeId.fulfillmentId", Matchers.is("P000018113"))
                .body(box78 + ".korobyte.width.toInteger()", Matchers.is(13))
                .body(box78 + ".korobyte.height.toInteger()", Matchers.is(31))
                .body(box78 + ".korobyte.length.toInteger()", Matchers.is(30))
                .body(box78 + ".korobyte.weightGross.toDouble()", Matchers.closeTo(2.5, 10e-2));
    }

}
