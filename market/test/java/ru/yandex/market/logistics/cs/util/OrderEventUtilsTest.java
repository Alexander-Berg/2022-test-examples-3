package ru.yandex.market.logistics.cs.util;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;

import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;

class OrderEventUtilsTest {
    private static Stream<Arguments> validEventTypeMappingProvider() {
        return Stream.concat(
                Arrays.stream(OrderStatus.values())
                        .flatMap(orderBeforeStatus -> Arrays.stream(OrderStatus.values())
                                .map(orderAfterStatus -> Pair.of(orderBeforeStatus, orderAfterStatus)))
                        .map(pair -> Arguments.of(NEW_ORDER, pair.getKey(), pair.getValue(), EventType.NEW)),
                Arrays.stream(OrderStatus.values())
                        .filter(status -> status != OrderStatus.CANCELLED)
                        .map(orderBeforeStatus -> Arguments.of(
                                ORDER_STATUS_UPDATED,
                                orderBeforeStatus,
                                OrderStatus.CANCELLED,
                                EventType.CANCELLED
                        ))
        );
    }


}
