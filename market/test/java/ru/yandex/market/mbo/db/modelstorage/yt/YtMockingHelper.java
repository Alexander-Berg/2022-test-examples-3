package ru.yandex.market.mbo.db.modelstorage.yt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.yt.ytclient.proxy.ApiServiceClient;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

/**
 * @author apluhin
 * @created 1/18/21
 */
public class YtMockingHelper {

    private YtMockingHelper() {
    }

    public static <T> UnversionedRowset buildModelStoreResponse(ApiServiceClient mockClient,
                                                                YtTableRpcApi rpcApi,
                                                                List<T> elements,
                                                                BiFunction<Integer, T, Map<String, UnversionedValue>>
                                                                    converter) {
        List<Map<String, UnversionedValue>> valueMap = new ArrayList<>();
        AtomicInteger i = new AtomicInteger();
        List<UnversionedRow> rows = elements.stream().map(it -> {
            Map<String, UnversionedValue> rowValues = converter.apply(i.get(), it);
            valueMap.add(i.getAndIncrement(), rowValues);
            return new UnversionedRow(new ArrayList<>(rowValues.values()));
        }).collect(Collectors.toList());
        UnversionedRowset unversionedRowset = new UnversionedRowset(Mockito.mock(TableSchema.class), rows);

        Mockito.when(mockClient.lookupRows(Mockito.any())).thenReturn(
            CompletableFuture.completedFuture(unversionedRowset));
        Mockito.when(rpcApi.getValue(Mockito.any(), Mockito.any()))
            .thenAnswer((Answer<UnversionedValue>) invocation -> {
                UnversionedRow argument = invocation.getArgument(0);
                String fieldName = invocation.getArgument(1);
                int id = argument.getValues().get(0).getId();
                return valueMap.get(id).get(fieldName);
            });
        return unversionedRowset;
    }

    public static UnversionedValue buildValue(int id, ColumnValueType type, Object value) {
        return new UnversionedValue(id, type, false, value);
    }
}
