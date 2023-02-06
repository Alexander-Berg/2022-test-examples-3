package ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.MessagesAmountInformation;
import ru.yandex.market.fulfillment.stockstorage.repository.replica.ReplicaMonitoringRepository;
import ru.yandex.solomon.sensors.registry.SensorsRegistry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.FULL_SYNC_STOCK;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueTypeEnum.KOROBYTE_SYNC;

public class MessageAmountInExecutionQueueMonitoringJobTest {

    @Test
    public void shouldFillSensorsCorrectly() {
        List<MessagesAmountInformation> messagesAmountFullInformation = getMessagesAmountFullInformation();
        ReplicaMonitoringRepository repositoryMock = mockReplicaMonitoringRepository(messagesAmountFullInformation);
        MessageAmountInExecutionQueueMonitoringJob job = new MessageAmountInExecutionQueueMonitoringJob(
                null,
                repositoryMock,
                null
        );

        SensorsRegistry sensorsRegistry = new SensorsRegistry();
        job.fillSensors(sensorsRegistry);

        assertEquals(4, sensorsRegistry.estimateSensorsCount());
    }

    @Test
    public void shouldFillSensorsCorrectlyWithNoneWarehouseId() {
        List<MessagesAmountInformation> messagesAmountFullInformation =
                getMessagesAmountFullInformationNoneWarehouseId();
        ReplicaMonitoringRepository repositoryMock = mockReplicaMonitoringRepository(messagesAmountFullInformation);

        MessageAmountInExecutionQueueMonitoringJob job = new MessageAmountInExecutionQueueMonitoringJob(
                null,
                repositoryMock,
                null
        );

        SensorsRegistry sensorsRegistry = new SensorsRegistry();
        job.fillSensors(sensorsRegistry);

        assertEquals(3, sensorsRegistry.estimateSensorsCount());
    }

    private List<MessagesAmountInformation> getMessagesAmountFullInformation() {
        List<MessagesAmountInformation> messagesAmountFullInformation = new ArrayList<>();
        messagesAmountFullInformation.add(new MessagesAmountInformation(KOROBYTE_SYNC.name(), 141, 10L));
        messagesAmountFullInformation.add(new MessagesAmountInformation(FULL_SYNC_STOCK.name(), 147, 0L));
        messagesAmountFullInformation.add(new MessagesAmountInformation(FULL_SYNC_STOCK.name(), 145, 2L));
        messagesAmountFullInformation.add(new MessagesAmountInformation("UNKNOWN", 147, 5L));
        return messagesAmountFullInformation;
    }

    private List<MessagesAmountInformation> getMessagesAmountFullInformationNoneWarehouseId() {
        List<MessagesAmountInformation> messagesAmountFullInformation = new ArrayList<>();
        messagesAmountFullInformation.add(new MessagesAmountInformation(KOROBYTE_SYNC.name()));
        messagesAmountFullInformation.add(new MessagesAmountInformation(FULL_SYNC_STOCK.name()));
        messagesAmountFullInformation.add(new MessagesAmountInformation("UNKNOWN"));
        return messagesAmountFullInformation;
    }

    private ReplicaMonitoringRepository mockReplicaMonitoringRepository(
            List<MessagesAmountInformation> messagesAmountFullInformation
    ) {
        ReplicaMonitoringRepository repositoryMock = mock(ReplicaMonitoringRepository.class);
        when(repositoryMock.findMessagesAmountFullInformation())
                .thenReturn(messagesAmountFullInformation);
        return repositoryMock;
    }
}
