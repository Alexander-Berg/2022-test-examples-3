package ru.yandex.market.logshatter.parser.internal;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.common.collect.HashMultiset;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.health.configs.logshatter.LogBatch;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.health.configs.logshatter.config.LogSource;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.trace.HealthEnvironment;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

public class LogshatterPerformanceLogTest {
    private static final String SUCCESSFUL_EXAMPLE_LINE = "tskv\tdate=2017-03-15T06:27:22.538+03:00\t" +
        "status=SUCCESS\tconfig=memory_report_by_proc\tparser=LogshatterPerformanceLog\t" +
        "table=market.memory_report_by_proc\tlog_batch_count=3\tline_count=79\tparse_error_count=0" +
        "\tobsolete_error_count=0\t" +
        "output_count=79\tbytes_count=18378\tfirst_row_date=2017-03-15T06:26:49.000+03:00\t" +
        "last_row_date=2017-03-15T06:27:15.000+03:00\tread_time_ms=3\tparse_time_ms=1\toutput_time_ms=1068\t" +
        "full_output_time_ms=1333\tschema=logbroker\tsave_try_number=1\t" +
        "source_names=sourceName1,sourceName2\t" +
        "source_hosts_by_top_lag=sourceHost\t" +
        "source_hosts_by_batch_size=sourceHostWithMaxSize\t" +
        "source_hosts_top_batch_size=10\t" +
        "source_hosts_by_lines_count=sourceHostWithMaxLinesCount\t" +
        "source_hosts_log_path=log/path.log\t" +
        "project_id=no_projects\t" +
        "timestamp_seconds_to_line_count_keys=1489548409,1489548435\t" +
        "timestamp_seconds_to_line_count_values=5,6";
    private static final String FAILED_EXAMPLE_LINE = "tskv\tdate=2017-03-15T06:27:22.538+03:00\t" +
        "status=FAILURE\tconfig=memory_report_by_proc\tparser=LogshatterPerformanceLog\t" +
        "table=market.memory_report_by_proc\tlog_batch_count=3\tline_count=79\tparse_error_count=0" +
        "\tobsolete_error_count=0\t" +
        "output_count=79\tbytes_count=18378\tfirst_row_date=2017-03-15T06:26:49.000+03:00\t" +
        "last_row_date=2017-03-15T06:27:15.000+03:00\tread_time_ms=3\tparse_time_ms=1\toutput_time_ms=100500\t" +
        "full_output_time_ms=-1\tschema=logbroker\tsave_try_number=6\t" +
        "timestamp_seconds_to_line_count_keys=1489548409,1489548435\t" +
        "timestamp_seconds_to_line_count_values=5,6\t" +
        "source_names=sourceName1,sourceName2\t" +
        "source_hosts_by_top_lag=sourceHost\t" +
        "source_hosts_by_batch_size=sourceHostWithMaxSize\t" +
        "source_hosts_top_batch_size=10\t" +
        "source_hosts_by_lines_count=sourceHostWithMaxLinesCount\t" +
        "source_hosts_log_path=log/path.log\t" +
        "project_id=no_projects";
    private final LogParserChecker checker = new LogParserChecker(new LogshatterPerformanceLog());

    @Test
    public void format() throws Exception {
        LogshatterPerformanceLog.BatchesStats batchesStats = new LogshatterPerformanceLog.BatchesStats();
        batchesStats.logBatchCount = 3;
        batchesStats.lineCount = 79;
        batchesStats.parseErrorCount = 0;
        batchesStats.outputCount = 79;
        batchesStats.bytesCount = 18378;
        batchesStats.firstRowTimeMillis = 1489548409000L;
        batchesStats.lastRowTimeMillis = 1489548435000L;
        batchesStats.readTimeMillis = 3;
        batchesStats.parseTimeMillis = 1;
        HashMultiset<Long> lineCountsPerTimestampSeconds = HashMultiset.create();
        lineCountsPerTimestampSeconds.add(1489548409L, 2);
        lineCountsPerTimestampSeconds.add(1489548409L, 3);
        lineCountsPerTimestampSeconds.add(1489548435L, 3);
        lineCountsPerTimestampSeconds.add(1489548435L, 3);
        batchesStats.lineCountsPerTimestampSeconds = lineCountsPerTimestampSeconds;

        assertEquals(SUCCESSFUL_EXAMPLE_LINE, LogshatterPerformanceLog.format(
                new Date(1489548442538L),
                LogshatterPerformanceLog.OutputStatus.SUCCESS,
                LogShatterConfig.newBuilder()
                    .setConfigId("memory_report_by_proc")
                    .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
                    .setDataClickHouseTable(new ClickHouseTableDefinitionImpl("market.memory_report_by_proc",
                        Collections.emptyList(), null))
                    .setSources(singletonList(LogSource.create("logbroker://qwerty")))
                    .build(),
                batchesStats,
                1068,
                1333,
                1,
                "sourceName1,sourceName2",
                "sourceHost",
                "sourceHostWithMaxSize",
                "10",
                "sourceHostWithMaxLinesCount",
                "log/path.log"
            )
        );
    }

    @Test
    public void parseSuccessfulExampleLine() throws Exception {
        checker.check(
            SUCCESSFUL_EXAMPLE_LINE,
            new Date(1489548442538L),
            checker.getHost(), "memory_report_by_proc", "LogshatterPerformanceLog",
            "market.memory_report_by_proc", 3, 79, 0, 0, 79, 18378L,
            new Date(1489548409000L), new Date(1489548435000L), new Date(1489548442538L),
            3, 1, 1068, 1333, new Long[]{1489548409L, 1489548435L}, new Integer[]{5, 6}, "logbroker",
            LogshatterPerformanceLog.OutputStatus.SUCCESS, 1, new String[]{"sourceName1", "sourceName2"},
            HealthEnvironment.UNKNOWN,
            new String[]{"sourceHost"},
            new String[]{"sourceHostWithMaxSize"},
            new Integer[]{10},
            new String[]{"sourceHostWithMaxLinesCount"},
            new String[]{"log/path.log"},
            "no_projects"
        );
    }

    @Test
    public void parseFailedExampleLine() throws Exception {
        checker.check(
            FAILED_EXAMPLE_LINE,
            new Date(1489548442538L),
            checker.getHost(), "memory_report_by_proc", "LogshatterPerformanceLog",
            "market.memory_report_by_proc", 3, 79, 0, 0, 79, 18378L,
            new Date(1489548409000L), new Date(1489548435000L), new Date(1489548442538L),
            3, 1, 100500, -1, new Long[]{1489548409L, 1489548435L}, new Integer[]{5, 6}, "logbroker",
            LogshatterPerformanceLog.OutputStatus.FAILURE, 6, new String[]{"sourceName1", "sourceName2"},
            HealthEnvironment.UNKNOWN,
            new String[]{"sourceHost"},
            new String[]{"sourceHostWithMaxSize"},
            new Integer[]{10},
            new String[]{"sourceHostWithMaxLinesCount"},
            new String[]{"log/path.log"},
            "no_projects"
        );
    }

    @Test
    public void truncateDate() {
        ZonedDateTime currentDateTime = ZonedDateTime.parse("2017-09-29T23:00:00+03:00");
        // меньше 2-х минут — без округления
        testTruncateDate(
            ZonedDateTime.parse("2017-09-29T22:59:31+03:00"),
            ZonedDateTime.parse("2017-09-29T22:59:31+03:00"), currentDateTime
        );

        // старше 2 минут — пятисекундное округление
        testTruncateDate(
            ZonedDateTime.parse("2017-09-29T22:57:05+03:00"),
            ZonedDateTime.parse("2017-09-29T22:57:08+03:00"), currentDateTime
        );

        // старше 5 минут — 15-секундное
        testTruncateDate(
            ZonedDateTime.parse("2017-09-29T22:54:15+03:00"),
            ZonedDateTime.parse("2017-09-29T22:54:23+03:00"), currentDateTime
        );

        // старше 15 минут — минутное
        testTruncateDate(
            ZonedDateTime.parse("2017-09-29T22:43:00+03:00"),
            ZonedDateTime.parse("2017-09-29T22:43:56+03:00"), currentDateTime
        );

        // старше часа — 10-минутное
        testTruncateDate(
            ZonedDateTime.parse("2017-09-29T21:50:00+03:00"),
            ZonedDateTime.parse("2017-09-29T21:59:56+03:00"), currentDateTime
        );

        // старше 6 часов — часовое
        testTruncateDate(
            ZonedDateTime.parse("2017-09-29T16:00:00+03:00"),
            ZonedDateTime.parse("2017-09-29T16:59:56+03:00"), currentDateTime
        );

        // больше суток — суточное
        testTruncateDate(
            ZonedDateTime.parse("2017-09-28T03:00:00+03:00"),
            ZonedDateTime.parse("2017-09-28T22:59:56+03:00"), currentDateTime
        );
    }

    private void testTruncateDate(ZonedDateTime expected, ZonedDateTime input, ZonedDateTime current) {
        Date truncatedDate = LogshatterPerformanceLog.BatchesStats.truncateDate(
            current.toInstant().toEpochMilli(), new Date(input.toInstant().toEpochMilli())
        );

        ZonedDateTime actual = ZonedDateTime.ofInstant(
            truncatedDate.toInstant(), ZoneId.ofOffset("", ZoneOffset.ofHours(3))
        );
        assertEquals(expected, actual);
    }

    @Test
    public void batchStatsCountingTest() {
        LogBatch batch = new LogBatch(
            Stream.empty(), 0, 0, 0, Duration.ofMillis(0),
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("timestamp", ColumnType.UInt32),
                new Column("value", ColumnType.Int32)
            ),
            "sourceName",
            "sourceHost",
            null
        );

        Instant now = Instant.now();
        Date firstDate = Date.from(now.minus(2 * 30, ChronoUnit.DAYS));
        Date lastDate = Date.from(now.minus(2, ChronoUnit.MINUTES));
        List<Date> dates = Arrays.asList(
            firstDate,
            Date.from(now.minus(2 * 7, ChronoUnit.DAYS)),
            Date.from(now.minus(2, ChronoUnit.DAYS)),
            Date.from(now.minus(2, ChronoUnit.HOURS)),
            lastDate
        );

        for (Date date : dates) {
            for (int i = 0; i < 5; i++) {
                batch.write(date, i);
            }
        }
        batch.onParseComplete(Duration.ofMillis(0), dates.size(), 0, 0);

        LogshatterPerformanceLog.BatchesStats batchesStats = new LogshatterPerformanceLog.BatchesStats(
            Collections.singletonList(batch)
        );

        assertEquals(dates.size(), batchesStats.lineCountsPerTimestampSeconds.elementSet().size());
        assertEquals(firstDate.getTime(), batchesStats.firstRowTimeMillis);
        assertEquals(lastDate.getTime(), batchesStats.lastRowTimeMillis);

        int i = 0;
        for (Long truncatedTimestampSeconds : batchesStats.lineCountsPerTimestampSeconds.elementSet()) {
            assertTrue(
                String.format("%s should be greater or equals to %s", dates.get(i), truncatedTimestampSeconds),
                TimeUnit.MILLISECONDS.toSeconds(dates.get(i).getTime()) >= truncatedTimestampSeconds
            );
            assertEquals(5, batchesStats.lineCountsPerTimestampSeconds.count(truncatedTimestampSeconds));
            ++i;
        }
    }

    @Test
    public void batchStatsCountingTestEmptyOutput() {
        LogBatch batch = new LogBatch(
            Stream.empty(), 0, 0, 0, Duration.ofMillis(0),
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("timestamp", ColumnType.UInt32),
                new Column("value", ColumnType.Int32)
            ),
            "sourceName",
            "sourceHost",
            null
        );

        batch.onParseComplete(Duration.ofMillis(0), 0, 0, 0);

        LogshatterPerformanceLog.BatchesStats batchesStats = new LogshatterPerformanceLog.BatchesStats(
            Collections.singletonList(batch)
        );

        assertEquals(0, batchesStats.lineCountsPerTimestampSeconds.elementSet().size());
        assertEquals(0, batchesStats.firstRowTimeMillis);
        assertEquals(0, batchesStats.lastRowTimeMillis);
    }

    @Test
    public void testGetHostSourcesByTopLag() {
        LogBatch logBatch1 = mockLogBatch(Arrays.asList(minutesToDate(0L), minutesToDate(10L)), "host1");
        LogBatch logBatch2 = mockLogBatch(Arrays.asList(minutesToDate(5L), minutesToDate(20L)), "host2");
        LogBatch logBatch3 = mockLogBatch(Arrays.asList(minutesToDate(15L), minutesToDate(30L)), "host3");
        LogBatch logBatch4 = mockLogBatch(Arrays.asList(minutesToDate(25L), minutesToDate(40L)), "host4");
        LogBatch logBatch5 = mockLogBatch(Arrays.asList(minutesToDate(35L), minutesToDate(50L)), "host5");
        LogBatch logBatch6 = mockLogBatch(Arrays.asList(minutesToDate(45L), minutesToDate(60L)), "host6");

        List<LogBatch> logBatches = Arrays.asList(logBatch6, logBatch5, logBatch4, logBatch3, logBatch2, logBatch1);

        String hostSourcesByTopLag = LogshatterPerformanceLog.getHostSourcesByTopLag(logBatches);
        assertEquals("host1,host2,host3,host4,host5", hostSourcesByTopLag);
    }

    @Test
    public void testGetHostInfoBySize() {
        LogBatch logBatch1 = mockLogBatchBySize(1000L, "host1", "log/path_1.log");
        LogBatch logBatch2 = mockLogBatchBySize(7500L, "host2", "log/path_2.log");
        LogBatch logBatch3 = mockLogBatchBySize(9100L, "host3", "log/path_3.log");
        LogBatch logBatch4 = mockLogBatchBySize(30L, "host4", "log/path_4.log");
        LogBatch logBatch5 = mockLogBatchBySize(150L, "host5", "log/path_5.log");
        LogBatch logBatch6 = mockLogBatchBySize(500L, "host6", null);
        LogBatch logBatch7 = mockLogBatchBySize(5000L, "host7", "log/path_7.log");
        LogBatch logBatch8 = mockLogBatchBySize(300L, null, "log/path_8.log");
        LogBatch logBatch9 = mockLogBatchBySize(500L, "host3", "log/path_3.log");

        List<LogBatch> logBatches = Arrays.asList(logBatch1, logBatch2, logBatch3, logBatch4, logBatch5, logBatch6,
            logBatch7, logBatch8, logBatch9);

        Map<LogshatterPerformanceLog.TopHosts, String> topHostsInfoBySize =
            LogshatterPerformanceLog.getTopHostsInfoBySize(logBatches);
        assertEquals("host3,host2,host7,host1,host6", topHostsInfoBySize.get(LogshatterPerformanceLog.TopHosts.HOSTS));
        assertEquals("9600,7500,5000,1000,500", topHostsInfoBySize.get(LogshatterPerformanceLog.TopHosts.SIZE));
        assertEquals("log/path_1.log,log/path_2.log,log/path_3.log,log/path_7.log",
            topHostsInfoBySize.get(LogshatterPerformanceLog.TopHosts.LOGS));
    }

    @Test
    public void testGetHostSourcesByLinesCount() {
        LogBatch logBatch1 = mockLogBatchByLinesCount(1000, "host1");
        LogBatch logBatch2 = mockLogBatchByLinesCount(7500, "host2");
        LogBatch logBatch3 = mockLogBatchByLinesCount(9100, "host3");
        LogBatch logBatch4 = mockLogBatchByLinesCount(30, "host4");
        LogBatch logBatch5 = mockLogBatchByLinesCount(150, "host5");
        LogBatch logBatch6 = mockLogBatchByLinesCount(500, "host6");
        LogBatch logBatch7 = mockLogBatchByLinesCount(5000, "host7");
        LogBatch logBatch8 = mockLogBatchByLinesCount(300, null);

        List<LogBatch> logBatches = Arrays.asList(logBatch1, logBatch2, logBatch3, logBatch4, logBatch5, logBatch6,
            logBatch7, logBatch8);

        String hostSourcesByTopLag = LogshatterPerformanceLog.getHostSourcesByLinesCount(logBatches);
        assertEquals("host3,host2,host7,host1,host6", hostSourcesByTopLag);
    }

    private Date minutesToDate(long minutes) {
        return new Date(TimeUnit.MINUTES.toMillis(minutes));
    }

    private LogBatch mockLogBatch(List<Date> parseDates, String host) {
        LogBatch logBatch = Mockito.mock(LogBatch.class);
        Mockito.when(logBatch.getParsedDates()).thenReturn(parseDates);
        Mockito.when(logBatch.getSourceHost()).thenReturn(host);
        return logBatch;
    }

    private LogBatch mockLogBatchBySize(Long batchSize, String host, String logPath) {
        LogBatch logBatch = Mockito.mock(LogBatch.class);
        Mockito.when(logBatch.getBatchSizeBytes()).thenReturn(batchSize);
        Mockito.when(logBatch.getSourceHost()).thenReturn(host);
        Mockito.when(logBatch.getSourcePath()).thenReturn(logPath != null ? Paths.get(logPath) : null);
        return logBatch;
    }

    private LogBatch mockLogBatchByLinesCount(int linesCount, String host) {
        LogBatch logBatch = Mockito.mock(LogBatch.class);
        Mockito.when(logBatch.getLinesCount()).thenReturn(linesCount);
        Mockito.when(logBatch.getSourceHost()).thenReturn(host);
        return logBatch;
    }

}
