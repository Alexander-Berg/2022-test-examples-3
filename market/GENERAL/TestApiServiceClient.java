package ru.yandex.market.mbo.ytclient;

import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeBinarySerializer;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.yt.rpcproxy.ERowModificationType;
import ru.yandex.yt.ytclient.proxy.ApiServiceClientImpl;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransactionOptions;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;
import ru.yandex.yt.ytclient.proxy.request.StartTransaction;
import ru.yandex.yt.ytclient.rpc.RpcClient;
import ru.yandex.yt.ytclient.tables.ColumnSchema;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

public class TestApiServiceClient extends ApiServiceClientImpl {
    public static final CompletableFuture<Object> COMPLETABLED_FUTURE = CompletableFuture.completedFuture(null);
    private final TestYt testYt;

    public TestApiServiceClient(TestYt testYt) {
        super((RpcClient) null);
        this.testYt = testYt;
    }

    @Override
    public CompletableFuture<ApiServiceTransaction> startTransaction(StartTransaction startTransaction) {
        ApiServiceTransaction transaction = Mockito.mock(ApiServiceTransaction.class);
        Mockito.doAnswer(invocation -> {
            ModifyRowsRequest request = invocation.getArgument(0);
            if (!request.getRowModificationTypes().stream()
                    .allMatch(v -> v == ERowModificationType.RMT_WRITE)) {
                throw new RuntimeException("Implemented only for RMT_WRITE: request: " + request);
            }

            TableSchema schema = request.getSchema();
            Comparator<YTreeMapNode> sorting = (o1, o2) -> 0;
            for (ColumnSchema column : schema.getColumns()) {
                if (column.getSortOrder() != null) {
                    sorting = sorting.thenComparing(Comparator.<YTreeMapNode, Comparable>comparing(v -> {
                        YTreeNode node = v.get(column.getName()).get();
                        if (node.isBooleanNode()) {
                            return node.boolValue();
                        }
                        if (node.isDoubleNode()) {
                            return node.doubleValue();
                        }
                        if (node.isIntegerNode()) {
                            return node.longValue();
                        }
                        if (node.isStringNode()) {
                            return node.stringValue();
                        }
                        throw new IllegalStateException("Unsupported type: " + node);
                    }));
                }
            }

            // Обновление будем производить простым способом
            // сначала считываем, все что хранится в таблице
            // потом обновляем строки
            // потом сохраняем все
            TreeSet<YTreeMapNode> valuesTreeSet = new TreeSet<>(sorting);

            YPath path = YPath.simple(request.getPath());
            valuesTreeSet.addAll(testYt.tables().readToList(path, YTableEntryTypes.YSON));

            request.getRows().stream()
                    .map(row -> {
                        YTreeBuilder yTreeBuilder = YTree.mapBuilder();
                        for (UnversionedValue unversionedValue : row.getValues()) {
                            String columnName = schema.getColumnName(unversionedValue.getId());
                            Object value = prepareColumnValue(unversionedValue);
                            yTreeBuilder.key(columnName).value(value);
                        }
                        return yTreeBuilder.buildMap();
                    })
                    .forEach(e -> {
                        // Чтобы поменять значение в tree-сете,
                        // надо сначала удалить старую версию, а потом добавить новую
                        valuesTreeSet.remove(e);
                        valuesTreeSet.add(e);
                    });
            testYt.tables().write(path, YTableEntryTypes.YSON, valuesTreeSet);
            return COMPLETABLED_FUTURE;
        }).when(transaction).modifyRows(Mockito.any());

        Mockito.doReturn(COMPLETABLED_FUTURE).when(transaction).ping();
        Mockito.doReturn(COMPLETABLED_FUTURE).when(transaction).commit();
        return CompletableFuture.completedFuture(transaction);
    }

    private Object prepareColumnValue(UnversionedValue unversionedValue) {
        if (unversionedValue.getType() == ColumnValueType.ANY) {
            byte[] bytes = (byte[]) unversionedValue.getValue();
            return YTreeBinarySerializer.deserialize(new ByteArrayInputStream(bytes)).asMap();
        }
        return unversionedValue.getValue();
    }

    @Override
    public CompletableFuture<ApiServiceTransaction> startTransaction(ApiServiceTransactionOptions options) {
        return this.startTransaction((StartTransaction) null);
    }
}
