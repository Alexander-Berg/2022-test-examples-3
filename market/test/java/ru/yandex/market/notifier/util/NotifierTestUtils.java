package ru.yandex.market.notifier.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

/**
 * @author korolyov
 * 08.12.16
 */
public final class NotifierTestUtils {

    private NotifierTestUtils() {
        throw new UnsupportedOperationException();
    }

    //2016-12-09 10:00:00 MSK
    public static final Instant DEFAULT_ORDER_CREATION_DATE = Instant.ofEpochSecond(1481266800L);
    //2016-12-09 10:01:00 MSK
    public static final Instant DEFAULT_EVENT_DATE = Instant.ofEpochSecond(1481266860L);

    private static Buyer generateBuyer() {
        Buyer buyer = new Buyer(0);
        buyer.setEmail("email@example.com");
        buyer.setNormalizedPhone("+79072234562");
        return buyer;
    }

    private static Order generateOrder(OrderStatus orderStatus, OrderSubstatus substatus) {
        Order order = new Order();
        order.setId(0L);
        order.setStatus(orderStatus);
        order.setSubstatus(substatus);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.setBuyer(generateBuyer());
        order.setShopId(0L);
        order.setNoAuth(true);
        order.setRgb(Color.BLUE);
        order.setStatusExpiryDate(Date.from(DEFAULT_ORDER_CREATION_DATE.plus(Duration.ofDays(1))));
        return order;
    }

    public static OrderHistoryEvent generateOrderHistoryEvent(long id) {
        return generateOrderHistoryEvent(id, OrderStatus.CANCELLED, OrderSubstatus.SHOP_FAILED);
    }

    public static OrderHistoryEvent generateOrderHistoryEvent(long id, OrderStatus orderStatus, OrderSubstatus orderSubstatus) {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(id);
        orderHistoryEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        orderHistoryEvent.setAuthor(new ClientInfo(ClientRole.SHOP, 0L));
        orderHistoryEvent.setFromDate(Date.from(DEFAULT_EVENT_DATE));
        orderHistoryEvent.setOrderBefore(generateOrder(orderStatus, orderSubstatus));
        orderHistoryEvent.setOrderAfter(generateOrder(orderStatus, orderSubstatus));
        return orderHistoryEvent;
    }

    public static OrderHistoryEvent generateOrderHistoryEvent(long id,
                                                              OrderStatus orderStatus,
                                                              OrderSubstatus orderSubstatus,
                                                              Color rgb
    ) {
        OrderHistoryEvent orderHistoryEvent = generateOrderHistoryEvent(id, orderStatus, orderSubstatus);
        orderHistoryEvent.getOrderAfter().setRgb(rgb);
        return orderHistoryEvent;
    }

    public static PersNotifyClient getMockClient(PersNotifyClient mockClient, List<NotificationEventSource> mockCol)
            throws PersNotifyClientException {
        Mockito.doAnswer(invocationOnMock -> {
            NotificationEventSource source = (NotificationEventSource) invocationOnMock.getArguments()[0];

            mockCol.add(source);

            return mockCol.get(mockCol.size() - 1);
        }).when(mockClient).createEvent(Mockito.any(NotificationEventSource.class));
        return mockClient;
    }
}
