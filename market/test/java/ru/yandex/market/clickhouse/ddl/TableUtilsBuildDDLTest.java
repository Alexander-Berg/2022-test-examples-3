package ru.yandex.market.clickhouse.ddl;

import java.util.Optional;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 20.08.16
 */
public class TableUtilsBuildDDLTest {
    private static final String TABLE_NAME = "market.TEMP_TABLE";
    private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE %s (date Date, %s) ENGINE = MergeTree(date, " +
        "(%s), 8192)";

    @Test
    public void buildDDLDropColumn() throws Exception {
        String existingTable = getTableDDL("number UInt16,  string String", null);
        String table = getTableDDL("number UInt16", null);

        DDL ddl = buildDDL(existingTable, table);

        // в общем случае, тип колонки можно поменять только вручную
        assertEquals(1, ddl.getManualUpdates().size());
        assertEquals(DdlQueryType.DROP_COLUMN, ddl.getManualUpdates().get(0).getType());
        assertEquals(0, ddl.getUpdates().size());
    }

    @Test
    public void buildDDLAddColumn() throws Exception {
        String existingTable = getTableDDL("number UInt16", null);
        String table = getTableDDL("number UInt16,  magicNumber UInt16 DEFAULT 42", null);

        DDL ddl = buildDDL(existingTable, table);

        // в общем случае, тип колонки можно поменять только вручную
        assertEquals(1, ddl.getUpdates().size());
        assertEquals(0, ddl.getManualUpdates().size());

        DdlQuery query = ddl.getUpdates().get(0);
        assertEquals(DdlQueryType.ADD_COLUMN, query.getType());
        assertTrue(
            "ADD COLUMN DDL query should contain default value expression, actual: " + query.getQueryString(),
            query.getQueryString().contains("DEFAULT 42")
        );
    }

    @Test
    public void buildDDLColumnTypeChange() throws Exception {
        String existingTable = getTableDDL("number UInt16", null);
        String table = getTableDDL("number UInt8", null);

        DDL ddl = buildDDL(existingTable, table);

        // в общем случае, тип колонки можно поменять только вручную
        assertEquals(0, ddl.getUpdates().size());
        assertEquals(1, ddl.getManualUpdates().size());
        assertEquals(DdlQueryType.MODIFY_COLUMN, ddl.getManualUpdates().get(0).getType());
    }

    @Test
    public void buildDDLColumnAddCodec() throws Exception {
        String existingTable = getTableDDL("number UInt16", null);
        String table = getTableDDL("number UInt16 CODEC(ZSTD(9))", null);

        DDL ddl = buildDDL(existingTable, table);

        assertEquals(0, ddl.getUpdates().size());
        assertEquals(1, ddl.getManualUpdates().size());
        assertEquals(DdlQueryType.MODIFY_COLUMN, ddl.getManualUpdates().get(0).getType());
        assertEquals(ddl.getManualUpdates().get(0).getQueryString(), "ALTER TABLE market.TEMP_TABLE MODIFY COLUMN " +
            "number CODEC(ZSTD(9))");
    }

    @Test
    public void buildDDLColumnCodecChange() throws Exception {
        String existingTable = getTableDDL("number UInt16 CODEC(ZSTD(7))", null);
        String table = getTableDDL("number UInt16 CODEC(ZSTD(9))", null);

        DDL ddl = buildDDL(existingTable, table);

        assertEquals(0, ddl.getUpdates().size());
        assertEquals(1, ddl.getManualUpdates().size());
        assertEquals(DdlQueryType.MODIFY_COLUMN, ddl.getManualUpdates().get(0).getType());
        assertEquals(ddl.getManualUpdates().get(0).getQueryString(), "ALTER TABLE market.TEMP_TABLE MODIFY COLUMN " +
            "number CODEC(ZSTD(9))");
    }

    @Test
    public void buildDDLColumnResetDefaultCodec() throws Exception {
        String existingTable = getTableDDL("number UInt16 CODEC(LZ4)", null);
        String table = getTableDDL("number UInt16", null);

        DDL ddl = buildDDL(existingTable, table);

        assertEquals(0, ddl.getUpdates().size());
        assertEquals(0, ddl.getManualUpdates().size());
    }

    @Test
    public void buildDDLColumnResetCodec() throws Exception {
        String existingTable = getTableDDL("number UInt16 CODEC(ZSTD(9))", null);
        String table = getTableDDL("number UInt16", null);

        DDL ddl = buildDDL(existingTable, table);

        assertEquals(0, ddl.getUpdates().size());
        assertEquals(1, ddl.getManualUpdates().size());
        assertEquals(DdlQueryType.MODIFY_COLUMN, ddl.getManualUpdates().get(0).getType());
        assertEquals(ddl.getManualUpdates().get(0).getQueryString(), "ALTER TABLE market.TEMP_TABLE MODIFY COLUMN " +
            "number CODEC(LZ4)");
    }

    @Test
    public void buildDDLEnumValueAddition() throws Exception {
        String existingTable = getTableDDL("type Enum8('IN' = 0, 'OUT' = 1)", null);
        String table = getTableDDL("type Enum8('IN' = 0, 'OUT' = 1, 'PROXY' = 2)", null);

        DDL ddl = buildDDL(existingTable, table);

        // так как в enum добавилось значение, этот апдейт можно провести автоматически
        assertEquals(1, ddl.getUpdates().size());
        assertEquals(DdlQueryType.MODIFY_COLUMN, ddl.getUpdates().get(0).getType());

        assertEquals(0, ddl.getManualUpdates().size());
    }

    @Test
    public void buildDDLEnumValueAdditionUsedInEngine() throws Exception {
        String existingTable = getTableDDL("type Enum8('IN' = 0, 'OUT' = 1)", "type");
        String table = getTableDDL("type Enum8('IN' = 0, 'OUT' = 1, 'PROXY' = 2)", "type");

        DDL ddl = buildDDL(existingTable, table);

        //https://st.yandex-team.ru/CLICKHOUSE-2795
        assertEquals(1, ddl.getUpdates().size());
        assertEquals(0, ddl.getManualUpdates().size());
        assertEquals(0, ddl.getErrors().size());
    }

    @Test
    public void buildDDLWithWrapper() {
        buildDDLWithWrapperDifferentParameters(false, 1, 0);
        buildDDLWithWrapperDifferentParameters(true, 0, 1);
    }

    private void buildDDLWithWrapperDifferentParameters(
        boolean autoManualDDLEnabled,
        int manualUpdateCount,
        int updateCount
    ) {
        String existingTable = getTableDDL("number UInt16", null);
        String expectedTable = getTableDDL("number UInt8", null);

        ClickHouseTableDefinition table = TableUtils.parseDDL(expectedTable);
        LogshatterClickHouseTableDefinitionWrapper wrapper = new LogshatterClickHouseTableDefinitionWrapper(
            table,
            autoManualDDLEnabled
        );

        DDL result = buildDDL(wrapper, existingTable);
        assertEquals(manualUpdateCount, result.getManualUpdates().size());
        assertEquals(updateCount, result.getUpdates().size());
    }

    private DDL buildDDL(ClickHouseTableDefinition table, String existingTable) {
        return TableUtils.buildDDL(
            table,
            Optional.ofNullable(existingTable),
            "some.host"
        );
    }

    private DDL buildDDL(final String existingTable, String table) {
        return TableUtils.buildDDL(
            TableUtils.parseDDL(table),
            Optional.ofNullable(existingTable),
            "some.host"
        );
    }

    private String getTableDDL(String columnsDDL, Object engineColumnsDDL) {
        return String.format(CREATE_TABLE_TEMPLATE, TABLE_NAME, columnsDDL, engineColumnsDDL);
    }
}
