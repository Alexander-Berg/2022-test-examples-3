package ru.yandex.market.logistics.lrm.queue.payload;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.dbqueue.payload.QueuePayload;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.queue.payload.meta.CreateEntityMetaPayload;
import ru.yandex.market.logistics.lrm.queue.payload.meta.ReturnSegmentShipmentChangedMetaWrapper;
import ru.yandex.market.logistics.lrm.service.meta.model.ReturnSegmentShipmentChangedMeta;

@DisplayName("Десериализация dbqueue пэйлоада")
class DbqueuePayloadDeserializerTest extends AbstractIntegrationTest {

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Успех")
    void deserializePayload(
        @SuppressWarnings("unused") String displayName,
        String payloadString,
        QueuePayload payloadObject
    ) throws JsonProcessingException {
        Assertions.assertThat(objectMapper.readValue(payloadString, payloadObject.getClass()))
            .usingRecursiveComparison()
            .isEqualTo(payloadObject);
    }

    @Nonnull
    private static Stream<Arguments> deserializePayload() {
        return Stream.of(
            Arguments.of(
                "CreateEntityMetaPayload",
                """
                        {
                            "requestId": "test-request-id/4",
                            "entityType": "RETURN_SEGMENT",
                            "entityMetaWrappers": [
                                {
                                    "@type": "RETURN_SEGMENT_SHIPMENT_CHANGED",
                                    "entityId": 11,
                                    "entityMeta": {
                                        "datetime": "2022-01-01T12:00:00Z"
                                    }
                                }
                            ]
                        }
                    """,
                CreateEntityMetaPayload.builder()
                    .requestId("test-request-id/4")
                    .entityType(EntityType.RETURN_SEGMENT)
                    .entityMetaWrappers(
                        List.of(
                            ReturnSegmentShipmentChangedMetaWrapper.builder()
                                .entityId(11L)
                                .entityMeta(
                                    ReturnSegmentShipmentChangedMeta.builder()
                                        .datetime(Instant.parse("2022-01-01T12:00:00Z"))
                                        .build()
                                )
                                .build()
                        )
                    )
                    .build()

            )
        );
    }
}
