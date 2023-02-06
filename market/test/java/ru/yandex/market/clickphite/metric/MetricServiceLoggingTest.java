package ru.yandex.market.clickphite.metric;

import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
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
import ru.yandex.market.health.configs.clickphite.TimeRange;
import ru.yandex.market.health.configs.clickphite.metric.MetricContext;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;
import ru.yandex.market.health.configs.clickphite.metric.SentMetricsStat;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 26.10.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "clickphite.clickhouse.db=market")
public class MetricServiceLoggingTest {
    @Autowired
    private Function<String, ConfigurationService> configurationServiceFactory;
    private ConfigurationService configurationService;

    public static final String EXPECTED_LINE = "tskv\t" +
        "date=[26/Oct/2017:19:19:04 +0300]\t" +
        "table=market.some_table\t" +
        "period=ONE_MIN\t" +
        "metric_ids=graphite.one_min.foo.${bar}.${baz}.foo,graphite.one_min.quantile.${bar}.${baz}\t" +
        "storage_per_id=GRAPHITE,GRAPHITE\t" +
        "send_time_millis_per_id=11,13\t" +
        "metrics_sent_per_id=10,12\t" +
        "start_timestamp_milliseconds=100500000\t" +
        "end_timestamp_milliseconds=100600000\t" +
        "query_time_millis=15\t" +
        "rows_read=16\t" +
        "rows_ignored=17\t" +
        "invalid_rows_ignored_per_id=100,120\t" +
        "total_metrics_count_in_group=2\t" +
        "query_weight=LIGHT\t" +
        "pool_name=TEST_POOL\t" +
        "config_ids=config\t" +
        "status=SUCCESS\t" +
        "total_time_millis=25";

    @BeforeClass
    public static void setUpLocale() {
        Locale.setDefault(Locale.US);
    }

    @Before
    public void setUp() throws Exception {
        configurationService = configurationServiceFactory.apply(
            ResourceUtils.getResourcePath("metric_service_logging_test")
        );
    }

    @Test
    public void logQuery() throws Exception {
        MetricContextGroup group = configurationService.getConfiguration().getMetricContextGroups().get(0);
        MetricContext firstMetricContext = group.getMetricContexts().get(0);
        MetricContext secondMetricContext = group.getMetricContexts().get(1);
        TimeRange timeRange = new TimeRange(100500, 100600);
        SentMetricsStat sentMetricsStat = SentMetricsStat.builder()
            .addMetricSendStats(firstMetricContext, new MetricContext.SendStats(10, 100))
            .addMetricSendDuration(firstMetricContext, Duration.ofMillis(11))
            .addMetricSendStats(secondMetricContext, new MetricContext.SendStats(12, 120))
            .addMetricSendDuration(secondMetricContext, Duration.ofMillis(13))
            .build();

        String actualLine = MetricService.getQueryLogLine(
            new Date(1509034744000L), group, timeRange, 15, 16, 17, sentMetricsStat,
            QueryWeight.LIGHT, "TEST_POOL", Status.SUCCESS, null, 25
        );
        Assert.assertEquals(EXPECTED_LINE, actualLine);
    }

}
