package ru.yandex.market.mbo.solomon;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Unit;
import ru.yandex.inside.solomon.pusher.SolomonPusher;
import ru.yandex.misc.monica.solomon.sensors.PushSensorsData;
import ru.yandex.misc.monica.solomon.sensors.Sensor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author apluhin
 * @created 10/27/20
 */
public class SolomonPushServiceTest {
    private static final String SERVICE_NAME = "test-serivce";
    private SolomonPushService solomonPushService;
    private SolomonPusher solomonPusher;

    @Before
    public void setUp() throws Exception {
        solomonPusher = Mockito.mock(SolomonPusher.class);
        solomonPushService = new SolomonPushService(solomonPusher, "test","cluster", SERVICE_NAME);
    }

    @Test
    public void testPushMetrics() {
        when(solomonPusher.push(any())).thenReturn(CompletableFuture.completedFuture(Unit.U));

        var metric1 = generateTestMetric(1);
        var metric2 = generateTestMetric(2);
        solomonPushService.push(List.of(metric1, metric2));
        var captor = ArgumentCaptor.forClass(PushSensorsData.class);
        verify(solomonPusher, Mockito.times(1)).push(captor.capture());
        var pushData = captor.getValue();
        var sensors = pushData.sensors;
        assertEquals(2, sensors.size());

        for (int i = 1; i <= 2; i++) {
            var testSensor = sensors.get(i - 1);
            assertEquals(Double.valueOf(i + ""), Double.valueOf(testSensor.value));
            assertEquals("test-sensor-" + i, testSensor.labels.get("sensor"));
            assertEquals("someValue-" + i, testSensor.labels.get("l1"));
        }
    }

    @Test
    public void testPushMetricsFailed() {
        when(solomonPusher.push(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("error")));
        var metric = generateTestMetric(1);
        //catch error during send
        solomonPushService.push(metric);
    }

    private Sensor generateTestMetric(long index) {
        return new Sensor(
                Map.of(
                        "sensor", "test-sensor-" + index,
                        "l1", "someValue-" + index
                ),
                index
        );
    }
}
