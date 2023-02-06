package ru.yandex.market.clickhouse.ddl;

import org.junit.Test;

import ru.yandex.market.clickhouse.ddl.enums.EnumColumnType;

import static org.junit.Assert.assertEquals;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 18/08/16
 */
public class EnumColumnTest {
    @Test
    public void testToString() {
        Column column = new Column("type", EnumColumnType.enum8(TestData.RequestType.class));
        assertEquals("type Enum8('IN' = 0, 'OUT' = 1, 'PROXY' = 2)", column.toString());
    }

    @Test
    public void testToStringWithDefault() {
        Column column = new Column("type", EnumColumnType.enum8(TestData.RequestType.class), "'IN'");
        assertEquals("type Enum8('IN' = 0, 'OUT' = 1, 'PROXY' = 2) DEFAULT 'IN'", column.toString());
    }
}
