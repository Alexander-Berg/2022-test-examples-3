package ru.yandex.market.clickhouse.ddl.enums;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinition;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.TableUtils;
import ru.yandex.market.clickhouse.ddl.TestData;
import ru.yandex.market.clickhouse.ddl.engine.MergeTree;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.10.17
 */
public class EnumArrayColumnTypeTest {
    @Test
    public void testDefaultValueMatch() {
        ClickHouseTableDefinition tableDefinition = new ClickHouseTableDefinitionImpl(
            "market.nginx2_lr",
            Collections.singletonList(
                new Column("environment", EnumArrayColumnType.enum8Array(Environment.class), "'UNKNOWN'")
            ),
            MergeTree.fromOldDefinition("vhost", "http_code")
        );

        ClickHouseTableDefinition existingTableDefinition = TableUtils.parseDDL(
            "CREATE TABLE market.nginx2_lr ( " +
                "some_column UInt32,  " +
                "environment Array(Enum8('DEVELOPMENT' = 0, 'TESTING' = 1, 'PRESTABLE' = 2, 'PRODUCTION' = 3, " +
                "'UNKNOWN' = 4)) DEFAULT CAST('UNKNOWN' AS Array(Enum8('DEVELOPMENT' = 0, 'TESTING' = 1, 'PRESTABLE' " +
                "= 2, 'PRODUCTION' = 3, 'UNKNOWN' = 4))),  " +
                "another_column UInt32)" +
                "ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/market.nginx2_lr', '{replica}', date, " +
                "(timestamp, vhost, http_code), 8192)");

        Assert.assertEquals(tableDefinition.getColumn("environment"), existingTableDefinition.getColumn("environment"));
    }

    @Test
    public void testTrickyDefaultValueMatch() {
        ClickHouseTableDefinition tableDefinition = new ClickHouseTableDefinitionImpl(
            "market.nginx2_lr",
            Collections.singletonList(
                new Column(
                    "storage_per_id", EnumArrayColumnType.enum8Array(Storage.class),
                    "arrayMap(id -> if((id LIKE 'graphite%') = 1, 'GRAPHITE', 'STATFACE'), metric_ids)"
                )
            ),
            MergeTree.fromOldDefinition("vhost", "http_code")
        );

        ClickHouseTableDefinition existingTableDefinition = TableUtils.parseDDL(
            "CREATE TABLE market.nginx2_lr ( " +
                "storage_per_id Array(Enum8('GRAPHITE' = 0, 'STATFACE' = 1)) DEFAULT CAST(arrayMap(id -> if((id LIKE " +
                "'graphite%') = 1, 'GRAPHITE', 'STATFACE'), metric_ids), 'Array(Enum8(\\'GRAPHITE\\' = 0, " +
                "\\'STATFACE\\' = 1))'))" +
                "ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/market.nginx2_lr', '{replica}', date, " +
                "(timestamp, vhost, http_code), 8192)");

        Assert.assertEquals(tableDefinition.getColumn("storage_per_id"), existingTableDefinition.getColumn(
            "storage_per_id"));
    }

    @Test
    public void testValidate() {
        boolean isValid = EnumArrayColumnType.enum8Array(TestData.RequestType.class).validate(new String[]{"IN"});
        assertTrue(isValid);
    }

    @Test
    public void testValidateEnumValue() {
        boolean isValid = EnumArrayColumnType.enum8Array(TestData.RequestType.class)
            .validate(new TestData.RequestType[]{TestData.RequestType.PROXY});

        assertTrue(isValid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateUnknownTypeValue() {
        boolean isValid = EnumArrayColumnType.enum8Array(TestData.RequestType.class).validate(new Integer[]{5});
        assertFalse(isValid);
    }

    @Test
    public void testValidateInvalidValue() {
        assertFalse(EnumArrayColumnType.enum8Array(TestData.RequestType.class).validate("INVALID"));
        assertFalse(EnumArrayColumnType.enum8Array(TestData.RequestType.class).validate(new String[]{"INVALID"}));
    }

    @Test
    public void testParseValue() {
        Object[] parsedValue = (Object[]) EnumArrayColumnType.enum8Array(TestData.RequestType.class)
            .parseValue("IN,OUT", null);
        assertArrayEquals(new Object[]{"IN", "OUT"}, parsedValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseValueInvalid() {
        EnumArrayColumnType.enum8Array(TestData.RequestType.class).parseValue("[INVALID]", null);
    }

    @Test
    public void testToClickhouseDDLOneValue() {
        String ddl = EnumArrayColumnType.enum8Array(OneValueEnum.class).toClickhouseDDL();
        assertEquals("Array(Enum8('VALUE' = 0))", ddl);
    }

    @Test
    public void testEqualsTrue() {
        EnumArrayColumnType oneValue8 = EnumArrayColumnType.enum8Array(OneValueEnum.class);
        EnumArrayColumnType anotherOneValue8 = EnumArrayColumnType.enum8Array(OneValueEnum.class);

        assertEquals(oneValue8, anotherOneValue8);
        assertEquals(anotherOneValue8, oneValue8);
    }

    @Test
    public void testEqualsFalse() {
        EnumArrayColumnType oneValue8 = EnumArrayColumnType.enum8Array(OneValueEnum.class);
        EnumArrayColumnType oneValue16 = EnumArrayColumnType.enum16Array(OneValueEnum.class);
        EnumArrayColumnType requestType8 = EnumArrayColumnType.enum8Array(TestData.RequestType.class);
        EnumArrayColumnType requestTypeReordered16 =
            EnumArrayColumnType.enum8Array(TestData.RequestTypeReordered.class);

        assertNotEquals(oneValue8, oneValue16);
        assertNotEquals(oneValue8, requestType8);
        assertNotEquals(requestType8, requestTypeReordered16);
    }

    enum Environment {
        DEVELOPMENT,
        TESTING,
        PRESTABLE,
        PRODUCTION,
        UNKNOWN
    }

    enum Storage {
        GRAPHITE,
        STATFACE
    }

    enum OneValueEnum {
        VALUE
    }

}
