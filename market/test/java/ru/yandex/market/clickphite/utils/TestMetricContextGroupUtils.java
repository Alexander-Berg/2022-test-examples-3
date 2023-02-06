package ru.yandex.market.clickphite.utils;

import java.util.NoSuchElementException;

import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 21.09.17
 */
public class TestMetricContextGroupUtils {
    private TestMetricContextGroupUtils() {
    }

    public static MetricContextGroup getMetricContextGroupByMetric(String metricName,
                                                                   ConfigurationService configurationService) {
        return configurationService.getConfiguration()
            .getMetricContextGroups()
            .stream()
            .filter(g -> g.getMetricContexts().stream().anyMatch(m -> {
                return m.getId().endsWith(metricName);
            }))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No group containing metric " + metricName));
    }
}
