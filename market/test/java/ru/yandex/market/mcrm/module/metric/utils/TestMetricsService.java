package ru.yandex.market.mcrm.module.metric.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import ru.yandex.market.mcrm.module.metric.MetricDataType;
import ru.yandex.market.mcrm.module.metric.MetricGroup;
import ru.yandex.market.mcrm.module.metric.MetricType;
import ru.yandex.market.mcrm.module.metric.impl.MetricsServiceImpl;

@Primary
@Component
public class TestMetricsService extends MetricsServiceImpl {

    private final List<LoggedItem> loggedItems;

    public TestMetricsService() {
        this.loggedItems = new ArrayList<>();
    }

    public synchronized void clear() {
        this.loggedItems.clear();
    }

    public synchronized List<LoggedItem> getLoggedItemsByCode(String code) {
        if (null == code) {
            return Collections.emptyList();
        }
        return this.loggedItems
                .stream()
                .filter(x -> code.equals(x.getMetricType().getCode()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void logMetric(
            @Nonnull MetricGroup group,
            @Nonnull String metric,
            @Nonnull MetricDataType dataType,
            @Nonnull Number value,
            String context
    ) {
        this.loggedItems.add(new LoggedItem(new MetricType(metric, group, dataType), value, context));
        super.logMetric(group, metric, dataType, value, context);
    }

    public static class LoggedItem {

        private final MetricType metricType;
        private final Number value;
        private final String context;

        public LoggedItem(MetricType metricType,
                          Number value,
                          String context) {
            this.metricType = metricType;
            this.value = value;
            this.context = context;
        }

        public MetricType getMetricType() {
            return metricType;
        }

        public Number getValue() {
            return value;
        }

        public String getContext() {
            return context;
        }
    }
}
