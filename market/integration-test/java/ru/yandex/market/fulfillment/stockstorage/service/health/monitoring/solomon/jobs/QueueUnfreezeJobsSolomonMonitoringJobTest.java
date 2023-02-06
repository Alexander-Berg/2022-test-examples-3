package ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.PusherToSolomonClient;
import ru.yandex.solomon.sensors.labels.Labels;
import ru.yandex.solomon.sensors.primitives.GaugeInt64;
import ru.yandex.solomon.sensors.registry.SensorsRegistry;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.QueuedUnfreezeJobsSolomonMonitoringJob.AMOUNT_FEW_ATTEMPTS;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.QueuedUnfreezeJobsSolomonMonitoringJob.AMOUNT_MANY_ATTEMPTS;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.QueuedUnfreezeJobsSolomonMonitoringJob.AMOUNT_READY_TO_RUN;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.QueuedUnfreezeJobsSolomonMonitoringJob.AMOUNT_ZERO_ATTEMPTS;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.QueuedUnfreezeJobsSolomonMonitoringJob.MESSAGES_ATTEMPT_NUMBERS_MONITORING_LABEL;
import static ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.QueuedUnfreezeJobsSolomonMonitoringJob.UNFREEZE_JOB_LAG_SECONDS;

public class QueueUnfreezeJobsSolomonMonitoringJobTest extends AbstractContextualTest {
    @Autowired
    private QueuedUnfreezeJobsSolomonMonitoringJob job;

    @MockBean
    private PusherToSolomonClient pusherToSolomonClient;

    @Test
    @DatabaseSetup("classpath:database/states/unfreeze_jobs/queued/setup.xml")
    public void testFillSensors() {
        doNothing().when(pusherToSolomonClient).push(anyString());
        SensorsRegistry sensorsRegistry = new SensorsRegistry();
        job.fillSensors(sensorsRegistry);

        Labels filledLabels = Labels.of(MESSAGES_ATTEMPT_NUMBERS_MONITORING_LABEL);
        GaugeInt64 sensor = sensorsRegistry.gaugeInt64(
                AMOUNT_ZERO_ATTEMPTS,
                filledLabels
        );
        assertions().assertThat(sensor.get()).isEqualTo(2);

        sensor = sensorsRegistry.gaugeInt64(
                AMOUNT_FEW_ATTEMPTS,
                filledLabels
        );
        assertions().assertThat(sensor.get()).isEqualTo(3);

        sensor = sensorsRegistry.gaugeInt64(
                AMOUNT_MANY_ATTEMPTS,
                filledLabels
        );
        assertions().assertThat(sensor.get()).isEqualTo(1);

        sensor = sensorsRegistry.gaugeInt64(
                AMOUNT_READY_TO_RUN,
                filledLabels
        );
        assertions().assertThat(sensor.get()).isEqualTo(5);

        sensor = sensorsRegistry.gaugeInt64(
                UNFREEZE_JOB_LAG_SECONDS,
                filledLabels
        );
        assertions().assertThat(sensor.get()).isEqualTo(225);
    }
}
