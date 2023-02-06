package ru.yandex.market.mboc.tms.executors.contentprocessing;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository;
import ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository.ContentProcessingResponseQueueStats;
import ru.yandex.misc.monica.solomon.sensors.Sensor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbo.solomon.SolomonPushService.SENSOR_TAG;
import static ru.yandex.market.mboc.tms.executors.contentprocessing.ContentProcessingResponseQueueMonitoringExecutor.IN_QUEUE_COUNT_METRIC;
import static ru.yandex.market.mboc.tms.executors.contentprocessing.ContentProcessingResponseQueueMonitoringExecutor.OLDEST_IN_QUEUE_SEC_METRIC;
import static ru.yandex.market.mboc.tms.executors.contentprocessing.ContentProcessingResponseQueueMonitoringExecutor.OVER_SLA_COUNT_METRIC;

public class ContentProcessingResponseQueueMonitoringExecutorTest {
    private QueueFromContentProcessingRepository repository;
    private SolomonPushService solomonPushService;

    private ContentProcessingResponseQueueMonitoringExecutor executor;

    @Before
    public void setUp() {
        repository = Mockito.mock(QueueFromContentProcessingRepository.class);
        solomonPushService = Mockito.mock(SolomonPushService.class);

        executor = new ContentProcessingResponseQueueMonitoringExecutor(repository, solomonPushService);
    }

    @Test
    public void pushesStatsToSolomon() {
        doReturn(new ContentProcessingResponseQueueStats(
            500, 3600, 100
        )).when(repository).collectStats();

        @SuppressWarnings("unchecked")
        var captor = (ArgumentCaptor<List<Sensor>>) (Object) ArgumentCaptor.forClass(List.class);

        executor.execute();

        verify(solomonPushService, times(1)).push(captor.capture());

        var sensors = captor.getValue();
        assertThat(sensors).hasSize(3);
        assertSensor(sensors, IN_QUEUE_COUNT_METRIC, 500);
        assertSensor(sensors, OLDEST_IN_QUEUE_SEC_METRIC, 3600);
        assertSensor(sensors, OVER_SLA_COUNT_METRIC, 100);
    }

    private void assertSensor(Collection<Sensor> sensors, String name, double value) {
        assertThat(sensors)
            .filteredOn(s -> s.labels.equals(Map.of(SENSOR_TAG, name)))
            .hasSize(1)
            .element(0)
            .extracting(s -> s.value)
            .asInstanceOf(InstanceOfAssertFactories.DOUBLE)
            .isCloseTo(value, Percentage.withPercentage(0.99999));
    }
}
