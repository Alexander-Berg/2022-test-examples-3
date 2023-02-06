package ru.yandex.market.clickphite;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 12/04/2017
 */
public class ClickHouseTableTest {
    @Test
    public void create() throws Exception {
        ClickHouseTable table1 = ClickHouseTable.create("db1.table1", "otherdb");
        Assert.assertEquals(table1.getFullName(), "db1.table1");

        ClickHouseTable table2 = ClickHouseTable.create("table2", "db2");
        Assert.assertEquals(table2.getFullName(), "db2.table2");

        ClickHouseTable table3 = ClickHouseTable.create("db3.table3", null);
        Assert.assertEquals(table3.getFullName(), "db3.table3");
    }

}