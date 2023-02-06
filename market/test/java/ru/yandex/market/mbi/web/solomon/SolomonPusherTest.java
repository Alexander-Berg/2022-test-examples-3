package ru.yandex.market.mbi.web.solomon;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.inside.solomon.pusher.SolomonPusher;
import ru.yandex.market.mbi.web.solomon.common.MetricsProvider;
import ru.yandex.market.mbi.web.solomon.metrics.JmxMetricsProvider;
import ru.yandex.market.mbi.web.solomon.metrics.JvmRuntimeMetricsProvider;
import ru.yandex.market.mbi.web.solomon.push.MetricsPusher;
import ru.yandex.misc.monica.solomon.sensors.PushSensorsData;
import ru.yandex.misc.monica.solomon.sensors.Sensor;
import ru.yandex.misc.thread.executor.SyncExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SolomonPusherTest {
    public interface ValueMBean {
        long getValue();

        Long getNullValue();
    }

    private class Value implements ValueMBean {
        private long value = 0;

        @Override
        public long getValue() {
            return value++;
        }

        @Override
        public Long getNullValue() {
            return null;
        }
    }

    @Test
    void pusherTest() {
        // Given
        MetricsProvider jvmRuntimeMetrics = new JvmRuntimeMetricsProvider();
        SolomonPusher solomonPusher = mock(SolomonPusher.class);
        MetricsPusher metricsPusher = new MetricsPusher(solomonPusher, "project", "cluster", "service",
                Collections.singletonList(jvmRuntimeMetrics), new SyncExecutor(), ImmutableMap.of("host", "hostName"));

        // When
        metricsPusher.pushMetrics();

        // Then
        ArgumentCaptor<PushSensorsData> pusherArgumentCaptor = ArgumentCaptor.forClass(PushSensorsData.class);
        verify(solomonPusher).push(pusherArgumentCaptor.capture());
        Map<String, String> labels = pusherArgumentCaptor.getValue().commonLabels;
        List<Sensor> sensors = pusherArgumentCaptor.getValue().sensors;

        assertEquals("project", labels.get("project"));
        assertEquals("cluster", labels.get("cluster"));
        assertEquals("service", labels.get("service"));
        assertEquals("hostName", labels.get("host"));

        sensors.forEach(sensor -> assertTrue(sensor.value > 0 && sensor.ts.map(ts -> ts > 0).isSome(true)));
    }

    @Test
    void jmxTest() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name;
        try {
            name = ObjectName.getInstance("com.example:type=Value");
            Value mBean = new Value();
            mbs.registerMBean(mBean, name);
        } catch (JMException ignored) {
        }

        MetricsProvider jmxMetricsProvider = new JmxMetricsProvider(
                JmxMetricsProvider.toObjectNameGracefully(
                        Collections.singletonMap("com.example:type=Value", Collections.singleton("Value"))
                )
        );

        Sensor firstSensor = jmxMetricsProvider.getMetricsForPush().get(0).createSolomonMetric(42L);
        Sensor secondSensor = jmxMetricsProvider.getMetricsForPush().get(0).createSolomonMetric(43L);

        assertTrue(firstSensor.value == 0 && firstSensor.ts.isSome(42L));
        assertTrue(secondSensor.value == 1 && secondSensor.ts.isSome(43L));
    }

    @Test
    void jmxAttributeReturnNull() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name;
        try {
            name = ObjectName.getInstance("com.example:type=Value");
            Value mBean = new Value();
            mbs.registerMBean(mBean, name);
        } catch (JMException ignored) {
        }

        MetricsProvider jmxMetricsProvider = new JmxMetricsProvider(
                JmxMetricsProvider.toObjectNameGracefully(
                        Collections.singletonMap("com.example:type=Value", Collections.singleton("NullValue"))
                )
        );

        assertThrows(NullPointerException.class,
                () -> jmxMetricsProvider.getMetricsForPush().get(0).createSolomonMetric(42L));
    }

    @Test
    void illegalAttributeValue() {
        String domainName = RandomStringUtils.random(201, 'a');
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name;
        try {
            RandomStringUtils.random(201);
            name = ObjectName.getInstance(domainName + ":type=Value");
            Value mBean = new Value();
            mbs.registerMBean(mBean, name);
        } catch (JMException ignored) {
        }

        assertThrows(IllegalStateException.class, () -> new JmxMetricsProvider(
                JmxMetricsProvider.toObjectNameGracefully(
                        Collections.singletonMap(domainName + ":type=Value", Collections.singleton("Value"))
                )
        ));
    }

}
