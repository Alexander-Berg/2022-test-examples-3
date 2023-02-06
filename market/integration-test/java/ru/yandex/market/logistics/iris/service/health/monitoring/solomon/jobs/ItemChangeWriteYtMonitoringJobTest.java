package ru.yandex.market.logistics.iris.service.health.monitoring.solomon.jobs;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.service.health.monitoring.solomon.PusherToSolomonClient;
import ru.yandex.solomon.sensors.labels.Labels;
import ru.yandex.solomon.sensors.primitives.GaugeInt64;
import ru.yandex.solomon.sensors.registry.SensorsRegistry;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static ru.yandex.market.logistics.iris.service.health.monitoring.solomon.jobs.ItemChangeWriteYtMonitoringJob.AMOUNT_MESSAGE_ITEM_CHANGE_IN_DB_MONITORING_LABEL;
import static ru.yandex.market.logistics.iris.service.health.monitoring.solomon.jobs.ItemChangeWriteYtMonitoringJob.AMOUNT_MESSAGE_ITEM_CHANGE_IN_DB_SENSOR_NAME;
import static ru.yandex.market.logistics.iris.service.health.monitoring.solomon.jobs.ItemChangeWriteYtMonitoringJob.LIMIT_ITEM_CHANGE_ROW_READ_SENSOR_NAME;

public class ItemChangeWriteYtMonitoringJobTest extends AbstractContextualTest {

    @Autowired
    private ItemChangeWriteYtMonitoringJob job;

    @MockBean
    private PusherToSolomonClient pusherToSolomonClient;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/jobs/item_change_monitoring_job/1.xml")
    public void testFillSensors() {
        doNothing().when(pusherToSolomonClient).push(anyString());
        SensorsRegistry sensorsRegistry = new SensorsRegistry();
        job.fillSensors(sensorsRegistry);

        Labels filledLabels = Labels.of(AMOUNT_MESSAGE_ITEM_CHANGE_IN_DB_MONITORING_LABEL);
        GaugeInt64 sensor = sensorsRegistry.gaugeInt64(
                AMOUNT_MESSAGE_ITEM_CHANGE_IN_DB_SENSOR_NAME,
                filledLabels
        );
        assertEquals(sensor.get(), 2);

        sensor = sensorsRegistry.gaugeInt64(
                LIMIT_ITEM_CHANGE_ROW_READ_SENSOR_NAME,
                filledLabels
        );
        assertEquals(sensor.get(), 500_000);
    }
}
