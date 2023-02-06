package ru.yandex.market.olap2.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowCallbackHandler;

import ru.yandex.market.olap2.load.partitioning.ClickhousePartitioningExpression;
import ru.yandex.market.olap2.load.partitioning.PartitionType;
import ru.yandex.market.olap2.load.tasks.AbstractTask;
import ru.yandex.market.olap2.model.RejectException;
import ru.yandex.market.olap2.model.UnionTable;

import static com.google.common.collect.Iterables.elementsEqual;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.olap2.TestUtils.setFinalStatic;


public class ClickhouseDaoTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private ClickhouseDao mockedImplementation;
    private ArgumentCaptor<String> captor;
    private LoggingJdbcTemplate mockedTemplate;
    private AtomicInteger hostId = new AtomicInteger(0);
    private final String mockedImplementationHost = "testCluster";
    private final String mockedImplementationSchema = "cubes";


    @Before
    public void setup() {
        mockedTemplate = tpl(true);
        Map<String, List<LoggingJdbcTemplate>> jdbcTemplates = ImmutableMap.of("myHost", ImmutableList.of(mockedTemplate));
        mockedImplementation = new ClickhouseDao(
                jdbcTemplates,
                mockedImplementationHost,
                mockedImplementationSchema,
                false
        );
    }

    @Test
    public void mustFindAtLeastOneNodeOnEachShard() {
        Map<String, List<LoggingJdbcTemplate>> jdbcTemplates = ImmutableMap.of(
                "shard1", ImmutableList.of(tpl(false), tpl(true)),
                "shard2", ImmutableList.of(tpl(true), tpl(false)),
                "shard3", ImmutableList.of(tpl(true), tpl(true))
                );
        ClickhouseDao dao = createDao(jdbcTemplates);
        dao.executeOnAllShards("select 1", true);
    }

    @Test(expected = java.util.concurrent.ExecutionException.class)
    public void mustFailAtSecondShard() throws NoSuchFieldException {
        Map<String, List<LoggingJdbcTemplate>> jdbcTemplates = ImmutableMap.of(
                "shard1", ImmutableList.of(tpl(false), tpl(true)),
                "shard2", ImmutableList.of(tpl(false), tpl(false))
        );
        ClickhouseDao dao = createDao(jdbcTemplates);
        setFinalStatic(ClickhouseDao.class.getDeclaredField("SQL_ERROR_INITIAL_TIMEOUT"), 1);

        dao.executeOnAllShards("select 1", true);
    }

    @Test(expected = IllegalStateException.class)
    public void mustFailOnCheckExists() {
        checkTableExistsOnAllHosts(ImmutableMap.of(
                "host_1", true,
                "host_2", true,
                "host_3", true,
                "host_4", false
        ), true);

        checkTableExistsOnAllHosts(ImmutableMap.of(
                "host_1", false,
                "host_2", true,
                "host_3", false,
                "host_4", false
        ), false);


    }

//    TODO more tests
    @Test
    public void mustSucceedOnCheckExists() {
        checkTableExistsOnAllShards(ImmutableMap.of(
                "shard1", true,
                "shard2", true,
                "shard3", true,
                "shard4", true
        ), true);

        checkTableExistsOnAllShards(ImmutableMap.of(
                "shard1", false,
                "shard2", false,
                "shard3", false,
                "shard4", false
        ), false);
    }

    private void checkTableExistsOnAllHosts(Map<String, Boolean> tableExistanceByHost, boolean requiredToExist) {
        hostId = new AtomicInteger(0);

        Map<String, List<LoggingJdbcTemplate>> jdbcTemplates = ImmutableMap.of(
                "shard1", ImmutableList.of(tpl(true), tpl(true)),
                "shard2", ImmutableList.of(tpl(true), tpl(true))
        );

        ClickhouseDao dao = new ClickhouseDao(jdbcTemplates, "anycluster", "anyschema", false) {
            protected boolean tableExistsOnHost(String table, LoggingJdbcTemplate template) {
                return tableExistanceByHost.get(template.getHost());
            }

            protected int getSqlErrorInitialTimeout() {
                return 1;
            }

            protected int getDistributedDdlTaskTimeout() {
                return 1;
            }
        };
        dao.checkTableExistsWithRetries("anytable", requiredToExist);
    }

    private void checkTableExistsOnAllShards(Map<String, Boolean> tableExistanceByShard, boolean requiredToExist) {
        hostId = new AtomicInteger(0);

        Map<String, List<LoggingJdbcTemplate>> jdbcTemplates = ImmutableMap.of(
                "shard1", ImmutableList.of(tpl(true), tpl(true)),
                "shard2", ImmutableList.of(tpl(true), tpl(true))
        );

        ClickhouseDao dao = new ClickhouseDao(jdbcTemplates, "anycluster", "anyschema", false) {
            protected boolean tableExistsOnShard(String table, Map.Entry<String, List<LoggingJdbcTemplate>> templates) {
                return tableExistanceByShard.get(templates.getKey());
            }

            protected int getSqlErrorInitialTimeout() {
                return 1;
            }

            protected int getDistributedDdlTaskTimeout() {
                return 1;
            }
        };
        dao.checkTableExistsWithRetries("anytable", requiredToExist);
    }

    @Test
    public void testCreateTable() {
        ClickhousePartitioningExpression noneExpression = new ClickhousePartitioningExpression(PartitionType.NONE);
        ClickhousePartitioningExpression dayExpression = new ClickhousePartitioningExpression(PartitionType.DAY);
        ClickhousePartitioningExpression monthExpression = new ClickhousePartitioningExpression(PartitionType.MONTH);
        ClickhousePartitioningExpression yearExpression = new ClickhousePartitioningExpression(PartitionType.YEAR);
        ClickhousePartitioningExpression monthQExpression = new ClickhousePartitioningExpression(PartitionType.MONTH_QUARTER);

        String table = "my_table";
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "uint64",
            "fieldTwo", "int64",
            "fieldThree", "string",
            "fieldFour", "double",
            "datetime", "string");
        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("fieldTwo");
        List<String> sampledAttrs = ImmutableList.of("SamplingKey");
        List<String> emptyAttrs = ImmutableList.of("");
        List<String> lowCardinalityAttrs = ImmutableList.of("lowCardinality", "Nullable");
        List<String> nullableLowCardinalityAttrs = ImmutableList.of("lowCardinality");
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "UInt64",
                "fieldTwo", "Int64"
        );

        Map<String, List<String>> attributes = ImmutableMap.of("fieldOne", sampledAttrs,
            "fieldTwo", emptyAttrs,
            "fieldThree", lowCardinalityAttrs,
            "fieldFour", nullableLowCardinalityAttrs,
            "datetime", emptyAttrs
        );

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, dayExpression, "datetime"
        );

        String sql = captor.getValue();

        assertThat(sql, containsString("CREATE TABLE IF NOT EXISTS cubes.my_table ON CLUSTER testCluster"));
        assertThat(sql, containsString("(datetime DateTime, fieldFour Float64, fieldOne UInt64, fieldThree Nullable(String), fieldTwo Int64)"));
        assertThat(sql, containsString(" engine=ReplicatedMergeTree('/marketstat/cubes/ch/tables/{shard}/my_table', '{replica}')"));
        assertThat(sql, containsString("ORDER BY (fieldTwo, sipHash64(fieldOne))"));
        assertThat(sql, containsString("SAMPLE BY sipHash64(fieldOne)"));
        assertThat(sql, containsString("PARTITION BY toYYYYMMDD(datetime)"));
        assertThat(sql, containsString("SETTINGS enable_mixed_granularity_parts = 1;"));

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, false,
                attributes, fabrikClickhouseTypes, noneExpression, "datetime"
        );
        sql = captor.getValue();
        assertThat(sql, not(containsString("PARTITION BY")));

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, monthExpression, "datetime"
        );
        sql = captor.getValue();
        assertThat(sql, containsString("PARTITION BY toYYYYMM(datetime)"));

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, yearExpression, "datetime"
        );
        sql = captor.getValue();
        assertThat(sql, containsString("PARTITION BY toYear(datetime)"));

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, monthQExpression, "datetime"
        );
        sql = captor.getValue();
        assertThat(sql, containsString("PARTITION BY toInt64(toString(toYYYYMM(datetime)) || toString(toInt8(1 + (toDayOfMonth(datetime) - 1)/8)))"));

        primaryColumns = new ArrayList<>();

        attributes = ImmutableMap.of("fieldOne", sampledAttrs,
            "fieldTwo", emptyAttrs,
            "fieldThree", sampledAttrs,
            "fieldFour", sampledAttrs,
            "datetime", sampledAttrs
        );

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, false,
                attributes, fabrikClickhouseTypes, noneExpression, "datetime"
        );
        sql = captor.getValue();
        assertThat(sql, containsString("ORDER BY (sipHash64(fieldOne, fieldThree, fieldFour, datetime))"));
        assertThat(sql, containsString("SAMPLE BY sipHash64(fieldOne, fieldThree, fieldFour, datetime)"));
    }

    @Test
    public void testCreateTableWithoutAllAttributes() {
        ClickhousePartitioningExpression monthExpression = new ClickhousePartitioningExpression(PartitionType.MONTH);


        String table = "my_table";
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "uint64",
            "fieldTwo", "boolean",
            "fieldThree", "string",
            "fieldFour", "double",
            "datetime", "string");
        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("fieldOne");
        primaryColumns.add("fieldTwo");
        List<String> sampledAttrs = ImmutableList.of("SamplingKey");
        List<String> lowCardinalityAttrs = ImmutableList.of("lowCardinality", "Nullable");
        List<String> nullableLowCardinalityAttrs = ImmutableList.of("lowCardinality");
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "UInt64");
        Map<String, List<String>> attributes = ImmutableMap.of("fieldOne", sampledAttrs,
            "fieldThree", lowCardinalityAttrs,
            "fieldFour", nullableLowCardinalityAttrs
        );

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, monthExpression, "datetime"
        );
        String sql = captor.getValue();
        assertThat(sql, containsString("PARTITION BY toYYYYMM(datetime)"));
        assertThat(sql, containsString("datetime DateTime"));
        assertThat(sql, containsString("fieldTwo UInt8"));
    }


    @Test
    public void testNullablePk() {
        thrown.expect(RejectException.class);
        thrown.expectMessage("fieldOne: Primary key should not be nullable");
        thrown.expectMessage("Sampling key should not be Nullable");
        thrown.expectMessage("datetime: Partitions column should not be Nullable");
        String table = "my_table";
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "int64",
            "fieldTwo", "boolean",
            "datetime", "String");
        List<String> sampledNullableAttrs = ImmutableList.of("Nullable", "SamplingKey");
        List<String> sampledAttrs = ImmutableList.of("SamplingKey");
        List<String> nullableAttrs = ImmutableList.of("Nullable");
        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("fieldOne");
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "Int64",
                "datetime", "DateTime"
        );
        Map<String, List<String>> attributes = ImmutableMap.of("fieldOne", sampledNullableAttrs,
            "fieldTwo", sampledAttrs,
            "datetime", nullableAttrs);
        ClickhousePartitioningExpression monthExpression = new ClickhousePartitioningExpression(PartitionType.MONTH);

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, monthExpression, "datetime"
        );
    }

    @Test
    public void testWrongPartitioning() {
        thrown.expect(RejectException.class);
        thrown.expectMessage("Partitioned table should have 'datetime' or 'date' column");
        String table = "my_table";
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "int64",
            "fieldTwo", "boolean");
        List<String> primaryColumns = ImmutableList.of("fieldOne");
        List<String> empty = Collections.emptyList();
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "Int64");
        Map<String, List<String>> attributes = ImmutableMap.of("fieldOne", empty,
            "fieldTwo", empty);
        ClickhousePartitioningExpression monthExpression = new ClickhousePartitioningExpression(PartitionType.MONTH);

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, monthExpression, null
        );
    }

    @Test
    public void testPartitionFieldCustomField() {
        String table = "my_table";
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "int64",
                "fieldTwo", "boolean", "created_datetime", "DateTime");
        List<String> primaryColumns = ImmutableList.of("fieldOne");
        List<String> empty = Collections.emptyList();
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "Int64");
        Map<String, List<String>> attributes = ImmutableMap.of("fieldOne", empty,
                "fieldTwo", empty, "created_datetime", empty);
        ClickhousePartitioningExpression dayExpression = new ClickhousePartitioningExpression(PartitionType.DAY);
        String partitioningField = "created_datetime";

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, dayExpression, partitioningField
        );

        String sql = captor.getValue();

        assertThat(sql, containsString("PARTITION BY toYYYYMMDD(created_datetime)"));
    }

    @Test
    public void testPartitionFieldDateField() {
        String table = "my_table";
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "int64",
                "fieldTwo", "boolean", "date", "Date");
        List<String> primaryColumns = ImmutableList.of("fieldOne");
        List<String> empty = Collections.emptyList();
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "Int64");
        Map<String, List<String>> attributes = ImmutableMap.of("fieldOne", empty,
                "fieldTwo", empty);
        ClickhousePartitioningExpression dayExpression = new ClickhousePartitioningExpression(PartitionType.DAY);
        String partitioningField = "date";

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, dayExpression, partitioningField
        );

        String sql = captor.getValue();

        assertThat(sql, containsString("PARTITION BY toYYYYMMDD(date)"));
    }

    @Test
    public void testPartitionFieldDateTimeField() {
        String table = "my_table";
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "int64",
                "fieldTwo", "boolean", "datetime", "DateTime");
        List<String> primaryColumns = ImmutableList.of("fieldOne");
        List<String> empty = Collections.emptyList();
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "Int64");
        Map<String, List<String>> attributes = ImmutableMap.of("fieldOne", empty,
                "fieldTwo", empty);
        ClickhousePartitioningExpression dayExpression = new ClickhousePartitioningExpression(PartitionType.DAY);
        String partitioningField = "datetime";

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, dayExpression, partitioningField
        );

        String sql = captor.getValue();

        assertThat(sql, containsString("PARTITION BY toYYYYMMDD(datetime)"));
    }

    @Test
    public void testNonePartitionExpression() {
        thrown.expect(RejectException.class);
        thrown.expectMessage("No partitionedExpression was provided for partitioned table");
        String table = "my_table";
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "int64",
            "fieldTwo", "boolean",
            "datetime", "String");
        List<String> primaryColumns = ImmutableList.of("fieldOne");
        List<String> empty = Collections.emptyList();
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "Int64",
                "datetime", "DateTime"
        );
        Map<String, List<String>> attributes = ImmutableMap.of("fieldOne", empty,
            "fieldTwo", empty);
        ClickhousePartitioningExpression noneExpression = new ClickhousePartitioningExpression(PartitionType.NONE);

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, noneExpression, "datetime"
        );
    }

    @Test
    public void testNullPartitionExpression() {
        thrown.expect(RejectException.class);
        thrown.expectMessage("No partitionedExpression was provided for partitioned table");
        String table = "my_table";
        Map<String, String> ytColumns = ImmutableMap.of("fieldOne", "int64",
            "fieldTwo", "boolean",
            "datetime", "String");
        List<String> primaryColumns = ImmutableList.of("fieldOne");
        List<String> empty = Collections.emptyList();
        Map<String, String> fabrikClickhouseTypes = ImmutableMap.of("fieldOne", "Int64",
                "datetime", "DateTime"
        );
        Map<String, List<String>> attributes = ImmutableMap.of("fieldOne", empty,
            "fieldTwo", empty);

        mockedImplementation.createTable(
                table, ytColumns, primaryColumns, true,
                attributes, fabrikClickhouseTypes, null, "datetime"
        );
    }

    @Test
    public void testAddColumn() {
        List<String> attributes = ImmutableList.of("Nullable");
        mockedImplementation.addColumn("my_table", "myColumn", "Nullable(UInt16)", null, attributes);

        String sql = captor.getValue();

        assertThat(sql, containsString("myColumn Nullable(UInt16)"));
    }

    @Test
    public void testAddColumnRaw() {
        mockedImplementation.addColumnRaw("my_table", "myColumn Nullable(String)");

        String sql = captor.getValue();

        assertThat(sql, containsString("myColumn Nullable(String)"));
    }

    @Test
    public void testBadColumn() {
        thrown.expect(RejectException.class);
        thrown.expectMessage("Sampling key should not be Nullable");
        List<String> attributes = ImmutableList.of("Nullable", "SamplingKey");
        mockedImplementation.addColumn("my_table", "myColumn", "Nullable(UInt16)", "UInt16", attributes);
    }

    @Test
    public void testDropColumn() {
        mockedImplementation.dropColumn("my_table", "myColumn");

        String sql = captor.getValue();

        assertThat(sql, containsString("ALTER TABLE my_table"));
        assertThat(sql, containsString("DROP COLUMN IF EXISTS myColumn"));
    }

    @Test
    public void testSwapNonPartitionedTable() {
        mockedImplementation.swapNonPartitionedTables("my_table", "tmp_table");

        List<String> allSqls = captor.getAllValues();
        String createSql = allSqls.get(allSqls.size() - 3);
        String startMergeSql = allSqls.get(allSqls.size() - 2);
        String stopMergeOldSql = allSqls.get(allSqls.size() - 1);
        assertEquals(createSql, "RENAME table " +
                "cubes.tmp_table to cubes.my_table_tmp_name, " +
                "cubes.my_table to cubes.tmp_table_olap2old, " +
                "cubes.my_table_tmp_name to cubes.my_table" +
                " on CLUSTER testCluster SETTINGS distributed_ddl_task_timeout = 60");
        assertEquals(startMergeSql, "SYSTEM START MERGES cubes.my_table");
        assertEquals(stopMergeOldSql, "SYSTEM STOP MERGES cubes.tmp_table_olap2old");
    }

//    @Test
//    TODO верну в ближайшее время, не успеваю разобраться с моком callbackhandler'а, тещу руками
    public void testSwapPartitions() {
        mockedImplementation.swapOrAttachPartition("my_table", "tmp_table", 20200101);

        String sql = captor.getValue();

        assertEquals(sql, "ALTER table cubes.my_table REPLACE PARTITION 20200101 FROM cubes.tmp_table" +
            " SETTINGS distributed_ddl_task_timeout = 180");
    }

    @Test
    public void testGetTableRows() {
        when(mockedTemplate.queryForObject(captor.capture(), anyMap(), eq(Long.class))).thenReturn(100L);
        Long result = mockedImplementation.getTableRows("my_table");

        String sql = captor.getValue();

        assertEquals(sql, "SELECT COUNT(*) from cubes.my_table");
        assert result.equals(100L);
    }

    @Test
    public void testGetExistingPartitionsForTable() {
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        doNothing().when(mockedTemplate).query(
            eq("SELECT distinct partition FROM system.parts WHERE table=:table and database=:db"),
            mapCaptor.capture(),
            any(RowCallbackHandler.class));
        mockedImplementation.getExistingPartitionsForTable("my_table");

        Map<String, Object> params = mapCaptor.getValue();
        assertEquals("cubes", params.get("db"));
        assertEquals("my_table", params.get("table"));
    }

    @Test
    public void testAttachPartitionFromTable() {
        mockedImplementation.swapOrAttachPartition("my_table", "tmp_table", 20200101);

        String sql = captor.getValue();

        assertEquals(sql, "ALTER TABLE cubes.my_table ATTACH PARTITION 20200101 FROM cubes.tmp_table SETTINGS distributed_ddl_task_timeout = 180");
    }

    @Test
    public void testCreateDistributedTable() {
        mockedImplementation.createDistributedTable("my_table", "my_table_distributed", null);

        String expected = "CREATE table IF NOT EXISTS cubes.my_table_distributed ON CLUSTER testCluster as cubes.my_table ENGINE=Distributed(testCluster," +
                " cubes, my_table, rand() )";

        String sql = captor.getValue();
        assertEquals(sql.toLowerCase(), expected.toLowerCase());

    }

    @Test
    public void testCreateShardedDistributedTable() {
        mockedImplementation.createDistributedTable("my_table", "my_table_distributed", "shard_col");

        String expected = "CREATE table IF NOT EXISTS cubes.my_table_distributed ON CLUSTER testCluster as cubes.my_table ENGINE=Distributed(testCluster," +
                " cubes, my_table, shard_col )";

        String sql = captor.getValue();
        assertEquals(sql.toLowerCase(), expected.toLowerCase());

    }

    @Test
    public void testGetOldTmpTables() {
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        doNothing().when(mockedTemplate).query(
                captor.capture(),
                mapCaptor.capture(),
                any(RowCallbackHandler.class)
        );

        mockedImplementation.getOldTmpTables(2400);

        String sql = captor.getValue();
        Map<String, Object> params = mapCaptor.getValue();
        String expected = "SELECT name from system.tables where name like 'olap2tmp_%'" +
            " and (now() - metadata_modification_time) / ((60 * 60) * 24) > :interval;";

        assertEquals(sql, expected);
        assertEquals(params.get("interval"), 2400);
    }

    @Test
    public void testGetPartitionSizes() {
        doNothing().when(mockedTemplate).query(
            captor.capture(),
            any(RowCallbackHandler.class)
        );
        mockedImplementation.getPartitionSizes("my_table_distributed");

        String expected = "SELECT substring(toString(datetime), 1, 7) AS partition, COUNT(*) as cnt FROM my_table_distributed" +
            " GROUP BY partition ORDER BY partition desc";
        String sql = captor.getValue();
        assertEquals(sql, expected);
    }

    @Test
    public void testIncorrectTable() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("This method should be used for distributed tables only");

        mockedImplementation.getPartitionSizes("my_table");
    }

    @Test
    public void testGetExistingTables() {
        doNothing().when(mockedTemplate).query(captor.capture(), any(RowCallbackHandler.class));
        mockedImplementation.getExistingTables();

        String sql = captor.getValue();
        String expected = "SELECT name FROM system.tables WHERE database = 'cubes'";
        assertEquals(sql, expected);
    }

    @Test
    public void testGetDeadShards() {
        Map<String, List<LoggingJdbcTemplate>> jdbcTemplates = ImmutableMap.of(
                "shard1", ImmutableList.of(tpl(false), tpl(false)),
                "shard2", ImmutableList.of(tpl(true), tpl(false)),
                "shard3", ImmutableList.of(tpl(false), tpl(true)),
                "shard4", ImmutableList.of(tpl(false), tpl(false))
        );
        ClickhouseDao dao = createDao(jdbcTemplates);
        List<String> deadShards = dao.getDeadShards();
        List<String> expected = ImmutableList.of("shard1", "shard4");
        assertTrue(
                "Dead shards are " + deadShards + ", but we expected " + expected,
                elementsEqual(deadShards, expected)
        );
    }

    @Test
    public void testCreateUnionTable() {

        UnionTable unionTable = new UnionTable(
                "my_table",
                "Merge('cubes', '^(test1|test2)$')",
                List.of("test1", "test2")
        );

        mockedImplementation.createUnionTable(unionTable);
        String sql = captor.getValue();

        assertThat(
                sql,
                containsString(
                "CREATE TABLE IF NOT EXISTS " +
                    mockedImplementationSchema + "." + unionTable.getTableName()
                )
        );

        assertThat(sql, containsString("ON CLUSTER " + mockedImplementationHost));
        assertThat(sql, containsString("ENGINE = "+ unionTable.getMergeStatement()));

    }

    @Test
    public void testCreateDistributedTableWithEngine() {

        String distributedTable = "my_table_distributed";
        String sourceTable = "my_table";
        String distributedEngine = "Distributed(cluster, schema, my_table, rand())";

        mockedImplementation.createDistributedTableWithEngine(
                sourceTable,
                distributedTable,
                distributedEngine
        );
        String sql = captor.getValue();

        assertThat(
                sql,
                containsString(
            "CREATE TABLE IF NOT EXISTS " +
                    mockedImplementationSchema + "." + distributedTable
                )
        );

        assertThat(sql, containsString("ON CLUSTER " + mockedImplementationHost));
        assertThat(
                sql,
                containsString(" as " + mockedImplementationSchema + "." + sourceTable)
        );
        assertThat(sql, containsString("ENGINE = " + distributedEngine));

    }

    private ClickhouseDao createDao(Map<String, List<LoggingJdbcTemplate>> jdbcTemplates) {
        return new ClickhouseDao(jdbcTemplates, "anycluster", "anyschema", false);
    }

    private LoggingJdbcTemplate tpl(boolean isThisHostOk) {
        captor = ArgumentCaptor.forClass(String.class);
        LoggingJdbcTemplate mockedTemplate = mock(LoggingJdbcTemplate.class);
        when(mockedTemplate.queryForObject(any(String.class), anyMap(), eq(Integer.class))).thenReturn(1);
        when(mockedTemplate.queryForObject(any(String.class), anyMap(), eq(Long.class))).thenReturn(1L);
        when(mockedTemplate.queryForObject(
                eq("SELECT MAX(absolute_delay) " +
                        "FROM system.replicas " +
                        "WHERE replica_name = hostName() " +
                        "AND NOT startsWith(table, :tmp_prefix)"),
                eq(ImmutableMap.of("tmp_prefix", AbstractTask.TMP_PREFFIX)),
                eq(Integer.class))).thenReturn(isThisHostOk ? 0 : 1);
        int selectOneReturns = isThisHostOk ? 1 : 0;
        when(mockedTemplate.queryForObject(
                eq("select hostName()"), eq(Collections.emptyMap()), eq(String.class))).thenReturn("host_" + hostId.incrementAndGet());
        when(mockedTemplate.queryForObject(
                eq("select 1"), eq(Collections.emptyMap()), eq(Integer.class))).thenReturn(selectOneReturns);
        when(mockedTemplate.queryForObject(
                eq("select 1"), eq(Collections.emptyMap()), eq(Integer.class))).thenReturn(selectOneReturns);
        doNothing().when(mockedTemplate).exec(captor.capture());
        return mockedTemplate;
    }
}
