package ru.yandex.market.partner.notification.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mbi.web.solomon.common.MetricsProvider;
import ru.yandex.market.mbi.web.solomon.common.PullMetric;
import ru.yandex.market.mbi.web.solomon.pull.jvm.JvmMetricsCollector;
import ru.yandex.market.mbi.web.solomon.pull.jvm.TimedMetrics;
import ru.yandex.monlib.metrics.Metric;
import ru.yandex.monlib.metrics.registry.MetricId;

@Configuration
public class SolomonTestJvmConfig {

    @Bean
    public JvmMetricsCollector jvmMetricsCollector() {
        return new TestJvmMetricsCollector(new TestMetricsProvider());
    }

    static class TestJvmMetricsCollector extends JvmMetricsCollector {
        private static final long FIXED_TS_MILLIS = 1005000;

        private MetricsProvider testMetric;

        TestJvmMetricsCollector(MetricsProvider testMetric) {
            super(Collections.singletonList(testMetric));
            this.testMetric = testMetric;
        }

        @Override
        public List<TimedMetrics> getCurrentSensors() {
            Map<MetricId, Metric> sensors = testMetric.getMetricsForPull()
                    .stream()
                    .collect(Collectors.toMap(PullMetric::createSolomonMetricId, PullMetric::createSolomonMetric));
            return Collections.singletonList(new TimedMetrics(sensors, FIXED_TS_MILLIS));
        }

        @Override
        public void afterPropertiesSet() {
            //do nothing (do not start executor service)
        }
    }

    static class TestMetricsProvider extends MetricsProvider {
        TestMetricsProvider() {
            addMetric("test_sensor", () -> 123L, Map.of());
        }
    }
}
