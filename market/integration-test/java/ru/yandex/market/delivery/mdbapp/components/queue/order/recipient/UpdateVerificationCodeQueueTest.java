package ru.yandex.market.delivery.mdbapp.components.queue.order.recipient;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.CheckouterSetOrderStatusEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.verification.OrderVerificationCodeUpdateDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.verification.OrderVerificationCodeUpdateQueueConsumer;
import ru.yandex.market.delivery.mdbapp.configuration.LockerCodeProperties;
import ru.yandex.market.delivery.mdbapp.configuration.queue.QueueDefinition;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class UpdateVerificationCodeQueueTest extends AllMockContextualTest {
    private static final long ORDER_ID = 1001;
    @Autowired
    private OrderVerificationCodeUpdateQueueConsumer orderVerificationCodeUpdateQueueConsumer;
    @Autowired
    private QueueDefinition<OrderVerificationCodeUpdateDto> orderVerificationCodeQueueConfiguration;
    @Autowired
    private CheckouterAPI checkouterAPI;
    @Autowired
    private LockerCodeProperties lockerCodeProperties;
    @Autowired
    private CheckouterSetOrderStatusEnqueueService checkouterSetOrderStatusEnqueueService;

    @AfterEach
    public void tearDown() {
        lockerCodeProperties.setEnabled(false);
        lockerCodeProperties.setStartOrderId(0);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Обновление кода ПВЗ")
    @MethodSource("args")
    public void updatePvzVerificationCode(
        @SuppressWarnings("unused") String displayName,
        boolean lockerCodeEnabled,
        long lockerCodeStartOrderId
    ) {
        lockerCodeProperties.setEnabled(lockerCodeEnabled);
        lockerCodeProperties.setStartOrderId(lockerCodeStartOrderId);

        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, null);
        doNothing().when(checkouterAPI).updateDeliveryVerificationCode(
            anyLong(),
            anyString(),
            any(RequestClientInfo.class)
        );
        orderVerificationCodeUpdateQueueConsumer.execute(getTask(false));
        verify(checkouterAPI).updateDeliveryVerificationCode(
            eq(ORDER_ID),
            eq("54321"),
            refEq(requestClientInfo)
        );

        verify(checkouterSetOrderStatusEnqueueService, never()).enqueue(
            anyLong(),
            any(OrderStatus.class),
            any(OrderSubstatus.class)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Обновление кода Постамата")
    @MethodSource("args")
    public void updateLockerVerificationCode(
        @SuppressWarnings("unused") String displayName,
        boolean lockerCodeEnabled,
        long lockerCodeStartOrderId
    ) {
        lockerCodeProperties.setEnabled(lockerCodeEnabled);
        lockerCodeProperties.setStartOrderId(lockerCodeStartOrderId);

        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, null);
        doNothing().when(checkouterAPI).updateDeliveryVerificationCode(
            anyLong(),
            anyString(),
            any(RequestClientInfo.class)
        );
        orderVerificationCodeUpdateQueueConsumer.execute(getTask(true));

        if (lockerCodeEnabled && lockerCodeStartOrderId != 0 && ORDER_ID >= lockerCodeStartOrderId) {
            verify(checkouterAPI).updateDeliveryVerificationCode(
                eq(ORDER_ID),
                eq("54321"),
                refEq(requestClientInfo)
            );
            verify(checkouterSetOrderStatusEnqueueService).enqueue(
                eq(ORDER_ID),
                eq(OrderStatus.PICKUP),
                eq(OrderSubstatus.PICKUP_SERVICE_RECEIVED)
            );
        } else {
            verify(checkouterAPI, never()).updateDeliveryVerificationCode(
                anyLong(),
                anyString(),
                any(RequestClientInfo.class)
            );
            verify(checkouterSetOrderStatusEnqueueService, never()).enqueue(
                anyLong(),
                any(OrderStatus.class),
                any(OrderSubstatus.class)
            );
        }
    }

    private static Stream<Arguments> args() {
        return Stream.of(
            Arguments.of("Проброс кода постамата: отключен", false, 0),
            Arguments.of("Проброс кода постамата: только логирование", false, 1),
            Arguments.of("Проброс кода постамата: включен, заказ после переключения", true, 1),
            Arguments.of("Проброс кода постамата: включен, заказ перед переключением", true, 1002)
        );
    }

    @Test
    @DisplayName("При исключении - результат работы таски fail, "
        + "но так как максимальная попытка, то финальный статус finish")
    public void failOnException() {
        doThrow(ErrorCodeException.class).when(checkouterAPI).updateDeliveryVerificationCode(
            anyLong(),
            anyString(),
            any(RequestClientInfo.class)
        );
        assertThat(orderVerificationCodeUpdateQueueConsumer.execute(getTask(false, true)))
            .isEqualTo(TaskExecutionResult.finish());
    }

    @Test
    @DisplayName("Checkouter ответил ошибкой: заказ отменен")
    public void dontFailOnOrderCancelled() {
        doThrow(new OrderStatusNotAllowedException("code", "status", 0, OrderStatus.CANCELLED, null))
            .when(checkouterAPI)
            .updateDeliveryVerificationCode(
                anyLong(),
                anyString(),
                any(RequestClientInfo.class)
            );
        assertThat(orderVerificationCodeUpdateQueueConsumer.execute(getTask(false)))
            .isEqualTo(TaskExecutionResult.finish());
    }

    @Nonnull
    private Task<OrderVerificationCodeUpdateDto> getTask(boolean isLocker) {
        return getTask(isLocker, false);
    }

    @Nonnull
    private Task<OrderVerificationCodeUpdateDto> getTask(boolean isLocker, boolean isMaxAttempts) {
        return new Task<>(
            new QueueShardId("QUEUE_SHARD_ID"),
            new OrderVerificationCodeUpdateDto(ORDER_ID, "54321", isLocker),
            isMaxAttempts ? orderVerificationCodeQueueConfiguration.getMaxAttempts() : 0,
            ZonedDateTime.now(),
            null,
            null
        );
    }
}
