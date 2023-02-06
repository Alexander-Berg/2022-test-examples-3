package ru.yandex.market.logistics.nesu.configuration.queue;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.configuration.queue.support.JsonSerializablePayloadTransformer;
import ru.yandex.market.logistics.nesu.jobs.model.ExecutionQueueItemPayload;
import ru.yandex.market.logistics.nesu.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.nesu.jobs.model.ProcessUploadFeedPayload;
import ru.yandex.market.logistics.nesu.jobs.model.QueueType;
import ru.yandex.market.logistics.nesu.jobs.model.SenderModifiersUploadPayload;
import ru.yandex.market.logistics.nesu.jobs.model.ShopIdPayload;

@DisplayName("Преобразование сообщений для очереди")
class JsonSerializablePayloadTransformerTest extends AbstractContextualTest {

    @DisplayName("Преобразование сообщений")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("transformSource")
    void transform(
        QueueType queueType,
        ExecutionQueueItemPayload expectedPayload
    ) {
        JsonSerializablePayloadTransformer transformer = JsonSerializablePayloadTransformer.of(
            queueType,
            objectMapper
        );

        //noinspection unchecked
        String payloadString = transformer.fromObject(expectedPayload);
        ExecutionQueueItemPayload actualPayload = transformer.toObject(payloadString);

        softly.assertThat(actualPayload)
            .isEqualToComparingFieldByField(expectedPayload);
    }

    @Nonnull
    private static Stream<Arguments> transformSource() {
        return Stream.of(
            Arguments.of(QueueType.PROCESS_UPLOADED_FEED, new ProcessUploadFeedPayload(1L, "reqId")),
            Arguments.of(QueueType.MODIFIER_UPLOAD, new SenderModifiersUploadPayload("reqId", 1L)),
            Arguments.of(QueueType.CREATE_TRUST_PRODUCT, new ShopIdPayload("reqId", 1L)),
            Arguments.of(QueueType.REGISTER_ORDER_CAPACITY, new OrderIdPayload(1L, "reqId"))
        );
    }
}
