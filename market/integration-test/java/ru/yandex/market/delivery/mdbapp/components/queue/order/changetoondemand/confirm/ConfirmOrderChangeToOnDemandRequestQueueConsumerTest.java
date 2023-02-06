package ru.yandex.market.delivery.mdbapp.components.queue.order.changetoondemand.confirm;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.change.ConfirmChangeOrderToOnDemandRequest;
import ru.yandex.market.logistics.lom.model.dto.change.DenyChangeOrderToOnDemandRequest;
import ru.yandex.market.logistics.lom.model.dto.change.RequestBase;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DisplayName("Подтверждение/отклонение заявки на преобразование заказа в заказ с доставкой по клику")
class ConfirmOrderChangeToOnDemandRequestQueueConsumerTest extends AllMockContextualTest {

    private static final long LOM_CHANGE_ORDER_REQUEST_ID = 456;

    @Autowired
    private ConfirmOrderChangeToOnDemandRequestQueueConsumer consumer;

    @Autowired
    private ConfirmOrderChangeToOnDemandRequestTransformer transformer;

    @Autowired
    private LomClient lomClient;

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("Проверка посылаемого в LOM запроса о подтверждении/отклонении заявки")
    void testExecute(
        @SuppressWarnings("unused") String displayName,
        Boolean isSuccess,
        @SuppressWarnings("rawtypes") RequestBase expectedRequestDto
    ) {
        TaskExecutionResult result = consumer.execute(createTask(isSuccess));

        softly.assertThat(result)
            .as("Asserting the result is success")
            .isEqualTo(TaskExecutionResult.finish());

        verify(lomClient)
            .processChangeOrderRequest(eq(LOM_CHANGE_ORDER_REQUEST_ID), eq(expectedRequestDto));
    }

    @Test
    @DisplayName("Проверка корректности десериализации задачи из очереди")
    public void testDeserializeQueueTask() {
        softly.assertThat(transformer.toObject(
                String.format(
                    "{\"lomChangeOrderRequestId\":%d,\"isSuccess\":%b,}",
                    LOM_CHANGE_ORDER_REQUEST_ID,
                    true
                )
            ))
            .as("Asserting that the task is deserialized correctly")
            .usingRecursiveComparison()
            .isEqualTo(createConfirmOrderChangeToOnDemandRequestDto(true));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(
                "Подтверждаем заявку, если isSuccess == true",
                true,
                new ConfirmChangeOrderToOnDemandRequest()
            ),
            Arguments.of("Отклоняем заявку, если isSuccess == false", false, new DenyChangeOrderToOnDemandRequest()),
            Arguments.of("Отклоняем заявку, если isSuccess == null", null, new DenyChangeOrderToOnDemandRequest())
        );
    }

    private Task<ConfirmOrderChangeToOnDemandRequestDto> createTask(Boolean isSuccess) {
        return new Task<>(
            new QueueShardId("order.changetoondemand.confirm"),
            createConfirmOrderChangeToOnDemandRequestDto(isSuccess),
            5,
            ZonedDateTime.now(),
            null,
            null
        );
    }

    private ConfirmOrderChangeToOnDemandRequestDto createConfirmOrderChangeToOnDemandRequestDto(Boolean isSuccess) {
        return new ConfirmOrderChangeToOnDemandRequestDto(LOM_CHANGE_ORDER_REQUEST_ID, isSuccess);
    }
}
