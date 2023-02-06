package ru.yandex.market.mbo.mdm.common.util.batcher;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class BatcherTest {
    @Test
    public void testSimpleBatching() {
        // given
        List<List<Integer>> batchesHolder = new ArrayList<>();
        Batcher<Integer> batcher = new Batcher<>(3, batchesHolder::add);

        // when
        IntStream.rangeClosed(1, 10).forEach(batcher::accept);
        batcher.flush();

        // then
        Assertions.assertThat(batchesHolder).containsExactly(
            List.of(1, 2, 3),
            List.of(4, 5, 6),
            List.of(7, 8, 9),
            List.of(10)
        );
    }

    @Test
    public void testWeightBatching() {
        // given
        List<List<Integer>> batchesHolder = new ArrayList<>();
        Batcher<Integer> batcher = new Batcher<>(10, Integer::longValue, batchesHolder::add);

        // when
        List<BatcherFlushResult<Integer>> flushResults = IntStream.rangeClosed(1, 6)
            .mapToObj(batcher::accept)
            .collect(Collectors.toCollection(ArrayList::new));
        flushResults.add(batcher.flush());

        // then
        Assertions.assertThat(batchesHolder).containsExactly(
            List.of(1, 2, 3, 4),
            List.of(5),
            List.of(6)
        );
        Assertions.assertThat(flushResults).containsExactly(
            BatcherFlushResult.emptyResult(), // add 1
            BatcherFlushResult.emptyResult(), // add 2
            BatcherFlushResult.emptyResult(), // add 3
            BatcherFlushResult.emptyResult(), // add 4
            new BatcherFlushResult<>(List.of(1, 2, 3, 4), 10L), // add 5
            new BatcherFlushResult<>(List.of(5), 5L), // add 6
            new BatcherFlushResult<>(List.of(6), 6L) // flush
        );
        Assertions.assertThat(batcher.flush()).isEqualTo(BatcherFlushResult.emptyResult());
    }

    @Test
    public void testLongMaxWeights() {
        // given
        List<List<Integer>> batchesHolder = new ArrayList<>();
        Batcher<Integer> batcher = new Batcher<>(Long.MAX_VALUE, i -> Long.MAX_VALUE, batchesHolder::add);

        // when
        List<BatcherFlushResult<Integer>> flushResults = IntStream.rangeClosed(1, 2)
            .mapToObj(batcher::accept)
            .collect(Collectors.toCollection(ArrayList::new));
        flushResults.add(batcher.flush());

        // then
        Assertions.assertThat(batchesHolder).containsExactly(
            List.of(1),
            List.of(2)
        );
    }

    @Test
    public void whenElementIsLargeThanMaxWeightThrowException() {
        Batcher<Integer> batcher = new Batcher<>(
            Long.MAX_VALUE - 1,
            i -> Long.MAX_VALUE,
            System.out::println
        );
        Assertions.assertThatExceptionOfType(BatcherError.class)
            .isThrownBy(() -> batcher.accept(1))
            .withMessage(
                "Got item with weight greater then max batch weight. " +
                    "Item weight 9223372036854775807. " +
                    "Max batch weight: 9223372036854775806. " +
                    "Item: 1."
            );
    }

    @Test
    public void whenElementWeightIsNegativeThrowException() {
        Batcher<Integer> batcher = new Batcher<>(
            Long.MAX_VALUE,
            i -> -i,
            System.out::println
        );
        Assertions.assertThatExceptionOfType(BatcherError.class)
            .isThrownBy(() -> batcher.accept(1))
            .withMessage("Got item with not positive weight.Item weight -1. Item: 1.");
    }
}
