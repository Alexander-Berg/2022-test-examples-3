package ru.yandex.market.checkout.checkouter.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CheckoutClientGetOrderTest extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Test
    public void canGetOrdersBySystem() {
        Order firstOrder = orderServiceHelper.prepareOrder();
        Order secondOrder = orderServiceHelper.createOrder();

        OrderSearchRequest searchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withFromDate(DateUtil.prevDay(DateUtil.getToday()))
                .withToDate(DateUtil.nextDay(DateUtil.getToday()))
                .withStatuses(OrderStatus.PENDING)
                .withPageInfo(Pager.atPage(0, 50))
                .build();
        PagedOrders pagedOrders = client.getOrders(new RequestClientInfo(ClientRole.SYSTEM, null), searchRequest);

        assertTwoOrdersEqual(firstOrder, secondOrder, pagedOrders);
    }

    @Test
    public void canGetOrdersCount() {
        Order firstOrder = orderServiceHelper.prepareOrder();
        Order secondOrder = orderServiceHelper.createPostOrder();
        OrderSearchRequest searchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .build();
        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SHOP, firstOrder.getShopId());
        int count = client.getOrdersCount(requestClientInfo, searchRequest);
        assertEquals(2, count);
    }

    @Test
    public void canGetOrdersCountByTimestamp() {
        Order firstOrder = orderServiceHelper.prepareOrder();
        Order secondOrder = orderServiceHelper.createPostOrder();
        long fromTimestamp = DateUtil.min(firstOrder.getCreationDate(), secondOrder.getCreationDate()).getTime() + 1;
        OrderSearchRequest searchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withFromTimestamp(fromTimestamp).build();
        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SHOP, firstOrder.getShopId());
        int count = client.getOrdersCount(requestClientInfo, searchRequest);
        assertEquals(1, count);
    }

    @Test
    public void canGetOrdersCountFiltersByShop() {
        Order firstOrder = orderServiceHelper.prepareOrder();
        Order secondOrder = orderServiceHelper.createOrder();
        OrderSearchRequest searchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .build();
        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SHOP, secondOrder.getShopId());
        int count = client.getOrdersCount(requestClientInfo, searchRequest);
        assertEquals(1, count);
    }

    @Test
    public void canGetOrdersCountFiltersByBusiness() {
        Order firstOrder = orderServiceHelper.prepareOrder();
        Order secondOrder = orderServiceHelper.createOrder();
        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.BUSINESS, secondOrder.getBusinessId());
        int count = client.getOrdersCount(requestClientInfo, OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .build());
        assertEquals(1, count);
    }

    @Test
    public void canGetOrdersBySearchRequest() {
        Order firstOrder = orderServiceHelper.prepareOrder();
        Order secondOrder = orderServiceHelper.createPostOrder();

        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .build();
        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SHOP, secondOrder.getShopId());
        PagedOrders pagedOrders = client.getOrders(requestClientInfo, request);

        assertTwoOrdersEqual(firstOrder, secondOrder, pagedOrders);
    }

    @Test
    public void canGetOrdersMiltiClientId() {
        Order firstOrder = orderServiceHelper.prepareOrder();
        Order secondOrder = orderServiceHelper.createPostOrder(order -> order.setShopId(775L));

        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE, Color.WHITE)
                .build();
        RequestClientInfo requestClientInfo = RequestClientInfo.builder(ClientRole.SHOP)
                .withClientIds(Set.of(1L, 774L, 775L))
                .build();
        PagedOrders pagedOrders = client.getOrders(requestClientInfo, request);

        assertTwoOrdersEqual(firstOrder, secondOrder, pagedOrders);
    }

    @Test
    public void canGetOrdersBySearchRequestBusiness() {
        Order firstOrder = orderServiceHelper.prepareOrder();
        Order secondOrder = orderServiceHelper.createPostOrder();

        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .build();
        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.BUSINESS, secondOrder.getBusinessId());
        PagedOrders pagedOrders = client.getOrders(requestClientInfo, request);

        assertTwoOrdersEqual(firstOrder, secondOrder, pagedOrders);
    }

    @Test
    public void canGetOrdersByUser() {
        Order firstOrder = orderServiceHelper.prepareOrder();
        Order secondOrder = orderServiceHelper.createPostOrder();

        Date yesterday = DateUtil.prevDay(DateUtil.getToday());
        Date tomorrow = DateUtil.nextDay(DateUtil.getToday());
        OrderSearchRequest searchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withFromDate(yesterday)
                .withToDate(tomorrow)
                .withStatuses(OrderStatus.PENDING)
                .withFake(true)
                .withPageInfo(Pager.atPage(0, 50))
                .build();
        PagedOrders pagedOrders = client.getOrdersByUser(searchRequest, firstOrder.getBuyer().getUid());

        assertTwoOrdersEqual(firstOrder, secondOrder, pagedOrders);
    }

    @Test
    public void canGetOrdersByUserRecent() {
        Order firstOrder = orderServiceHelper.prepareOrder();
        Order secondOrder = orderServiceHelper.createPostOrder();
        Assertions.assertNotEquals(firstOrder.getId(), secondOrder.getId());

        List<Order> actualOrders = client.getOrdersByUserRecent(
                firstOrder.getBuyer().getUid(),
                new Color[]{firstOrder.getRgb()},
                new OrderStatus[]{firstOrder.getStatus()},
                null,
                new OptionalOrderPart[]{OptionalOrderPart.DELIVERY, OptionalOrderPart.BUYER},
                false
        );
        Assertions.assertEquals(2, actualOrders.size());

        assertEquals(actualOrders.get(0).getId(), secondOrder.getId());
        assertEquals(actualOrders.get(0).getStatus(), secondOrder.getStatus());
        assertNull(actualOrders.get(0).getItems());
        assertEquals(actualOrders.get(0).getDelivery().getType(), secondOrder.getDelivery().getType());
        assertEquals(actualOrders.get(0).getBuyer().getId(), secondOrder.getBuyer().getId());

        assertEquals(actualOrders.get(1).getId(), firstOrder.getId());
        assertEquals(actualOrders.get(1).getStatus(), firstOrder.getStatus());
        assertNull(actualOrders.get(1).getItems());
        assertEquals(actualOrders.get(1).getDelivery().getType(), firstOrder.getDelivery().getType());
        assertEquals(actualOrders.get(1).getBuyer().getId(), firstOrder.getBuyer().getId());
    }

    @Test
    public void canGetOrdersByShop() {
        Order firstOrder = orderServiceHelper.prepareOrder();
        Order secondOrder = orderServiceHelper.createPostOrder();

        Date yesterday = DateUtil.prevDay(DateUtil.getToday());
        Date tomorrow = DateUtil.nextDay(DateUtil.getToday());
        OrderSearchRequest searchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withFromDate(yesterday)
                .withToDate(tomorrow)
                .withStatuses(OrderStatus.PENDING)
                .withFake(true)
                .withPageInfo(Pager.atPage(0, 50))
                .build();
        PagedOrders pagedOrders = client.getOrdersByShop(searchRequest, firstOrder.getShopId());

        assertTwoOrdersEqual(firstOrder, secondOrder, pagedOrders);
    }

    private void assertTwoOrdersEqual(Order firstOrder, Order secondOrder, PagedOrders pagedOrders) {
        assertEquals(2, (long) pagedOrders.getPager().getTotal());
        List<Order> actualOrders = new ArrayList<>(pagedOrders.getItems());
        assertEquals(2, (long) actualOrders.size());

        actualOrders.sort(Comparator.comparingLong(BasicOrder::getId));
        List<Order> expectedOrders = new ArrayList<Order>() {{
            add(firstOrder);
            add(secondOrder);
        }};
        expectedOrders.sort(Comparator.comparingLong(BasicOrder::getId));

        assertEquals(expectedOrders.get(0).getId(), actualOrders.get(0).getId());
        assertEquals(expectedOrders.get(0).getStatus(), actualOrders.get(0).getStatus());
        assertEquals(expectedOrders.get(0).getItems().size(), actualOrders.get(0).getItems().size());
        assertEquals(expectedOrders.get(0).getDelivery().getType(), actualOrders.get(0).getDelivery().getType());

        assertEquals(expectedOrders.get(1).getId(), actualOrders.get(1).getId());
        assertEquals(expectedOrders.get(1).getStatus(), actualOrders.get(1).getStatus());
        assertEquals(expectedOrders.get(1).getItems().size(), actualOrders.get(1).getItems().size());
        assertEquals(expectedOrders.get(1).getDelivery().getType(), actualOrders.get(1).getDelivery().getType());
    }


}
