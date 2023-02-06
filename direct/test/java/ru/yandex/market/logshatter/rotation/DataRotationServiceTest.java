package ru.yandex.market.logshatter.rotation;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinition;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.TableDescription;
import ru.yandex.market.logshatter.parser.internal.LogshatterPerformanceLog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DataRotationServiceTest {
    @Test
    public void calcMaxPartitionTest() throws Exception {
        final int dataRotationDays = 14;

        final Calendar calendar = Calendar.getInstance();
        calendar.set(2017, Calendar.MARCH, 13);
        Assert.assertEquals(201702, DataRotationService.calcMaxPartition(calendar.getTime(), dataRotationDays));

        calendar.set(2017, Calendar.MARCH, 15);
        Assert.assertEquals(201703, DataRotationService.calcMaxPartition(calendar.getTime(), dataRotationDays));

        Assert.assertEquals(0, DataRotationService.calcMaxPartition(new Date(), 0));
    }

    @Test
    public void toArchiveSettings() throws Exception {
        final List<LogShatterConfig> configs = new ArrayList<>();
        LogParserProvider provider = new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null);

        ClickHouseTableDefinition first = new ClickHouseTableDefinitionImpl("db.first", Collections.emptyList(), null);
        ClickHouseTableDefinition second = new ClickHouseTableDefinitionImpl("db.second", Collections.emptyList(), null);
        ClickHouseTableDefinition third = new ClickHouseTableDefinitionImpl("db.second", Collections.emptyList(), null);

        configs.add(LogShatterConfig.newBuilder().setDataClickHouseTable(first).setDataRotationDays(15).setConfigFileName("/var/log/fst.log").setParserProvider(provider).build());
        configs.add(LogShatterConfig.newBuilder().setDataClickHouseTable(second).setDataRotationDays(10).setConfigFileName("/var/log/snd.log").setParserProvider(provider).build());
        configs.add(LogShatterConfig.newBuilder().setDataClickHouseTable(third).setConfigFileName("/var/log/third.log").setParserProvider(provider).build());

        List<DataRotationService.ArchiveSettings> archiveSettings = DataRotationService.toArchiveSettings(configs);
        Assert.assertEquals(2, archiveSettings.stream().filter(o -> (o.tableName.equals("db.first") && o.dataRotationDays == 15) || (o.tableName.equals("db.second") && o.dataRotationDays == 10)).count());
        Assert.assertTrue(archiveSettings.stream().noneMatch(o -> o.tableName.equals("third")));

        archiveSettings = DataRotationService.toArchiveSettings(configs);
        Assert.assertEquals(2, archiveSettings.stream().filter(o -> (o.tableName.equals("db.first") && o.dataRotationDays == 15) || (o.tableName.equals("db.second") && o.dataRotationDays == 10)).count());

        //Check remove duplicates
        configs.add(LogShatterConfig.newBuilder().setDataClickHouseTable(first).setConfigFileName("/var/log/fst-duplicate.log").setParserProvider(provider).build());
        archiveSettings = DataRotationService.toArchiveSettings(configs);
        Assert.assertEquals(2, archiveSettings.size());

        configs.add(LogShatterConfig.newBuilder().setDataClickHouseTable(first).setDataRotationDays(10).setConfigFileName("/var/log/fst-failed.log").setParserProvider(provider).build());
        try {
            DataRotationService.toArchiveSettings(configs);
            Assert.fail();
        } catch (RuntimeException ignore) {
        }
    }
}