package ru.yandex.market.logshatter.config.ddl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.clickhouse.ClickHouseSource;
import ru.yandex.market.clickhouse.ddl.ClickHouseDdlServiceOld;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.DDL;

public class UpdateDDLServiceTest {
    private ClickHouseSource clickHouseSource;
    private UpdateDDLService updateDDLService;

    @Before
    public void init() {
        clickHouseSource = Mockito.mock(ClickHouseSource.class);
        Mockito.when(
            clickHouseSource.getShard2Hosts()
        ).thenReturn(null);

        updateDDLService = new UpdateDDLService(
            clickHouseSource,
            null,
            Mockito.mock(ClickHouseDdlServiceOld.class),
            1, 600, 10, false, false,
            null, true
        );
    }

    @Test
    public void sequentialExecuteManualDDLsTest() {
        List<DDL> manualDDLs = new ArrayList<>();
        manualDDLs.add(new DDL("foo.com",
            new ClickHouseTableDefinitionImpl("db.table", Collections.emptyList(), null)));
        manualDDLs.add(new DDL("bar.com",
            new ClickHouseTableDefinitionImpl("db.table", Collections.emptyList(), null)));

        ManualDDLExecutionResult manualDDLExecutionResult = updateDDLService.executeManualDDLs(manualDDLs);

        //Так как выполнение последовательное, то можно просто сравнить коллекции
        Assert.assertEquals(manualDDLs, manualDDLExecutionResult.getSucceededDDLs());
    }

    @Test
    public void parallelExecuteManualDDLsTest() {
        Multimap<Integer, String> shard2Host = ArrayListMultimap.create();
        shard2Host.put(1, "foo1.com");
        shard2Host.put(1, "foo2.com");
        shard2Host.put(2, "bar1.com");
        shard2Host.put(2, "bar2.com");
        shard2Host.put(3, "baz1.com");
        shard2Host.put(3, "baz2.com");

        Mockito.when(
            clickHouseSource.getShard2Hosts()
        ).thenReturn(shard2Host);

        List<DDL> manualDDLs = new ArrayList<>();
        ClickHouseTableDefinitionImpl clickHouseTableDefinition =
            new ClickHouseTableDefinitionImpl("db.table", Collections.emptyList(), null);
        shard2Host.values().forEach(
            host -> manualDDLs.add(new DDL(host, clickHouseTableDefinition))
        );

        ManualDDLExecutionResult manualDDLExecutionResult = updateDDLService.executeManualDDLs(manualDDLs);

        List<DDL> succeededDDLs = manualDDLExecutionResult.getSucceededDDLs();
        Assert.assertEquals(manualDDLs.size(), succeededDDLs.size());
        manualDDLs.forEach(
            manualDDL -> Assert.assertTrue(succeededDDLs.contains(manualDDL))
        );
    }
}
