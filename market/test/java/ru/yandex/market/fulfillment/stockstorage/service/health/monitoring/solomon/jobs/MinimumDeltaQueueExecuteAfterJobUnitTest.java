package ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.repository.replica.ReplicaMonitoringRepository;
import ru.yandex.solomon.sensors.labels.Labels;
import ru.yandex.solomon.sensors.labels.string.StringLabelAllocator;
import ru.yandex.solomon.sensors.registry.SensorsRegistry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.FULL_SYNC_STOCK;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.KOROBYTE_SYNC;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.AbstractSolomonMonitoringJob.STOCK_TYPE_LABEL_NAME;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.MinimumDeltaQueueExecuteAfterJob.MIN_DELTA_EXECUTE_AFTER_LABEL;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.MinimumDeltaQueueExecuteAfterJob.MIN_DELTA_EXECUTE_AFTER_SENSOR_NAME;

public class MinimumDeltaQueueExecuteAfterJobUnitTest {
    @Test
    public void shouldFillSensorsCorrectly() {
        Map<String, LocalDateTime> executeAfterInformation = getExecuteAfterInformation();
        ReplicaMonitoringRepository repositoryMock = mockReplicaMonitoringRepository(executeAfterInformation);

        MinimumDeltaQueueExecuteAfterJob job = new MinimumDeltaQueueExecuteAfterJob(
                getClock(),
                repositoryMock,
                null
        );
        SensorsRegistry sensorsRegistry = new SensorsRegistry();
        job.fillSensors(sensorsRegistry);

        Assertions.assertEquals(2, sensorsRegistry.estimateSensorsCount());
        Assertions.assertEquals(120, sensorsRegistry.gaugeInt64(
                MIN_DELTA_EXECUTE_AFTER_SENSOR_NAME,
                Labels.of(MIN_DELTA_EXECUTE_AFTER_LABEL,
                        StringLabelAllocator.SELF.alloc(STOCK_TYPE_LABEL_NAME, KOROBYTE_SYNC.name()))
        ).get());

        Assertions.assertEquals(90, sensorsRegistry.gaugeInt64(
                MIN_DELTA_EXECUTE_AFTER_SENSOR_NAME,
                Labels.of(MIN_DELTA_EXECUTE_AFTER_LABEL,
                        StringLabelAllocator.SELF.alloc(STOCK_TYPE_LABEL_NAME, FULL_SYNC_STOCK.name()))
        ).get());
    }


    private Map<String, LocalDateTime> getExecuteAfterInformation() {

        return Map.of(KOROBYTE_SYNC.name(), LocalDateTime.of(2021, 7, 12, 12, 0),
                FULL_SYNC_STOCK.name(), LocalDateTime.of(2021, 7, 12, 12, 30));
    }



    private ReplicaMonitoringRepository mockReplicaMonitoringRepository(
            Map<String, LocalDateTime> messagesAmountFullInformation
    ) {
        ReplicaMonitoringRepository repositoryMock = mock(ReplicaMonitoringRepository.class);
        when(repositoryMock.findMinExecuteAfterForEachQueue())
                .thenReturn(messagesAmountFullInformation);
        return repositoryMock;
    }

    private Clock getClock() {
        return Clock.fixed(Instant.parse("2021-07-12T14:00:00.00Z"), ZoneOffset.UTC);
    }

}
