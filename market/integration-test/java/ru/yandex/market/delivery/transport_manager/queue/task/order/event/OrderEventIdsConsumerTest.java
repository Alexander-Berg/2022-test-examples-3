package ru.yandex.market.delivery.transport_manager.queue.task.order.event;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.util.DbQueueUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class OrderEventIdsConsumerTest extends AbstractContextualTest {
    @Autowired
    private OrderEventIdsConsumer orderEventIdsConsumer;

    @Test
    @DatabaseSetup("/repository/order_event/before/many_events_for_many_orders.xml")
    @ExpectedDatabase(
        value = "/repository/order_event/after/processed_except_103.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(value = "/service/event/db/init_order.xml", assertionMode = NON_STRICT_UNORDERED)
    void processEvent() {
        orderEventIdsConsumer.execute(
            DbQueueUtils.createTask(new OrderEventIdsDto(List.of(101L, 106L)))
        );
    }

    @Test
    void skipDeletedEvent() {
        orderEventIdsConsumer.execute(
            DbQueueUtils.createTask(new OrderEventIdsDto(List.of(102L)))
        );
    }
}
