package ru.yandex.market.delivery.mdbapp.components.queue.items.instances;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItemCisesValidationException;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstancesPutRequest;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.instances.OrderItemInstancesUpdateDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.instances.OrderItemInstancesUpdateQueueConsumer;
import ru.yandex.market.delivery.mdbapp.components.service.LogisticsOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.integration.converter.BlueOrderItemConverter;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

@ExtendWith(SpringExtension.class)
public class OrderItemInstancesUpdateQueueConsumerTest {

    @MockBean
    LogisticsOrderService logisticsOrderService;
    @MockBean
    CheckouterOrderService checkouterOrderService;

    OrderItemInstancesUpdateQueueConsumer consumer;

    private final BlueOrderItemConverter orderItemConverter = new BlueOrderItemConverter(null, null, null);

    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                "Ошибку валидации кизов из чекаутера",
                "Some cises are note passed validation in checkouter",
                OrderItemCisesValidationException.tooManyCises(2L, 1, 2)
            ),
            Arguments.of("Рандомная ошибка с ненайденным заказом", "Some exception on processing update order items " +
                "instances while sending instances to checkout",
                new OrderNotFoundException(1L))
        );
    }

    @BeforeEach
    void setUp() {
        consumer = new OrderItemInstancesUpdateQueueConsumer(
            checkouterOrderService,
            logisticsOrderService,
            orderItemConverter
        );
    }

    @Test
    @DisplayName("Экзекрутер выполняется c пустым пэйлодом и отправляет пустой пустой запрос в чекаутер удаляя кизы")
    void testExecuteFinishWithEmptyRequest() {
        Mockito.when(logisticsOrderService.getByIdOrThrow(Mockito.anyLong(), Mockito.anySet()))
            .thenReturn(new OrderDto().setExternalId("1"));
        var o = new Order();
        o.setId(1L);
        Mockito.when(checkouterOrderService.getOrder(Mockito.anyLong())).thenReturn(o);
        Assertions.assertEquals(
            TaskExecutionResult.finish(),
            consumer.execute(createTask(new OrderItemInstancesUpdateDto(1L)))
        );
        Mockito.verify(checkouterOrderService, Mockito.never()).putOrderItemInstances(
            Mockito.eq(o.getId()),
            Mockito.refEq(new OrderItemInstancesPutRequest(List.of()))
        );
    }

    @DisplayName("Экзекутор выполняется c пустым пэйлодом и получает ошибку валидации кизов из чекаутера")
    @ParameterizedTest()
    @MethodSource("data")
    void testExecuteFinishOrderNotFoundExc(String name, String message, Exception exception) {
        Mockito.when(logisticsOrderService.getByIdOrThrow(Mockito.anyLong(), Mockito.anySet()))
            .thenReturn(new OrderDto().setExternalId("1"));
        var o = new Order();
        o.setId(1L);
        Mockito.when(checkouterOrderService.getOrder(Mockito.anyLong())).thenReturn(o);
        Mockito.when(
            checkouterOrderService.putOrderItemInstances(
                Mockito.anyLong(),
                Mockito.any(OrderItemInstancesPutRequest.class)
            )
        ).thenThrow(exception);

        Assertions.assertEquals(
            TaskExecutionResult.finish(),
            consumer.execute(createTask(new OrderItemInstancesUpdateDto(1L)))
        );

        Mockito.verify(checkouterOrderService, Mockito.never()).putOrderItemInstances(
            Mockito.eq(o.getId()),
            Mockito.refEq(new OrderItemInstancesPutRequest(List.of()))
        );
    }

    @Test
    @DisplayName("Получаем плохой LomOrder.externalId и сваливаемся в ошибку")
    void testExecuteFailBadLomExtOrderIdExc() {
        Mockito.when(logisticsOrderService.getByIdOrThrow(Mockito.anyLong(), Mockito.anySet()))
            .thenReturn(new OrderDto().setExternalId("bad_id"));
        Assertions.assertEquals(
            TaskExecutionResult.fail(),
            consumer.execute(createTask(new OrderItemInstancesUpdateDto(1L)))
        );
    }

    @Test
    @DisplayName("Не можем найти заказ в чекаутере")
    void testExecuteFinishCheckoutOrderNotFound() {
        Mockito.when(logisticsOrderService.getByIdOrThrow(Mockito.anyLong(), Mockito.anySet()))
            .thenReturn(new OrderDto().setExternalId("1"));
        Mockito.when(checkouterOrderService.getOrder(Mockito.eq(1L)))
            .thenThrow(new OrderNotFoundException(1L));

        Assertions.assertEquals(
            TaskExecutionResult.fail(),
            consumer.execute(createTask(new OrderItemInstancesUpdateDto(1L)))
        );
    }

    Task<OrderItemInstancesUpdateDto> createTask(OrderItemInstancesUpdateDto payload) {
        return new Task<>(new QueueShardId("1"), payload, 1, ZonedDateTime.now(), null, null);
    }
}
