package ru.yandex.market.health.metric;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class SumMetricReducerTest {
    private static final Map<String, String> TEST_LABELS = Map.of(
        "labelName1", "labelValue1",
        "labelName2", "labelValue2"
    );
    private static final String TEST_SENSOR = "sensor";
    private static final long[] TEST_VALUES = new long[]{1, 2, 3, 4, 5};

    @Test
    public void testSumMetricReducer() {
        List<MetricToWrite> metricToWrite = new SumMetricReducer(TEST_SENSOR)
            .reduce(createMetricCollector())
            .collect(Collectors.toList());

        Assert.assertEquals(
            List.of(new MetricToWrite(TEST_SENSOR, 15L)),
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
