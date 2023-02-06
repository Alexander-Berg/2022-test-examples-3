package ru.yandex.market.clickhouse.ddl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.clickhouse.ddl.engine.DistributedEngine;
import ru.yandex.market.clickhouse.ddl.engine.MergeTree;
import ru.yandex.market.clickhouse.ddl.engine.ReplacingMergeTree;
import ru.yandex.market.clickhouse.ddl.engine.ReplicatedMergeTree;
import ru.yandex.market.clickhouse.ddl.enums.EnumArrayColumnType;
import ru.yandex.market.clickhouse.ddl.enums.EnumColumnType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author kukabara
 */
public class TableUtilsTest {
    @Test
    public void testParseDDL() {
        String ddl = "CREATE TABLE market.TEMP_TABLE ( " +
            "   date Date,  " +
            "   timestamp UInt32,  " +
            "   host String DEFAULT 'em\\\\\\'pt\\\\ty \\\\nhost', " +
            "   vhost String, " +
            "   http_code UInt16 DEFAULT toUInt16(200),  " +
            "   resptime_ms Int32 DEFAULT toInt32(toUInt32(0)),  " +
            "   date1 Date DEFAULT toDate('2015-10-22'),  " +
            "   dateTime1 DateTime DEFAULT toDateTime('2015-10-22 12:12:12'), " +
            "   test_id Array(Int16)" +
            ") " +
            "ENGINE = MergeTree(date, (vhost, http_code), 8192)";
        ClickHouseTableDefinition tableDefinition = TableUtils.parseDDL(ddl);
        assertNotNull(tableDefinition);
        List<Column> columns = tableDefinition.getColumns();
        assertEquals(9, columns.size());

        assertEquals(ColumnType.Date, columns.get(6).getType());
        assertEquals("toDate('2015-10-22')", columns.get(6).getDefaultExpr());

        assertEquals(ColumnType.DateTime, columns.get(7).getType());
        assertEquals("toDateTime('2015-10-22 12:12:12')", columns.get(7).getDefaultExpr());

        assertEquals(ColumnType.String, columns.get(2).getType());

        assertEquals("http_code", columns.get(4).getName());
        assertEquals(ColumnType.UInt16, columns.get(4).getType());
        assertEquals("toUInt16(200)", columns.get(4).getDefaultExpr());

        assertEquals(MergeTree.class, tableDefinition.getEngine().getClass());

        MergeTree mergeTree = (MergeTree) tableDefinition.getEngine();
        assertEquals("toYYYYMM(date)", mergeTree.getPartitionBy());
        assertEquals(8192, mergeTree.getIndexGranularity());
        assertEquals(Arrays.asList("vhost", "http_code"), mergeTree.getOrderBy());
        assertNull(mergeTree.getSampleBy());

        assertEquals("market.TEMP_TABLE", tableDefinition.getFullTableName());
        assertEquals("market", tableDefinition.getDatabaseName());
        assertEquals("TEMP_TABLE", tableDefinition.getTableName());
    }

    @Test
    public void testParseDdl2() {
        String ddl = "CREATE TABLE market.servers_counting_lr " +
            "( date Date,  timestamp UInt32,  catalog_name String,  server_name String,  " +
            "environment Enum8('UNSTABLE' = 0, 'TESTING' = 1, 'PRESTABLE' = 2, 'STABLE' = 3, 'UNKNOWN' = 4) " +
            "   DEFAULT CAST('UNKNOWN', 'Enum8(\\'UNSTABLE\\' = 0, \\'TESTING\\' = 1, \\'PRESTABLE\\' = 2, \\'STABLE\\' = 3, \\'UNKNOWN\\' = 4)'),  " +
            "daily_cpu_usage Int16,  conductor_groups Array(String),  only_daytime_cpu_usage Int16,  " +
            "dc Enum8('UNKNOWN' = 0, 'SAS' = 1, 'MANTSALA' = 2, 'VLADIMIR' = 3, 'IVA' = 4, 'MYT' = 5, 'MOW' = 6) " +
            "   DEFAULT CAST('UNKNOWN', 'Enum8(\\'UNKNOWN\\' = 0, \\'SAS\\' = 1, \\'MANTSALA\\' = 2, \\'VLADIMIR\\' = 3, \\'IVA\\' = 4, \\'MYT\\' = 5, \\'MOW\\' = 6)')" +
            ") ENGINE = ReplicatedMergeTree(" +
            "'/clickhouse/tables/{shard}/market.servers_counting_lr', '{replica}', date, timestamp, 8192" +
            ")";
        ClickHouseTableDefinition actualDefinition = TableUtils.parseDDL(ddl);


        ClickHouseTableDefinition expectedDefinition = new ClickHouseTableDefinitionImpl(
            "market.servers_counting_lr",
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("timestamp", ColumnType.UInt32),
                new Column("catalog_name", ColumnType.String),
                new Column("server_name", ColumnType.String),
                new Column(
                    "environment",
                    EnumColumnType.fromClickhouseDDL(
                        "Enum8('UNSTABLE' = 0, 'TESTING' = 1, 'PRESTABLE' = 2, 'STABLE' = 3, 'UNKNOWN' = 4)"
                    ),
                    "'UNKNOWN'"
                ),
                new Column("daily_cpu_usage", ColumnType.Int16),
                new Column("conductor_groups", ColumnType.ArrayString),
                new Column("only_daytime_cpu_usage", ColumnType.Int16),
                new Column(
                    "dc",
                    EnumColumnType.fromClickhouseDDL(
                        "Enum8('UNKNOWN' = 0, 'SAS' = 1, 'MANTSALA' = 2, 'VLADIMIR' = 3, 'IVA' = 4, 'MYT' = 5, 'MOW' = 6)"
                    ),
                    "'UNKNOWN'"
                )
            ),
            new ReplicatedMergeTree(
                new MergeTree("toYYYYMM(date)", Collections.singletonList("timestamp")),
                "/clickhouse/tables/{shard}/market.servers_counting_lr",
                "{replica}"
            )
        );

        Assert.assertEquals(expectedDefinition, actualDefinition);


    }

    @Test
    public void parseColumnsEnumColumnsWithDefault() {
        String columnDdl = "server_name String,  " +
            "environment Enum8('UNSTABLE' = 0, 'TESTING' = 1, 'PRESTABLE' = 2, 'STABLE' = 3, 'UNKNOWN' = 4) " +
            "DEFAULT CAST('UNKNOWN', 'Enum8(\\'UNSTABLE\\' = 0, \\'TESTING\\' = 1, \\'PRESTABLE\\' = 2, \\'STABLE\\' = 3, \\'UNKNOWN\\' = 4)'),  " +
            "daily_cpu_usage Int16";


        List<Column> expected = Arrays.asList(
            new Column("server_name", ColumnType.String),
            new Column(
                "environment",
                EnumColumnType.fromClickhouseDDL(
                    "Enum8('UNSTABLE' = 0, 'TESTING' = 1, 'PRESTABLE' = 2, 'STABLE' = 3, 'UNKNOWN' = 4)"
                ),
                "'UNKNOWN'"
            ),
            new Column("daily_cpu_usage", ColumnType.Int16)
        );
        List<Column> actual = TableUtils.parseColumns(columnDdl);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void parseColumnsOfClickhouse_18_14_Version() {
        String columnDdl = "hid Int64,  geo_id Int64,  shops_count Int64,  date Date";
        parseMarketstatForecastsShopsCountLrColumns(columnDdl);
    }

    @Test
    public void parseColumnsOfClickhouse_19_5_2_6_Version() {
        String columnDdl = "`hid` Int64, `geo_id` Int64, `shops_count` Int64, `date` Date";
        parseMarketstatForecastsShopsCountLrColumns(columnDdl);
    }

    private void parseMarketstatForecastsShopsCountLrColumns(String columnDdl) {
        List<Column> expected = Arrays.asList(
            new Column("hid", ColumnType.Int64),
            new Column("geo_id", ColumnType.Int64),
            new Column("shops_count", ColumnType.Int64),
            new Column("date", ColumnType.Date)
        );
        List<Column> actual = TableUtils.parseColumns(columnDdl);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseDDLEnums() {
        String ddl = "CREATE TABLE market.TEMP_TABLE ( " +
            "   date Date,  " +
            "   type8 Enum8('IN' = 0, 'OUT' = 1, 'PROXY' = 2),  " +
            "   type16 Enum16('IN' = 0, 'OUT' = 1, 'PROXY' = 2)  " +
            ") " +
            "ENGINE = MergeTree(date, (type8, type16), 8192)";

        ClickHouseTableDefinition tableDefinition = TableUtils.parseDDL(ddl);
        assertNotNull(tableDefinition);

        List<Column> columns = tableDefinition.getColumns();

        assertEquals("type8", columns.get(1).getName());
        assertTrue(columns.get(1).getType().equals(EnumColumnType.enum8(TestData.RequestType.class)));

        assertEquals("type16", columns.get(2).getName());
        assertTrue(columns.get(2).getType().equals(EnumColumnType.enum16(TestData.RequestType.class)));
    }

    @Test
    public void testParseDDLNullable() {
        String ddl = "CREATE TABLE market.TEMP_TABLE ( " +
            "   date Date,  " +
            "   region Nullable(UInt16),  " +
            "   page Nullable(String)  " +
            ") " +
            "ENGINE = MergeTree(date, (date), 8192)";

        ClickHouseTableDefinition actualDefinition = TableUtils.parseDDL(ddl);
        assertNotNull(actualDefinition);

        List<Column> columns = actualDefinition.getColumns();

        assertEquals("region", columns.get(1).getName());
        assertEquals(ColumnType.NullableUInt16, columns.get(1).getType());
        assertEquals(null, columns.get(1).getDefaultExpr());

        assertEquals("page", columns.get(2).getName());
        assertEquals(ColumnType.NullableString, columns.get(2).getType());
        assertEquals(null, columns.get(2).getDefaultExpr());

        ClickHouseTableDefinition expectedDefinition = new ClickHouseTableDefinitionImpl(
            "market.TEMP_TABLE",
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("region", ColumnType.NullableUInt16),
                new Column("page", ColumnType.NullableString)
            ),
            new MergeTree(
                "toYYYYMM(date)",
                Collections.singletonList("date")
            )
        );

        Assert.assertEquals(expectedDefinition, actualDefinition);
    }

    @Test
    public void testConstructDDL() {
        String localTableName = "TEMP_TABLE";

        ClickHouseTableDefinition table = new ClickHouseTableDefinitionImpl(
            "market",
            "TEMP_TABLE",
            Arrays.asList(
                new Column("host", ColumnType.String, "'em\\'pt\ty \nho\\st'"),
                new Column("vhost", ColumnType.String),
                new Column("http_code", ColumnType.UInt16, "toUInt16(200)"),
                new Column("resptime_ms", ColumnType.Int32, "toUInt32(0)"),
                new Column("date1", ColumnType.Date, "toDate('2015-10-22')"),
                new Column("dateTime1", ColumnType.DateTime, "toDateTime('2015-10-22 12:12:12')"),
                new Column("test_id", ColumnType.ArrayInt16) // список идентификаторов экспериментов
            ),
            MergeTree.fromOldDefinition("vhost", "http_code")
        );


        String columnsList = StringUtils.join(table.getColumns(), ", ");
        String engineDDL = table.getEngine().createEngineDDL();

        // create
        String ddl = String.format(TableUtils.CREATE_TABLE_FORMAT, localTableName, columnsList, engineDDL);
        System.out.println(ddl);

        Column newColumn = new Column("dynamic", ColumnType.UInt8, "toUint8(0)");

        // alter add column
        ddl = String.format(TableUtils.ADD_COLUMN_FORMAT, localTableName, newColumn.getName(), newColumn.getType().toClickhouseDDL());
        System.out.println(ddl);

        // alter modify format
        newColumn = new Column("dynamic", ColumnType.String, "''");
        ddl = String.format(TableUtils.MODIFY_COLUMN_FORMAT, localTableName, newColumn.getName(), newColumn.getType().toClickhouseDDL());
        System.out.println(ddl);

        // alter modify default
        ddl = String.format(TableUtils.MODIFY_COLUMN_FORMAT, localTableName, newColumn.getName(),
            newColumn.getDefaultExpr() == null ? newColumn.getType().toClickhouseDDL() : "DEFAULT " + newColumn.getDefaultExpr());
        System.out.println(ddl);

        // delete
        ddl = String.format(TableUtils.DROP_COLUMN_FORMAT, localTableName, newColumn.getName());
        System.out.println(ddl);
    }

    @Test
    public void testParseDistributedEngine() {
        //Оказывается в Distributed может быть запись как с одиночными кавычками, так и без
        assertEquals(
            new DistributedEngine("market_mbi_clickhouse_testing", "mbi", "clicks_by_shop_lr", null),
            TableUtils.parseEngine("Distributed(market_mbi_clickhouse_testing, 'mbi', 'clicks_by_shop_lr')")
        );

        assertEquals(
            new DistributedEngine("market_mbi_clickhouse_testing", "mbi", "clicks_by_shop_lr", "rand()"),
            TableUtils.parseEngine("Distributed(market_mbi_clickhouse_testing, 'mbi', 'clicks_by_shop_lr', rand())")
        );

        assertEquals(
            new DistributedEngine("market_mbi_clickhouse_testing", "mbi", "clicks_by_shop_lr", null),
            TableUtils.parseEngine("Distributed(market_mbi_clickhouse_testing, mbi, clicks_by_shop_lr)")
        );

        assertEquals(
            new DistributedEngine("market_mbi_clickhouse_testing", "mbi", "clicks_by_shop_lr", "rand()"),
            TableUtils.parseEngine("Distributed(market_mbi_clickhouse_testing, mbi, clicks_by_shop_lr, rand())")
        );
    }

    @Test
    public void testParseOldMergeTree() {
        assertEquals(
            new ReplicatedMergeTree(MergeTree.fromOldDefinition("date", "timestamp"), "/clickhouse/tables/{shard}/market.logshatter_lr", "{replica}"),
            TableUtils.parseEngine("ReplicatedMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', '{replica}', date, timestamp, 8192)")
        );

        assertEquals(
            new ReplicatedMergeTree(MergeTree.fromOldDefinition("date", "timestamp"), "/clickhouse/tables/{shard}/market.logshatter_lr", "{replica}"),
            TableUtils.parseEngine("ReplicatedMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', '{replica}', date, (timestamp), 8192)")
        );

        assertEquals(
            new ReplicatedMergeTree(MergeTree.fromOldDefinition("date", "timestamp", "host"), "/clickhouse/tables/{shard}/market.logshatter_lr", "{replica}"),
            TableUtils.parseEngine("ReplicatedMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', '{replica}', date, (timestamp, host), 8192)")
        );

        assertEquals(
            MergeTree.fromOldDefinition("date", "timestamp"),
            TableUtils.parseEngine("MergeTree(date, timestamp, 8192)")
        );

        assertEquals(
            MergeTree.fromOldDefinition("date", "timestamp"),
            TableUtils.parseEngine("MergeTree(date, (timestamp), 8192)")
        );

        assertEquals(
            MergeTree.fromOldDefinition("date", "timestamp", "host"),
            TableUtils.parseEngine("MergeTree(date, (timestamp, host), 8192)")
        );
    }

    @Test
    public void testParseNewMergeTreeEngine() {
        assertEquals(
            new ReplicatedMergeTree(
                new MergeTree("toYYYYMM(date)", Arrays.asList("host", "date"), "host"),
                "/clickhouse/tables/{shard}/market.logshatter_lr", "{replica}"
            ),
            TableUtils.parseEngine(
                "ReplicatedMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', '{replica}') " +
                    "PARTITION BY toYYYYMM(date) ORDER BY (host, date) SAMPLE BY host SETTINGS index_granularity = 8192")
        );

        assertEquals(
            new ReplicatedMergeTree(
                new ReplacingMergeTree("ver", "toYYYYMM(date)", Arrays.asList("host", "date"), "host"),
                "/clickhouse/tables/{shard}/market.logshatter_lr", "{replica}"
            ),
            TableUtils.parseEngine(
                "ReplicatedReplacingMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', '{replica}', ver) " +
                    "PARTITION BY toYYYYMM(date) ORDER BY (host, date) SAMPLE BY host SETTINGS index_granularity = 8192")
        );

        assertEquals(
            new ReplicatedMergeTree(
                new ReplacingMergeTree("toYYYYMM(date)", Arrays.asList("host", "date"), "host"),
                "/clickhouse/tables/{shard}/market.logshatter_lr", "{replica}"
            ),
            TableUtils.parseEngine(
                "ReplicatedReplacingMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', '{replica}') " +
                    "PARTITION BY toYYYYMM(date) ORDER BY (host, date) SAMPLE BY host SETTINGS index_granularity = 8192")
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEngineIllegalDistEngine() {
        TableUtils.parseEngine("Distributed(market_mbi_clickhouse_testing, mbi)");
    }

    @Test
    public void testParseNewMergeTreeSettings() {
        Assert.assertEquals(
            new MergeTree("toYYYYMM(date)", Arrays.asList("host", "date"), "host", 8192),
            TableUtils.parseNewMergeTreeSettings(
                "PARTITION BY toYYYYMM(date) ORDER BY (host, date) SAMPLE BY host SETTINGS index_granularity = 8192")
        );

        Assert.assertEquals(
            new MergeTree("toYYYYMM(date)", Arrays.asList("host", "date"), null, 8192),
            TableUtils.parseNewMergeTreeSettings(
                "PARTITION BY toYYYYMM(date) ORDER BY (host, date) SETTINGS index_granularity = 8192")
        );

        Assert.assertEquals(
            new MergeTree("toYYYYMM(date)", Arrays.asList("timestamp"), null, 8192),
            TableUtils.parseNewMergeTreeSettings(
                "PARTITION BY toYYYYMM(date) ORDER BY timestamp SETTINGS index_granularity = 8192"
            )
        );
    }

    @Test
    public void testParseNewMergeTree() {

        Assert.assertEquals(
            new ClickHouseTableDefinitionImpl(
                "market.main_report_blue_nginx_lr",
                Arrays.asList(
                    new Column("date", ColumnType.Date),
                    new Column("timestamp", ColumnType.UInt32),
                    new Column("host", ColumnType.String),
                    new Column("upstream_resp_time_ms", ColumnType.Int32),
                    new Column("req_time_ms", ColumnType.Int32),
                    new Column("upstream_header_time_ms", ColumnType.Int32),
                    new Column("place", ColumnType.String),
                    new Column("status", ColumnType.UInt16),
                    new Column("request_length", ColumnType.UInt32),
                    new Column("bytes_sent", ColumnType.UInt32)
                ),
                new ReplicatedMergeTree(
                    new MergeTree("toYYYYMM(date)", Arrays.asList("timestamp")),
                    "/clickhouse/tables/{shard}/market.main_report_blue_nginx_lr", "{replica}"
                )
            ),
            TableUtils.parseDDL(
                "CREATE TABLE market.main_report_blue_nginx_lr ( " +
                    "date Date,  timestamp UInt32,  host String,  upstream_resp_time_ms Int32,  req_time_ms Int32,  " +
                    "upstream_header_time_ms Int32,  place String,  status UInt16,  request_length UInt32,  " +
                    "bytes_sent UInt32) " +
                    "ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/market.main_report_blue_nginx_lr', '{replica}') " +
                    "PARTITION BY toYYYYMM(date) ORDER BY timestamp SETTINGS index_granularity = 8192"
            )
        );
    }

    @Test
    public void testUnwrapDefaultExprSimple() {
        Assert.assertEquals(
            "-1",
            TableUtils.unwrapDefaultExpr(
                ColumnType.Int32,
                "CAST(-1, 'Int32')"
            )
        );
    }

    @Test
    public void testUnwrapDefaultExprEnum() {
        Assert.assertEquals(
            "'UNKNOWN'",
            TableUtils.unwrapDefaultExpr(
                EnumColumnType.fromClickhouseDDL(
                    "Enum8('UNSTABLE' = 0, 'TESTING' = 1, 'PRESTABLE' = 2, 'STABLE' = 3, 'UNKNOWN' = 4)"
                ),
                "CAST('UNKNOWN', 'Enum8(\\'UNSTABLE\\' = 0, \\'TESTING\\' = 1, \\'PRESTABLE\\' = 2, \\'STABLE\\' = 3, \\'UNKNOWN\\' = 4)')"
            )
        );
    }

    @Test
    public void testUnwrapDefaultExprArrayEnum() {
        Assert.assertEquals(
            "arrayMap(id -> if((id LIKE 'graphite%') = 1, 'GRAPHITE', 'STATFACE'), metric_ids)",
            TableUtils.unwrapDefaultExpr(
                EnumArrayColumnType.fromClickhouseDDL(
                    "Array(Enum8('GRAPHITE' = 0, 'SOLOMON' = 1, 'STATFACE' = 2))"
                ),
                "CAST(arrayMap(id -> if((id LIKE 'graphite%') = 1, 'GRAPHITE', 'STATFACE'), metric_ids), 'Array(Enum8(\\'GRAPHITE\\' = 0, \\'SOLOMON\\' = 1, \\'STATFACE\\' = 2))')"
            )
        );
    }

    @Test
    public void testUnwrapDefaultExprtNotTouchString() {
        Assert.assertEquals(
            "CAST('UNKNOWN', 'Enum8(\\'UNSTABLE\\' = 0, \\'TESTING\\' = 1, \\'PRESTABLE\\' = 2, \\'STABLE\\' = 3, \\'UNKNOWN\\' = 4)')",
            TableUtils.unwrapDefaultExpr(
                ColumnType.String,
                "CAST('UNKNOWN', 'Enum8(\\'UNSTABLE\\' = 0, \\'TESTING\\' = 1, \\'PRESTABLE\\' = 2, \\'STABLE\\' = 3, \\'UNKNOWN\\' = 4)')"
            )
        );
    }
}
