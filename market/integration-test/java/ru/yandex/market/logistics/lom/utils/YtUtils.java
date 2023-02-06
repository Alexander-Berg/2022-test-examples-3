package ru.yandex.market.logistics.lom.utils;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.inside.yt.kosher.cypress.Range;
import ru.yandex.inside.yt.kosher.cypress.RichYPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeEntityNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
public final class YtUtils {

    private YtUtils() {
        throw new UnsupportedOperationException();
    }

    public static long getLowerRowIndex(RichYPath richYPath) {
        return ((Range) richYPath.getRanges().get(0)).lower.rowIndex;
    }

    public static long getUpperRowIndex(RichYPath richYPath) {
        return ((Range) richYPath.getRanges().get(0)).upper.rowIndex;
    }

    @Nonnull
    public static <T> CloseableIterator<T> getIterator(Iterable<T> iterable) {
        return CloseableIterator.wrap(iterable.iterator());
    }

    @Nonnull
    public static YTreeMapNode buildMapNode(Map<String, ?> map) {
        YTreeBuilder yTreeBuilder = new YTreeBuilder().beginMap();

        map.forEach((key, value) -> yTreeBuilder.key(key).value(value));

        return (YTreeMapNode) yTreeBuilder.endMap().build();
    }

    @Nonnull
    public static YTreeEntityNode rowCountNode(long rowsCount) {
        return new YTreeEntityNodeImpl(
            Map.of("row_count", new YTreeIntegerNodeImpl(true, rowsCount, null))
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> void mockSelectRowsFromYtQueryStartsWith(
        YtTables ytTables,
        T returnValue,
        String query
    ) {
        doReturn(returnValue).when(ytTables).selectRows(
            startsWith(query),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(true),
            eq(YTableEntryTypes.YSON),
            any(Function.class)
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> void mockSelectRowsFromYt(
        YtTables ytTables,
        T returnValue,
        String query
    ) {
        doReturn(returnValue).when(ytTables).selectRows(
            eq(query),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(true),
            eq(YTableEntryTypes.YSON),
            any(Function.class)
        );
    }

    @SuppressWarnings("unchecked")
    public static void verifySelectRowsInteractionsQueryStartsWith(YtTables ytTables, String query) {
        verifySelectRowsInteractionsQueryStartsWith(ytTables, query, 1);
    }

    @SuppressWarnings("unchecked")
    public static void verifySelectRowsInteractionsQueryStartsWith(
        YtTables ytTables,
        String query,
        int times
    ) {
        verify(ytTables, times(times)).selectRows(
            startsWith(query),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(true),
            eq(YTableEntryTypes.YSON),
            any(Function.class)
        );
    }

    @SuppressWarnings("unchecked")
    public static void verifySelectRowsInteractions(YtTables ytTables, String query, int times) {
        verify(ytTables, times(times)).selectRows(
            eq(query),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(true),
            eq(YTableEntryTypes.YSON),
            any(Function.class)
        );
    }

    public static void verifySelectRowsInteractions(YtTables ytTables, String query) {
        verifySelectRowsInteractions(ytTables, query, 1);
    }

    @SuppressWarnings("unchecked")
    public static void mockExceptionCallingYt(YtTables ytTables, String query, Exception ex) {
        doThrow(ex)
            .when(ytTables).selectRows(
                eq(query),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(true),
                eq(YTableEntryTypes.YSON),
                any(Function.class)
            );
    }

    @SuppressWarnings("unchecked")
    public static void mockExceptionCallingYtQueryStartsWith(YtTables ytTables, String query, Exception ex) {
        doThrow(ex)
            .when(ytTables).selectRows(
                startsWith(query),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(true),
                eq(YTableEntryTypes.YSON),
                any(Function.class)
            );
    }
}
