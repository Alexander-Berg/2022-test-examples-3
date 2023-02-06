package ru.yandex.market.clickhouse.ddl;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.clickhouse.ddl.engine.MergeTree;
import ru.yandex.market.clickhouse.ddl.enums.EnumColumnType;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 18/08/16
 */
public class EnumColumnTypeTest {


    public static final ClickHouseTableDefinition TABLE_DEFINITION = new ClickHouseTableDefinitionImpl(
        "market.nginx2_lr",
        Collections.singletonList(new Column("environment", EnumColumnType.enum8(EnumColumnTypeTest.Environment.class), "'UNKNOWN'")),
        MergeTree.fromOldDefinition("vhost", "http_code")
    );

    @Test
    public void testDefaultValueMatch() {
        ClickHouseTableDefinition existingTableDefinition = TableUtils.parseDDL(
            "CREATE TABLE market.nginx2_lr ( " +
                "some_column UInt32,  " +
                "environment Enum8('DEVELOPMENT' = 0, 'TESTING' = 1, 'PRESTABLE' = 2, 'PRODUCTION' = 3, 'UNKNOWN' = 4) DEFAULT CAST('UNKNOWN', 'Enum8(\\'DEVELOPMENT\\' = 0, \\'TESTING\\' = 1, \\'PRESTABLE\\' = 2, \\'PRODUCTION\\' = 3, \\'UNKNOWN\\' = 4)'),  " +
                "another_column UInt32)" +
                "ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/market.nginx2_lr', '{replica}', date, (timestamp, vhost, http_code), 8192)");

        Assert.assertEquals(TABLE_DEFINITION.getColumn("environment"), existingTableDefinition.getColumn("environment"));
    }

    enum Environment {
        DEVELOPMENT,
        TESTING,
        PRESTABLE,
        PRODUCTION,
        UNKNOWN
    }

    @Test
    public void canBeModifiedToAutomatically() {
        assertFalse("bit size change", canBeModifiedToAutomatically("Enum8('VALUE' = 0)", "Enum16('VALUE' = 0)"));
        assertFalse("ordinal change", canBeModifiedToAutomatically("Enum8('VALUE' = 0)", "Enum8('VALUE' = 1)"));
        assertFalse("name change", canBeModifiedToAutomatically("Enum8('VALUE1' = 0)", "Enum8('VALUE2' = 0)"));

        assertTrue(
            "right expansion",
            canBeModifiedToAutomatically("Enum8('VALUE' = 0)", "Enum8('VALUE' = 0, 'VALUE2' = 1)")
        );

        assertTrue(
            "left expansion",
            canBeModifiedToAutomatically("Enum8('VALUE2' = 1)", "Enum8('VALUE' = 0, 'VALUE2' = 1)")
        );

        assertTrue(
            "left-right expansion",
            canBeModifiedToAutomatically("Enum8('VALUE2' = 1)", "Enum8('VALUE' = 0, 'VALUE2' = 1, 'VALUE3' = 2)")
        );
    }

    private boolean canBeModifiedToAutomatically(String from, String to) {
        ColumnTypeBase replacement = ColumnTypeUtils.fromClickhouseDDL(to);
        return ColumnTypeUtils.fromClickhouseDDL(from).canBeModifiedToAutomatically(replacement);
    }

    @Test
    public void testValidate() {
        boolean isValid = EnumColumnType.enum8(TestData.RequestType.class).validate("IN");
        assertTrue(isValid);
    }

    @Test
    public void testValidateEnumValue() {
        boolean isValid = EnumColumnType.enum8(TestData.RequestType.class).validate(TestData.RequestType.PROXY);
        assertTrue(isValid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateUnknownTypeValue() {
        boolean isValid = EnumColumnType.enum8(TestData.RequestType.class).validate(5);
        assertFalse(isValid);
    }

    @Test
    public void testValidateInvalidValue() {
        boolean isValid = EnumColumnType.enum8(TestData.RequestType.class).validate("INVALID");
        assertFalse(isValid);
    }

    @Test
    public void testParseValue() {
        Object parsedValue = EnumColumnType.enum8(TestData.RequestType.class).parseValue("IN", null);
        assertEquals("IN", parsedValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseValueInvalid() {
        EnumColumnType.enum8(TestData.RequestType.class).parseValue("INVALID", null);
    }

    enum OneValueEnum {
        VALUE
    }

    @Test
    public void testToClickhouseDDLOneValue() {
        String ddl = EnumColumnType.enum8(OneValueEnum.class).toClickhouseDDL();
        assertEquals("Enum8('VALUE' = 0)", ddl);
    }

    @Test
    public void testEqualsFalse() {
        EnumColumnType oneValue8 = EnumColumnType.enum8(OneValueEnum.class);
        EnumColumnType oneValue16 = EnumColumnType.enum16(OneValueEnum.class);
        EnumColumnType requestType8 = EnumColumnType.enum8(TestData.RequestType.class);
        EnumColumnType requestTypeReordered16 = EnumColumnType.enum8(TestData.RequestTypeReordered.class);

        assertFalse(oneValue8.equals(oneValue16));
        assertFalse(oneValue8.equals(requestType8));
        assertFalse(requestType8.equals(requestTypeReordered16));
    }
}
