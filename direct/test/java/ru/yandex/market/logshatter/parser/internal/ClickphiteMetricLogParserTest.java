package ru.yandex.market.logshatter.parser.internal;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 06.10.17
 */
public class ClickphiteMetricLogParserTest {
    private static final String LINE = "[06/Oct/2017:14:39:57 +0300]\t\t" +
        "market.clickphite_metric_lag\t" +
        "graphite.one_min.DEV.clickphite.lag.fullLagSeconds.table.${table},graphite.one_min.DEV.clickphite.lag.fullQueuePeriods.table.${table},graphite.one_min.DEV.clickphite.lag.fullQueueSeconds.table.${table},graphite.one_min.DEV.clickphite.lag.realTimeLagSeconds.table.${table},graphite.one_min.DEV.clickphite.lag.realTimeQueuePeriods.table.${table},graphite.one_min.DEV.clickphite.lag.realTimeQueueSeconds.table.${table}\t" +
        "GRAPHITE,GRAPHITE,GRAPHITE,GRAPHITE,GRAPHITE,GRAPHITE\t" +
        "ONE_MIN\t" +
        "1507283760\t" +
        "1507283940\t" +
        "162\t" +
        "193,194,195,196,197,198\t" +
        "45792,45793,45794,45795,45796,45797\t" +
        "0\t" +
        "636\t" +
        "0";

    private static final String TSKV_LINE = "tskv\t" +
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
        "invalid_rows_ignored_per_id=18,19\t" +
        "total_metrics_count_in_group=2\t" +
        "query_weight=LIGHT";

    private final LogParserChecker checker = new LogParserChecker(new ClickphiteMetricLogParser());

    @Test
    public void parseTabSeparated() throws Exception {
        checker.check(
            LINE,
            new Date(1507289997000L),
            checker.getHost(),
            "",
            "market.clickphite_metric_lag",
            "",
            "",
            "ONE_MIN",
            new Date(1507283760000L),
            new Date(1507283940000L),
            162,
            0,
            0,
            0,
            636,
            0,
            new Integer[]{},
            new String[]{
                "graphite.one_min.DEV.clickphite.lag.fullLagSeconds.table.${table}",
                "graphite.one_min.DEV.clickphite.lag.fullQueuePeriods.table.${table}",
                "graphite.one_min.DEV.clickphite.lag.fullQueueSeconds.table.${table}",
                "graphite.one_min.DEV.clickphite.lag.realTimeLagSeconds.table.${table}",
                "graphite.one_min.DEV.clickphite.lag.realTimeQueuePeriods.table.${table}",
                "graphite.one_min.DEV.clickphite.lag.realTimeQueueSeconds.table.${table}"
            },
            new String[]{},
            new Integer[]{193, 194, 195, 196, 197, 198},
            new String[]{"GRAPHITE", "GRAPHITE", "GRAPHITE", "GRAPHITE", "GRAPHITE", "GRAPHITE"},
            new Integer[]{45792, 45793, 45794, 45795, 45796, 45797},
            6,
            ClickphiteMetricLogParser.QueryWeight.LIGHT
        );
    }

    @Test
    public void parseTskv() throws Exception {
        checker.check(
            TSKV_LINE,
            new Date(1509034744000L),
            checker.getHost(),
            "",
            "market.some_table",
            "",
            "",
            "ONE_MIN",
            new Date(100500000L),
            new Date(100600000L),
            15,
            0,
            0,
            0,
            16,
            17,
            new Integer[]{18, 19},
            new String[]{
                "graphite.one_min.foo.${bar}.${baz}.foo",
                "graphite.one_min.quantile.${bar}.${baz}"
            },
            new String[]{},
            new Integer[]{11, 13},
            new String[]{"GRAPHITE", "GRAPHITE"},
            new Integer[]{10, 12},
            2,
            "LIGHT"
        );
    }

}