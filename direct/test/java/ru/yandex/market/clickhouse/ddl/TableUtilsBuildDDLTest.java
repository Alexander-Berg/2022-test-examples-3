package ru.yandex.market.clickhouse.ddl;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 20.08.16
 */
public class TableUtilsBuildDDLTest {
    private static final String TABLE_NAME = "market.TEMP_TABLE";
    private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE %s (date Date, %s) ENGINE = MergeTree(date, (%s), 8192)";

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
