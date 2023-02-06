package ru.yandex.market.mboc.common.services.offers.queue;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.offers.repository.queue.OfferQueueRepository;
import ru.yandex.misc.monica.solomon.sensors.Sensor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.solomon.SolomonPushService.SENSOR_TAG;
import static ru.yandex.market.mboc.common.services.offers.queue.OfferQueueSolomonMonitoringExecutor.IN_QUEUE_COUNT_METRIC;
import static ru.yandex.market.mboc.common.services.offers.queue.OfferQueueSolomonMonitoringExecutor.OLDEST_IN_QUEUE_SEC_METRIC;

public class OfferQueueSolomonMonitoringExecutorTest {

    private SolomonPushService solomonPushServiceMock;
    private OfferQueueRepository offerQueueRepositoryMock;

    private OfferQueueSolomonMonitoringExecutor monitoringExecutor;

    @Before
    public void setUp() {
        solomonPushServiceMock = mock(SolomonPushService.class);
        offerQueueRepositoryMock = mock(OfferQueueRepository.class);

        monitoringExecutor = new OfferQueueSolomonMonitoringExecutor(
            offerQueueRepositoryMock,
            solomonPushServiceMock,
            "test"
        );
    }

    @Test
    public void testPushSolomonSensorValues() {
        OfferQueueRepository.OfferQueueStats stats = OfferQueueRepository.OfferQueueStats.builder()
            .inQueueCount(777)
            .oldestInQueueSec(999)
            .build();
        when(offerQueueRepositoryMock.countStats())
            .thenReturn(stats);

        monitoringExecutor.execute();

        @SuppressWarnings("unchecked")
        var sensorCaptor = (ArgumentCaptor<List<Sensor>>) (Object) ArgumentCaptor.forClass(List.class);

        verify(solomonPushServiceMock, times(1)).push(sensorCaptor.capture());

        var sensors = sensorCaptor.getValue();
        assertThat(sensors)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
            new Sensor(
                Map.of(SENSOR_TAG, String.format(IN_QUEUE_COUNT_METRIC, "test")),
                stats.getInQueueCount()
            ),
            new Sensor(
                Map.of(SENSOR_TAG, String.format(OLDEST_IN_QUEUE_SEC_METRIC, "test")),
                stats.getOldestInQueueSec()
            )
        );
    }
}
