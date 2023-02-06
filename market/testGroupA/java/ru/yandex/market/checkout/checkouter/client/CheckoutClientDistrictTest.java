package ru.yandex.market.checkout.checkouter.client;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CheckoutClientDistrictTest extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Test
    public void mustReturnDistrictFromGetOrdersByUser() {
        Order order = orderServiceHelper.prepareOrder();
        String expectedDistrict = order.getDelivery().getBuyerAddress().getDistrict();
        assertNotNull(expectedDistrict);

        OrderSearchRequest searchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .build();
        PagedOrders pagedOrders = client.getOrdersByUser(searchRequest, order.getBuyer().getUid());
        String actualDistrict = pagedOrders.getItems()
                .stream()
                .findFirst()
                .get()
                .getDelivery()
                .getBuyerAddress()
                .getDistrict();

        assertEquals(expectedDistrict, actualDistrict);
    }

    @Test
    public void mustReturnDistrictFromGetOrdersByUserRecent() {
        Order order = orderServiceHelper.prepareOrder();
        String expectedDistrict = order.getDelivery().getBuyerAddress().getDistrict();
        assertNotNull(expectedDistrict);

        List<Order> actualOrders = client.getOrdersByUserRecent(
                order.getBuyer().getUid(),
                new Color[]{order.getRgb()},
                new OrderStatus[]{order.getStatus()},
                null,
                new OptionalOrderPart[]{OptionalOrderPart.DELIVERY, OptionalOrderPart.BUYER},
                false
        );
        String actualDistrict = actualOrders.get(0)
                .getDelivery()
                .getBuyerAddress()
                .getDistrict();

        assertEquals(expectedDistrict, actualDistrict);
    }

    @Test
    public void mustReturnDistrictFromGetOrderHistoryEvents() {
        Order order = orderServiceHelper.prepareOrder();
        String expectedDistrict = order.getDelivery().getBuyerAddress().getDistrict();
        assertNotNull(expectedDistrict);
        PagedEvents events = client.orderHistoryEvents()
                .getOrderHistoryEvents(order.getId(), ClientRole.SYSTEM, null, 0, 10);

        events.getItems()
                .stream()
                .map(OrderHistoryEvent::getOrderBefore)
                .filter(Objects::nonNull)
                .map(Order::getDelivery)
                .map(Delivery::getBuyerAddress)
                .map(Address::getDistrict)
                .forEach(district -> assertEquals(expectedDistrict, district));
        events.getItems()
                .stream()
                .map(OrderHistoryEvent::getOrderAfter)
                .filter(Objects::nonNull)
                .map(Order::getDelivery)
                .map(Delivery::getBuyerAddress)
                .map(Address::getDistrict)
                .forEach(district -> assertEquals(expectedDistrict, district));
    }

    @Test
    public void mustReturnDistrictFromGetOrders() {
        Order order = orderServiceHelper.prepareOrder();
        String expectedDistrict = order.getDelivery().getBuyerAddress().getDistrict();
        assertNotNull(expectedDistrict);

        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .build();
        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SHOP, order.getShopId());
        PagedOrders pagedOrders = client.getOrders(requestClientInfo, request);
        String actualDistrict = pagedOrders.getItems()
                .stream()
                .findFirst()
                .get()
                .getDelivery()
                .getBuyerAddress()
                .getDistrict();

        assertEquals(expectedDistrict, actualDistrict);
    }
}
