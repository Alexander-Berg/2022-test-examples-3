package ru.yandex.market.mbo.yt.operations;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

public class TestYtUtils {
    private TestYtUtils() {
    }

    public static Object getColumnValue(Object inputRow, String fieldName) {
        YTreeMapNode row = (YTreeMapNode) inputRow;
        return row.get(fieldName).orElseThrow(() -> new RuntimeException("No value in column: " + fieldName + ", " +
                "values:\n" + row));
    }

    public static <TInput> Stream<TInput> flatEntries(Map<Integer, Iterator<TInput>> entries,
                                                      boolean setTableIndex) {
        return entries.entrySet().stream()
                .flatMap(e -> {
                    Integer tableIndex = e.getKey();
                    Iterator<TInput> iterator = e.getValue();
                    return setTableIndex
                            ? toStream(iterator).map(v -> copyWithTableIndexAttribute(v, tableIndex))
                            : toStream(iterator);
                });
    }

    public static <TInput> Stream<TInput> toStream(Iterator<TInput> iterator) {
        return StreamSupport.stream(((Iterable<TInput>) () -> iterator).spliterator(), false);
    }

    /**
     * Made copy of {@param toCopy} and set tableIndex.
     */
    public static <TInput> TInput copyWithTableIndexAttribute(TInput toCopy, int tableIndex) {
        //noinspection unchecked
        return (TInput) YTree.builder()
                .beginAttributes()
                .key("table_index").value(tableIndex)
                .endAttributes()
                .value(toCopy)
                .build().mapNode();
    }
}
