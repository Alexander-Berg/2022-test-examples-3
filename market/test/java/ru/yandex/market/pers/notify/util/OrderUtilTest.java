package ru.yandex.market.pers.notify.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.pers.notify.mock.MarketMailerMockFactory;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.pers.notify.mock.MarketMailerMockFactory.generateChangeRequest;

public class OrderUtilTest extends MarketMailerMockedDbTest {

    private static final Long ORDER_ID = 1231231L;
    private static final String SHOP_ORDER_ID = "\"12313\"";

    @Autowired
    private OrderUtil orderUtil;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testItemsJson() {
        Order order = MarketMailerMockFactory.generateOrder();
        List<OrderUtil.Item> items = orderUtil.getItems(order.getItems());
        for (OrderUtil.Item item : items) {
            assertNotNull(item.getFfShopInfo());
        }
        assertNotNull(items);
        assertFalse(items.isEmpty());
    }

    @Test
    public void testCheckOrderCancelledEventWithoutRequest() {
        Order order = createOrder(OrderStatus.CANCELLED, Color.WHITE);

        NotificationEventStatus check = OrderUtil.checkOrderCancelledEvent(order);
        assertEquals(NotificationEventStatus.SENT, check);
    }

    @Test
    public void testCheckOrderCancelledEvent() {
        Order order = createOrder(OrderStatus.CANCELLED, Color.WHITE);
        ChangeRequest changeRequest = generateChangeRequest(
                ChangeRequestType.CANCELLATION,
                ChangeRequestStatus.NEW,
                Instant.now());
        order.setChangeRequests(Collections.singletonList(changeRequest));

        NotificationEventStatus check = OrderUtil.checkOrderCancelledEvent(order);
        assertEquals(NotificationEventStatus.SENT, check);
    }

    @Test
    public void testCheckOrderCancelledEventOld() {
        Order order = createOrder(OrderStatus.CANCELLED, Color.WHITE);
        ChangeRequest changeRequest = generateChangeRequest(
                ChangeRequestType.CANCELLATION,
                ChangeRequestStatus.NEW,
                Instant.now().minus(30, ChronoUnit.DAYS));
        order.setChangeRequests(Collections.singletonList(changeRequest));

        NotificationEventStatus check = OrderUtil.checkOrderCancelledEvent(order);
        assertEquals(NotificationEventStatus.EXPIRED, check);
    }

    @Test
    public void testCheckOrderCancelledEventRejected() {
        Order order = createOrder(OrderStatus.CANCELLED, Color.WHITE);
        ChangeRequest changeRequest = generateChangeRequest(
                ChangeRequestType.CANCELLATION,
                ChangeRequestStatus.REJECTED,
                Instant.now());
        order.setChangeRequests(Collections.singletonList(changeRequest));

        NotificationEventStatus check = OrderUtil.checkOrderCancelledEvent(order);
        assertEquals(NotificationEventStatus.SENT, check);
    }

    @Test
    public void testCheckOrderCancelledEventInvalid() {
        Order order = createOrder(OrderStatus.CANCELLED, Color.WHITE);
        ChangeRequest changeRequest = generateChangeRequest(
                ChangeRequestType.CANCELLATION,
                ChangeRequestStatus.INVALID,
                Instant.now());
        order.setChangeRequests(Collections.singletonList(changeRequest));

        NotificationEventStatus check = OrderUtil.checkOrderCancelledEvent(order);
        assertEquals(NotificationEventStatus.SENT, check);
    }


    private Order createOrder(OrderStatus orderStatus, Color color) {
        Order order = MarketMailerMockFactory.generateOrder();
        order.setId(ORDER_ID);
        order.setStatus(orderStatus);
        order.setShopOrderId(SHOP_ORDER_ID);
        order.setRgb(color);
        return order;
    }
}
