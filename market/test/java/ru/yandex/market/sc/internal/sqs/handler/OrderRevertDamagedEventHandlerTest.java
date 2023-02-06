package ru.yandex.market.sc.internal.sqs.handler;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.OrderDamagedEvent;
import ru.yandex.market.logistics.les.RevertOrderDamagedEvent;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.sqs.SqsEventType;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.SqsEventFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbIntTest
class OrderRevertDamagedEventHandlerTest {

    private static final Instant INSTANT =
            LocalDateTime.of(2021, 9, 16, 10, 0, 0).atZone(DateTimeUtils.MOSCOW_ZONE).toInstant();

    @Autowired
    private OrderMarkDamagedEventHandler orderMarkDamagedEventHandler;
    @Autowired
    private OrderRevertDamagedEventHandler orderRevertDamagedEventHandler;
    @Autowired
    private SqsEventFactory sqsEventFactory;
    @Autowired
    private TestFactory testFactory;

    private SortingCenter sortingCenter;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");
    }

    @Test
    void revertMarkDamaged() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort()
                .ship()
                .get();
        Long orderId = order.getId();
        String externalOrderId = order.getExternalId();

        orderMarkDamagedEventHandler.handle(sqsEventFactory.makeSqsEvent(SqsEventType.ORDER_DAMAGED,
                INSTANT.toEpochMilli(),
                new OrderDamagedEvent(externalOrderId)));

        orderRevertDamagedEventHandler.handle(sqsEventFactory.makeSqsEvent(SqsEventType.REVERT_ORDER_DAMAGED,
                INSTANT.toEpochMilli(),
                new RevertOrderDamagedEvent(externalOrderId)));

        order = testFactory.getOrder(orderId);
        assertThat(order.isDamaged()).isEqualTo(false);
    }
}
