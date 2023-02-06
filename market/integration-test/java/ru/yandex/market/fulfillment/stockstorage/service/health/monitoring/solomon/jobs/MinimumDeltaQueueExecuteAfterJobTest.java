package ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.PusherToSolomonClient;
import ru.yandex.solomon.sensors.labels.Label;
import ru.yandex.solomon.sensors.labels.Labels;
import ru.yandex.solomon.sensors.labels.string.StringLabelAllocator;
import ru.yandex.solomon.sensors.primitives.GaugeInt64;
import ru.yandex.solomon.sensors.registry.SensorsRegistry;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.AbstractSolomonMonitoringJob.STOCK_TYPE_LABEL_NAME;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.MinimumDeltaQueueExecuteAfterJob.MIN_DELTA_EXECUTE_AFTER_LABEL;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.MinimumDeltaQueueExecuteAfterJob.MIN_DELTA_EXECUTE_AFTER_SENSOR_NAME;

public class MinimumDeltaQueueExecuteAfterJobTest extends AbstractContextualTest {

    @Autowired
    private MinimumDeltaQueueExecuteAfterJob minimumDeltaQueueExecuteAfterJob;

    @MockBean
    private PusherToSolomonClient pusherToSolomonClient;

    public static final Label SLOW_FULL_SYNC_STOCK = StringLabelAllocator.SELF.alloc(
            STOCK_TYPE_LABEL_NAME,
            "SLOW_FULL_SYNC_STOCK"
    );

    public static final Label PRIORITY_FULL_SYNC_STOCK = StringLabelAllocator.SELF.alloc(
            STOCK_TYPE_LABEL_NAME,
            "PRIORITY_FULL_SYNC_STOCK"
    );

    public static final Label FULL_SYNC_STOCK = StringLabelAllocator.SELF.alloc(
            STOCK_TYPE_LABEL_NAME,
            "FULL_SYNC_STOCK"
    );

    @Test
    @DatabaseSetup("classpath:database/states/health/min_delta_queue/setup.xml")
    public void testFillSensors() {
        doNothing().when(pusherToSolomonClient).push(anyString());
        SensorsRegistry sensorsRegistry = new SensorsRegistry();
        minimumDeltaQueueExecuteAfterJob.fillSensors(sensorsRegistry);


        Labels filledLabels = Labels.of(MIN_DELTA_EXECUTE_AFTER_LABEL, SLOW_FULL_SYNC_STOCK);
        GaugeInt64 sensor = sensorsRegistry.gaugeInt64(
                MIN_DELTA_EXECUTE_AFTER_SENSOR_NAME,
                filledLabels
        );
        Assertions.assertEquals(0, sensor.get());

        filledLabels = Labels.of(MIN_DELTA_EXECUTE_AFTER_LABEL, FULL_SYNC_STOCK);
        sensor = sensorsRegistry.gaugeInt64(
                MIN_DELTA_EXECUTE_AFTER_SENSOR_NAME,
                filledLabels
        );
        Assertions.assertEquals(30, sensor.get());

        filledLabels = Labels.of(MIN_DELTA_EXECUTE_AFTER_LABEL, PRIORITY_FULL_SYNC_STOCK);
        sensor = sensorsRegistry.gaugeInt64(
                MIN_DELTA_EXECUTE_AFTER_SENSOR_NAME,
                filledLabels
        );
        Assertions.assertEquals(0, sensor.get());

    }

}
