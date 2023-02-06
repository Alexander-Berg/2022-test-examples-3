package ru.yandex.direct.binlogbroker.replicatetoyt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.yt.rpcproxy.EAtomicity;
import ru.yandex.yt.rpcproxy.ETransactionType;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransactionOptions;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;

import static com.google.common.collect.ImmutableMap.of;

/**
 * создает тестовые таблицы в "//home/direct/tmp/${username}" (важно, чтобы по данному пути было дано право
 * mount table),
 * берет токен для доступа в YT из ~/.yt/token
 * Пока предназначено только для ручного прогона.
 * В дальнейшем будет адаптировано для CI
 * TODO(semals) проверить вставку с неполным набором колонок
 * TODO(semals) проверить удаление с неполным набором колонок в primary key
 */
@Ignore
@ParametersAreNonnullByDefault
public class TableDataYtReplicatorTest {
    @ClassRule
    public static JunitRealYt realYt = new JunitRealYt(YtCluster.HAHN);
    private static final String SCN = YtReplicator.SOURCE_COLUMN_NAME;

    private static YtClient client;
    private static String basePath;

    public static ApiServiceTransactionOptions transactionOptions =
            new ApiServiceTransactionOptions(ETransactionType.TT_MASTER)
                    // Все настройки, кроме sticky, взяты с потолка. Любой желающий может вынести
                    // в поля и/или переопределить.
                    .setAtomicity(EAtomicity.A_FULL)
                    .setPing(true)
                    .setPingPeriod(Duration.ofSeconds(2))
                    .setSticky(true)
                    .setTimeout(Duration.ofSeconds(10));

    public static final TableSchema SIMPLE_SCHEMA = new TableSchema.Builder()
            .addKey(SCN, ColumnValueType.STRING)
            .addKey("ID", ColumnValueType.INT64)
            .addValue("name", ColumnValueType.STRING)
            .build();

    public static final TableSchema ALL_TYPES_SCHEMA = new TableSchema.Builder()
            .addKey(SCN, ColumnValueType.STRING)
            .addKey("ID", ColumnValueType.INT64)
            .addValue("str", ColumnValueType.STRING)
            .addValue("bytes", ColumnValueType.STRING)
            .addValue("date", ColumnValueType.STRING)
            .addValue("date-time", ColumnValueType.STRING)
            .addValue("big-int", ColumnValueType.STRING)
            .addValue("big-decimal", ColumnValueType.STRING)
            .addValue("long", ColumnValueType.INT64)
            .addValue("int", ColumnValueType.INT64)
            .build();

    public static final TableSchema BLOB_SCHEMA = new TableSchema.Builder()
            .addKey(SCN, ColumnValueType.STRING)
            .addKey("ID", ColumnValueType.INT64)
            .addValue("bytes", ColumnValueType.STRING)
            .build();

    public static final TableSchema SIMPLE_SCHEMA_2 = new TableSchema.Builder()
            .addKey(SCN, ColumnValueType.STRING)
            .addKey("ID", ColumnValueType.INT64)
            .addValue("name", ColumnValueType.STRING)
            .addValue("age", ColumnValueType.INT64)
            .build();


    /**
     * шаблоны
     */
    public static final BinlogEvent INSERT = new BinlogEvent()
            .withServerUuid("abc")
            .withSource("src")
            .withDb("db")
            .withTransactionId(123L)
            .withUtcTimestamp(LocalDateTime.now())
            .withQueryIndex(456)
            .withOperation(Operation.INSERT);
    public static final BinlogEvent UPDATE = new BinlogEvent()
            .withServerUuid("abc")
            .withSource("src")
            .withDb("db")
            .withTransactionId(123L)
            .withUtcTimestamp(LocalDateTime.now())
            .withQueryIndex(456)
            .withOperation(Operation.UPDATE);
    public static final BinlogEvent DELETE = new BinlogEvent()
            .withServerUuid("abc")
            .withSource("src")
            .withDb("db")
            .withTransactionId(123L)
            .withUtcTimestamp(LocalDateTime.now())
            .withQueryIndex(456)
            .withOperation(Operation.DELETE);


    public static final List<Map<String, Object>> TEST_ROW = Arrays.asList(
            of(SCN, "src", "ID", 1, "name", "Yandex")
    );
    public static final List<Map<String, Object>> TEST_ROWS = Arrays.asList(
            of(SCN, "src", "ID", 1, "name", "Yandex.search"),
            of(SCN, "src", "ID", 2, "name", "Yandex.taxi"),
            of(SCN, "src", "ID", 3, "name", "Yandex.money"),
            of(SCN, "src", "ID", 4, "name", "Yandex.zen"),
            of(SCN, "src", "ID", 5, "name", "Yandex.music")
    );

    @BeforeClass
    public static void beforeClass() {
        client = realYt.getYtClient();
        basePath = realYt.getBasePath();
    }

    private void testInsert(String tableName, TableSchema schema, List<Map<String, Object>> rows,
                            int batchCount) throws TimeoutException, InterruptedException {
        final TestTable table = new TestTable(basePath + "/" + tableName, schema);
        table.createYtTable(client);
        table.addRows(rows);
        final TableDataYtReplicator tableDataYtReplicator = new TableDataYtReplicator(client, table.getNode(), false);
        tableDataYtReplicator.processEventBatch(table.toBinlogEvent(INSERT, batchCount), transactionOptions);
        table.verifyYtRows(client);
    }

    @Test
    public void acceptInsert1() throws TimeoutException, InterruptedException {
        testInsert("acceptInsert1", SIMPLE_SCHEMA, TEST_ROW, 1);
    }

    @Test
    public void acceptInsert5() throws TimeoutException, InterruptedException {
        testInsert("acceptInsert5", SIMPLE_SCHEMA, TEST_ROWS, 3);
    }

    @Test
    public void acceptInsertAllTypes() throws TimeoutException, InterruptedException {
        final List<Map<String, Object>> row = Arrays.asList(
                ImmutableMap.<String, Object>builder()
                        .put(SCN, "src")
                        .put("ID", 1)
                        .put("str", "str")
                        .put("bytes", new byte[]{1, 2, 3, 4, 5})
                        .put("date", LocalDate.now())
                        .put("date-time", LocalDateTime.now())
                        .put("big-int", new BigInteger("10"))
                        .put("big-decimal", new BigDecimal("1234.5678"))
                        .put("long", 987654321L)
                        .put("int", 7777)
                        .build()
        );

        testInsert("all-types-insert", ALL_TYPES_SCHEMA, row, 1);
    }

    /**
     * Вставка события в несуществующую таблицу приводит к Exception
     */
    @Test(expected = java.util.concurrent.CompletionException.class)
    public void acceptInsertNoTable() {
        final TestTable table = new TestTable(basePath + "/simple-non-existing", SIMPLE_SCHEMA);
        table.addRows(TEST_ROW);

        final TableDataYtReplicator tableDataYtReplicator = new TableDataYtReplicator(client, table.getNode(), false);
        tableDataYtReplicator.processEventBatch(table.toBinlogEvent(INSERT, 1), transactionOptions);
    }

    /**
     * Вставка уже существующих строк приводит к их обновлению. Вставляем те же самые строки
     */
    @Test
    public void acceptInsertUpdate1() throws TimeoutException, InterruptedException {
        final TestTable table = new TestTable(basePath + "/acceptInsertUpdate1", SIMPLE_SCHEMA);
        table.createYtTable(client);
        table.addRows(TEST_ROWS);
        table.insertYtRows(client);
        final TableDataYtReplicator tableDataYtReplicator = new TableDataYtReplicator(client, table.getNode(), false);
        tableDataYtReplicator.processEventBatch(table.toBinlogEvent(INSERT, 3), transactionOptions);
        table.verifyYtRows(client);
    }

    /**
     * Вставка уже существующих строк приводит к их обновлению. Вставляем строки с тем же primary key,
     * но другими остальными значениями
     */
    @Test
    public void acceptInsertUpdate2() throws TimeoutException, InterruptedException {
        final TestTable table = new TestTable(basePath + "/acceptInsertUpdate2", SIMPLE_SCHEMA);
        table.createYtTable(client);
        table.addRows(TEST_ROWS);
        table.insertYtRows(client);
        table.mutateRows(r -> r.setValue("name", ((String) r.getValue("name")).toUpperCase()));

        final TableDataYtReplicator tableDataYtReplicator = new TableDataYtReplicator(client, table.getNode(), false);
        tableDataYtReplicator.processEventBatch(table.toBinlogEvent(INSERT, 3), transactionOptions);
        table.verifyYtRows(client);
    }

    @Test
    public void acceptInsertBlob() throws TimeoutException, InterruptedException {
        testInsert("blobs", BLOB_SCHEMA,
                Arrays.asList(
                        of(SCN, "src", "ID", 1, "bytes", new byte[]{1, 2, 3, 4, 5})
                ),
                1
        );
    }

    private void testDelete(String tableName, TableSchema schema, List<Map<String, Object>> rows,
                            Predicate<TestRow> deleteSelector, Function<TestRow, TestRow> rowMutator, int batchCount)
            throws TimeoutException, InterruptedException {
        final TestTable table = new TestTable(basePath + "/" + tableName, schema);
        table.createYtTable(client);
        table.addRows(rows);
        table.insertYtRows(client);

        final TableDataYtReplicator tableDataYtReplicator = new TableDataYtReplicator(client, table.getNode(), false);
        tableDataYtReplicator.processEventBatch(
                table.toBinlogEvent(DELETE, r -> deleteSelector.test(r) ? rowMutator.apply(r) : null, batchCount),
                transactionOptions);

        table.verifyYtRows(client, deleteSelector.negate());

    }


    @Test
    public void acceptDelete1() throws TimeoutException, InterruptedException {
        // удаляем одну строку
        testDelete("acceptDelete1", SIMPLE_SCHEMA, TEST_ROW, x -> true, r -> r, 1);
    }

    private boolean hasOddId(TestRow row) {
        return ((Number) row.getValue("ID")).longValue() % 2 == 1;
    }

    @Test
    public void acceptDelete5() throws TimeoutException, InterruptedException {
        // удаляем строки с нечетным ID используя primaryKey
        testDelete("acceptDelete5", SIMPLE_SCHEMA, TEST_ROWS, this::hasOddId, TestRow::primaryKey, 2);
    }

    /**
     * удаление не существующей строки ничего не делает
     */
    @Test
    public void acceptDeleteNonExisting() throws TimeoutException, InterruptedException {
        final TestRow notExistingRow = new TestRow(SIMPLE_SCHEMA, of(SCN, "src", "ID", 2, "name", "ZZZ"));
        final TestTable table = new TestTable(basePath + "/acceptDeleteNonExisting", SIMPLE_SCHEMA);
        table.createYtTable(client, TEST_ROW);
        final TableDataYtReplicator tableDataYtReplicator = new TableDataYtReplicator(client, table.getNode(), false);
        tableDataYtReplicator
                .processEventBatch(table.toBinlogEvent(DELETE, r -> notExistingRow, 1), transactionOptions);
        table.verifyYtRows(client);
    }


    private void testUpdate(String tableName, TableSchema schema, List<Map<String, Object>> rows,
                            Function<TestRow, TestRow> rowMutator, int batchCount
    ) throws TimeoutException, InterruptedException {
        final TestTable table = new TestTable(basePath + "/" + tableName, schema);
        table.createYtTable(client, rows);

        final TableDataYtReplicator tableDataYtReplicator = new TableDataYtReplicator(client, table.getNode(), false);
        tableDataYtReplicator
                .processEventBatch(table.toBinlogEvent(UPDATE, rowMutator, batchCount), transactionOptions);

        table.updateRows(rowMutator);
        table.verifyYtRows(client);
    }

    @Test
    public void acceptUpdate1() throws TimeoutException, InterruptedException {
        testUpdate("acceptUpdate1", SIMPLE_SCHEMA, TEST_ROW, r -> r.setValue("name", "Xednay"), 1);
    }

    @Test
    public void acceptUpdate5() throws TimeoutException, InterruptedException {
        testUpdate("acceptUpdate5", SIMPLE_SCHEMA, TEST_ROWS, r -> r.setValue("name", "Updated " + r.getValue("name")),
                3);
    }

    /**
     * обновление несуществующей строки приводит к ее вставке (не существующая строка содержит все колонки)
     */
    @Test
    public void acceptUpdateNotExisting1() throws TimeoutException, InterruptedException {
        final ImmutableMap<String, Object> notExistingRow = of(SCN, "src", "ID", 2, "name", "ZZZ");
        final TestTable table = new TestTable(basePath + "/acceptUpdateNotExisting1", SIMPLE_SCHEMA);
        table.createYtTable(client, TEST_ROW);

        final TableDataYtReplicator tableDataYtReplicator = new TableDataYtReplicator(client, table.getNode(), false);
        tableDataYtReplicator
                .processEventBatch(table.toBinlogEvent(UPDATE, r -> new TestRow(SIMPLE_SCHEMA, notExistingRow), 1),
                        transactionOptions);

        table.addRow(notExistingRow); // в YT теперь две строки
        table.verifyYtRows(client);
    }

    /**
     * Обновление несуществующей строки с данными не для всех колонок ничего не делает
     * (так ведет себя YT)
     */
    @Test
    public void acceptUpdateNotExisting2() throws TimeoutException, InterruptedException {
        final TestTable table = new TestTable(basePath + "/acceptUpdateNotExisting2", SIMPLE_SCHEMA_2);
        table.createYtTable(client, Arrays.asList(
                of(SCN, "cde", "ID", 0, "name", "John", "age", 20),
                of(SCN, "cde", "ID", 1, "name", "Bob", "age", 40)
        ));

        final List<Map<String, Object>> notExistingRows = Arrays.asList(
                of(SCN, "cde", "ID", 2, "name", "Alice"),
                of(SCN, "cde", "ID", 3, "age", 25)
        );

        final TableDataYtReplicator tableDataYtReplicator = new TableDataYtReplicator(client, table.getNode(), false);
        tableDataYtReplicator.processEventBatch(table.toBinlogEvent(UPDATE,
                r -> new TestRow(SIMPLE_SCHEMA_2, notExistingRows.get((Integer) r.getValue("ID"))),
                2), transactionOptions);

        table.verifyYtRows(client);
    }
}
