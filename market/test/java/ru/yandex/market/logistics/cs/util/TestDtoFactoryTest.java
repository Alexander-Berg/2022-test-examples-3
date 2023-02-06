package ru.yandex.market.logistics.cs.util;

import java.util.HashSet;

import org.junit.jupiter.api.RepeatedTest;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.logistics.cs.util.TestDtoFactory.IndexedOrderHistoryEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;


class TestDtoFactoryTest {

    @RepeatedTest(10)
    void noDuplicateDeliveryServicesEmitted() {
        IndexedOrderHistoryEvent indexedOrderHistoryEvent = TestDtoFactory.randomHistoryEventWithRoute(
            HistoryEventType.NEW_ORDER,
            OrderStatus.PLACING,
            OrderStatus.RESERVED
        );

        assertEquals(
            indexedOrderHistoryEvent.getServiceList().size(),
            new HashSet<>(indexedOrderHistoryEvent.getServiceList()).size()
        );
    }
}
