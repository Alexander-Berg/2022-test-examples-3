package ru.yandex.market.mbo.yt.operations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.yt.utils.YieldStub;

/**
 * @author s-ermakov
 */
public class TestYtSortUtils {
    private TestYtSortUtils() {
    }

    @SafeVarargs
    public static <TInput> YieldStub<TInput> run(List<String> sortBy, TInput... entries) {
        return run(sortBy, Arrays.asList(entries));
    }

    public static <TInput> YieldStub<TInput> run(List<String> sortBy, Collection<TInput> entries) {
        YieldStub<TInput> yield = new YieldStub<>();
        run(sortBy, entries, yield);
        return yield;
    }

    public static <TInput> void run(List<String> sortBy, Collection<TInput> entries, Yield<TInput> yield) {
        run(sortBy, entries.iterator(), yield);
    }

    public static <TInput> void run(List<String> sortBy, Iterator<TInput> entries, Yield<TInput> yield) {
        Stream<TInput> sortEntries = run(sortBy, entries);
        sortEntries.forEach(e -> yield.yield(0, e));
    }

    public static <TInput> Stream<TInput> run(List<String> sortBy, Iterator<TInput> entries) {
        return TestYtUtils.toStream(entries).sorted((o1, o2) -> {
            int compareResult = 0;

            for (String sortField : sortBy) {
                Object value1 = getColumnValue(o1, sortField);
                Object value2 = getColumnValue(o2, sortField);

                if (value1 instanceof Comparable && value2 instanceof Comparable) {
                    compareResult = ((Comparable) value1).compareTo(value2);
                    if (compareResult != 0) {
                        return compareResult;
                    }
                } else {
                    throw new IllegalStateException("Key fields has to be comparable");
                }
            }

            return compareResult;
        });
    }

    private static <TInput> Object getColumnValue(TInput inputRow, String fieldName) {
        YTreeMapNode row = (YTreeMapNode) inputRow;
        YTreeNode node = row.get(fieldName).orElseThrow(() -> new RuntimeException("No value in column: " + fieldName));
        return innerValue(fieldName, node);
    }

    private static Object innerValue(String fieldName, YTreeNode node) {
        if (node.isStringNode()) {
            return node.stringValue();
        } else if (node.isBooleanNode()) {
            return node.boolValue();
        } else if (node.isDoubleNode()) {
            return node.doubleValue();
        } else if (node.isIntegerNode()) {
            return node.intValue();
        }
        throw new IllegalStateException("Failed to get object from '" + fieldName + "', node: " + node);
    }
}
