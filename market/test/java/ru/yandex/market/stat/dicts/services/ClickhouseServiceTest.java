package ru.yandex.market.stat.dicts.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.stat.dicts.services.ClickhouseService.Column;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;

/**
 * @author nettoyeur
 */
@RunWith(MockitoJUnitRunner.class)
public class ClickhouseServiceTest {

    private ClickhouseService clickhouseService;
    @Mock
    private JdbcTemplate clickhouseJdbcTemplate;

    @Before
    public void setUp() {
        clickhouseService = new ClickhouseService(clickhouseJdbcTemplate);
    }

    @Test
    public void validDdlForReplicatedMergeTree() {
        ClickhouseService.ClickhouseTable simpleTable = table("dontknowmymind");
        ClickhouseService.ClickhouseTableWithColumns table = withColumns(simpleTable);
        clickhouseService.replicatedMergeTree(table, "(some_field, and_other)", "date_OfCourse");
        verify(clickhouseJdbcTemplate).execute("CREATE TABLE IF NOT EXISTS marketstat.dontknowmymind ON CLUSTER mstat (\n" +
                "column3 UInt64,\n" +
                "column1 String\n" +
                ") ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/marketstat.dontknowmymind', '{replica}', date_OfCourse, (some_field, and_other), 8192)");
    }

    @Test
    public void validDdlForDistributed() {
        ClickhouseService.ClickhouseTable simpleTable = table("dontknowmymind");
        ClickhouseService.ClickhouseTable lrTable = table("dontknowmymind_lr");
        ClickhouseService.ClickhouseTableWithColumns table = withColumns(simpleTable);
        clickhouseService.distributed(table, lrTable);
        verify(clickhouseJdbcTemplate).execute("CREATE TABLE IF NOT EXISTS marketstat.dontknowmymind ON CLUSTER mstat (\n" +
                "column3 UInt64,\n" +
                "column1 String\n" +
                ") ENGINE = Distributed(mstat, 'marketstat', 'dontknowmymind_lr')");
    }

    private ClickhouseService.ClickhouseTableWithColumns withColumns(ClickhouseService.ClickhouseTable simpleTable) {
        return ClickhouseService.ClickhouseTableWithColumns.builder()
                    .table(simpleTable)
                    .columns(asList(new Column("column3", "UInt64"), new Column("column1", "String")))
                    .build();
    }

    private ClickhouseService.ClickhouseTable table(String name) {
        return ClickhouseService.ClickhouseTable.builder().cluster("mstat").database("marketstat").name(name).build();
    }
}
