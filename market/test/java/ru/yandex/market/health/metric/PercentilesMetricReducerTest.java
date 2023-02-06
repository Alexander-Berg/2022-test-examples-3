package ru.yandex.market.health.metric;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;

public class PercentilesMetricReducerTest {
    private static final Map<String, String> TEST_LABELS = Map.of(
        "labelName1", "labelValue1",
        "labelName2", "labelValue2"
    );
    private static final String TEST_SENSOR = "sensor";
    private static final long[] TEST_VALUES;

    static {
        TEST_VALUES = new long[100];
        for (int i = 0; i < 100; i++) {
            TEST_VALUES[i] = i;
        }
    }

    @Test
    public void testPercentilesMetricReducer() {
        Set<MetricToWrite> metricToWrite = new PercentilesMetricReducer(TEST_SENSOR)
            .reduce(createMetricCollector())
            .collect(Collectors.toSet());

        Assert.assertEquals(
            Set.of(
                new MetricToWrite(Labels.of(MetricReducer.QUANTILE_LABEL, "80"), TEST_SENSOR, 79L),
                new MetricToWrite(Labels.of(MetricReducer.QUANTILE_LABEL, "90"), TEST_SENSOR, 89L),
                new MetricToWrite(Labels.of(MetricReducer.QUANTILE_LABEL, "95"), TEST_SENSOR, 94L),
                new MetricToWrite(Labels.of(MetricReducer.QUANTILE_LABEL, "97"), TEST_SENSOR, 96L),
                new MetricToWrite(Labels.of(MetricReducer.QUANTILE_LABEL, "99"), TEST_SENSOR, 98L)
            ),
            metricToWrite
        );
    }

    private MetricCollector createMetricCollector() {
        MetricCollector metricCollector = new MetricCollector();

        for (long testValue : TEST_VALUES) {
            metricCollector.appendMetricRow(
                TEST_LABELS,
                TEST_SENSOR,
                testValue
            );
        }

        return metricCollector;
    }

}
