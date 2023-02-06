package ru.yandex.direct.binlogbroker.replicatetoyt;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import one.util.streamex.StreamEx;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.ColumnType;
import ru.yandex.direct.binlog.model.CreateOrModifyColumn;
import ru.yandex.direct.binlog.model.CreateTable;
import ru.yandex.direct.binlog.model.DropColumn;
import ru.yandex.direct.binlog.model.DropTable;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.binlog.model.RenameColumn;
import ru.yandex.direct.binlog.model.RenameTable;
import ru.yandex.direct.binlog.model.SchemaChange;
import ru.yandex.direct.ytwrapper.YtUtils;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeBooleanNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeDoubleNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeIntegerNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.yt.ytclient.proxy.LookupRowsRequest;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;
import ru.yandex.yt.ytclient.tables.ColumnSchema;
import ru.yandex.yt.ytclient.tables.ColumnSortOrder;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;

import static ru.yandex.direct.binlogbroker.replicatetoyt.YtReplicator.HASH_COLUMN_NAME;

/**
 * Тест предназначен для ручного запуска, т.к. ходит в настоящий продакшновый YT.
 * Для CI в таком виде тест непригоден.
 */
@Ignore
@ParametersAreNonnullByDefault
public class YtReplicatorTest {
    private static final String SERVER_UUID = "12345678-9abc-def0-1234-56789abcdef0";
    private static final String TEST_SOURCE = "test_source";
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2018, 6, 20, 14, 4);

    @ClassRule
    public static JunitRealYt realYt = new JunitRealYt(YtCluster.HAHN);

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private static Object ytNodeValue(YTreeNode node) {
        if (node instanceof YTreeBooleanNode) {
            return ((YTreeBooleanNode) node).getValue();
        } else if (node instanceof YTreeDoubleNode) {
            return ((YTreeDoubleNode) node).getValue();
        } else if (node instanceof YTreeIntegerNode) {
            return ((YTreeIntegerNode) node).getLong();
        } else if (node instanceof YTreeStringNode) {
            return ((YTreeStringNode) node).getValue();
        } else {
            throw new IllegalArgumentException(
                    String.format("Can't handle `%s` (type `%s`)", node, node.getClass().getCanonicalName()));
        }
    }

    private static List<Map> lookupAsMaps(YtClient client, LookupRowsRequest request) {
        return client.lookupRows(request)
                .join()
                .getYTreeRows()
                .stream()
                .map(row -> StreamEx.of(row.spliterator())
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> ytNodeValue(e.getValue()))))
                .collect(Collectors.toList());
    }

    @Test
    public void severalTables() throws TimeoutException, InterruptedException {
        Yt yt = realYt.getYt();
        YtClient client = realYt.getYtClient();
        String basePath = realYt.getBasePath();

        TestTable testTable1 = new TestTable(basePath + "/test_source/table1", new TableSchema.Builder()
                .addKey(YtReplicator.SOURCE_COLUMN_NAME, ColumnValueType.STRING)
                .addKey("id", ColumnValueType.INT64)
                .addValue("value1", ColumnValueType.STRING)
                .build());
        TestTable testTable2 = new TestTable(basePath + "/test_source/table2", new TableSchema.Builder()
                .addKey(YtReplicator.SOURCE_COLUMN_NAME, ColumnValueType.STRING)
                .addKey("id", ColumnValueType.INT64)
                .addValue("value2", ColumnValueType.DOUBLE)
                .build());

        client.createNode(new CreateNode(basePath + "/test_source", ObjectType.MapNode));
        testTable1.createYtTable(client);
        testTable2.createYtTable(client);

        List<BinlogEvent> sourceEvents = ImmutableList.of(
                new BinlogEvent()
                        .withDb("test_db")
                        .withOperation(Operation.INSERT)
                        .withQueryIndex(0)
                        .withServerUuid(SERVER_UUID)
                        .withSource("test_source")
                        .withTable("table1")
                        .withTransactionId(1)
                        .withUtcTimestamp(TIMESTAMP)
                        .withAddedRows(new BinlogEvent.Row()
                                .withRowIndex(0)
                                .withPrimaryKey(Map.of("id", 1))
                                .withBefore(Map.of())
                                .withAfter(Map.of("value1", "hello")))
                        .validate(),
                new BinlogEvent()
                        .withDb("test_db")
                        .withOperation(Operation.INSERT)
                        .withQueryIndex(0)
                        .withServerUuid(SERVER_UUID)
                        .withSource("test_source")
                        .withTable("table2")
                        .withTransactionId(2)
                        .withUtcTimestamp(TIMESTAMP)
                        .withAddedRows(new BinlogEvent.Row()
                                .withRowIndex(0)
                                .withPrimaryKey(Map.of("id", 2))
                                .withBefore(Map.of())
                                .withAfter(Map.of("value2", 1.23)))
                        .validate(),
                new BinlogEvent()
                        .withDb("test_db")
                        .withOperation(Operation.INSERT)
                        .withQueryIndex(0)
                        .withServerUuid(SERVER_UUID)
                        .withSource("test_source")
                        .withTable("table1")
                        .withTransactionId(3)
                        .withUtcTimestamp(TIMESTAMP)
                        .withAddedRows(new BinlogEvent.Row()
                                .withRowIndex(0)
                                .withPrimaryKey(Map.of("id", 3))
                                .withBefore(Map.of())
                                .withAfter(Map.of("value1", "world")))
                        .validate(),
                new BinlogEvent()
                        .withDb("test_db")
                        .withOperation(Operation.INSERT)
                        .withQueryIndex(0)
                        .withServerUuid(SERVER_UUID)
                        .withSource("test_source")
                        .withTable("table2")
                        .withTransactionId(4)
                        .withUtcTimestamp(TIMESTAMP)
                        .withAddedRows(new BinlogEvent.Row()
                                .withRowIndex(0)
                                .withPrimaryKey(Map.of("id", 4))
                                .withBefore(Map.of())
                                .withAfter(Map.of("value2", 4.56)))
                        .validate());

        new YtReplicatorImpl(client, yt, YPath.simple(basePath), YPath.simple(basePath), false)
                .acceptDML(sourceEvents, DummyStateManager.SHARD_OFFSET_SAVER);

        List<Map> data = lookupAsMaps(client, testTable1.lookupRowsRequest()
                .addFilter("test_source", 1)
                .addFilter("test_source", 3));
        softly.assertThat(data)
                .describedAs("table1")
                .containsExactlyInAnyOrder(
                        ImmutableMap.of(YtReplicator.SOURCE_COLUMN_NAME, "test_source", "id", 1L, "value1", "hello"),
                        ImmutableMap.of(YtReplicator.SOURCE_COLUMN_NAME, "test_source", "id", 3L, "value1", "world"));

        data = lookupAsMaps(client, testTable2.lookupRowsRequest()
                .addFilter("test_source", 2)
                .addFilter("test_source", 4));
        softly.assertThat(data)
                .describedAs("table2")
                .containsExactlyInAnyOrder(
                        ImmutableMap.of(YtReplicator.SOURCE_COLUMN_NAME, "test_source", "id", 2L, "value2", 1.23),
                        ImmutableMap.of(YtReplicator.SOURCE_COLUMN_NAME, "test_source", "id", 4L, "value2", 4.56));
    }

    @Test
    public void testAlterModifyColumns() throws TimeoutException, InterruptedException {
        Yt yt = realYt.getYt();
        YtClient ytClient = realYt.getYtClient();
        YPath basePath = YPath.simple(realYt.getBasePath());
        YPath tablePath = basePath.child(TEST_SOURCE).child("dynamic_table");
        YtAttrStateManager stateManager = new YtAttrStateManager(yt, basePath);

        TestTable table = new TestTable(
                tablePath.toString(),
                new TableSchema.Builder()
                        .addKeyExpression(HASH_COLUMN_NAME, ColumnValueType.INT64, "int64(farm_hash(x))")
                        .addKey("x", ColumnValueType.INT64)
                        .addValue("y", ColumnValueType.INT64)
                        .addValue("z", ColumnValueType.STRING)
                        .build());

        table.createYtTable(ytClient);
        TabletUtils.waitForTablets(yt, tablePath, TabletUtils.TabletState.MOUNTED, Duration.ofSeconds(30));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (long x = 0; x < 100; ++x) {
            rows.add(ImmutableMap.of("x", x, "y", x % 50, "z", String.valueOf(x)));
        }
        table.addRows(rows);
        table.insertYtRows(ytClient);

        List<SchemaChange> schemaChanges = new ArrayList<>();
        schemaChanges.add(new CreateOrModifyColumn()
                .withColumnName("y")
                .withColumnType(ColumnType.STRING)
                .withNullable(false));
        schemaChanges.add(new CreateOrModifyColumn()
                .withColumnName("q")
                .withColumnType(ColumnType.TIMESTAMP)
                .withDefaultValue("CURRENT_TIMESTAMP")
                .withNullable(false));
        schemaChanges.add(new CreateOrModifyColumn()
                .withColumnName("w")
                .withColumnType(ColumnType.DATE)
                .withNullable(true));
        schemaChanges.add(new CreateOrModifyColumn()
                .withColumnName("b")
                .withColumnType(ColumnType.BYTES)
                .withNullable(false));
        schemaChanges.add(new DropColumn().withColumnName("z"));

        BinlogEvent schemaChange = new BinlogEvent()
                .withDb("test_db")
                .withOperation(Operation.SCHEMA)
                .withQueryIndex(0)
                .withServerUuid(SERVER_UUID)
                .withSource(TEST_SOURCE)
                .withTable("dynamic_table")
                .withTransactionId(2)
                .withUtcTimestamp(TIMESTAMP)
                .withSchemaChanges(schemaChanges)
                .validate();

        AlterHandler alterHandler = new AlterHandler(yt, basePath, basePath);
        alterHandler.handle(schemaChange, stateManager.shardOffsetSaver(TEST_SOURCE, 123));

        TabletUtils.waitForTablets(yt, tablePath, TabletUtils.TabletState.MOUNTED, Duration.ofSeconds(30));

        softly.assertThat(stateManager.getShardOffset(TEST_SOURCE)).isEqualTo(Optional.of(123L));

        List<ColumnSchema> expectedSchema = ImmutableList.<ColumnSchema>builder()
                .add(new ColumnSchema.Builder(HASH_COLUMN_NAME, ColumnValueType.INT64)
                        .setSortOrder(ColumnSortOrder.ASCENDING)
                        .setExpression("int64(farm_hash(x))")
                        .build())
                .add(new ColumnSchema.Builder("x", ColumnValueType.INT64)
                        .setSortOrder(ColumnSortOrder.ASCENDING)
                        .build())
                .add(new ColumnSchema.Builder("y", ColumnValueType.STRING).build())
                .add(new ColumnSchema.Builder("q", ColumnValueType.STRING).build())
                .add(new ColumnSchema.Builder("w", ColumnValueType.STRING).build())
                .add(new ColumnSchema.Builder("b", ColumnValueType.STRING).build())
                .build();

        TableSchema tableSchema = TableSchema.fromYTree(yt.cypress().get(tablePath.attribute(YtUtils.SCHEMA_ATTR)));

        checkSchema(tableSchema, expectedSchema);

        List<YTreeMapNode> alteredRows = table.fetchYtRows(ytClient);
        long x = 0;
        for (YTreeMapNode alteredRow : alteredRows) {
            Optional<YTreeNode> maybeX = alteredRow.get("x");
            softly.assertThat(maybeX.isPresent()).isTrue();
            if (maybeX.isPresent()) {
                softly.assertThat(maybeX.get().isIntegerNode()).isTrue();
                softly.assertThat(maybeX.get().longValue()).isEqualTo(x);
            }

            Optional<YTreeNode> maybeY = alteredRow.get("y");
            softly.assertThat(maybeY.isPresent()).isTrue();
            if (maybeY.isPresent()) {
                softly.assertThat(maybeY.get().isStringNode()).isTrue();
                softly.assertThat(maybeY.get().stringValue()).isEqualTo(String.valueOf(x % 50));
            }

            Optional<YTreeNode> maybeZ = alteredRow.get("z");
            softly.assertThat(maybeZ.isPresent()).isFalse();

            Optional<YTreeNode> maybeQ = alteredRow.get("q");
            softly.assertThat(maybeQ.isPresent()).isTrue();
            if (maybeQ.isPresent()) {
                softly.assertThat(maybeQ.get().isStringNode()).isTrue();
                softly.assertThat(maybeQ.get().stringValue()).isEqualTo(TIMESTAMP.toString());
            }

            Optional<YTreeNode> maybeW = alteredRow.get("w");
            softly.assertThat(maybeW.isPresent()).isTrue();
            if (maybeW.isPresent()) {
                softly.assertThat(maybeW.get().isEntityNode()).isTrue();
            }

            Optional<YTreeNode> maybeB = alteredRow.get("b");
            softly.assertThat(maybeB.isPresent()).isTrue();
            if (maybeB.isPresent()) {
                softly.assertThat(maybeB.get().isStringNode()).isTrue();
                softly.assertThat(maybeB.get().bytesValue()).isEqualTo(new byte[0]);
            }

            ++x;
        }
    }

    @Test
    public void testAlterRenameColumn() throws TimeoutException, InterruptedException {
        Yt yt = realYt.getYt();
        YtClient ytClient = realYt.getYtClient();
        YPath basePath = YPath.simple(realYt.getBasePath());
        YPath tablePath = basePath.child(TEST_SOURCE).child("dynamic_table_2");
        YtAttrStateManager stateManager = new YtAttrStateManager(yt, basePath);

        TestTable table = new TestTable(
                tablePath.toString(),
                new TableSchema.Builder()
                        .addKeyExpression(HASH_COLUMN_NAME, ColumnValueType.INT64, "int64(farm_hash(x) % 5)")
                        .addKey("x", ColumnValueType.INT64)
                        .addValue("a", ColumnValueType.INT64)
                        .addValue("c", ColumnValueType.INT64)
                        .addValue("__dummy", ColumnValueType.INT64)
                        .addValue(YtReplicator.SOURCE_COLUMN_NAME, ColumnValueType.STRING)
                        .build());

        table.createYtTable(ytClient);
        TabletUtils.waitForTablets(yt, tablePath, TabletUtils.TabletState.MOUNTED, Duration.ofSeconds(30));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (long x = 0; x < 100; ++x) {
            if (x < 50) {
                // __dummy = null
                rows.add(ImmutableMap.of(YtReplicator.SOURCE_COLUMN_NAME, "development:ppc:1", "x", x, "a", x % 10,
                        "c", x % 3));
            } else {
                // c = null
                rows.add(ImmutableMap.of(YtReplicator.SOURCE_COLUMN_NAME, "development:ppc:1", "x", x, "a", x % 10,
                        "__dummy", -x));
            }
        }
        table.addRows(rows);
        table.insertYtRows(ytClient);

        List<SchemaChange> schemaChanges = new ArrayList<>();
        schemaChanges.add(new RenameColumn()
                .withOldColumnName("x")
                .withNewColumnName("y")
        );
        schemaChanges.add(new RenameColumn()
                .withOldColumnName("__dummy")
                .withNewColumnName("w")
        );
        schemaChanges.add(new RenameColumn()
                .withOldColumnName("a")
                .withNewColumnName("b")
        );
        schemaChanges.add(new RenameColumn()
                .withOldColumnName("c")
                .withNewColumnName("d")
        );
        schemaChanges.add(new CreateOrModifyColumn()
                .withColumnName("d")
                .withColumnType(ColumnType.STRING)
                .withNullable(true)
        );
        schemaChanges.add(new CreateOrModifyColumn()
                .withColumnName("w")
                .withColumnType(ColumnType.STRING)
                .withDefaultValue("default")
        );

        BinlogEvent schemaChange = new BinlogEvent()
                .withDb("test_db")
                .withOperation(Operation.SCHEMA)
                .withQueryIndex(0)
                .withServerUuid(SERVER_UUID)
                .withSource(TEST_SOURCE)
                .withTable("dynamic_table_2")
                .withTransactionId(2)
                .withUtcTimestamp(TIMESTAMP)
                .withSchemaChanges(schemaChanges)
                .validate();

        AlterHandler alterHandler = new AlterHandler(yt, basePath, basePath);
        alterHandler.handle(schemaChange, stateManager.shardOffsetSaver(TEST_SOURCE, 123));

        TabletUtils.waitForTablets(yt, tablePath, TabletUtils.TabletState.MOUNTED, Duration.ofSeconds(60));

        List<ColumnSchema> expectedSchema = ImmutableList.<ColumnSchema>builder()
                .add(new ColumnSchema.Builder(HASH_COLUMN_NAME, ColumnValueType.INT64)
                        .setSortOrder(ColumnSortOrder.ASCENDING)
                        .setExpression("int64(farm_hash(y) % 32)")
                        .build())
                .add(new ColumnSchema.Builder("y", ColumnValueType.INT64)
                        .setSortOrder(ColumnSortOrder.ASCENDING)
                        .build())
                .add(new ColumnSchema.Builder("w", ColumnValueType.STRING).build())
                .add(new ColumnSchema.Builder("b", ColumnValueType.INT64).build())
                .add(new ColumnSchema.Builder("d", ColumnValueType.STRING).build())
                .add(new ColumnSchema.Builder(YtReplicator.SOURCE_COLUMN_NAME,
                        ColumnValueType.STRING)
                        .build())
                .build();

        TableSchema tableSchema = TableSchema.fromYTree(yt.cypress().get(tablePath.attribute(YtUtils.SCHEMA_ATTR)));

        checkSchema(tableSchema, expectedSchema);

        TestTable alteredTable = new TestTable(
                table,
                new TableSchema.Builder()
                        .addKeyExpression(HASH_COLUMN_NAME, ColumnValueType.INT64, "int64(farm_hash(y % 32))")
                        .addKey("y", ColumnValueType.INT64)
                        .addValue("b", ColumnValueType.INT64)
                        .addValue("d", ColumnValueType.STRING)
                        .addValue("w", ColumnValueType.STRING)
                        .addValue(YtReplicator.SOURCE_COLUMN_NAME, ColumnValueType.STRING)
                        .build(),
                values -> values.put("y", values.remove("x")));

        List<YTreeMapNode> alteredRows = alteredTable.fetchYtRows(ytClient);
        long x = 0;
        for (YTreeMapNode alteredRow : alteredRows) {
            Optional<YTreeNode> maybeY = alteredRow.get("y");
            softly.assertThat(maybeY.isPresent()).isTrue();
            if (maybeY.isPresent()) {
                softly.assertThat(maybeY.get().isIntegerNode()).isTrue();
                softly.assertThat(maybeY.get().longValue()).isEqualTo(x);
            }

            Optional<YTreeNode> maybeB = alteredRow.get("b");
            softly.assertThat(maybeB.isPresent()).isTrue();
            if (maybeB.isPresent()) {
                softly.assertThat(maybeB.get().isIntegerNode()).isTrue();
                softly.assertThat(maybeB.get().longValue()).isEqualTo(x % 10);
            }

            Optional<YTreeNode> maybeD = alteredRow.get("d");
            softly.assertThat(maybeD.isPresent()).isTrue();
            if (maybeD.isPresent()) {
                if (x < 50) {
                    softly.assertThat(maybeD.get().isStringNode()).isTrue();
                    softly.assertThat(maybeD.get().stringValue()).isEqualTo(String.valueOf(x % 3));
                } else {
                    softly.assertThat(maybeD.get().isEntityNode()).isTrue();
                }
            }

            Optional<YTreeNode> maybeW = alteredRow.get("w");
            softly.assertThat(maybeW.isPresent()).isTrue();
            if (maybeW.isPresent()) {
                softly.assertThat(maybeW.get().isStringNode()).isTrue();
                softly.assertThat(maybeW.get().stringValue()).isEqualTo(x < 50 ? "default" : String.valueOf(-x));
            }

            ++x;
        }
    }

    @Test
    public void testAlterNoRealModifyColumns() throws TimeoutException, InterruptedException {
        Yt yt = realYt.getYt();
        YtClient ytClient = realYt.getYtClient();
        YPath basePath = YPath.simple(realYt.getBasePath());
        YPath tablePath = basePath.child(TEST_SOURCE).child("unchanged_dynamic_table");
        YtAttrStateManager stateManager = new YtAttrStateManager(yt, basePath);

        TestTable table = new TestTable(
                tablePath.toString(),
                new TableSchema.Builder()
                        .addKey("x", ColumnValueType.INT64)
                        .addValue("y", ColumnValueType.STRING)
                        .build());

        table.createYtTable(ytClient);
        TabletUtils.waitForTablets(yt, tablePath, TabletUtils.TabletState.MOUNTED, Duration.ofSeconds(30));

        List<SchemaChange> schemaChanges = new ArrayList<>();
        schemaChanges.add(new CreateOrModifyColumn()
                .withColumnName("y")
                .withColumnType(ColumnType.TIMESTAMP)
                .withNullable(false));

        BinlogEvent schemaChange = new BinlogEvent()
                .withDb("test_db")
                .withOperation(Operation.SCHEMA)
                .withQueryIndex(0)
                .withServerUuid(SERVER_UUID)
                .withSource(TEST_SOURCE)
                .withTable("unchanged_dynamic_table")
                .withTransactionId(3)
                .withUtcTimestamp(TIMESTAMP)
                .withSchemaChanges(schemaChanges)
                .validate();

        AlterHandler alterHandler = new AlterHandler(yt, basePath, basePath);
        alterHandler.handle(schemaChange, stateManager.shardOffsetSaver(TEST_SOURCE, 123));

        softly.assertThat(stateManager.getShardOffset(TEST_SOURCE)).isEqualTo(Optional.of(123L));

        List<ColumnSchema> expectedSchema = ImmutableList.<ColumnSchema>builder()
                .add(new ColumnSchema.Builder("x", ColumnValueType.INT64)
                        .setSortOrder(ColumnSortOrder.ASCENDING)
                        .build())
                .add(new ColumnSchema.Builder("y", ColumnValueType.STRING).build())
                .build();

        TableSchema tableSchema = TableSchema.fromYTree(yt.cypress().get(tablePath.attribute(YtUtils.SCHEMA_ATTR)));

        checkSchema(tableSchema, expectedSchema);
    }

    @Test
    public void testAlterCreateTable() throws TimeoutException, InterruptedException {
        Yt yt = realYt.getYt();
        YPath basePath = YPath.simple(realYt.getBasePath());
        YPath tablePath = basePath.child(TEST_SOURCE).child("new_dynamic_table");
        YtAttrStateManager stateManager = new YtAttrStateManager(yt, basePath);

        List<SchemaChange> schemaChanges = new ArrayList<>();
        schemaChanges.add(new CreateTable()
                .withPrimaryKey(ImmutableList.of("x", "w"))
                .withAddedColumns(
                        new CreateOrModifyColumn()
                                .withColumnName("w")
                                .withColumnType(ColumnType.INTEGER)
                                .withNullable(false),
                        new CreateOrModifyColumn()
                                .withColumnName("x")
                                .withColumnType(ColumnType.INTEGER)
                                .withNullable(false),
                        new CreateOrModifyColumn()
                                .withColumnName("y")
                                .withColumnType(ColumnType.STRING)
                                .withNullable(false),
                        new CreateOrModifyColumn()
                                .withColumnName("z")
                                .withColumnType(ColumnType.FLOATING_POINT)
                                .withNullable(true)
                ));
        BinlogEvent schemaChange = new BinlogEvent()
                .withDb("test_db")
                .withOperation(Operation.SCHEMA)
                .withQueryIndex(0)
                .withServerUuid(SERVER_UUID)
                .withSource(TEST_SOURCE)
                .withTable("new_dynamic_table")
                .withTransactionId(4)
                .withUtcTimestamp(TIMESTAMP)
                .withSchemaChanges(schemaChanges)
                .validate();

        AlterHandler alterHandler = new AlterHandler(yt, basePath, basePath);
        alterHandler.handle(schemaChange, stateManager.shardOffsetSaver(TEST_SOURCE, 123));

        softly.assertThat(stateManager.getShardOffset(TEST_SOURCE)).isEqualTo(Optional.of(123L));

        boolean tableExists = yt.cypress().exists(tablePath);
        softly.assertThat(tableExists).isTrue();
        if (tableExists) {
            List<ColumnSchema> expectedSchema = ImmutableList.<ColumnSchema>builder()
                    .add(new ColumnSchema.Builder(HASH_COLUMN_NAME, ColumnValueType.INT64)
                            .setSortOrder(ColumnSortOrder.ASCENDING)
                            .setExpression(SchemaManager.getHashColumnExpression(List.of("x", "w"),
                                    YtReplicator.DEFAULT_PARTITIONS_COUNT))
                            .build())
                    .add(new ColumnSchema.Builder("x", ColumnValueType.INT64)
                            .setSortOrder(ColumnSortOrder.ASCENDING)
                            .build())
                    .add(new ColumnSchema.Builder("w", ColumnValueType.INT64)
                            .setSortOrder(ColumnSortOrder.ASCENDING)
                            .build())
                    .add(new ColumnSchema.Builder("y", ColumnValueType.STRING).build())
                    .add(new ColumnSchema.Builder("z", ColumnValueType.DOUBLE).build())
                    .add(new ColumnSchema.Builder(YtReplicator.SOURCE_COLUMN_NAME, ColumnValueType.STRING)
                                    .build())
                    .build();

            TableSchema tableSchema = TableSchema.fromYTree(yt.cypress().get(tablePath.attribute(YtUtils.SCHEMA_ATTR)));
            checkSchema(tableSchema, expectedSchema);

            softly.assertThat(yt.cypress().get(tablePath.attribute("chunk_row_count")).longValue()).isEqualTo(0);
        }
    }

    @Test
    public void testAlterDropTable() throws TimeoutException, InterruptedException {
        Yt yt = realYt.getYt();
        YtClient ytClient = realYt.getYtClient();
        YPath basePath = YPath.simple(realYt.getBasePath());
        YPath tablePath = basePath.child(TEST_SOURCE).child("old_dynamic_table");
        YtAttrStateManager stateManager = new YtAttrStateManager(yt, basePath);

        TestTable table = new TestTable(
                tablePath.toString(),
                new TableSchema.Builder()
                        .addKey("x", ColumnValueType.INT64)
                        .addValue("y", ColumnValueType.INT64)
                        .addValue("z", ColumnValueType.STRING)
                        .build());

        table.createYtTable(ytClient);

        List<SchemaChange> schemaChanges = new ArrayList<>();
        schemaChanges.add(new DropTable("old_dynamic_table"));
        BinlogEvent schemaChange = new BinlogEvent()
                .withDb("test_db")
                .withOperation(Operation.SCHEMA)
                .withQueryIndex(0)
                .withServerUuid(SERVER_UUID)
                .withSource(TEST_SOURCE)
                .withTable("old_dynamic_table")
                .withTransactionId(5)
                .withUtcTimestamp(TIMESTAMP)
                .withSchemaChanges(schemaChanges)
                .validate();

        boolean tableExists = yt.cypress().exists(tablePath);
        softly.assertThat(tableExists).isTrue();

        if (tableExists) {
            AlterHandler alterHandler = new AlterHandler(yt, basePath, basePath);
            alterHandler.handle(schemaChange, stateManager.shardOffsetSaver(TEST_SOURCE, 123));

            softly.assertThat(yt.cypress().exists(tablePath)).isFalse();

            softly.assertThat(stateManager.getShardOffset(TEST_SOURCE)).isEqualTo(Optional.of(123L));
        }
    }

    @Test
    public void testAlterRenameTable() throws TimeoutException, InterruptedException {
        Yt yt = realYt.getYt();
        YtClient ytClient = realYt.getYtClient();
        YPath basePath = YPath.simple(realYt.getBasePath());
        YPath shardPath = basePath.child(TEST_SOURCE);
        YtAttrStateManager stateManager = new YtAttrStateManager(yt, basePath);

        YPath t1Path = shardPath.child("rename_dynamic_table_1");
        YPath t2Path = shardPath.child("rename_dynamic_table_2");
        YPath t3Path = shardPath.child("rename_dynamic_table_3");
        YPath t4Path = shardPath.child("rename_dynamic_table_4");
        YPath tmpPath = shardPath.child("rename_dynamic_table_tmp");

        TestTable t1 = new TestTable(
                t1Path.toString(),
                new TableSchema.Builder()
                        .addKey("x1", ColumnValueType.INT64)
                        .addValue("y1", ColumnValueType.INT64)
                        .build());
        TestTable t2 = new TestTable(
                t2Path.toString(),
                new TableSchema.Builder()
                        .addKey("x2", ColumnValueType.INT64)
                        .addValue("y2", ColumnValueType.INT64)
                        .build());
        TestTable t3 = new TestTable(
                t3Path.toString(),
                new TableSchema.Builder()
                        .addKey("x3", ColumnValueType.INT64)
                        .addValue("y3", ColumnValueType.INT64)
                        .build());

        t1.createYtTable(ytClient);
        t2.createYtTable(ytClient);
        t3.createYtTable(ytClient);

        List<SchemaChange> schemaChanges = new ArrayList<>();
        schemaChanges.add(new RenameTable()
                .withAddRename(t1Path.name(), tmpPath.name())
                .withAddRename(t2Path.name(), t1Path.name())
                .withAddRename(tmpPath.name(), t2Path.name())
                .withAddRename(t3Path.name(), t4Path.name()));
        BinlogEvent schemaChange = new BinlogEvent()
                .withDb("test_db")
                .withOperation(Operation.SCHEMA)
                .withQueryIndex(0)
                .withServerUuid(SERVER_UUID)
                .withSource(TEST_SOURCE)
                .withTable("rename_dynamic_table_1")
                .withTransactionId(6)
                .withUtcTimestamp(TIMESTAMP)
                .withSchemaChanges(schemaChanges)
                .validate();

        AlterHandler alterHandler = new AlterHandler(yt, basePath, basePath);
        alterHandler.handle(schemaChange, stateManager.shardOffsetSaver(TEST_SOURCE, 123));

        softly.assertThat(stateManager.getShardOffset(TEST_SOURCE)).isEqualTo(Optional.of(123L));

        boolean t1Exists = yt.cypress().exists(t1Path);
        softly.assertThat(t1Exists).isTrue();
        if (t1Exists) {
            TableSchema tableSchema = TableSchema.fromYTree(yt.cypress().get(t1Path.attribute(YtUtils.SCHEMA_ATTR)));
            softly.assertThat(tableSchema).isEqualTo(t2.getSchema());
            softly.assertThat(TabletUtils.getTableState(yt, t1Path)).isEqualTo(TabletUtils.TabletState.MOUNTED);
        }

        boolean t2Exists = yt.cypress().exists(t2Path);
        if (t2Exists) {
            TableSchema tableSchema = TableSchema.fromYTree(yt.cypress().get(t2Path.attribute(YtUtils.SCHEMA_ATTR)));
            softly.assertThat(tableSchema).isEqualTo(t1.getSchema());
            softly.assertThat(TabletUtils.getTableState(yt, t2Path)).isEqualTo(TabletUtils.TabletState.MOUNTED);
        }

        softly.assertThat(yt.cypress().exists(tmpPath)).isFalse();

        softly.assertThat(yt.cypress().exists(t3Path)).isFalse();

        boolean t4Exists = yt.cypress().exists(t4Path);
        softly.assertThat(t4Exists).isTrue();
        if (t4Exists) {
            TableSchema tableSchema = TableSchema.fromYTree(yt.cypress().get(t4Path.attribute(YtUtils.SCHEMA_ATTR)));
            softly.assertThat(tableSchema).isEqualTo(t3.getSchema());
            softly.assertThat(TabletUtils.getTableState(yt, t4Path)).isEqualTo(TabletUtils.TabletState.MOUNTED);
        }
    }

    private void checkSchema(TableSchema tableSchema, List<ColumnSchema> expectedSchema) {
        List<ColumnSchema> actualSchema = tableSchema.getColumns();
        softly.assertThat(actualSchema).isEqualTo(expectedSchema);
        softly.assertThat(tableSchema.isStrict()).isTrue();
        softly.assertThat(tableSchema.isUniqueKeys()).isTrue();
    }
}
