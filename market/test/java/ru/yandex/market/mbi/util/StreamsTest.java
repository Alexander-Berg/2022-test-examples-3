package ru.yandex.market.mbi.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.common.util.collections.CollectionFactory.set;

class StreamsTest {
    @Test
    void batching() {
        List<List<Integer>> result = Streams.batching(Stream.of(1, 2, 3, 4, 5), 2).collect(Collectors.toList());
        assertThat(result).containsExactly(
                list(1, 2),
                list(3, 4),
                list(5)
        );
    }

    @Test
    void batchingSet() {
        List<Set<Integer>> result = Streams.batching(Stream.of(1, 1, 2, 3, 4), 2, Collectors.toSet())
                .collect(Collectors.toList());
        assertThat(result).containsExactly(
                set(1),
                set(2, 3),
                set(4)
        );
    }

    @Test
    void batchingEmpty() {
        List<List<Integer>> result = Streams.batching(Stream.<Integer>empty(), 2).collect(Collectors.toList());
        assertThat(result).isEmpty();
    }

    @Test
    void throwIfEmptyTestEmptyStream() {
        Stream<Integer> emptyStream = Stream.empty();
        Assertions.assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
            Streams.throwIfEmpty(emptyStream, () -> new IllegalStateException("Empty stream"))
                    .reduce(0, Integer::sum);
        }).withMessage("Empty stream");
    }

    @Test
    void throwIfEmptyTestNonEmptyStream() {
        Stream<Integer> nonEmptyStream = Stream.of(1, 2, 3);
        Assertions.assertThat(
                Streams.throwIfEmpty(nonEmptyStream, () -> new IllegalStateException("Empty stream"))
                        .reduce(0, Integer::sum)
        ).isEqualTo(6);
    }

    @Test
    void throwIfEmptyTestParallelStream() {
        Stream<Integer> nonEmptyStream2 = Stream.of(1, 2, 3);
        Assertions.assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
            Streams.throwIfEmpty(nonEmptyStream2, () -> new IllegalStateException("Empty stream"))
                    .parallel()
                    .reduce(0, Integer::sum);
        }).withMessage("Parallelism is not supported");
    }
}
