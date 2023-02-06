package ru.yandex.market.markup2.tasks.fill_param_values_metric.metric;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.markup2.tasks.fill_param_values_metric.MetricAgregator;
import ru.yandex.market.markup2.tasks.fill_param_values_metric.MetricData;
import ru.yandex.market.markup2.utils.ParameterTestUtils;
import ru.yandex.market.mbo.export.MboParameters;

/**
 * @author V.Zaytsev
 * @since 19.07.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MetricAgregatorTest {

    private static final MboParameters.Parameter PARAMETER1 =
        ParameterTestUtils.createParameterBuilder(1L, MboParameters.ValueType.ENUM, "p1")
            .setImportant(false)
            .build();
    private static final MboParameters.Parameter PARAMETER2 =
        ParameterTestUtils.createParameterBuilder(2L, MboParameters.ValueType.ENUM, "p2")
            .setImportant(true)
            .build();
    private static final MboParameters.Parameter PARAMETER3 =
        ParameterTestUtils.createParameterBuilder(3L, MboParameters.ValueType.ENUM, "p3")
            .setImportant(false)
            .build();

    @Test
    public void importantCalculatedSeparately() {
        MetricAgregator metricAgregator = new MetricAgregator();
        metricAgregator.incFilledSame(PARAMETER1);
        metricAgregator.incFilledSame(PARAMETER2);
        metricAgregator.incFilledInEtalon(PARAMETER3);

        metricAgregator.makeSnapshotAndClean();

        metricAgregator.incFilledSame(PARAMETER1);
        metricAgregator.incFilledDifferent(PARAMETER2);
        metricAgregator.incFilledSame(PARAMETER3);

        metricAgregator.makeSnapshotAndClean();

        MetricData allMetricData = metricAgregator.makeAvgAllMetric();
        MetricData importantMetricData = metricAgregator.makeAvgImportantMetric();

        assertDouble(0.8333, allMetricData.getPrecision());
        assertDouble(0.8333, allMetricData.getRecall());
        assertDouble(0.5, importantMetricData.getPrecision());
        assertDouble(1, importantMetricData.getRecall());
    }

    private static void assertDouble(double expected, double actual) {
        Assert.assertEquals(expected, actual, 0.0001);
    }
}
