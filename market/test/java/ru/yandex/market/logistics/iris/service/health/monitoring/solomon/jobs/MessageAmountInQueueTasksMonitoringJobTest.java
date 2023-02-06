package ru.yandex.market.logistics.iris.service.health.monitoring.solomon.jobs;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.logistics.iris.jobs.model.QueueType;
import ru.yandex.market.logistics.iris.model.MessagesAmountInformation;
import ru.yandex.market.logistics.iris.repository.ReplicaMonitoringRepository;

import ru.yandex.solomon.sensors.registry.SensorsRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageAmountInQueueTasksMonitoringJobTest {

    @Test
    public void shouldFillSensorsCorrectly() {
        List<MessagesAmountInformation> messagesAmountFullInformation = getMessagesAmountFullInformation();
        ReplicaMonitoringRepository repositoryMock = mockReplicaMonitoringRepository(messagesAmountFullInformation);

        MessageAmountInQueueTasksMonitoringJob job = new MessageAmountInQueueTasksMonitoringJob(
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
        messagesAmountFullInformation.add(new MessagesAmountInformation(QueueType.REFERENCE_SYNC.name()));
        messagesAmountFullInformation.add(new MessagesAmountInformation(QueueType.SYNC_STOCKS_BY_LIFETIME.name()));
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