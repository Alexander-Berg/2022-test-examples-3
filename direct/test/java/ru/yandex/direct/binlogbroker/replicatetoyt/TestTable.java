package ru.yandex.direct.binlogbroker.replicatetoyt;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlogbroker.logbroker_utils.models.BinlogEventWithOffset;
import ru.yandex.direct.utils.MonotonicTime;
import ru.yandex.direct.utils.NanoTimeClock;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.rpcproxy.ETransactionType;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransactionOptions;
import ru.yandex.yt.ytclient.proxy.LookupRowsRequest;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;
import ru.yandex.yt.ytclient.proxy.request.RemoveNode;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;

import static org.junit.Assert.assertEquals;

/**
 * Хранит внутри себя исходные тестовые данные для одной таблицы,
 * предоставляет вспомогательные методы для:
 * - (пере)создания таблицы в YT,
 * - создания {@link BinlogEvent}
 * - проверки наличия данных в таблице в YT
 */
@ParametersAreNonnullByDefault
public class TestTable implements Iterable<TestRow> {

    private final String node;
    private final TableSchema schema;
    private final List<TestRow> rows = new ArrayList<>();

    public TestTable(String node, TableSchema schema) {
        this.node = node;
        this.schema = schema;
    }

    public TestTable(TestTable testTable, TableSchema newSchema, Consumer<Map<String, Object>> valuesConsumer) {
        this(testTable.node, newSchema);
        this.rows.addAll(testTable.rows.stream()
                .map(r -> {
                    Map<String, Object> values = r.getValues();
                    valuesConsumer.accept(values);
                    return new TestRow(newSchema, values);
                })
                .collect(Collectors.toList()));
    }

    public TestTable(String node, String key, ColumnValueType keyType,
                     String value, ColumnValueType valueType) {
        this(node, new TableSchema.Builder().addKey(key, keyType).addValue(value, valueType).build());
    }

    public TestTable(String node, String key1, ColumnValueType keyType1,
                     String key2, ColumnValueType keyType2,
                     String value, ColumnValueType valueType) {
        this(node, new TableSchema.Builder()
                .addKey(key1, keyType1)
                .addKey(key2, keyType2)
                .addValue(value, valueType)
                .build());
    }

    @SuppressWarnings("checkstyle:parameternumber")
    public TestTable(String node, String key1, ColumnValueType keyType1,
                     String key2, ColumnValueType keyType2,
                     String value1, ColumnValueType valueType1,
                     String value2, ColumnValueType valueType2) {
        this(node, new TableSchema.Builder()
                .addKey(key1, keyType1)
                .addKey(key2, keyType2)
                .addValue(value1, valueType1)
                .addValue(value2, valueType2)
                .build());
    }

    /**
     * @return полный путь ноды в YT соответствующей данной таблице
     */
    public String getNode() {
        return node;
    }

    /**
     * @return схему таблицы в YT представлении
     */
    public TableSchema getSchema() {
        return schema;
    }

    /**
     * создает динамическую таблицу в YT
     */
    public GUID createYtTable(YtClient client) throws TimeoutException, InterruptedException {
        ApiServiceTransactionOptions transactionOptions =
                new ApiServiceTransactionOptions(ETransactionType.TT_MASTER)
                        .setTimeout(Duration.ofSeconds(10))
                        .setSticky(true);
        try (ApiServiceTransaction t = client.startTransaction(transactionOptions).join()) {
            final CreateNode request = new CreateNode(node, ObjectType.Table).setRecursive(true);
            request.addAttribute("dynamic", YTree.booleanNode(true));
            request.addAttribute("schema", schema.toYTree());

            GUID guid = t.createNode(request).join();
            t.commit().join();
            mountTablets(client);
            return guid;
        }
    }

    private void mountTablets(YtClient client) throws InterruptedException, TimeoutException {
        client.mountTable(node).join();
        NanoTimeClock clock = new NanoTimeClock();
        MonotonicTime deadline = clock.getTime().plus(Duration.ofMinutes(1));
        do {
            String tabletState = client.getNode(node + "/@tablet_state").join().stringValue();
            if ("mounted".equals(tabletState)) {
                return;
            }
            clock.sleep(Duration.ofSeconds(1));
        } while (clock.getTime().isBefore(deadline));
        throw new TimeoutException("Failed to mount tablets of table " + node);
    }

    /**
     * создает динамическую таблицу в YT и наполняет ее указанными данными
     */
    public GUID createYtTable(YtClient client, List<Map<String, Object>> rows)
            throws TimeoutException, InterruptedException {
        final GUID guid = createYtTable(client);
        addRows(rows);
        insertYtRows(client);
        return guid;
    }

    /**
     * Добавляет строку данных во внутреннее представление.
     *
     * @param row данные строки в виде мапы имя_колонки->значение
     */
    public void addRow(Map<String, Object> row) {
        rows.add(new TestRow(schema, row));
    }

    /**
     * добавляет строки данных во внутреннее представление.
     *
     * @param rows данные строки в виде списка мап имя_колонки->значение
     */
    public void addRows(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            addRow(row);
        }
    }

    /**
     * записывает строки из внутреннего представления в YT таблицу
     *
     * @param client клиент для работы с YT
     */
    public void insertYtRows(YtClient client) {
        ApiServiceTransactionOptions transactionOptions =
                new ApiServiceTransactionOptions(ETransactionType.TT_MASTER)
                        .setSticky(true);
        try (ApiServiceTransaction t = client.startTransaction(transactionOptions).join()) {
            final ModifyRowsRequest modifyRowsRequest = new ModifyRowsRequest(node, schema.toWrite());
            for (TestRow row : rows) {
                modifyRowsRequest.addInsert(row.toTreeNode().asMap());
            }
            t.modifyRows(modifyRowsRequest).join();
            t.commit().join();
        }
    }

    @Override
    public Iterator<TestRow> iterator() {
        return rows.iterator();
    }

    /**
     * Проверяет, что YT таблица содержит строки, выбранные из внутреннего представления предикатом expectedRowsSelector
     *
     * @param client               клиент для работы с YT
     * @param expectedRowsSelector выбирает какие строки сравнивать
     */
    public void verifyYtRows(YtClient client, Predicate<TestRow> expectedRowsSelector) {
        final List<YTreeNode> expectedRows = rows.stream()
                .filter(expectedRowsSelector)
                .map(TestRow::toTreeNode)
                .collect(Collectors.toList());
        assertEquals(expectedRows, fetchYtRows(client));
    }

    /**
     * проверяет что YT таблица содержит все строки из внутреннего представления.
     *
     * @param client клиент для работы с YT
     */
    public void verifyYtRows(YtClient client) {
        verifyYtRows(client, x -> true);
    }


    /**
     * извлекает данные из YT таблицы по primary key взятому из строк внутренного представляения,
     * выбранных предикатом rowSelector
     *
     * @param client      клиент для работы с YT
     * @param rowSelector выбирает какие строки извлекать
     * @return список строк (возможно пустой)
     */
    public List<YTreeMapNode> fetchYtRows(YtClient client, Predicate<TestRow> rowSelector) {
        final LookupRowsRequest lookupRowsRequest = new LookupRowsRequest(node, schema.toLookup());
        lookupRowsRequest.setKeepMissingRows(false); // на случай, если поменяют значение по умолчанию
        for (TestRow row : rows) {
            if (rowSelector.test(row)) {
                lookupRowsRequest.addFilter(row.getLookupFilter());
            }
        }
        return client.lookupRows(lookupRowsRequest).join().getYTreeRows();
    }

    /**
     * извлекает данные из YT таблицы по primary key взятому из строк внутренного представляения
     *
     * @param client клиент для работы с YT
     * @return список строк (возможно пустой)
     */
    public List<YTreeMapNode> fetchYtRows(YtClient client) {
        return fetchYtRows(client, x -> true);
    }

    /**
     * Преобразует данные таблицы в один BinlogEvent
     *
     * @param template шаблон для заполнения полей типа serverUuid, source, db, etc
     * @return BinlogEvent
     */
    public BinlogEvent toBinlogEvent(BinlogEvent template) {
        return toBinlogEvent(template, x -> x, 0, rows.size());
    }

    /**
     * Создает BinlogEvent на основе подмножества строк таблицы
     *
     * @param template шаблон для заполнения полей типа serverUuid, source, db, etc
     * @param offset   - индекс первой строки
     * @param length   - количество строк
     * @return BinlogEvent
     */
    public BinlogEvent toBinlogEvent(BinlogEvent template, int offset, int length) {
        return toBinlogEvent(template, x -> x, offset, length);
    }

    /**
     * Создает BinlogEvent на основе подмножества измененых строк таблицы
     *
     * @param template  шаблон для заполнения полей типа serverUuid, source, db, etc
     * @param rowMapper мутатор строк, если возвращает null, строка будет игнорирована
     * @param offset    - индекс первой строки
     * @param length    - количество строк
     * @return BinlogEvent
     */
    public BinlogEvent toBinlogEvent(BinlogEvent template, Function<TestRow, TestRow> rowMapper, int offset,
                                     int length) {
        final BinlogEvent event = new BinlogEvent();
        event.setServerUuid(template.getServerUuid());
        event.setSource(template.getSource());
        event.setDb(template.getDb());
        event.setTable(node.substring(Math.max(0, node.lastIndexOf('/')), node.length()));
        event.setOperation(template.getOperation());
        event.setTransactionId(template.getTransactionId());
        event.setUtcTimestamp(template.getUtcTimestamp());
        event.setQueryIndex(template.getQueryIndex());
        event.setRows(
                rows.subList(offset, Math.min(offset + length, rows.size())).stream()
                        .map(rowMapper)
                        .filter(Objects::nonNull)
                        .map(TestRow::toBinlogEventRow)
                        .collect(Collectors.toList())
        );
        event.setTraceInfoReqId(template.getTraceInfoReqId());
        event.setTraceInfoService(template.getTraceInfoService());
        event.setTraceInfoMethod(template.getTraceInfoMethod());
        event.setTraceInfoOperatorUid(template.getTraceInfoOperatorUid());
        event.validate();
        return event;
    }

    /**
     * Создает список BinlogEvent'ов на основе данных таблицы. В каждый BinlogEvent попадает
     * не более rowsPerEvent строк.
     *
     * @param template     шаблон для заполнения полей типа serverUuid, source, db, etc
     * @param rowsPerEvent количество строк для упаковки в один BinlogEvent
     * @return список BinlogEvent
     */
    public List<BinlogEvent> toBinlogEvent(BinlogEvent template, int rowsPerEvent) {
        return toBinlogEvent(template, x -> x, rowsPerEvent);
    }

    /**
     * Создает список BinlogEvent'ов на основе измененных данных таблицы.
     * В каждый BinlogEvent попадает не более rowsPerEvent строк.
     *
     * @param template     шаблон для заполнения полей типа serverUuid, source, db, etc
     * @param rowMapper    мутатор строк, если возвращает null, строка будет игнорирована
     * @param rowsPerEvent количество строк для упаковки в один BinlogEvent
     * @return список BinlogEvent
     */
    public List<BinlogEvent> toBinlogEvent(BinlogEvent template, Function<TestRow, TestRow> rowMapper,
                                           int rowsPerEvent) {
        final ArrayList<BinlogEvent> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i += rowsPerEvent) {
            result.add(toBinlogEvent(template, rowMapper, i, rowsPerEvent));
        }
        return result;
    }

    private List<BinlogEventWithOffset> addOffset(List<BinlogEvent> events, int firstOffset) {
        List<BinlogEventWithOffset> result = new ArrayList<>();
        for (BinlogEvent event : events) {
            result.add(new BinlogEventWithOffset(event, firstOffset++, 0, 0L));
        }
        return result;
    }

    public List<BinlogEventWithOffset> toBinlogEventWithOffset(BinlogEvent template, int rowsPerEvent,
                                                               int firstOffset) {
        List<BinlogEvent> events = toBinlogEvent(template, rowsPerEvent);
        return addOffset(events, firstOffset);
    }

    public List<BinlogEventWithOffset> toBinlogEventWithOffset(BinlogEvent template,
                                                               Function<TestRow, TestRow> rowMapper,
                                                               int rowsPerEvent, int firstOffset) {
        List<BinlogEvent> events = toBinlogEvent(template, rowMapper, rowsPerEvent);
        return addOffset(events, firstOffset);
    }

    /**
     * Изменяет данные внутреннего представления.
     *
     * @param mutator мутатор строк, если возвращает null, строка будет удалена
     */
    public void mutateRows(Function<TestRow, TestRow> mutator) {
        final List<TestRow> mutatedRows =
                rows.stream().map(mutator).filter(Objects::nonNull).collect(Collectors.toList());
        rows.clear();
        rows.addAll(mutatedRows);
    }

    /**
     * Изменяет данные внутреннего представления. Дополняет значения отсутствующих колонок в новых строках
     * соответствующими данными из старых строк.
     *
     * @param mutator мутатор строк, если возвращает null, строка будет удалена
     */
    public void updateRows(Function<TestRow, TestRow> mutator) {
        mutateRows(r -> r.update(mutator));
    }

    public LookupRowsRequest lookupRowsRequest() {
        return new LookupRowsRequest(getNode(), getSchema().toLookup());
    }

    /**
     * Удаление таблицы, отсутствие игнорируется
     */
    public void removeYtTable(YtClient client) {
        ApiServiceTransactionOptions transactionOptions =
                new ApiServiceTransactionOptions(ETransactionType.TT_MASTER)
                        .setTimeout(Duration.ofSeconds(10))
                        .setSticky(true);
        try (ApiServiceTransaction t = client.startTransaction(transactionOptions).join()) {
            final RemoveNode request = new RemoveNode(node).setForce(true);

            t.removeNode(request).join();
            t.commit().join();
        }
    }
}
