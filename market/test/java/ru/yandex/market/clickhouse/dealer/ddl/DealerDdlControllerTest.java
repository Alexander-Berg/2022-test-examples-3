package ru.yandex.market.clickhouse.dealer.ddl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.DDL;
import ru.yandex.market.clickhouse.ddl.DdlQuery;
import ru.yandex.market.clickhouse.ddl.DdlQueryType;
import ru.yandex.market.clickhouse.dealer.config.DealerConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 09.10.2019
 */
public class DealerDdlControllerTest {
    private static final DealerConfig.Key KEY1 = new DealerConfig.Key("yt1", "ch1", "table1");

    private final DealerDdlController sut = new DealerDdlController(8080);

    @Test
    public void empty() {
        assertTrue(sut.getManualDdlJson().queries.isEmpty());
        sut.approveManualDdl();
    }

    @Test
    public void twoHostsWithTheSameQueries() {
        assertFalse(sut.canApply(KEY1, ddl("host1", query("query1"), query("query2"))));
        assertFalse(sut.canApply(KEY1, ddl("host2", query("query1"), query("query2"))));

        DealerDdlController.ManualDdlJson manualDdlJson = sut.getManualDdlJson();
        assertEquals(1, manualDdlJson.queries.size());
        assertEquals(new HashSet<>(Arrays.asList("host1", "host2")), manualDdlJson.queries.get(0).hosts);
        assertEquals(new HashSet<>(Arrays.asList("query1", "query2")), manualDdlJson.queries.get(0).queries);
    }

    @Test
    public void twoHostsWithDifferentQueries() {
        assertFalse(sut.canApply(KEY1, ddl("host1", query("query1"), query("query2"))));
        assertFalse(sut.canApply(KEY1, ddl("host2", query("query1"))));

        DealerDdlController.ManualDdlJson manualDdlJson = sut.getManualDdlJson();
        assertEquals(2, manualDdlJson.queries.size());
        if ("host1".equals(manualDdlJson.queries.get(0).hosts.iterator().next())) {
            assertEquals(new HashSet<>(Arrays.asList("query1", "query2")), manualDdlJson.queries.get(0).queries);
            assertEquals(new HashSet<>(Arrays.asList("query1")), manualDdlJson.queries.get(1).queries);
        } else {
            assertEquals(new HashSet<>(Arrays.asList("query1")), manualDdlJson.queries.get(0).queries);
            assertEquals(new HashSet<>(Arrays.asList("query1", "query2")), manualDdlJson.queries.get(1).queries);
        }
    }

    @Test
    public void approve() {
        // Первый вызов canApply добавляет DDL в список известных
        assertFalse(sut.canApply(KEY1, ddl("host1", query("query1"))));
        assertFalse(sut.canApply(KEY1, ddl("host2", query("query1"))));

        // Второй вызов canApply возвращает false потому что DDL известен, но не поаппрувлен
        assertFalse(sut.canApply(KEY1, ddl("host1", query("query1"))));
        assertFalse(sut.canApply(KEY1, ddl("host2", query("query1"))));

        // Аппрувим
        sut.approveManualDdl();

        // После аппрува DDL должен быть поаппрувлен
        assertTrue(sut.canApply(KEY1, ddl("host1", query("query1"))));
        assertTrue(sut.canApply(KEY1, ddl("host1", query("query1"))));
        assertTrue(sut.canApply(KEY1, ddl("host2", query("query1"))));
        assertTrue(sut.canApply(KEY1, ddl("host2", query("query1"))));
    }

    @Test
    public void updateApproved() {
        // Первый вызов canApply добавляет DDL в список известных
        assertFalse(sut.canApply(KEY1, ddl("host1", query("query1"))));

        // Аппрувим
        sut.approveManualDdl();

        // Второй вызов canApply меняет запрос, DDL НЕ должен быть поаппрувлен
        assertFalse(sut.canApply(KEY1, ddl("host1", query("query2"))));

        // Первая версия тоже НЕ должна быть поаппрувлена
        assertFalse(sut.canApply(KEY1, ddl("host1", query("query1"))));
        assertFalse(sut.canApply(KEY1, ddl("host1", query("query1"))));
    }

    private static DDL ddl(String host, DdlQuery... queries) {
        DDL ddl = new DDL(host, new ClickHouseTableDefinitionImpl("test", "test", Collections.emptyList(), null));
        Stream.of(queries).forEach(ddl::addManualQuery);
        return ddl;
    }

    private static DdlQuery query(String query) {
        return new DdlQuery(DdlQueryType.DROP_COLUMN, query);
    }
}
