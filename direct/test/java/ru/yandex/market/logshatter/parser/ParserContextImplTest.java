package ru.yandex.market.logshatter.parser;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.logshatter.LogBatch;
import ru.yandex.market.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.parser.internal.LogshatterPerformanceLog;
import ru.yandex.market.logshatter.useragent.FakeUserAgentDetector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public class ParserContextImplTest {

    @Test
    public void isObsoleteDataTest() throws Exception {
        final LogShatterConfig configWithRotation = LogShatterConfig.newBuilder()
            .setDataClickHouseTable(new ClickHouseTableDefinitionImpl("db.withRotation", Collections.emptyList(), null))
            .setDataRotationDays(10)
            .setConfigFileName("/var/log/fst.log")
            .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
            .build();

        checkObsoleteData(configWithRotation, true);

        final LogShatterConfig configWithoutRotation = LogShatterConfig.newBuilder()
            .setDataClickHouseTable(new ClickHouseTableDefinitionImpl("db.withoutRotation", Collections.emptyList(), null))
            .setConfigFileName("/var/log/snd.log")
            .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
            .build();

        checkObsoleteData(configWithoutRotation, false);
    }

    private void checkObsoleteData(LogShatterConfig config, boolean obsoleteExpected) {
        final Calendar dataDate = Calendar.getInstance();
        dataDate.set(2017, Calendar.MARCH, 1);
        final long dateMillis = dataDate.getTimeInMillis();

        final Calendar currentDate = Calendar.getInstance();
        currentDate.setTimeInMillis(dateMillis);
        Assert.assertFalse(ParserContextImpl.isObsoleteData(dateMillis, currentDate.getTimeInMillis(), config));

        currentDate.set(Calendar.DAY_OF_MONTH, 5);
        Assert.assertFalse(ParserContextImpl.isObsoleteData(dateMillis, currentDate.getTimeInMillis(), config));

        currentDate.set(Calendar.DAY_OF_MONTH, 11);
        Assert.assertFalse(ParserContextImpl.isObsoleteData(dateMillis, currentDate.getTimeInMillis(), config));

        currentDate.set(Calendar.DAY_OF_MONTH, 12);
        Assert.assertEquals(obsoleteExpected, ParserContextImpl.isObsoleteData(dateMillis, currentDate.getTimeInMillis(), config));
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
            "sourceName"
        );
        ParserContextImpl parserContext = new ParserContextImpl(
            logBatch, null, null, false, 0.0f, new FakeUserAgentDetector());

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
            "sourceName"
        );
        ParserContextImpl parserContext = new ParserContextImpl(
            logBatch, null, null, false, 1.0f, new FakeUserAgentDetector());

        parserContext.write(new Date());

        Assert.assertTrue("There are one record in fields", logBatch.getParsedDates().size() == 1);
    }
}
