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
import ru.yandex.market.clickhouse.ddl.engine.ReplicatedReplacingMergeTree;
import ru.yandex.market.clickhouse.ddl.enums.EnumArrayColumnType;
import ru.yandex.market.clickhouse.ddl.enums.EnumColumnType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
            "   dateTime1 DateTime DEFAULT toDateTime('2015-10-22 12:12:12', ''), " +
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
        assertEquals("toDateTime('2015-10-22 12:12:12', '')", columns.get(7).getDefaultExpr());

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
            "   DEFAULT CAST('UNKNOWN', 'Enum8(\\'UNSTABLE\\' = 0, \\'TESTING\\' = 1, \\'PRESTABLE\\' = 2, " +
            "\\'STABLE\\' = 3, \\'UNKNOWN\\' = 4)'),  " +
            "daily_cpu_usage Int16,  conductor_groups Array(String),  only_daytime_cpu_usage Int16,  " +
            "dc Enum8('UNKNOWN' = 0, 'SAS' = 1, 'MANTSALA' = 2, 'VLADIMIR' = 3, 'IVA' = 4, 'MYT' = 5, 'MOW' = 6) " +
            "   DEFAULT CAST('UNKNOWN', 'Enum8(\\'UNKNOWN\\' = 0, \\'SAS\\' = 1, \\'MANTSALA\\' = 2, \\'VLADIMIR\\' =" +
            " 3, \\'IVA\\' = 4, \\'MYT\\' = 5, \\'MOW\\' = 6)')" +
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
                        "Enum8('UNKNOWN' = 0, 'SAS' = 1, 'MANTSALA' = 2, 'VLADIMIR' = 3, 'IVA' = 4, 'MYT' = 5, 'MOW' " +
                            "= 6)"
                    ),
                    "'UNKNOWN'"
                )
            ),
            new ReplicatedMergeTree(
                "toYYYYMM(date)",
                Collections.singletonList("timestamp"),
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
            "DEFAULT CAST('UNKNOWN', 'Enum8(\\'UNSTABLE\\' = 0, \\'TESTING\\' = 1, \\'PRESTABLE\\' = 2, \\'STABLE\\' " +
            "= 3, \\'UNKNOWN\\' = 4)'),  " +
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
    @SuppressWarnings("MethodName")
    public void parseColumnsOfClickhouse_18_14_Version() {
        String columnDdl = "hid Int64,  geo_id Int64,  shops_count Int64,  date Date";
        parseMarketstatForecastsShopsCountLrColumns(columnDdl);
    }

    @Test
    @SuppressWarnings("MethodName")
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
        assertEquals(columns.get(1).getType(), EnumColumnType.enum8(TestData.RequestType.class));

        assertEquals("type16", columns.get(2).getName());
        assertEquals(columns.get(2).getType(), EnumColumnType.enum16(TestData.RequestType.class));
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
        assertNull(columns.get(1).getDefaultExpr());

        assertEquals("page", columns.get(2).getName());
        assertEquals(ColumnType.NullableString, columns.get(2).getType());
        assertNull(columns.get(2).getDefaultExpr());

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
    public void testParseDDLLowCardinality() {
        String ddl = "CREATE TABLE market.TEMP_TABLE ( " +
            "   date Date,  " +
            "   page LowCardinality(String),  " +
            "   flags Array(LowCardinality(String))  " +
            ") " +
            "ENGINE = MergeTree(date, (date), 8192)";

        ClickHouseTableDefinition actualDefinition = TableUtils.parseDDL(ddl);
        assertNotNull(actualDefinition);

        List<Column> columns = actualDefinition.getColumns();

        assertEquals("page", columns.get(1).getName());
        assertEquals(ColumnType.LowCardinalityString, columns.get(1).getType());
        assertNull(columns.get(1).getDefaultExpr());

        assertEquals("flags", columns.get(2).getName());
        assertEquals(ColumnType.ArrayLowCardinalityString, columns.get(2).getType());
        assertNull(columns.get(2).getDefaultExpr());

        ClickHouseTableDefinition expectedDefinition = new ClickHouseTableDefinitionImpl(
            "market.TEMP_TABLE",
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("page", ColumnType.LowCardinalityString),
                new Column("flags", ColumnType.ArrayLowCardinalityString)
            ),
            new MergeTree(
                "toYYYYMM(date)",
                Collections.singletonList("date")
            )
        );

        Assert.assertEquals(expectedDefinition, actualDefinition);
        Assert.assertEquals(expectedDefinition.getColumn("page").toString(), "page LowCardinality(String)");
        Assert.assertEquals(expectedDefinition.getColumn("flags").toString(), "flags Array(LowCardinality(String))");
    }

    @Test
    public void testParseDDLCodec() {
        String ddl = "CREATE TABLE market.TEMP_TABLE ( " +
            "   date Date,  " +
            "   page String CODEC(ZSTD(7)),  " +
            "   name Array(String) CODEC(LZ4),  " +
            "   project String DEFAULT toString(200) CODEC(Delta, ZSTD)  " +
            ") " +
            "ENGINE = MergeTree(date, (date), 8192)";

        ClickHouseTableDefinition actualDefinition = TableUtils.parseDDL(ddl);
        assertNotNull(actualDefinition);

        List<Column> columns = actualDefinition.getColumns();

        assertEquals("page", columns.get(1).getName());
        assertEquals(ColumnType.String, columns.get(1).getType());
        assertEquals("ZSTD(7)", columns.get(1).getCodec());

        assertEquals("name", columns.get(2).getName());
        assertEquals(ColumnType.ArrayString, columns.get(2).getType());
        assertEquals("LZ4", columns.get(2).getCodec());

        assertEquals("project", columns.get(3).getName());
        assertEquals(ColumnType.String, columns.get(3).getType());
        assertEquals("toString(200)", columns.get(3).getDefaultExpr());
        assertEquals("Delta, ZSTD", columns.get(3).getCodec());

        ClickHouseTableDefinition expectedDefinition = new ClickHouseTableDefinitionImpl(
            "market.TEMP_TABLE",
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("page", ColumnType.String, null, null, "ZSTD(7)"),
                new Column("name", ColumnType.ArrayString, null, null, "LZ4"),
                new Column("project", ColumnType.String, "toString(200)", null, "Delta, ZSTD")
            ),
            new MergeTree(
                "toYYYYMM(date)",
                Collections.singletonList("date")
            )
        );

        Assert.assertEquals(expectedDefinition, actualDefinition);
        Assert.assertEquals(expectedDefinition.getColumn("page").toString(), "page String CODEC(ZSTD(7))");
        Assert.assertEquals(expectedDefinition.getColumn("name").toString(), "name Array(String) CODEC(LZ4)");
        Assert.assertEquals(expectedDefinition.getColumn("project").toString(), "project String DEFAULT toString(200)" +
            " CODEC(Delta, ZSTD)");
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
                new Column("dateTime1", ColumnType.DateTime, "toDateTime('2015-10-22 12:12:12', '')"),
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
        ddl = String.format(TableUtils.ADD_COLUMN_FORMAT, localTableName, newColumn.getName(),
            newColumn.getType().toClickhouseDDL());
        System.out.println(ddl);

        // alter modify format
        newColumn = new Column("dynamic", ColumnType.String, "''");
        ddl = String.format(TableUtils.MODIFY_COLUMN_FORMAT, localTableName, newColumn.getName(),
            newColumn.getType().toClickhouseDDL());
        System.out.println(ddl);

        // alter modify default
        ddl = String.format(TableUtils.MODIFY_COLUMN_FORMAT, localTableName, newColumn.getName(),
            newColumn.getDefaultExpr() == null ? newColumn.getType().toClickhouseDDL() :
                "DEFAULT " + newColumn.getDefaultExpr());
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
            new DistributedEngine("market_mbi_clickhouse_testing", "mbi", "clicks_by_shop_lr", "rand()"),
            TableUtils.parseEngine("Distributed('market_mbi_clickhouse_testing', 'mbi', 'clicks_by_shop_lr', rand())")
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
            new ReplicatedMergeTree(MergeTree.fromOldDefinition("date", "timestamp"), "/clickhouse/tables/{shard" +
                "}/market.logshatter_lr", "{replica}"),
            TableUtils.parseEngine("ReplicatedMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', " +
                "'{replica}', date, timestamp, 8192)")
        );

        assertEquals(
            new ReplicatedMergeTree(MergeTree.fromOldDefinition("date", "timestamp"), "/clickhouse/tables/{shard" +
                "}/market.logshatter_lr", "{replica}"),
            TableUtils.parseEngine("ReplicatedMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', " +
                "'{replica}', date, (timestamp), 8192)")
        );

        assertEquals(
            new ReplicatedMergeTree(MergeTree.fromOldDefinition("date", "timestamp", "host"), "/clickhouse/tables" +
                "/{shard}/market.logshatter_lr", "{replica}"),
            TableUtils.parseEngine("ReplicatedMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', " +
                "'{replica}', date, (timestamp, host), 8192)")
        );

        assertEquals(
            new ReplicatedReplacingMergeTree(MergeTree.fromOldDefinition("date", "timestamp", "host"), "/clickhouse" +
                "/tables/{shard}/market.logshatter_lr", "{replica}"),
            TableUtils.parseEngine("ReplicatedReplacingMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', " +
                "'{replica}', date, (timestamp, host), 8192)")
        );

        assertEquals(
            ReplacingMergeTree.fromOldDefinition("date", "timestamp"),
            TableUtils.parseEngine("ReplacingMergeTree(date, timestamp, 8192)")
        );

        assertEquals(
            new ReplicatedReplacingMergeTree(MergeTree.fromOldDefinition("date", "timestamp", "host"), "/clickhouse" +
                "/tables/{shard}/market.logshatter_lr", "{replica}"),
            TableUtils.parseEngine("ReplicatedReplacingMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', " +
                "'{replica}', date, (timestamp, host), 8192)")
        );

        assertEquals(
            ReplacingMergeTree.fromOldDefinition("date", "timestamp"),
            TableUtils.parseEngine("ReplacingMergeTree(date, timestamp, 8192)")
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
                "toYYYYMM(date)", Arrays.asList("host", "date"), "host",
                "/clickhouse/tables/{shard}/market.logshatter_lr", "{replica}"
            ),
            TableUtils.parseEngine(
                "ReplicatedMergeTree('/clickhouse/tables/{shard}/market.logshatter_lr', '{replica}') " +
                    "PARTITION BY toYYYYMM(date) ORDER BY (host, date) SAMPLE BY host SETTINGS index_granularity = " +
                    "8192")
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
            new MergeTree("toYYYYMM(date)", Collections.singletonList("timestamp"), null, 8192),
            TableUtils.parseNewMergeTreeSettings(
                "PARTITION BY toYYYYMM(date) ORDER BY timestamp SETTINGS index_granularity = 8192"
            )
        );

        assertEquals(
            new MergeTree("toYYYYMMDD(date)",
                Arrays.asList(
                    "date", "id_ms", "id_hash", "id_seq", "start_time_ms", "end_time_ms", "source_module",
                    "source_host", "target_module", "target_host", "http_code", "host", "query_params", "yandex_uid",
                    "events_timestamps"
                ),
                "date", 8192),
            TableUtils.parseNewMergeTreeSettings("PARTITION BY toYYYYMMDD(date) ORDER BY (date, id_ms, id_hash, " +
                "id_seq, start_time_ms, end_time_ms, source_module, source_host, target_module, target_host, " +
                "http_code, host, query_params, yandex_uid, events_timestamps) SAMPLE BY date SETTINGS " +
                "index_granularity = 8192")
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
                    "toYYYYMM(date)", Collections.singletonList("timestamp"),
                    "/clickhouse/tables/{shard}/market.main_report_blue_nginx_lr", "{replica}"
                )
            ),
            TableUtils.parseDDL(
                "CREATE TABLE market.main_report_blue_nginx_lr ( " +
                    "date Date,  timestamp UInt32,  host String,  upstream_resp_time_ms Int32,  req_time_ms Int32,  " +
                    "upstream_header_time_ms Int32,  place String,  status UInt16,  request_length UInt32,  " +
                    "bytes_sent UInt32) " +
                    "ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/market.main_report_blue_nginx_lr', " +
                    "'{replica}') " +
                    "PARTITION BY toYYYYMM(date) ORDER BY timestamp SETTINGS index_granularity = 8192"
            )
        );

        Assert.assertEquals(
            new ClickHouseTableDefinitionImpl(
                "market.trace_archive_old_lr",
                Arrays.asList(
                    new Column("date", ColumnType.Date),
                    new Column("timestamp", ColumnType.UInt32),
                    new Column("id_ms", ColumnType.UInt64),
                    new Column("id_hash", ColumnType.String),
                    new Column("id_seq", ColumnType.ArrayUInt32),
                    new Column("start_time_ms", ColumnType.UInt64),
                    new Column("end_time_ms", ColumnType.UInt64),
                    new Column("duration_ms", ColumnType.UInt32),
                    new Column("type", EnumColumnType.enum8(TestData.RequestType.class)),
                    new Column("module", ColumnType.String),
                    new Column("host", ColumnType.String),
                    new Column("source_module", ColumnType.String),
                    new Column("source_host", ColumnType.String),
                    new Column("target_module", ColumnType.String),
                    new Column("target_host", ColumnType.String),
                    new Column("environment", EnumColumnType.enum8(EnumColumnTypeTest.Environment.class)),
                    new Column("request_method", ColumnType.String),
                    new Column("http_code", ColumnType.Int16),
                    new Column("retry_num", ColumnType.UInt8),
                    new Column("error_code", ColumnType.String),
                    new Column("protocol", ColumnType.String),
                    new Column("http_method", ColumnType.String),
                    new Column("query_params", ColumnType.String),
                    new Column("yandex_uid", ColumnType.String),
                    new Column("yandex_login", ColumnType.String),
                    new Column("kv_keys", ColumnType.ArrayString),
                    new Column("kv_values", ColumnType.ArrayString),
                    new Column("events_names", ColumnType.ArrayString),
                    new Column("events_timestamps", ColumnType.ArrayUInt64),
                    new Column("test_ids", ColumnType.ArrayUInt32),
                    new Column("page_id", ColumnType.String),
                    new Column("page_type", ColumnType.String),
                    new Column("response_size_bytes", ColumnType.Int32, "-1")
                ),
                new ReplicatedReplacingMergeTree(
                    new MergeTree("toYYYYMMDD(date)",
                        Arrays.asList(
                            "date", "id_ms", "id_hash", "id_seq", "start_time_ms", "end_time_ms", "source_module",
                            "source_host", "target_module", "target_host", "http_code", "host", "query_params",
                            "yandex_uid", "events_timestamps"
                        ),
                        "date", 8192),
                    "/clickhouse/tables/{shard}/market.trace_archive_old_lr",
                    "{replica}"
                )
            ),
            TableUtils.parseDDL(
                "CREATE TABLE market.trace_archive_old_lr (" +
                    " `date` Date, `timestamp` UInt32, `id_ms` UInt64, `id_hash` String, `id_seq` Array(UInt32)," +
                    " `start_time_ms` UInt64, `end_time_ms` UInt64, `duration_ms` UInt32," +
                    " `type` Enum8('IN' = 0, 'OUT' = 1, 'PROXY' = 2), `module` String," +
                    " `host` String, `source_module` String, `source_host` String," +
                    " `target_module` String, `target_host` String," +
                    " `environment` Enum8('DEVELOPMENT' = 0, 'TESTING' = 1, 'PRESTABLE' = 2, 'PRODUCTION' = 3, " +
                    "'UNKNOWN' = 4)," +
                    " `request_method` String, `http_code` Int16, `retry_num` UInt8, `error_code` String," +
                    " `protocol` String, `http_method` String, `query_params` String, `yandex_uid` String," +
                    " `yandex_login` String, `kv_keys` Array(String), `kv_values` Array(String)," +
                    " `events_names` Array(String), `events_timestamps` Array(UInt64), `test_ids` Array(UInt32)," +
                    " `page_id` String, `page_type` String, `response_size_bytes` Int32 DEFAULT CAST(-1, 'Int32')) " +
                    "ENGINE = ReplicatedReplacingMergeTree('/clickhouse/tables/{shard}/market.trace_archive_old_lr', " +
                    "'{replica}') " +
                    "PARTITION BY toYYYYMMDD(date) " +
                    "ORDER BY (" +
                    "date, id_ms, id_hash, id_seq, start_time_ms, end_time_ms, source_module, source_host, " +
                    "target_module, target_host, http_code, host, query_params, yandex_uid, events_timestamps) " +
                    "SAMPLE BY date SETTINGS index_granularity = 8192"
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
                "CAST('UNKNOWN', 'Enum8(\\'UNSTABLE\\' = 0, \\'TESTING\\' = 1, \\'PRESTABLE\\' = 2, \\'STABLE\\' = 3," +
                    " \\'UNKNOWN\\' = 4)')"
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
                "CAST(arrayMap(id -> if((id LIKE 'graphite%') = 1, 'GRAPHITE', 'STATFACE'), metric_ids), 'Array(Enum8" +
                    "(\\'GRAPHITE\\' = 0, \\'SOLOMON\\' = 1, \\'STATFACE\\' = 2))')"
            )
        );
    }

    @Test
    public void testUnwrapDefaultExprtNotTouchString() {
        Assert.assertEquals(
            "CAST('UNKNOWN', 'Enum8(\\'UNSTABLE\\' = 0, \\'TESTING\\' = 1, \\'PRESTABLE\\' = 2, \\'STABLE\\' = 3, " +
                "\\'UNKNOWN\\' = 4)')",
            TableUtils.unwrapDefaultExpr(
                ColumnType.String,
                "CAST('UNKNOWN', 'Enum8(\\'UNSTABLE\\' = 0, \\'TESTING\\' = 1, \\'PRESTABLE\\' = 2, \\'STABLE\\' = 3," +
                    " \\'UNKNOWN\\' = 4)')"
            )
        );
    }

    @Test
    @SuppressWarnings("MethodName")
    public void testParseDDLOfClickhouse_20_5_2_Version() {
        String ddl = "CREATE TABLE market.temp_table\n" +
            "(\n" +
            "\t`date` Date,\n" +
            "\t    `timestamp` DateTime,\n" +
            "    `user_id` Int64,\t\n" +
            "    `staff_login` String,\n" +
            "    `action_type` Enum8('CREATE' = 0, 'UPDATE' = 1, 'DELETE' = 2, 'CHECK' = 3),\n" +
            "    `entity_id` Int64,\n" +
            "    `entity_name` String\n" +
            ")\n" +
            "ENGINE = ReplicatedMergeTree('/clickhouse/tables/market.temp', '{replica}', date, (date, " +
            "entity_id, user_id, action_type), 8192)";
        ClickHouseTableDefinition tableDefinition = TableUtils.parseDDL(ddl);
        assertNotNull(tableDefinition);
        List<Column> columns = tableDefinition.getColumns();
        assertEquals(7, columns.size());

        assertEquals(ReplicatedMergeTree.class, tableDefinition.getEngine().getClass());
        ReplicatedMergeTree mergeTree = (ReplicatedMergeTree) tableDefinition.getEngine();
        assertEquals("toYYYYMM(date)", mergeTree.getPartitionBy());
        assertEquals(8192, mergeTree.getIndexGranularity());
        assertEquals(Arrays.asList("date", "entity_id", "user_id", "action_type"), mergeTree.getOrderBy());
        assertNull(mergeTree.getSampleBy());

        assertEquals("market.temp_table", tableDefinition.getFullTableName());
        assertEquals("market", tableDefinition.getDatabaseName());
        assertEquals("temp_table", tableDefinition.getTableName());
    }

    @Test
    public void testParseDDLWithIndex() {
        String ddl = "CREATE TABLE wms.general_log_lr\n" +
            "(\n" +
            "    `date` Date,\n" +
            "    `timestamp` UInt32,\n" +
            "    `message` String,\n" +
            "    INDEX message_idx message TYPE ngrambf_v1(3, 4096, 2, 0) GRANULARITY 1\n" +
            ")\n" +
            "ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/wms.general_log_lr', '{replica}')\n" +
            "PARTITION BY toYYYYMM(date)\n" +
            "ORDER BY timestamp\n" +
            "SETTINGS index_granularity = 8192";
        ClickHouseTableDefinition actualDefinition = TableUtils.parseDDL(ddl);

        ClickHouseTableDefinition expectedDefinition = new ClickHouseTableDefinitionImpl(
            "wms.general_log_lr",
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("timestamp", ColumnType.UInt32),
                new Column("message", ColumnType.String)
            ),
            new ReplicatedMergeTree(
                "toYYYYMM(date)",
                Collections.singletonList("timestamp"),
                "/clickhouse/tables/{shard}/wms.general_log_lr",
                "{replica}"
            )
        );

        Assert.assertEquals(expectedDefinition, actualDefinition);
    }

    @Test
    public void parseColumnsWithIndexes() {
        String columnDdl = "`date` Date," +
            "  `timestamp` UInt32," +
            "  `field1` String," +
            "  INDEX message_idx message TYPE ngrambf_v1(3, 4096, 2, 0) GRANULARITY 1," +
            "  `field2` String," +
            "  INDEX b (u64 * length(s)) TYPE set(1000) GRANULARITY 4";

        List<Column> expected = Arrays.asList(
            new Column("date", ColumnType.Date),
            new Column("timestamp", ColumnType.UInt32),
            new Column("field1", ColumnType.String),
            new Column("field2", ColumnType.String)
        );
        List<Column> actual = TableUtils.parseColumns(columnDdl);
        Assert.assertEquals(expected, actual);
    }
}
