package ru.yandex.market.logistics.management.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

public class BindVariablesPartitionUtilTest extends AbstractTest {

    private static final int LIMIT = BindVariablesPartitionUtil.SAFE_BIND_VARIABLES_LIMIT;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    public void testPartition(@SuppressWarnings("unused") String displayName, Iterable<Integer> iterable, int size) {
        Iterable<List<Integer>> partition = BindVariablesPartitionUtil.partition(iterable);

        assertThat(partition).hasSize(size);

        partition.forEach(actualLists -> assertThat(actualLists)
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(LIMIT)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    public void testNoDuplicatesPartition(@SuppressWarnings("unused") String displayName, Iterable<Integer> iterable) {
        Iterable<List<Integer>> partition = BindVariablesPartitionUtil.partitionNoDuplicates(iterable);

        var actualElements = Streams.stream(partition)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        assertThat(actualElements).doesNotHaveDuplicates();
    }

    private static List<Integer> rangeList(int fromInclusive, int toExclusive) {
        return IntStream.range(fromInclusive, toExclusive)
            .boxed()
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> testPartition() {
        return Stream.of(
            Arguments.of("empty", Collections.EMPTY_LIST, 0),
            Arguments.of("List within limit", List.of(1, 2, 3), 1),
            Arguments.of("rangeList 2 partitions", rangeList(0, LIMIT + 1), 2),
            Arguments.of("rangeList 3 partitions", rangeList(LIMIT, LIMIT * 3 + 2), 3),
            Arguments.of("Set within limit", Set.of(1, 2, 3), 1),
            Arguments.of("Set over limit", new HashSet<>(rangeList(0, LIMIT * 2 + 1)), 3)
        );
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> testNoDuplicatesPartition() {
        return Stream.of(
            Arguments.of("List", List.of(1, 2, 2, 3, 3)),
            Arguments.of("Set", Set.of(1, 2, 3))
        );
    }

}
