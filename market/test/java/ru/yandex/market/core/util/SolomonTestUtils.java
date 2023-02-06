package ru.yandex.market.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.market.mbi.web.solomon.pull.SolomonUtils;
import ru.yandex.monlib.metrics.MetricConsumer;
import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.histogram.HistogramSnapshot;
import ru.yandex.monlib.metrics.labels.Label;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.registry.MetricId;

public class SolomonTestUtils {

    private SolomonTestUtils() {
    }

    /**
     * Очищает метрики соломона.
     */
    public static void cleanMetrics() {
        final List<MetricId> metrics = new ArrayList<>();
        SolomonUtils.getMetricRegistry().supply(123L, new MetricConsumer() {
            private final Map<String, String> labels = new HashMap<>();

            @Override
            public void onStreamBegin(int countHint) {

            }

            @Override
            public void onStreamEnd() {

            }

            @Override
            public void onCommonTime(long tsMillis) {

            }

            @Override
            public void onMetricBegin(MetricType type) {

            }

            @Override
            public void onMetricEnd() {

            }

            @Override
            public void onLabelsBegin(int countHint) {
                labels.clear();
            }

            @Override
            public void onLabelsEnd() {
                final String sensorName = labels.remove("sensor");
                final MetricId metricId = new MetricId(sensorName, Labels.of(labels));
                metrics.add(metricId);
            }

            @Override
            public void onLabel(Label label) {
                labels.put(label.getKey(), label.getValue());
            }

            @Override
            public void onDouble(long tsMillis, double value) {

            }

            @Override
            public void onLong(long tsMillis, long value) {

            }

            @Override
            public void onHistogram(long tsMillis, HistogramSnapshot snapshot) {

            }
        });

        metrics.forEach(SolomonUtils.getMetricRegistry()::removeMetric);
    }
}
