package ru.yandex.market.logshatter.parser.internal;

import com.google.common.collect.HashMultiset;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.logshatter.LogBatch;
import ru.yandex.market.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.config.LogSource;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.trace.Environment;

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
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class LogshatterPerformanceLogTest {
    private final LogParserChecker checker = new LogParserChecker(new LogshatterPerformanceLog());

    private static final String SUCCESSFUL_EXAMPLE_LINE = "tskv\tdate=2017-03-15T06:27:22.538+03:00\t" +
        "status=SUCCESS\tconfig=memory_report_by_proc\tparser=LogshatterPerformanceLog\t" +
        "table=market.memory_report_by_proc\tlog_batch_count=3\tline_count=79\tparse_error_count=0\t" +
        "output_count=79\tbytes_count=18378\tfirst_row_date=2017-03-15T06:26:49.000+03:00\t" +
        "last_row_date=2017-03-15T06:27:15.000+03:00\tread_time_ms=3\tparse_time_ms=1\toutput_time_ms=1068\t" +
        "full_output_time_ms=1333\tschema=logbroker\tsave_try_number=1\t" +
        "source_names=sourceName1,sourceName2\t" +
        "timestamp_seconds_to_line_count_keys=1489548409,1489548435\t" +
        "timestamp_seconds_to_line_count_values=5,6";

    private static final String FAILED_EXAMPLE_LINE = "tskv\tdate=2017-03-15T06:27:22.538+03:00\t" +
        "status=FAILURE\tconfig=memory_report_by_proc\tparser=LogshatterPerformanceLog\t" +
        "table=market.memory_report_by_proc\tlog_batch_count=3\tline_count=79\tparse_error_count=0\t" +
        "output_count=79\tbytes_count=18378\tfirst_row_date=2017-03-15T06:26:49.000+03:00\t" +
        "last_row_date=2017-03-15T06:27:15.000+03:00\tread_time_ms=3\tparse_time_ms=1\toutput_time_ms=100500\t" +
        "full_output_time_ms=-1\tschema=logbroker\tsave_try_number=6\t" +
        "timestamp_seconds_to_line_count_keys=1489548409,1489548435\t" +
        "timestamp_seconds_to_line_count_values=5,6\t" +
        "source_names=sourceName1,sourceName2";

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
                .setConfigFileName("memory_report_by_proc")
                .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
                .setDataClickHouseTable(new ClickHouseTableDefinitionImpl("market.memory_report_by_proc", Collections.emptyList(), null))
                .setSources(singletonList(LogSource.create("logbroker://qwerty")))
                .build(),
            batchesStats,
            1068,
            1333,
            1,
            "sourceName1,sourceName2")
        );
    }

    @Test
    public void parseSuccessfulExampleLine() throws Exception {
        checker.check(
            SUCCESSFUL_EXAMPLE_LINE,
            new Date(1489548442538L),
            checker.getHost(), "memory_report_by_proc", "LogshatterPerformanceLog",
            "market.memory_report_by_proc", 3, 79, 0, 79, 18378L,
            new Date(1489548409000L), new Date(1489548435000L), new Date(1489548442538L),
            3, 1, 1068, 1333, new Long[]{1489548409L, 1489548435L}, new Integer[]{5, 6}, "logbroker",
            LogshatterPerformanceLog.OutputStatus.SUCCESS, 1, new String[]{"sourceName1", "sourceName2"},
            Environment.UNKNOWN
        );
    }

    @Test
    public void parseFailedExampleLine() throws Exception {
        checker.check(
            FAILED_EXAMPLE_LINE,
            new Date(1489548442538L),
            checker.getHost(), "memory_report_by_proc", "LogshatterPerformanceLog",
            "market.memory_report_by_proc", 3, 79, 0, 79, 18378L,
            new Date(1489548409000L), new Date(1489548435000L), new Date(1489548442538L),
            3, 1, 100500, -1, new Long[]{1489548409L, 1489548435L}, new Integer[]{5, 6}, "logbroker",
            LogshatterPerformanceLog.OutputStatus.FAILURE, 6, new String[]{"sourceName1", "sourceName2"},
            Environment.UNKNOWN
        );
    }

    @Test
    public void truncateDate() throws Exception {
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
        Assert.assertEquals(expected, actual);
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
            "sourceName"
        );

        Instant now = Instant.now();
        List<Date> dates = Arrays.asList(
            Date.from(now.minus(2 * 30, ChronoUnit.DAYS)),
            Date.from(now.minus(2 * 7, ChronoUnit.DAYS)),
            Date.from(now.minus(2, ChronoUnit.DAYS)),
            Date.from(now.minus(2, ChronoUnit.HOURS)),
            Date.from(now.minus(2, ChronoUnit.MINUTES))
        );

        for (Date date : dates) {
            for (int i = 0; i < 5; i++) {
                batch.write(date, i);
            }
        }
        batch.onParseComplete(Duration.ofMillis(0), dates.size(), 0);

        LogshatterPerformanceLog.BatchesStats batchesStats = new LogshatterPerformanceLog.BatchesStats(
            Collections.singletonList(batch)
        );

        Assert.assertEquals(dates.size(), batchesStats.lineCountsPerTimestampSeconds.elementSet().size());

        int i = 0;
        for (Long truncatedTimestampSeconds : batchesStats.lineCountsPerTimestampSeconds.elementSet()) {
            Assert.assertTrue(
                String.format("%s should be greater or equals to %s", dates.get(i), truncatedTimestampSeconds),
                TimeUnit.MILLISECONDS.toSeconds(dates.get(i).getTime()) >= truncatedTimestampSeconds
            );
            Assert.assertEquals(5, batchesStats.lineCountsPerTimestampSeconds.count(truncatedTimestampSeconds));
            ++i;
        }
    }
}
