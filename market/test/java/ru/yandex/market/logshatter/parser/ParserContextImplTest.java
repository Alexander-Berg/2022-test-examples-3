package ru.yandex.market.logshatter.parser;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.health.configs.logshatter.LogBatch;
import ru.yandex.market.health.configs.logshatter.config.ConfigValidationException;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.health.configs.logshatter.useragent.FakeUserAgentDetector;
import ru.yandex.market.logshatter.parser.internal.LogshatterPerformanceLog;

public class ParserContextImplTest {

    @Test
    public void isObsoleteDataTest() throws Exception {
        final LogShatterConfig configWithRotation = LogShatterConfig.newBuilder()
            .setDataClickHouseTable(
                new ClickHouseTableDefinitionImpl("db.withRotation", Collections.emptyList(), null))
            .setDataRotationDays(10)
            .setConfigId("/var/log/fst.log")
            .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
            .build();

        checkObsoleteDataForConfig(configWithRotation, true);

        final LogShatterConfig configWithoutRotation = LogShatterConfig.newBuilder()
            .setDataClickHouseTable(
                new ClickHouseTableDefinitionImpl("db.withoutRotation", Collections.emptyList(), null))
            .setConfigId("/var/log/snd.log")
            .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
            .build();

        checkObsoleteDataForConfig(configWithoutRotation, false);
    }

    private void checkObsoleteDataForConfig(LogShatterConfig config, boolean obsoleteExpected) {
        final Calendar dataDate = Calendar.getInstance();
        dataDate.set(2017, Calendar.MARCH, 1);
        final long dateMillis = dataDate.getTimeInMillis();

        final Calendar currentDate = Calendar.getInstance();
        currentDate.setTimeInMillis(dateMillis);
        Assert.assertFalse(
            ParserContextImpl.isObsoleteDataForConfig(dateMillis, currentDate.getTimeInMillis(), config)
        );

        currentDate.set(Calendar.DAY_OF_MONTH, 5);
        Assert.assertFalse(
            ParserContextImpl.isObsoleteDataForConfig(dateMillis, currentDate.getTimeInMillis(), config)
        );

        currentDate.set(Calendar.DAY_OF_MONTH, 11);
        Assert.assertFalse(
            ParserContextImpl.isObsoleteDataForConfig(dateMillis, currentDate.getTimeInMillis(), config)
        );

        currentDate.set(Calendar.DAY_OF_MONTH, 12);
        Assert.assertEquals(
            obsoleteExpected,
            ParserContextImpl.isObsoleteDataForConfig(dateMillis, currentDate.getTimeInMillis(), config)
        );
    }

    @Test
    public void checkObsoleteData() {
        final Calendar dataDate = Calendar.getInstance();
        dataDate.set(2022, Calendar.MAY, 31);
        final long dateMillis = dataDate.getTimeInMillis();

        final Calendar currentDate = Calendar.getInstance();
        currentDate.setTimeInMillis(dateMillis + TimeUnit.HOURS.toMillis(1));

        Assert.assertTrue(ParserContextImpl.isObsoleteData(
            dateMillis, currentDate.getTimeInMillis(), TimeUnit.MINUTES.toSeconds(30))
        );

        Assert.assertFalse(ParserContextImpl.isObsoleteData(
            dateMillis, currentDate.getTimeInMillis(), TimeUnit.MINUTES.toSeconds(90))
        );
    }

    @Test
    public void testReadBeforeDateTime() throws ConfigValidationException, ParseException {
        final LogShatterConfig config = LogShatterConfig.newBuilder()
            .setDataClickHouseTable(
                new ClickHouseTableDefinitionImpl("db.test", Collections.emptyList(), null))
            .setDataRotationDays(10)
            .setConfigId("/var/log/fst.log")
            .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
            .setReadBeforeDateTime(LogShatterConfig.parseDateTime("2021-10-15 12:00:00"))
            .build();

        Assert.assertTrue(isDataAllowedToWrite(
            getTimeMs("2021-10-15 11:00:00"),
            config
        ));
        Assert.assertFalse(isDataAllowedToWrite(
            getTimeMs("2021-10-15 12:00:00"),
            config
        ));
        Assert.assertFalse(isDataAllowedToWrite(
            getTimeMs("2021-10-15 13:00:00"),
            config
        ));
    }

    @Test
    public void testReadAfterOrEqualDateTime() throws ConfigValidationException, ParseException {
        final LogShatterConfig config = LogShatterConfig.newBuilder()
            .setDataClickHouseTable(
                new ClickHouseTableDefinitionImpl("db.test", Collections.emptyList(), null))
            .setDataRotationDays(10)
            .setConfigId("/var/log/fst.log")
            .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
            .setReadAfterOrEqualDateTime(LogShatterConfig.parseDateTime("2021-10-15 12:00:00"))
            .build();

        Assert.assertFalse(isDataAllowedToWrite(
            getTimeMs("2021-10-15 11:00:00"),
            config
        ));
        Assert.assertTrue(isDataAllowedToWrite(
            getTimeMs("2021-10-15 12:00:00"),
            config
        ));
        Assert.assertTrue(isDataAllowedToWrite(
            getTimeMs("2021-10-15 13:00:00"),
            config
        ));
    }

    @Test
    public void testReadExcludeInterval() throws ConfigValidationException, ParseException {
        final LogShatterConfig config = LogShatterConfig.newBuilder()
            .setDataClickHouseTable(
                new ClickHouseTableDefinitionImpl("db.test", Collections.emptyList(), null))
            .setDataRotationDays(10)
            .setConfigId("/var/log/fst.log")
            .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
            .setReadBeforeDateTime(LogShatterConfig.parseDateTime("2021-10-15 12:00:00"))
            .setReadAfterOrEqualDateTime(LogShatterConfig.parseDateTime("2021-10-15 13:00:00"))
            .build();

        Assert.assertTrue(isDataAllowedToWrite(
            getTimeMs("2021-10-15 11:00:00"),
            config
        ));
        Assert.assertFalse(isDataAllowedToWrite(
            getTimeMs("2021-10-15 12:00:00"),
            config
        ));
        Assert.assertFalse(isDataAllowedToWrite(
            getTimeMs("2021-10-15 12:30:00"),
            config
        ));
        Assert.assertTrue(isDataAllowedToWrite(
            getTimeMs("2021-10-15 13:00:00"),
            config
        ));
        Assert.assertTrue(isDataAllowedToWrite(
            getTimeMs("2021-10-15 14:00:00"),
            config
        ));
    }

    @Test
    public void testReadInterval() throws ConfigValidationException, ParseException {
        final LogShatterConfig config = LogShatterConfig.newBuilder()
            .setDataClickHouseTable(
                new ClickHouseTableDefinitionImpl("db.test", Collections.emptyList(), null))
            .setDataRotationDays(10)
            .setConfigId("/var/log/fst.log")
            .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
            .setReadAfterOrEqualDateTime(LogShatterConfig.parseDateTime("2021-10-15 12:00:00"))
            .setReadBeforeDateTime(LogShatterConfig.parseDateTime("2021-10-15 13:00:00"))
            .build();

        Assert.assertFalse(isDataAllowedToWrite(
            getTimeMs("2021-10-15 11:00:00"),
            config
        ));
        Assert.assertTrue(isDataAllowedToWrite(
            getTimeMs("2021-10-15 12:00:00"),
            config
        ));
        Assert.assertTrue(isDataAllowedToWrite(
            getTimeMs("2021-10-15 12:30:00"),
            config
        ));
        Assert.assertFalse(isDataAllowedToWrite(
            getTimeMs("2021-10-15 13:00:00"),
            config
        ));
        Assert.assertFalse(isDataAllowedToWrite(
            getTimeMs("2021-10-15 14:00:00"),
            config
        ));
    }

    @Test
    public void testZeroSampleRatio() {
        List<Column> columns = new ArrayList<>(7);
        columns.add(new Column("date", ColumnType.Date));
        columns.add(new Column("timestamp", ColumnType.DateTime));

        LogBatch logBatch = new LogBatch(
            Stream.empty(),
            0,
            0,
            0,
            Duration.ofMillis(0),
            columns,
            "sourceName",
            "sourceHost",
            null
        );
        ParserContextImpl parserContext = new ParserContextImpl(
            logBatch, null, null, false, 0.0f, new FakeUserAgentDetector(), 0
        );

        parserContext.write(new Date());

        Assert.assertTrue("fields not stored on zero sample", logBatch.getParsedDates().isEmpty());
    }

    @Test
    public void testFullSampleRatio() {
        List<Column> columns = new ArrayList<>(7);
        columns.add(new Column("date", ColumnType.Date));
        columns.add(new Column("timestamp", ColumnType.DateTime));

        LogBatch logBatch = new LogBatch(
            Stream.empty(),
            0,
            0,
            0,
            Duration.ofMillis(0),
            columns,
            "sourceName",
            "sourceHost",
            null
        );
        ParserContextImpl parserContext = new ParserContextImpl(
            logBatch, null, null, false, 1.0f, new FakeUserAgentDetector(), 0
        );

        parserContext.write(new Date());

        Assert.assertTrue("There are one record in fields", logBatch.getParsedDates().size() == 1);
    }

    private boolean isDataAllowedToWrite(long dateMillis, LogShatterConfig logShatterConfig) {
        Instant readBeforeDateTime = logShatterConfig.getReadBeforeDateTime();
        Instant readAfterOrEqualDateTime = logShatterConfig.getReadAfterOrEqualDateTime();
        Long readBeforeDateTimeMs = readBeforeDateTime != null ? readBeforeDateTime.toEpochMilli() : null;
        Long readAfterOrEqualDateTimeMs = readAfterOrEqualDateTime != null
            ? readAfterOrEqualDateTime.toEpochMilli()
            : null;

        return ParserContextImpl.isDataAllowedToWrite(dateMillis, readBeforeDateTimeMs, readAfterOrEqualDateTimeMs);
    }

    private long getTimeMs(String dateTimeStr) throws ParseException {
        return LogShatterConfig.DATE_TIME_FORMAT.parse(dateTimeStr).getTime();
    }
}
