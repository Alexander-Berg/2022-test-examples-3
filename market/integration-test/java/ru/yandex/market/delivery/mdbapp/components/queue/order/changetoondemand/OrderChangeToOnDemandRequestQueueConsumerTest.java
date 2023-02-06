package ru.yandex.market.delivery.mdbapp.components.queue.order.changetoondemand;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.OnDemandType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ondemand.ConvertToOnDemandRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ondemand.ConvertToOnDemandRequestPayload;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.order.changetoondemand.confirm.ConfirmOrderChangeToOnDemandRequestEnqueueService;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@DisplayName("Преобразование заказа в заказ с доставкой по клику в Чекаутере")
public class OrderChangeToOnDemandRequestQueueConsumerTest extends AllMockContextualTest {

    private static final long CHECKOUTER_ORDER_ID = 123;
    private static final long LOM_CHANGE_ORDER_REQUEST_ID = 456;

    @Autowired
    private OrderChangeToOnDemandRequestQueueConsumer consumer;

    @Autowired
    private OrderChangeToOnDemandRequestTransformer transformer;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private ConfirmOrderChangeToOnDemandRequestEnqueueService confirmOrderChangeToOnDemandRequestEnqueueService;

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("Проверка изменения заказа в Чекаутере и создания задачи в очереди для информирования LOM")
    public void testExecute(
        @SuppressWarnings("unused") String displayName,
        ChangeRequestStatus changeRequestStatus,
        boolean expectedIsSuccess
    ) {
        doReturn(List.of(createChangeRequest(changeRequestStatus))).when(checkouterClient).editOrder(
            eq(CHECKOUTER_ORDER_ID),
            eq(ClientRole.SYSTEM),
            isNull(),
            eq(List.of(Color.BLUE)),
            eq(createOrderEditRequest())
        );

        TaskExecutionResult result = consumer.execute(createTask());

        softly.assertThat(result)
            .as("Asserting the result is success")
            .isEqualTo(TaskExecutionResult.finish());

        verify(checkouterClient).editOrder(
            eq(CHECKOUTER_ORDER_ID),
            eq(ClientRole.SYSTEM),
            isNull(),
            eq(List.of(Color.BLUE)),
            eq(createOrderEditRequest())
        );

        verify(confirmOrderChangeToOnDemandRequestEnqueueService)
            .enqueue(eq(LOM_CHANGE_ORDER_REQUEST_ID), eq(expectedIsSuccess));
    }

    @Test
    @DisplayName("Проверка корректности десериализации задачи из очереди")
    public void testDeserializeQueueTask() {
        softly.assertThat(transformer.toObject(
            String.format(
                "{\"checkouterOrderId\":%d,\"lomChangeOrderRequestId\":%d, \"reason\":null}",
                CHECKOUTER_ORDER_ID,
                LOM_CHANGE_ORDER_REQUEST_ID
            )
        ))
            .as("Asserting that the task is deserialized correctly")
            .usingRecursiveComparison()
            .isEqualTo(createOrderChangeToOnDemandRequestDto());
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> arguments() {
        return Arrays.stream(ChangeRequestStatus.values()).map(status -> {
            boolean isSuccess = Objects.equals(status, ChangeRequestStatus.APPLIED);
            return Arguments.of(
                "Статус " + status + " - " + (isSuccess ? "подтверждаем" : "отклоняем") + " заявку",
                status,
                isSuccess
            );
        });
    }

    private Task<OrderChangeToOnDemandRequestDto> createTask() {
        return new Task<>(
            new QueueShardId("order.changetoondemand"),
            createOrderChangeToOnDemandRequestDto(),
            5,
            ZonedDateTime.now(),
            null,
            null
        );
    }

    private ChangeRequest createChangeRequest(ChangeRequestStatus changeRequestStatus) {
        return new ChangeRequest(
            LOM_CHANGE_ORDER_REQUEST_ID,
            CHECKOUTER_ORDER_ID,
            new ConvertToOnDemandRequestPayload(OnDemandType.YALAVKA),
            changeRequestStatus,
            Instant.now(),
            "MESSAGE",
            ClientRole.SYSTEM
        );
    }

    private OrderChangeToOnDemandRequestDto createOrderChangeToOnDemandRequestDto() {
        return new OrderChangeToOnDemandRequestDto(
            CHECKOUTER_ORDER_ID,
            LOM_CHANGE_ORDER_REQUEST_ID,
            ChangeOrderRequestReason.DELIVERY_SERVICE_PROBLEM
        );
    }

    private OrderEditRequest createOrderEditRequest() {
        ConvertToOnDemandRequest convertToOnDemandRequest = new ConvertToOnDemandRequest();
        convertToOnDemandRequest.setOnDemandType(OnDemandType.YALAVKA);

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setConvertToOnDemandRequest(convertToOnDemandRequest);

        return orderEditRequest;
    }
}
