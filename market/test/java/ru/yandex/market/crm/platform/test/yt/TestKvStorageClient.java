package ru.yandex.market.crm.platform.test.yt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.platform.yt.KvStorageClient;
import ru.yandex.market.crm.platform.yt.YTreeObjectMapper;
import ru.yandex.market.crm.platform.yt.YtTable;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.tables.ColumnSchema;
import ru.yandex.yt.ytclient.tables.TableSchema;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
public class TestKvStorageClient implements KvStorageClient {

    private static final CompletableFuture<?> ERROR_FUTURE;

    static {
        ERROR_FUTURE = new CompletableFuture<>();
        ERROR_FUTURE.completeExceptionally(new RuntimeException("Something wrong with storage"));
    }

    private final Map<String, List<YTreeMapNode>> tables = new ConcurrentHashMap<>();
    private final Map<String, List<YTreeMapNode>> queryResults = new ConcurrentHashMap<>();
    private volatile boolean isOk = true;

    private static <T> CompletableFuture<T> errorFuture() {
        return (CompletableFuture<T>) ERROR_FUTURE;
    }

    @Override
    public <T> CompletableFuture<List<T>> get(YPath path,
                                              TableSchema schema,
                                              Iterable<List<?>> keys,
                                              YTreeObjectMapper<T> mapper) {
        if (!isOk) {
            return errorFuture();
        }

        List<YTreeMapNode> rows = tables.get(path.toString());

        if (CollectionUtils.isEmpty(rows)) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        List<ColumnSchema> lookupColumns = schema.getColumns();

        List<YTreeMapNode> nodeKeys = StreamSupport.stream(keys.spliterator(), false)
                .map(key -> toMapNode(lookupColumns, key))
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(
                rows.stream()
                        .filter(row -> rowSatisfies(row, schema, nodeKeys))
                        .map(mapper::map)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public CompletableFuture<?> put(ApiServiceTransaction tx, YPath path, TableSchema schema, List<?> row) {
        if (!isOk) {
            return errorFuture();
        }
        put(path, schema, toMapNode(schema.getColumns(), row));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<?> putAll(ApiServiceTransaction tx, YPath path, TableSchema schema,
                                       Iterable<List<?>> rows) {
        if (!isOk) {
            return errorFuture();
        }
        for (List<?> row : rows) {
            put(path, schema, toMapNode(schema.getColumns(), row));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <T> CompletableFuture<List<T>> select(YPath table, String query, YTreeObjectMapper<T> mapper) {
        if (!isOk) {
            return errorFuture();
        }

        return CompletableFuture.completedFuture(
                queryResults.getOrDefault(query, Collections.emptyList()).stream()
                        .map(mapper::map)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public <T> CompletableFuture<T> doInTx(Function<ApiServiceTransaction, CompletableFuture<T>> callback) {
        ApiServiceTransaction apiServiceTransaction = mock(ApiServiceTransaction.class);
        return callback.apply(apiServiceTransaction);
    }

    @Override
    public <T> CompletableFuture<T> doInTx(boolean repeatOnConflict,
                                           Function<ApiServiceTransaction, CompletableFuture<T>> callback) {
        return doInTx(callback);
    }

    public void add(YtTable table, YTreeMapNode value) {
        put(table.getPath(), table.getSchema(), value);
    }

    public void put(YPath path, TableSchema schema, YTreeMapNode value) {
        List<YTreeMapNode> rows = tables.computeIfAbsent(path.toString(), v -> new ArrayList<>());

        TableSchema keys = schema.toKeys();

        ListIterator<YTreeMapNode> i = rows.listIterator();
        while (i.hasNext()) {
            YTreeMapNode row = i.next();
            if (rowSatisfies(row, keys, Collections.singleton(value))) {
                i.set(value);
                return;
            }
        }
        rows.add(value);
    }

    public void onQuery(String query, List<YTreeMapNode> result) {
        queryResults.computeIfAbsent(query, x -> new ArrayList<>()).addAll(result);
    }

    public void clear() {
        tables.clear();
        queryResults.clear();
        isOk = true;
    }

    public List<YTreeMapNode> getRows(String path) {
        return tables.getOrDefault(path, Collections.emptyList());
    }

    public void mockMalfunction() {
        isOk = false;
    }

    private YTreeMapNode toMapNode(List<ColumnSchema> columns, List<?> values) {
        YTreeBuilder builder = YTree.mapBuilder();
        for (int i = 0; i < columns.size(); ++i) {
            builder.key(columns.get(i).getName()).value(YTree.builder().value(values.get(i)).build());
        }
        return builder.buildMap();
    }

    private boolean rowSatisfies(YTreeMapNode row, TableSchema schema, Iterable<YTreeMapNode> keys) {
        List<ColumnSchema> columns = schema.getColumns();
        return StreamSupport.stream(keys.spliterator(), false)
                .anyMatch(key -> columns.stream()
                        .map(ColumnSchema::getName)
                        .allMatch(name -> Objects.equals(
                                key.get(name).orElse(null),
                                row.get(name).orElse(null)
                        )));
    }
}
