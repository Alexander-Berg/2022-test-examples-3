package ru.yandex.market.mbo.db.modelstorage.index.yt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.ModelIndexPayload;

/**
 * @author apluhin
 * @created 11/16/20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class YtSkippedCursorIteratorTest {

    @Test
    public void testIterator() {
        List<Tuple2<List<ModelIndexPayload>, String>> tuple2s = Arrays.asList(
            Tuple2.tuple(generatePayload(1, 5), "5"),
            Tuple2.tuple(generatePayload(6, 10), "10"),
            Tuple2.tuple(Collections.emptyList(), "15"),
            Tuple2.tuple(generatePayload(17, 20), "20"),
            Tuple2.tuple(generatePayload(21, 25), null)
        );
        YtSkippedCursorIterator<ModelIndexPayload> iterator = getIterator(tuple2s, 25, 5);
        List<Long> result = new ArrayList<>();
        while (iterator.hasNext()) {
            long id = iterator.next().getId();
            result.add(id);
        }
        List<Long> expected = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 17L, 18L, 19L, 20L, 21L, 22L,
            23L, 24L, 25L);
        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testWithNotFullInterval() {
        List<Tuple2<List<ModelIndexPayload>, String>> tuple2s = Arrays.asList(
            Tuple2.tuple(generatePayload(1, 3), "5"),
            Tuple2.tuple(generatePayload(6, 8), "10"),
            Tuple2.tuple(Collections.emptyList(), "15"),
            Tuple2.tuple(generatePayload(16, 17), "20"),
            Tuple2.tuple(generatePayload(21, 25), "25"),
            Tuple2.tuple(generatePayload(26, 30), "30")
        );
        YtSkippedCursorIterator<ModelIndexPayload> iterator = getIterator(tuple2s, 26, 5);
        List<Long> result = new ArrayList<>();
        while (iterator.hasNext()) {
            long id = iterator.next().getId();
            result.add(id);
        }
        List<Long> expected = Arrays.asList(1L, 2L, 3L, 6L, 7L, 8L, 16L, 17L, 21L, 22L, 23L, 24L, 25L, 26L);
        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testWithEmptyInterval() {
        List<Tuple2<List<ModelIndexPayload>, String>> tuple2s = Arrays.asList(
            Tuple2.tuple(Collections.emptyList(), "5"),
            Tuple2.tuple(Collections.emptyList(), "10"),
            Tuple2.tuple(Collections.emptyList(), null)
        );
        YtSkippedCursorIterator<ModelIndexPayload> iterator = getIterator(tuple2s, 15, 5);
        List<Long> result = new ArrayList<>();
        while (iterator.hasNext()) {
            long id = iterator.next().getId();
            result.add(id);
        }
        Assertions.assertThat(result).isEqualTo(Collections.emptyList());
    }

    @Test
    public void testWithLimitSmallerResultSize() {
        List<Tuple2<List<ModelIndexPayload>, String>> tuple2s = Arrays.asList(
            Tuple2.tuple(generatePayload(1, 3), "5"),
            Tuple2.tuple(generatePayload(6, 8), "10"),
            Tuple2.tuple(Collections.emptyList(), "15"),
            Tuple2.tuple(generatePayload(16, 17), "20"),
            Tuple2.tuple(generatePayload(21, 25), "25"),
            Tuple2.tuple(generatePayload(26, 30), "30")
        );
        YtSkippedCursorIterator<ModelIndexPayload> iterator = getIterator(tuple2s, 21, 5);
        List<Long> result = new ArrayList<>();
        while (iterator.hasNext()) {
            long id = iterator.next().getId();
            result.add(id);
        }
        List<Long> expected = Arrays.asList(1L, 2L, 3L, 6L, 7L, 8L, 16L, 17L, 21L);
        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testWithSameResult() {
        List<Tuple2<List<ModelIndexPayload>, String>> tuple2s = Arrays.asList(
            Tuple2.tuple(generatePayload(1, 5), "5"),
            Tuple2.tuple(generatePayload(6, 10), null)
        );
        YtSkippedCursorIterator<ModelIndexPayload> iterator = getIterator(tuple2s, 10, 5);
        List<Long> result = new ArrayList<>();
        while (iterator.hasNext()) {
            long id = iterator.next().getId();
            result.add(id);
        }
        List<Long> expected = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testWithBeginAndEndInterval() {
        List<Tuple2<List<ModelIndexPayload>, String>> tuple2s = Arrays.asList(
            Tuple2.tuple(generatePayload(1, 1), "5"),
            Tuple2.tuple(generatePayload(10, 10), null)
        );
        YtSkippedCursorIterator<ModelIndexPayload> iterator = getIterator(tuple2s, 10, 5);
        List<Long> result = new ArrayList<>();
        while (iterator.hasNext()) {
            long id = iterator.next().getId();
            result.add(id);
        }
        List<Long> expected = Arrays.asList(1L, 10L);
        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testWithOneInterval() {
        List<Tuple2<List<ModelIndexPayload>, String>> tuple2s = Arrays.asList(
            Tuple2.tuple(Collections.emptyList(), "5"),
            Tuple2.tuple(generatePayload(5, 10), "10"),
            Tuple2.tuple(Collections.emptyList(), null)
        );
        YtSkippedCursorIterator<ModelIndexPayload> iterator = getIterator(tuple2s, 15, 5);
        List<Long> result = new ArrayList<>();
        while (iterator.hasNext()) {
            long id = iterator.next().getId();
            result.add(id);
        }
        List<Long> expected = Arrays.asList(5L, 6L, 7L, 8L, 9L, 10L);
        Assertions.assertThat(result).isEqualTo(expected);
    }

    @NotNull
    private YtSkippedCursorIterator<ModelIndexPayload> getIterator(
        List<Tuple2<List<ModelIndexPayload>, String>> tuple2s, int limit, int fetchSize) {
        AtomicInteger counter = new AtomicInteger(0);
        return new YtSkippedCursorIterator<>(
            MboIndexesFilter.newFilter(),
            fetchSize,
            limit,
            null,
            (filter, cursor) -> {
                Tuple2<List<ModelIndexPayload>, String> stringTuple2 = tuple2s.get(counter.getAndIncrement());
                return YtCursorHelper.getFilterYtCursor(filter, cursor, (f) -> stringTuple2);
            }
        );
    }

    private List<ModelIndexPayload> generatePayload(int from, int to) {
        List<ModelIndexPayload> l = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            l.add(new ModelIndexPayload(i, i, null));
        }
        return l;
    }
}
