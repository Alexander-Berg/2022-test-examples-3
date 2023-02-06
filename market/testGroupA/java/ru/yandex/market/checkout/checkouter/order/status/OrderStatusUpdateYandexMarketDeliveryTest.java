package ru.yandex.market.checkout.checkouter.order.status;

import java.util.Arrays;
import java.util.stream.Stream;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

public class OrderStatusUpdateYandexMarketDeliveryTest extends AbstractServicesTestBase {

    private static final long MANAGER_UID = 2234562L;
    @Autowired
    private OrderServiceHelper orderServiceHelper;

    private Order order;

    public static Stream<Arguments> parameterizedTestData() {
        Order order = OrderProvider.getOrderWithYandexMarketDelivery();
        return Arrays.asList(
                new Object[]{"shop", order, new ClientInfo(ClientRole.SHOP, order.getShopId())},
                new Object[]{"shop user", order, new ClientInfo(ClientRole.SHOP_USER, MANAGER_UID, order.getShopId())}
        ).stream().map(Arguments::of);
    }


    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("Чекаутер не должен разрешать переводить заказ в статус PICKUP")
    @ParameterizedTest(name = TEST_DISPLAY_NAME)
    @MethodSource("parameterizedTestData")
    public void shouldNotAllowUpdateToPickupStatus(String testCaseName, Order orderToSave, ClientInfo clientInfo) {
        this.order = orderServiceHelper.saveOrder(orderToSave);
        orderUpdateService.updateOrderStatus(this.order.getId(), OrderStatus.PROCESSING);
        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.DELIVERY, new ClientInfo(ClientRole.SYSTEM,
                null));

        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PICKUP, clientInfo);
        });
    }


    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("Чекаутер не должен разрешать переводить заказ в статус DELIVERED")
    @ParameterizedTest(name = TEST_DISPLAY_NAME)
    @MethodSource("parameterizedTestData")
    public void shouldNotAllowUpdateToDeliveredStatusFromDelivery(
            String testCaseName,
            Order orderToSave,
            ClientInfo clientInfo
    ) {
        this.order = orderServiceHelper.saveOrder(orderToSave);
        orderUpdateService.updateOrderStatus(this.order.getId(), OrderStatus.PROCESSING);
        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.DELIVERY, new ClientInfo(ClientRole.SYSTEM,
                null));

        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.DELIVERED, clientInfo);
        });
    }


    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("Чекаутер не должен разрешать переводить заказ в статус DELIVERED")
    @ParameterizedTest(name = TEST_DISPLAY_NAME)
    @MethodSource("parameterizedTestData")
    public void shouldNotAllowUpdateToDeliveredStatusFromPickup(
            String testCaseName,
            Order orderToSave,
            ClientInfo clientInfo
    ) {
        this.order = orderServiceHelper.saveOrder(orderToSave);
        orderUpdateService.updateOrderStatus(this.order.getId(), OrderStatus.PROCESSING);
        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.DELIVERY, new ClientInfo(ClientRole.SYSTEM,
                null));

        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PICKUP, ClientInfo.SYSTEM);
            orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.DELIVERED, clientInfo);
        });
    }
}
