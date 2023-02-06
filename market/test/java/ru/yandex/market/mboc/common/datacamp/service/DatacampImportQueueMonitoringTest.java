package ru.yandex.market.mboc.common.datacamp.service;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.datacamp.repository.DatacampImportQueueRepository;
import ru.yandex.market.mboc.common.datacamp.repository.DatacampImportQueueRepository.DatacampImportQueueStats;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.misc.monica.solomon.sensors.Sensor;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbo.solomon.SolomonPushService.SENSOR_TAG;
import static ru.yandex.market.mboc.common.datacamp.service.DatacampImportQueueMonitoring.AMOUNT_METRIC;
import static ru.yandex.market.mboc.common.datacamp.service.DatacampImportQueueMonitoring.MAX_AGE_METRIC;
import static ru.yandex.market.mboc.common.datacamp.service.DatacampImportQueueMonitoring.WARN_AMOUNT;
import static ru.yandex.market.mboc.common.datacamp.service.DatacampImportQueueMonitoring.WARN_MAX_AGE_SEC;

public class DatacampImportQueueMonitoringTest extends BaseDbTestClass {
    private DatacampImportQueueRepository datacampImportQueueRepository;
    private SolomonPushService solomonPushService;
    private DatacampImportQueueMonitoring monitoring;

    @Before
    public void setUp() {
        datacampImportQueueRepository = mock(DatacampImportQueueRepository.class);

        solomonPushService = mock(SolomonPushService.class);

        monitoring = new DatacampImportQueueMonitoring(
            datacampImportQueueRepository, solomonPushService);
    }

    @Test
    public void testMetrics() {
        var amount = WARN_AMOUNT + 16;
        var maxAge = WARN_MAX_AGE_SEC + 26;
        doReturn(new DatacampImportQueueStats(amount, maxAge)).when(datacampImportQueueRepository).collectStats();
        monitoring.doJob(null);

        @SuppressWarnings("unchecked")
        var captor = (ArgumentCaptor<List<Sensor>>) (Object) ArgumentCaptor.forClass(List.class);
        verify(solomonPushService, times(1)).push(captor.capture());

        var sensors = captor.getValue();
        Assertions.assertThat(sensors.get(0).labels).isEqualTo(Map.of(SENSOR_TAG, AMOUNT_METRIC));
        Assertions.assertThat(sensors.get(0).value).isCloseTo(amount, Percentage.withPercentage(99.999));
        Assertions.assertThat(sensors.get(2).labels).isEqualTo(Map.of(SENSOR_TAG, MAX_AGE_METRIC));
        Assertions.assertThat(sensors.get(2).value).isCloseTo(maxAge, Percentage.withPercentage(99.999));
    }
}
