package ru.yandex.market.clickphite;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.config.TestConfiguration;
import ru.yandex.market.clickphite.utils.ResourceUtils;
import ru.yandex.market.health.configs.clickphite.QueryBuilder;
import ru.yandex.market.health.configs.clickphite.TimeRange;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;

import static ru.yandex.market.health.configs.clickphite.QueryBuilder.EMPTY_TIME_ZONE;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "clickphite.clickhouse.db=market")
public class QueryBuilderTest {
    @Autowired
    private Function<String, ConfigurationService> configurationServiceFactory;
    private ConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        File file = ResourceUtils.getResourceFile("query_builder_test");
        configurationService = configurationServiceFactory.apply(file.getAbsolutePath());
    }

    @Test
    public void testSimpleConfig() {
        MetricContextGroup simpleConfigGroup = getMetricGroupByTableName("simple_test_table");

        String query = simpleConfigGroup.getQueries().getMainQuery();
        Assert.assertEquals(
            "SELECT toUInt32(toDateTime(date, " + EMPTY_TIME_ZONE + ")) AS metric_ts, " +
                "max(x) AS sum_0, " +
                "max(y) AS max_0, " +
                "sum_0 + max_0 AS sum_plus_max_0, " +
                "quantileTDigestIf(0.99)(sum_0 + max_0, type = 'sum_plus_max') AS sum_plus_max_q99_0 " +
                "FROM market.simple_test_table " +
                "WHERE ${SPLIT_WHITELIST} " +
                "AND ${CP_PERIOD} " +
                "GROUP BY metric_ts " +
                "ORDER BY metric_ts",
            query
        );
    }

    @Test
    public void testConfigWithArgMax() {
        MetricContextGroup argMaxConfigGroup = getMetricGroupByTableName("argmax_test_table");

        Assert.assertEquals(
            "SELECT metric_ts, " +
                "max(x) AS sum_0, " +
                "max(y) AS max_0 " +
                "FROM ( " +
                "SELECT toUInt32(toDateTime(date, " + EMPTY_TIME_ZONE + ")) AS metric_ts, " +
                "z, " +
                "argMax(x, timestamp) as x, " +
                "argMax(y, timestamp) as y " +
                "FROM market.argmax_test_table " +
                "WHERE ${CP_PERIOD} " +
                "GROUP BY metric_ts, z ) " +
                "WHERE ${SPLIT_WHITELIST} " +
                "GROUP BY metric_ts " +
                "ORDER BY metric_ts",
            argMaxConfigGroup.getQueries().getMainQuery()
        );
    }

    @Test
    public void testConfigWithSubAggregate() {
        MetricContextGroup argMaxConfigGroup = getMetricGroupByTableName("subaggregate_test_table");

        Assert.assertEquals(
            "SELECT metric_ts, " +
                "countIf(x > 5) AS value_0 " +
                "FROM ( " +
                "SELECT toUInt32(toDateTime(date, " + EMPTY_TIME_ZONE + ")) AS metric_ts, " +
                "z, " +
                "sum(x) as x " +
                "FROM market.subaggregate_test_table " +
                "WHERE ${CP_PERIOD} " +
                "GROUP BY metric_ts, z ) " +
                "WHERE ${SPLIT_WHITELIST} " +
                "GROUP BY metric_ts " +
                "ORDER BY metric_ts",
            argMaxConfigGroup.getQueries().getMainQuery()
        );
    }

    @Test
    public void testGroupedGraphiteMetrics() {
        MetricContextGroup argMaxConfigGroup = getMetricGroupByTableName("grouped_graphite_metrics");

        Assert.assertEquals(
            "SELECT multiply(intDiv(timestamp, 60), 60) AS metric_ts, " +
                "count() AS value_0, " +
                "quantilesTiming(0.5,0.6,0.7,0.80,0.90,0.95,0.97,0.99,0.995,0.997,0.999,1)(some_field_ms) AS value_1," +
                " " +
                "bar, " +
                "baz " +
                "FROM market.grouped_graphite_metrics " +
                "WHERE ${SPLIT_WHITELIST} " +
                "AND ${CP_PERIOD} " +
                "GROUP BY metric_ts, bar AS bar, baz AS baz " +
                "ORDER BY metric_ts",
            argMaxConfigGroup.getQueries().getMainQuery()
        );
    }

    @Test
    public void testRunningWindows() {
        MetricContextGroup simpleConfigGroup = getMetricGroupByTableName("moving_window");
        String queryTemplate = simpleConfigGroup.getQueries().getMainQuery();
        Assert.assertEquals(
            "SELECT ${CP_TIMESTAMP} AS metric_ts, " +
                "avg(x) AS avg_0 " +
                "FROM market.moving_window " +
                "WHERE ${SPLIT_WHITELIST} " +
                "AND ${CP_PERIOD} " +
                "GROUP BY metric_ts " +
                "ORDER BY metric_ts",
            queryTemplate
        );
        TimeRange timeRange = new TimeRange(1562533200, 1562619600);
        String query = QueryBuilder.placeTimeConditionToQuery(queryTemplate, timeRange,
            simpleConfigGroup.getMovingWindowPeriods());
        Assert.assertEquals(
            "SELECT 1562533200 AS metric_ts, " +
                "avg(x) AS avg_0 " +
                "FROM market.moving_window " +
                "WHERE ${SPLIT_WHITELIST} " +
                "AND timestamp >= 1554843600 " +
                "and timestamp <1562619600 " +
                "and date >= toDate(toDateTime(1554843600, " + EMPTY_TIME_ZONE + ")) " +
                "and date <= toDate(toDateTime(1562619600, " + EMPTY_TIME_ZONE + ")) " +
                "GROUP BY metric_ts " +
                "ORDER BY metric_ts",
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
