package ru.yandex.direct.ytwrapper.dynamic.testutil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import ru.yandex.yt.ytclient.tables.ColumnSchema;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

/**
 * Builder для удобного конструкрования {@link UnversionedRowset}.
 * <p>
 * Пример использования:
 * <pre>
 * UnversionedRowset rowset = rowsetBuilder()
 * .add(rowBuilder()
 * .withColValue("BID", 1L)
 * .withColValue("TYPE","desktop"))
 * .add(rowBuilder()
 * .withColValue("BID", 2L)
 * .withColValue("TYPE","mobile"))
 * .build();
 * </pre>
 */
public class RowsetBuilder {
    private List<RowBuilder> rows = new ArrayList<>();

    public static RowsetBuilder rowsetBuilder() {
        return new RowsetBuilder();
    }

    public RowsetBuilder add(RowBuilder rowBuilder) {
        rows.add(rowBuilder);
        return this;
    }

    public UnversionedRowset build() {
        List<ColumnSchema> externalColumns = rows.stream()
                .map(RowBuilder::getColumnSchemaList)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());

        checkColumnUnique(externalColumns);

        List<UnversionedRow> unversionedRows = rows.stream()
                .map(b -> b.build(externalColumns))
                .collect(Collectors.toList());
        TableSchema.Builder builder = new TableSchema.Builder();
        externalColumns.forEach(builder::add);
        return new UnversionedRowset(builder.setUniqueKeys(false).build(), unversionedRows);
    }

    private void checkColumnUnique(Collection<ColumnSchema> columns) {
        Map<String, Long> collect = StreamEx.of(columns)
                .collect(groupingBy(ColumnSchema::getName, counting()));

        Optional<String> columnWithDuplicate = EntryStream.of(collect)
                .filterValues(count -> count > 1)
                .keys()
                .findFirst();

        checkState(!columnWithDuplicate.isPresent(), "Column %s duplicated", columnWithDuplicate.orElse(null));
    }
}
