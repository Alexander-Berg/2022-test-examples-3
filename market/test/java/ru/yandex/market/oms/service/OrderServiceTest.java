package ru.yandex.market.oms.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.oms.AbstractFunctionalTest;
import ru.yandex.market.oms.util.DbTestUtils;

@ActiveProfiles("functionalTest")
public class OrderServiceTest extends AbstractFunctionalTest {
    @Autowired
    DbTestUtils dbTestUtils;
    @Autowired
    OrderService orderService;

    private final Long userId = 1L;
    private final Long orderId = 1L;
    private final Long itemId = 1L;
    private final Long deliveryId = 1L;
    private final Long addressId = 1L;

    @BeforeEach
    public void beforeEach() {
        dbTestUtils.insertOrder(orderId, userId, deliveryId, OrderStatus.PROCESSING);
        dbTestUtils.insertOrderItem(orderId, itemId);

        dbTestUtils.insertOrderDelivery(orderId, deliveryId, addressId, new Integer[]{});
        dbTestUtils.insertOrderProperties(orderId);
    }

    @AfterEach
    public void afterEach() {
        dbTestUtils.deleteOrderProperties(orderId);
        dbTestUtils.deleteOrderDelivery(deliveryId, addressId);

        dbTestUtils.deleteAllOrderItems(orderId);
        dbTestUtils.deleteOrder(orderId);
    }

    @Test
    public void getOrderTest() {
        var order = orderService.getOrder(orderId);
        Assertions.assertNotNull(order.getStatus());
        Assertions.assertNotNull(order.getSubstatus());
        Assertions.assertNotNull(order.getItems());
        Assertions.assertNotNull(order.getItems().get(0).getCargoTypes());
        Assertions.assertNotNull(order.getItems().get(0).getCount());
        Assertions.assertTrue(order.getFulfilment());
        Assertions.assertNotNull(order.getDelivery());
        Assertions.assertNotNull(order.getDeliveryOptions());
        Assertions.assertTrue(Boolean.parseBoolean("" + order.getProperties().get(OrderPropertyType.IS_EDA.getName())));
    }
}
