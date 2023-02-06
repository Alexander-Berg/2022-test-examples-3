package ru.yandex.market.clickphite.graphite;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.clickphite.metric.GraphiteMetricContext;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 08.08.17
 */
public class MetricTest {
    @Test
    public void metricWithoutSubstitution() throws Exception {
        String name = "some.metric";
        Metric metric = createSut(new String[]{name}, new String[0]);
        Assert.assertEquals(name, metric.getName());
    }

    @Test
    public void metricSubstitution() throws Exception {
        Metric metric = createSut(
            new String[]{"some.metric.", ".", ""}, new String[]{"value1", "value2"}, ".postfix"
        );

        Assert.assertEquals("some.metric.value1.value2.postfix", metric.getName());
    }

    @Test
    public void metricSubstitutionToFirstPosition() throws Exception {
        Metric metric = createSut(new String[]{"", ".some.metric"}, new String[]{"value1"});

        Assert.assertEquals("value1.some.metric", metric.getName());
    }

    private Metric createSut(String[] metricNameParts, String[] splitValues) {
        return new Metric(
            0, 0.0, GraphiteMetricContext.getTotalStringsLength(metricNameParts),
            metricNameParts, splitValues
        );
    }

    private Metric createSut(String[] metricNameParts, String[] splitValues, String postfix) {
        return new Metric(
            0, 0.0, GraphiteMetricContext.getTotalStringsLength(metricNameParts),
            metricNameParts, splitValues, postfix, ""
        );
    }
}