package ru.yandex.market.health.metric;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;

public class PercentilesByLabelMetricReducerTest {
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
    private static final long[] TEST_VALUES_1;
    private static final long[] TEST_VALUES_2;

    static {
        TEST_VALUES_1 = new long[100];
        TEST_VALUES_2 = new long[100];

        for (int i = 0; i < 100; i++) {
            TEST_VALUES_1[i] = i;
            TEST_VALUES_2[i] = i + 1;
        }
    }


    @Test
    public void testPercentilesByLabelMetricReducer() {
        Set<MetricToWrite> metricToWrite = new PercentilesByLabelMetricReducer(TEST_LABEL_NAME, TEST_SENSOR)
            .reduce(createMetricCollector())
            .collect(Collectors.toSet());

        Assert.assertEquals(
            Set.of(
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "80"
                    ),
                    TEST_SENSOR, 79L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "90"),
                    TEST_SENSOR, 89L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "95"
                    ), TEST_SENSOR, 94L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "97"
                    ),
                    TEST_SENSOR, 96L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "99"
                    ),
                    TEST_SENSOR, 98L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_2,
                        MetricReducer.QUANTILE_LABEL, "80"
                    ),
                    TEST_SENSOR, 80L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_2,
                        MetricReducer.QUANTILE_LABEL, "90"),
                    TEST_SENSOR, 90L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_2,
                        MetricReducer.QUANTILE_LABEL, "95"
                    ), TEST_SENSOR, 95L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_2,
                        MetricReducer.QUANTILE_LABEL, "97"
                    ),
                    TEST_SENSOR, 97L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_2,
                        MetricReducer.QUANTILE_LABEL, "99"
                    ),
                    TEST_SENSOR, 99L
                )
            ),
            metricToWrite
        );
    }

    @Test
    public void testPercentilesByLabelMetricReducerWithFilterByLabelValue() {
        Set<MetricToWrite> metricToWrite = new PercentilesByLabelMetricReducer(
            TEST_LABEL_NAME,
            Set.of(TEST_LABEL_VALUE_1)::contains,
            TEST_SENSOR
        )
            .reduce(createMetricCollector())
            .collect(Collectors.toSet());

        Assert.assertEquals(
            Set.of(

                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "80"
                    ),
                    TEST_SENSOR, 79L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "90"),
                    TEST_SENSOR, 89L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "95"
                    ), TEST_SENSOR, 94L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "97"
                    ),
                    TEST_SENSOR, 96L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "99"
                    ),
                    TEST_SENSOR, 98L
                )
            ),
            metricToWrite
        );
    }

    @Test
    public void testDuplicateValues() {
        MetricCollector metricsWithDuplicates = new MetricCollector();
        long value = 0L;
        for (int i = 1; i <= 100; i++) {
            metricsWithDuplicates.appendMetricRow(
                TEST_LABELS_1,
                TEST_SENSOR,
                value
            );
            if (i == 80 || i == 90 || i == 95 || i == 97 || i == 99) {
                ++value;
            }
        }

        Set<MetricToWrite> metricToWrite = new PercentilesByLabelMetricReducer(
            TEST_LABEL_NAME,
            Set.of(TEST_LABEL_VALUE_1)::contains,
            TEST_SENSOR
        )
            .reduce(metricsWithDuplicates)
            .collect(Collectors.toSet());

        Assert.assertEquals(
            Set.of(
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "80"
                    ),
                    TEST_SENSOR, 0L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "90"),
                    TEST_SENSOR, 1L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "95"
                    ), TEST_SENSOR, 2L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "97"
                    ),
                    TEST_SENSOR, 3L
                ),
                new MetricToWrite(
                    Labels.of(
                        TEST_LABEL_NAME, TEST_LABEL_VALUE_1,
                        MetricReducer.QUANTILE_LABEL, "99"
                    ),
                    TEST_SENSOR, 4L
                )
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
