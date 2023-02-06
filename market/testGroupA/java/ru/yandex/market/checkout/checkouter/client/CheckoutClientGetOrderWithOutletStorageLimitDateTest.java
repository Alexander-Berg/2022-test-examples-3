package ru.yandex.market.checkout.checkouter.client;

import java.time.LocalDate;
import java.util.Date;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CheckoutClientGetOrderWithOutletStorageLimitDateTest extends AbstractWebTestBase {

    private Order orderTillToday;
    private Order orderTillYesterday;

    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @BeforeEach
    public void setUp() {
        Date yesterday = DateUtil.prevDay(DateUtil.getToday());
        Date tomorrow = DateUtil.nextDay(DateUtil.getToday());

        orderTillToday = createOrderWithStorageLimitDate(DateUtil.asLocalDate(DateUtil.getToday()));
        orderTillYesterday = createOrderWithStorageLimitDate(DateUtil.asLocalDate(yesterday));

        createOrderWithStorageLimitDate(DateUtil.asLocalDate(tomorrow));
        createOrderWithStorageLimitDate(DateUtil.asLocalDate(DateUtil.prevDay(yesterday)));
    }

    @Test
    public void canGetOrdersWithStorageLimitDateFilter() {
        Date yesterday = DateUtil.prevDay(DateUtil.getToday());
        Date tomorrow = DateUtil.nextDay(DateUtil.getToday());

        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(Color.WHITE)
                .withFromDate(yesterday)
                .withToDate(tomorrow)
                .withStatuses(OrderStatus.PENDING)
                .withOutletStorageLimitDateFrom(yesterday)
                .withOutletStorageLimitDateTo(tomorrow)
                .build();

        PagedOrders pagedOrders = client.getOrders(new RequestClientInfo(ClientRole.SYSTEM, null), request);
        assertNotNull(pagedOrders);
        assertNotNull(pagedOrders.getItems());
        assertEquals(2, pagedOrders.getItems().size());
        assertThat(pagedOrders.getItems().stream()
                .map(Order::getId)
                .collect(Collectors.toList()), containsInAnyOrder(
                orderTillToday.getId(), orderTillYesterday.getId()));
    }

    private Order createOrderWithStorageLimitDate(LocalDate storageLimitDate) {
        return orderServiceHelper.saveOrder(
                OrderProvider.getWhiteOrder(o -> o.getDelivery().setOutletStorageLimitDate(storageLimitDate))
        );
    }
}
