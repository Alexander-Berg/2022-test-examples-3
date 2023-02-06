package ru.yandex.market.clickphite.utils;

import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.metric.MetricContextGroup;

import java.util.NoSuchElementException;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 21.09.17
 */
public class TestMetricContextGroupUtils {
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
