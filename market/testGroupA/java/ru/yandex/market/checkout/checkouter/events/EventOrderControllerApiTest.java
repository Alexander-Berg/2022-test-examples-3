package ru.yandex.market.checkout.checkouter.events;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.primitives.Longs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class EventOrderControllerApiTest extends AbstractWebTestBase {

    private List<Long> orderIds;

    @BeforeEach
    public void setUp() {
        Order order1 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Order order2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());

        orderIds = Stream.of(order1, order2)
                .peek(o -> orderStatusHelper.proceedOrderToStatus(o, OrderStatus.PROCESSING))
                .map(Order::getId)
                .collect(Collectors.toList());
    }

    @Test
    public void shouldReturnEventsByOrderIdsFromOldClient() {
        OrderHistoryEvents events = client.orderHistoryEvents().getOrdersHistoryEvents(
                Longs.toArray(orderIds),
                new OrderStatus[]{OrderStatus.PROCESSING},
                new HistoryEventType[]{HistoryEventType.ORDER_STATUS_UPDATED}
        );
        assertThat(events.getContent(), hasSize(2));
    }

    @Test
    public void shouldReturnEventsByOrderIdsFromNewClient() {
        OrderHistoryEvents events = client.orderHistoryEvents().getOrdersHistoryEvents(
                Longs.toArray(orderIds),
                new OrderStatus[]{OrderStatus.PROCESSING},
                new HistoryEventType[]{HistoryEventType.ORDER_STATUS_UPDATED}
        );

        assertThat(events.getContent(), hasSize(2));
    }
}
