package ru.yandex.market.delivery.mdbapp.components.queue.order.delivery;

import java.time.ZonedDateTime;
import java.util.EnumSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CheckouterSetOrderStatusConsumerTest extends AllMockContextualTest {

    private static final long ORDER_ID = 1111L;

    @MockBean
    private CheckouterAPI checkouterAPI;
    @Autowired
    private CheckouterServiceClient checkouterServiceClient;

    private CheckouterSetOrderStatusConsumer consumer;

    @Before
    public void setup() {
        consumer = new CheckouterSetOrderStatusConsumer(checkouterServiceClient);
    }

    @Test
    public void testNoOrderFound() {
        var task = createTask(new SetOrderStatusDto(ORDER_ID, OrderStatus.DELIVERY, null));
        var exception = new OrderNotFoundException(ORDER_ID);

        when(checkouterAPI.getOrder(
            eq(ORDER_ID),
            eq(ClientRole.SYSTEM),
            eq(null),
            eq(false)
        ))
            .thenThrow(exception);

        var result = consumer.execute(task);
        softly.assertThat(result).isEqualTo(TaskExecutionResult.fail());
    }

    @Test
    public void testSuccess() {
        var task = createTask(new SetOrderStatusDto(ORDER_ID, OrderStatus.DELIVERY, null));
        var order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setId(ORDER_ID);

        when(checkouterAPI.getOrder(
            eq(ORDER_ID),
            eq(ClientRole.SYSTEM),
            eq(null),
            eq(false)
        ))
            .thenReturn(order);

        var result = consumer.execute(task);
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());
        verify(checkouterAPI)
            .updateOrderStatus(
                eq(ORDER_ID),
                eq(ClientRole.SYSTEM),
                any(),
                any(),
                eq(OrderStatus.DELIVERY),
                eq(null)
            );
    }

    @Test
    public void testUpdateOnlySubstatus() {
        var task = createTask(
            new SetOrderStatusDto(ORDER_ID, OrderStatus.PICKUP, OrderSubstatus.PICKUP_USER_RECEIVED)
        );
        var order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.PICKUP);

        when(checkouterAPI.getOrder(
            eq(ORDER_ID),
            eq(ClientRole.SYSTEM),
            eq(null),
            eq(false)
        ))
            .thenReturn(order);

        var result = consumer.execute(task);
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());
        verify(checkouterAPI).updateOrderStatus(
            eq(ORDER_ID),
            eq(ClientRole.SYSTEM),
            any(),
            any(),
            eq(OrderStatus.PICKUP),
            eq(OrderSubstatus.PICKUP_USER_RECEIVED)
        );
    }

    @Test
    public void testNoop() {
        var noopStatuses = EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
        var task = createTask(new SetOrderStatusDto(ORDER_ID, OrderStatus.DELIVERY, null));

        for (var status : noopStatuses) {
            var order = new Order();
            order.setStatus(status);
            order.setId(ORDER_ID);

            when(checkouterAPI.getOrder(
                eq(ORDER_ID),
                eq(ClientRole.SYSTEM),
                eq(null),
                eq(false)
            ))
                .thenReturn(order);

            var result = consumer.execute(task);
            softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());
            verify(checkouterAPI, never())
                .updateOrderStatus(
                    eq(ORDER_ID),
                    eq(ClientRole.SYSTEM),
                    any(),
                    any(),
                    eq(OrderStatus.DELIVERY),
                    eq(null)
                );
        }
    }

    @Test
    public void testDeniedTransitionPickupToDelivery() {
        var task = createTask(new SetOrderStatusDto(ORDER_ID, OrderStatus.DELIVERY, null));

        var order = new Order();
        order.setStatus(OrderStatus.PICKUP);
        order.setId(ORDER_ID);

        when(checkouterAPI.getOrder(
            eq(ORDER_ID),
            eq(ClientRole.SYSTEM),
            eq(null),
            eq(false)
        ))
            .thenReturn(order);

        var result = consumer.execute(task);
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());
        verify(checkouterAPI, never())
            .updateOrderStatus(
                eq(ORDER_ID),
                eq(ClientRole.SYSTEM),
                any(),
                any(),
                eq(OrderStatus.DELIVERY),
                eq(null)
            );
    }

    private <T> Task<T> createTask(T payload) {
        return new Task<>(
            new QueueShardId("id"),
            payload,
            1L,
            ZonedDateTime.now(),
            null,
            null
        );
    }
}
