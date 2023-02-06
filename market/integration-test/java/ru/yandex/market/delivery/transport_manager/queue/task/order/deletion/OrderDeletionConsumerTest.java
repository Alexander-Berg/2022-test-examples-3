package ru.yandex.market.delivery.transport_manager.queue.task.order.deletion;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

public class OrderDeletionConsumerTest extends AbstractContextualTest {
    @Autowired
    private OrderDeletionConsumer orderDeletionConsumer;

    @Test
    @DatabaseSetup(
        value = {
            "/repository/order_route/orders.xml",
            "/repository/order_route/routes_for_binding.xml"
        }
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_orders_cleanup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteOrders() {
        orderDeletionConsumer.executeTask(
            Task.<OrderDeletionDto>builder(new QueueShardId("1"))
                .withPayload(new OrderDeletionDto(List.of(1L, 3L)))
                .build()
        );
    }
}
