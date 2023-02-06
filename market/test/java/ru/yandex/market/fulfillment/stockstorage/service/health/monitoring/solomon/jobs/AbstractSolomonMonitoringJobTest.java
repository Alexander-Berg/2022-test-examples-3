package ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.PusherToSolomonClient;
import ru.yandex.solomon.sensors.primitives.GaugeInt64;
import ru.yandex.solomon.sensors.registry.SensorsRegistry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AbstractSolomonMonitoringJobTest {

    private static final PusherToSolomonClient PUSHER = mock(PusherToSolomonClient.class);
    private static final AbstractSolomonMonitoringJob INSTANCE_TO_TEST = new AbstractSolomonMonitoringJob(
            Clock.systemUTC(),
            null,
            PUSHER
    ) {
        @Override
        void fillSensors(SensorsRegistry sensorsRegistry) {
            GaugeInt64 gaugeInt64 = sensorsRegistry.gaugeInt64("testLabel");
            gaugeInt64.set(10L);
        }
    };

    @Test
    public void shouldPushEncodedMonitoring() {
        INSTANCE_TO_TEST.trigger();

        ArgumentCaptor<String> captor = forClass(String.class);
        verify(PUSHER).push(captor.capture());
        String argument = captor.getValue();
        assertNotNull(argument);
        assertTrue(
                argument.contains("\"sensors\":[{\"kind\":\"IGAUGE\",\"labels\":{\"sensor\":\"testLabel\"}," +
                        "\"value\":10}]")
        );
    }
}
