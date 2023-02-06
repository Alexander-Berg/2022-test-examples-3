package ru.yandex.market.logistics.cs.dbqueue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.cs.dbqueue.valuecounter.CapacityCounterRecalculationPayload;
import ru.yandex.market.logistics.cs.dbqueue.valuecounter.CapacityCounterRecalculationProducer;
import ru.yandex.market.logistics.cs.domain.jdbc.VersionedServiceCapacityMapping;
import ru.yandex.market.logistics.cs.domain.jdbc.VersionedServiceCapacityMappingTuple;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Возможные флоу работы продюсера")
class CapacityCounterRecalculationProducerTest {

    private final QueueProducer<CapacityCounterRecalculationPayload> queueProducer = Mockito.mock(QueueProducer.class);

    private final CapacityCounterRecalculationProducer producer =
        new CapacityCounterRecalculationProducer(queueProducer);

    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("Задача не ставится в очередь, если переданные списки пустые.")
    void testPossibleFlows(
        List<VersionedServiceCapacityMapping> deletedMappings,
        List<VersionedServiceCapacityMappingTuple> updatedMappings,
        List<VersionedServiceCapacityMapping> addedMappings
    ) {
        producer.enqueue(deletedMappings, updatedMappings, addedMappings);
        if (!deletedMappings.isEmpty() || !updatedMappings.isEmpty() || !addedMappings.isEmpty()) {
            verify(queueProducer).enqueue(eq(EnqueueParams.create(new CapacityCounterRecalculationPayload(
                deletedMappings,
                updatedMappings,
                addedMappings
            ))));
        }
        verifyNoMoreInteractions(queueProducer);
    }

    private static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                List.of(new VersionedServiceCapacityMapping()),
                List.of(new VersionedServiceCapacityMappingTuple()),
                List.of(new VersionedServiceCapacityMapping())
            ),
            Arguments.of(
                List.of(new VersionedServiceCapacityMapping()),
                List.of(new VersionedServiceCapacityMappingTuple()),
                List.of()
            ),
            Arguments.of(
                List.of(new VersionedServiceCapacityMapping()),
                List.of(),
                List.of(new VersionedServiceCapacityMapping())
            ),
            Arguments.of(
                List.of(),
                List.of(new VersionedServiceCapacityMappingTuple()),
                List.of(new VersionedServiceCapacityMapping())
            ),
            Arguments.of(List.of(new VersionedServiceCapacityMapping()), List.of(), List.of()),
            Arguments.of(List.of(), List.of(new VersionedServiceCapacityMappingTuple()), List.of()),
            Arguments.of(List.of(), List.of(), List.of(new VersionedServiceCapacityMapping())),
            Arguments.of(List.of(), List.of(), List.of())
        );
    }
}
