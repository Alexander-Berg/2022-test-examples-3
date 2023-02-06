package ru.yandex.market.health.metric;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;

public class SumByLabelMetricReducerTest {
    private static final String TEST_LABEL_NAME = "labelName";
    private static final String TEST_LABEL_VALUE_1 = "labelValue1";
    private static final String TEST_LABEL_VALUE_2 = "labelValue2";
    private static final Map<String, String> TEST_LABELS_1 = Map.of(
        TEST_LABEL_NAME, TEST_LABEL_VALUE_1
    );
    private static final Map<String, String> TEST_LABELS_2 = Map.of(
        TEST_LABEL_NAME, TEST_LABEL_VALUE_2
    );
    private static final String TEST_SENSOR = "sensor";
    private static final long[] TEST_VALUES_1 = new long[]{1, 2, 3, 4, 5};
    private static final long[] TEST_VALUES_2 = new long[]{10, 20, 30, 40, 50};


    @Test
    public void testSumByLabelMetricReducer() {
        Set<MetricToWrite> metricToWrite = new SumByLabelMetricReducer(TEST_LABEL_NAME, TEST_SENSOR)
            .reduce(createMetricCollector())
            .collect(Collectors.toSet());

        Assert.assertEquals(
            Set.of(
                new MetricToWrite(Labels.of(TEST_LABEL_NAME, TEST_LABEL_VALUE_1), TEST_SENSOR, 15L),
                new MetricToWrite(Labels.of(TEST_LABEL_NAME, TEST_LABEL_VALUE_2), TEST_SENSOR, 150L)
            ),
            metricToWrite
        );
    }

    @Test
    public void testSumByLabelMetricReducerWithFilterByLabelValue() {
        Set<MetricToWrite> metricToWrite = new SumByLabelMetricReducer(
            TEST_LABEL_NAME,
            Set.of(TEST_LABEL_VALUE_1)::contains,
            TEST_SENSOR
        )
            .reduce(createMetricCollector())
            .collect(Collectors.toSet());

        Assert.assertEquals(
            Set.of(
                new MetricToWrite(Labels.of(TEST_LABEL_NAME, TEST_LABEL_VALUE_1), TEST_SENSOR, 15L)
            ),
            metricToWrite
        );
    }

    private MetricCollector createMetricCollector() {
        MetricCollector metricCollector = new MetricCollector();

        for (long testValue : TEST_VALUES_1) {
            metricCollector.appendMetricRow(
                TEST_LABELS_1,
                TEST_SENSOR,
                testValue
            );
        }

        for (long testValue : TEST_VALUES_2) {
            metricCollector.appendMetricRow(
                TEST_LABELS_2,
                TEST_SENSOR,
                testValue
            );
        }

        return metricCollector;
    }
}
