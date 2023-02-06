package ru.yandex.market.clickphite.metric;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.market.clickphite.TimeRange;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.config.TestConfiguration;

import java.io.File;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 26.10.17
 */
public class MetricServiceLoggingTest {
    private ConfigurationService configurationService;

    public static final String EXPECTED_LINE = "tskv\t" +
        "date=[26/Oct/2017:19:19:04 +0300]\t" +
        "table=market.some_table\t" +
        "period=ONE_MIN\t" +
        "metric_ids=graphite.one_min.foo.${bar}.${baz}.foo,graphite.one_min.quantile.${bar}.${baz}\t" +
        "storage_per_id=GRAPHITE,GRAPHITE\t" +
        "send_time_millis_per_id=11,13\t" +
        "metrics_sent_per_id=10,12\t" +
        "start_timestamp_seconds=100500\t" +
        "end_timestamp_seconds=100600\t" +
        "query_time_millis=15\t" +
        "rows_read=16\t" +
        "rows_ignored=17\t" +
        "invalid_rows_ignored_per_id=100,120\t" +
        "total_metrics_count_in_group=2\t" +
        "query_weight=LIGHT";

    @BeforeClass
    public static void setUpLocale() {
        Locale.setDefault(Locale.US);
    }

    @Before
    public void setUp() throws Exception {
        File file = new File("src/test/resources/metric_service_logging_test");
        configurationService = TestConfiguration.createConfigurationService(file.getAbsolutePath());
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
            QueryWeight.LIGHT
        );
        Assert.assertEquals(EXPECTED_LINE, actualLine);
    }

}