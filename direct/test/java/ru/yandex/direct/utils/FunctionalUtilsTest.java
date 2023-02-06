package ru.yandex.direct.utils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.batchDispatch;
import static ru.yandex.direct.utils.FunctionalUtils.invertIndexes;

public class FunctionalUtilsTest {
    @Test
    public void invertIndexes_ZeroEmpty() {
        assertThat(invertIndexes(0, emptyList()), empty());
    }

    @Test
    public void invertIndexes_InvertEmpty() {
        assertThat(invertIndexes(3, emptyList()), contains(0, 1, 2));
    }

    @Test
    public void invertIndexes_InvertOne() {
        assertThat(invertIndexes(3, List.of(1)), contains(0, 2));
    }

    @Test
    public void invertIndexes_InvertAll() {
        assertThat(invertIndexes(3, List.of(0, 1, 2)), empty());
    }

    @Test
    public void batchDispatch_Empty() {
        assertThat(batchDispatch(emptyList(), x -> true, Function.identity(), Function.identity()), empty());
    }

    @Test
    public void batchDispatch_Predicate() {
        assertThat(
                batchDispatch(List.of(1, 2, 3), x -> x > 1 & x < 3, this::mul2, Function.identity()),
                contains(1, 4, 3));
    }

    @Test
    public void batchDispatch_Indexes() {
        assertThat(
                batchDispatch(List.of(1, 2, 3), List.of(0, 2), this::mul2, Function.identity()),
                contains(2, 2, 6));
    }

    private List<Integer> mul2(List<Integer> items) {
        return items.stream().map(x -> x * 2).collect(Collectors.toList());
    }
}
