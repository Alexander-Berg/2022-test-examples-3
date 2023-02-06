package ru.yandex.market.logistics.cs.dbqueue.servicecounter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.cs.AbstractTest;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;

class ServiceCounterBatchPayloadTest extends AbstractTest {

    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @ParameterizedTest
    @MethodSource("testCompatibleDeserializationData")
    @SneakyThrows
    void testCompatibleDeserialization(String oldJsonPath, ServiceCounterBatchPayload expected) {
        softly.assertThat(mapper.readValue(extractFileContent(oldJsonPath), ServiceCounterBatchPayload.class))
            .isEqualTo(expected);
    }

    private static Stream<Arguments> testCompatibleDeserializationData() {
        return Stream.of(
            Arguments.of(
                "json/payload/service_counter_batch_payload_1.json",
                payload(0, 0)
            ),
            Arguments.of(
                "json/payload/service_counter_batch_payload_2.json",
                payload(3, 3)
            ),
            Arguments.of(
                "json/payload/service_counter_batch_payload_3.json",
                payload(3, 12)
            )
        );
    }

    private static ServiceCounterBatchPayload payload(int realItemCount, int itemCountWithFactor) {
        return ServiceCounterBatchPayload.builder()
            .eventId(10L)
            .eventType(EventType.NEW)
            .counters(List.of(
                new ServiceDeliveryDescriptor(11L, LocalDate.of(2021, 8, 23), realItemCount, itemCountWithFactor)
            ))
            .orderCount(3)
            .dummy(false)
            .build();
    }
}
