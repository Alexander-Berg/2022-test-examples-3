package ru.yandex.market.tpl.carrier.core.domain.util;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.EntryStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class IndexDifferenceCalculatorTest {

    private static final List<Integer> source = List.of(0, 1, 2, 3, 4, 5, 6);


    public static Stream<Set<Integer>> cases() {
        return Stream.of(
                Set.of(0),
                Set.of(1, 2),
                Set.of(3),
                Set.of(5, 6),
                Set.of(6)
        );
    }

    @MethodSource(value = "cases")
    @ParameterizedTest
    void shouldCalculateDifference(Set<Integer> indexesToRemove) {
        List<Integer> newList = EntryStream.of(source)
                .filterKeys(i -> !indexesToRemove.contains(i))
                .values()
                .collect(Collectors.toList());

        Assertions.assertThat(IndexDifferenceCalculator.calculateIndexDifference(source, newList, Objects::equals))
                .containsExactlyInAnyOrder(indexesToRemove.toArray(Integer[]::new));
    }

}
