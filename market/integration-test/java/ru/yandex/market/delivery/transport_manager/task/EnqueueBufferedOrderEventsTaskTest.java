package ru.yandex.market.delivery.transport_manager.task;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.order.event.OrderEventIdsDto;
import ru.yandex.market.delivery.transport_manager.queue.task.order.event.OrderEventIdsProducer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings("all")
class EnqueueBufferedOrderEventsTaskTest extends AbstractContextualTest {
    @Autowired
    private EnqueueBufferedOrderEventsTask enqueueBufferedOrderEventsTask;
    @Autowired
    private OrderEventIdsProducer orderEventIdsProducer;

    @Test
    @DatabaseSetup("/repository/order_event/before/many_events_for_one_order.xml")
    @ExpectedDatabase(
        value = "/repository/order_event/after/many_events_for_one_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void manyEventsForOneOrder() {
        enqueueBufferedOrderEventsTask.execute();
        verify(orderEventIdsProducer).produce(refEq(new OrderEventIdsDto(List.of(102L))), any());
    }

    @Test
    @DatabaseSetup("/repository/order_event/before/many_events_for_many_orders.xml")
    @ExpectedDatabase(
        value = "/repository/order_event/after/many_events_for_many_orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void manyEventsForManyOrders() {
        enqueueBufferedOrderEventsTask.execute();
        verify(orderEventIdsProducer).produce(refEq(new OrderEventIdsDto(List.of(101L, 102L))), any());
        verify(orderEventIdsProducer).produce(refEq(new OrderEventIdsDto(List.of(103L))), any());
    }

    @Test
    @DatabaseSetup("/repository/order_event/before/do_not_enqueue_twice.xml")
    @ExpectedDatabase(
        value = "/repository/order_event/after/many_events_for_many_orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotEnqueueTwice() {
        enqueueBufferedOrderEventsTask.execute();
        enqueueBufferedOrderEventsTask.execute();
        verify(orderEventIdsProducer).produce(refEq(new OrderEventIdsDto(List.of(102L))), any());
    }

    @Test
    void noEvents() {
        enqueueBufferedOrderEventsTask.execute();
        verifyNoMoreInteractions(orderEventIdsProducer);
    }
}
