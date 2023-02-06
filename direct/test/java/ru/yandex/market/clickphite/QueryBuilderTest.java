package ru.yandex.market.clickphite;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.config.TestConfiguration;
import ru.yandex.market.clickphite.metric.MetricContextGroup;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;

public class QueryBuilderTest {
    private ConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        File file = new File("src/test/resources/query_builder_test");
        configurationService = TestConfiguration.createConfigurationService(file.getAbsolutePath());
    }

    @Test
    public void testSimpleConfig() throws Exception {
        MetricContextGroup simpleConfigGroup = getMetricGroupByTableName("simple_test_table");

        String query = simpleConfigGroup.getQueries().getMainQuery();
        Assert.assertEquals(
            "SELECT toUInt32(toDateTime(date)) AS metric_ts, max(x) AS sum_0, max(y) AS max_0, sum_0 + max_0 AS sum_plus_max_0, quantileTDigestIf(0.99)(sum_0 + max_0, type = 'sum_plus_max') AS sum_plus_max_q99_0 FROM market.simple_test_table WHERE 1 AND ${CP_PERIOD} GROUP BY metric_ts ORDER BY metric_ts",
            query
        );
    }

    @Test
    public void testConfigWithArgMax() throws Exception {
        MetricContextGroup argMaxConfigGroup = getMetricGroupByTableName("argmax_test_table");

        Assert.assertEquals(
            "SELECT metric_ts, max(x) AS sum_0, max(y) AS max_0 FROM ( SELECT toUInt32(toDateTime(date)) AS metric_ts, z, argMax(x, timestamp) as x, argMax(y, timestamp) as y FROM market.argmax_test_table WHERE ${CP_PERIOD} GROUP BY metric_ts, z ) WHERE 1 GROUP BY metric_ts ORDER BY metric_ts",
            argMaxConfigGroup.getQueries().getMainQuery()
        );
    }

    @Test
    public void testConfigWithSubAggregate() throws Exception {
        MetricContextGroup argMaxConfigGroup = getMetricGroupByTableName("subaggregate_test_table");

        Assert.assertEquals(
                "SELECT metric_ts, countIf(x > 5) AS value_0 FROM ( SELECT toUInt32(toDateTime(date)) AS metric_ts, z, sum(x) as x FROM market.subaggregate_test_table WHERE ${CP_PERIOD} GROUP BY metric_ts, z ) WHERE 1 GROUP BY metric_ts ORDER BY metric_ts",
                argMaxConfigGroup.getQueries().getMainQuery()
        );
    }

    @Test
    public void testGroupedGraphiteMetrics() throws Exception {
        MetricContextGroup argMaxConfigGroup = getMetricGroupByTableName("grouped_graphite_metrics");

        Assert.assertEquals(
            "SELECT multiply(intDiv(timestamp, 60), 60) AS metric_ts, count() AS value_0, quantilesTiming(0.5,0.6,0.7,0.80,0.90,0.95,0.97,0.99,0.995,0.997,0.999,1)(some_field_ms) AS value_1, bar, baz FROM market.grouped_graphite_metrics WHERE 1 AND ${CP_PERIOD} GROUP BY metric_ts, bar AS bar, baz AS baz ORDER BY metric_ts",
            argMaxConfigGroup.getQueries().getMainQuery()
        );
    }

    @Test
    public void testRunningWindows() {
        MetricContextGroup simpleConfigGroup = getMetricGroupByTableName("moving_window");
        String queryTemplate = simpleConfigGroup.getQueries().getMainQuery();
        Assert.assertEquals(
            "SELECT ${CP_TIMESTAMP} AS metric_ts, avg(x) AS avg_0 FROM market.moving_window " +
                "WHERE 1 AND ${CP_PERIOD} GROUP BY metric_ts ORDER BY metric_ts",
            queryTemplate
        );
        TimeRange timeRange = new TimeRange(1562533200, 1562619600);
        String query = QueryBuilder.placeTimeConditionToQuery(queryTemplate, timeRange, simpleConfigGroup.getMovingWindowPeriods());
        Assert.assertEquals(
            "SELECT 1562533200 AS metric_ts, avg(x) AS avg_0 FROM market.moving_window " +
                "WHERE 1 AND timestamp >= 1554843600 and timestamp <1562619600 " +
                "and date >= toDate(toDateTime(1554843600)) and date <= toDate(toDateTime(1562619600)) " +
                "GROUP BY metric_ts ORDER BY metric_ts",
            query
        );

    }

    private MetricContextGroup getMetricGroupByTableName(String tableName) {
        List<MetricContextGroup> metricContextGroups = configurationService.getConfiguration().getMetricContextGroups();
        return metricContextGroups.stream()
            .filter(g -> g.getTable().getName().equals(tableName))
            .findFirst()
            .orElseThrow(NoSuchElementException::new);
    }

}
