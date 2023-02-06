package ru.yandex.market.checkout.checkouter.client;

import java.util.Date;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CheckoutClientGetOrdersWithDeliveryTypeTest extends AbstractWebTestBase {

    private Order firstDeliveryOrder;
    private Order secondDeliveryOrder;
    private Order postOrder;
    private Order pickupOrder;

    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @BeforeEach
    public void setUp() {
        Delivery delivery = DeliveryProvider
                .yandexDelivery()
                .today()
                .build();

        firstDeliveryOrder = createOrderWithDelivery(delivery);
        secondDeliveryOrder = createOrderWithDelivery(delivery);
        postOrder = createOrderWithDelivery(DeliveryProvider.getPostalDelivery());
        pickupOrder = createOrderWithDelivery(DeliveryProvider.yandexPickupDelivery().build());
    }

    @Test
    public void canGetOrdersWithDeliveryTypeDelivery() {
        OrderSearchRequest request = buildRequestWithDeliveryTypes(new DeliveryType[] {DeliveryType.DELIVERY});

        PagedOrders pagedOrders = client.getOrders(new RequestClientInfo(ClientRole.SYSTEM, null), request);
        assertNotNull(pagedOrders);
        assertNotNull(pagedOrders.getItems());
        assertEquals(2, pagedOrders.getItems().size());
        assertThat(pagedOrders.getItems().stream()
                .map(Order::getId)
                .collect(Collectors.toList()), containsInAnyOrder(
                        firstDeliveryOrder.getId(), secondDeliveryOrder.getId()));
    }

    @Test
    public void canGetOrdersWithDeliveryTypePost() {
        OrderSearchRequest request = buildRequestWithDeliveryTypes(new DeliveryType[] {DeliveryType.POST});

        PagedOrders pagedOrders = client.getOrders(new RequestClientInfo(ClientRole.SYSTEM, null), request);
        assertNotNull(pagedOrders);
        assertNotNull(pagedOrders.getItems());
        assertEquals(1, pagedOrders.getItems().size());
        assertEquals(postOrder.getId(), pagedOrders.getItems().iterator().next().getId());
    }

    @Test
    public void canGetOrdersWithDeliveryTypePickup() {
        OrderSearchRequest request = buildRequestWithDeliveryTypes(new DeliveryType[] {DeliveryType.PICKUP});

        PagedOrders pagedOrders = client.getOrders(new RequestClientInfo(ClientRole.SYSTEM, null), request);
        assertNotNull(pagedOrders);
        assertNotNull(pagedOrders.getItems());
        assertEquals(1, pagedOrders.getItems().size());
        assertEquals(pickupOrder.getId(), pagedOrders.getItems().iterator().next().getId());
    }

    @Test
    public void canGetOrdersWithDeliveryTypeMixed() {
        OrderSearchRequest request = buildRequestWithDeliveryTypes(
                new DeliveryType[] {DeliveryType.DELIVERY, DeliveryType.PICKUP});

        PagedOrders pagedOrders = client.getOrders(new RequestClientInfo(ClientRole.SYSTEM, null), request);
        assertNotNull(pagedOrders);
        assertNotNull(pagedOrders.getItems());
        assertEquals(3, pagedOrders.getItems().size());
        assertThat(pagedOrders.getItems().stream()
                .map(Order::getId)
                .collect(Collectors.toList()), containsInAnyOrder(
                firstDeliveryOrder.getId(), secondDeliveryOrder.getId(), pickupOrder.getId()));
    }


    private OrderSearchRequest buildRequestWithDeliveryTypes(DeliveryType[] deliveryTypes) {
        Date yesterday = DateUtil.prevDay(DateUtil.getToday());
        Date tomorrow = DateUtil.nextDay(DateUtil.getToday());

        return OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withFromDate(yesterday)
                .withToDate(tomorrow)
                .withStatuses(OrderStatus.PENDING)
                .withPageInfo(Pager.atPage(0, 50))
                .withDeliveryTypes(deliveryTypes)
                .build();
    }

    private Order createOrderWithDelivery(Delivery delivery) {
        return orderServiceHelper.saveOrder(OrderProvider.getBlueOrder(o -> {
            o.setDelivery(delivery);
            o.setBuyer(BuyerProvider.getBuyer());
        }));
    }
}
