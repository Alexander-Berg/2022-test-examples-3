package ru.yandex.market.mbo.cardrender.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.mockito.Mockito;

import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

/**
 * @author apluhin
 * @created 6/9/21
 */
public class YtMockRequestUtils {

    public static UnversionedValue buildValue(int id, ColumnValueType type, Object value) {
        return new UnversionedValue(id, type, false, value);
    }

    public static class ConvertResult<T> {
        UnversionedRowset rowset;
        List<Map<String, UnversionedValue>> valueMap;

        public ConvertResult(List<T> entity, BiFunction<T, AtomicInteger, Map<String, UnversionedValue>> rowConverter) {
            List<Map<String, UnversionedValue>> valueMap = new ArrayList<>();
            AtomicInteger i = new AtomicInteger();
            List<UnversionedRow> rows = entity.stream().map(it -> {
                Map<String, UnversionedValue> rowValues = rowConverter.apply(it, i);
                valueMap.add(i.getAndIncrement(), rowValues);
                return new UnversionedRow(new ArrayList<>(rowValues.values()));
            }).collect(Collectors.toList());
            this.rowset = new UnversionedRowset(Mockito.mock(TableSchema.class), rows);
            this.valueMap = valueMap;
        }

        public UnversionedRowset getRowset() {
            return rowset;
        }

        public List<Map<String, UnversionedValue>> getValueMap() {
            return valueMap;
        }
    }


}
