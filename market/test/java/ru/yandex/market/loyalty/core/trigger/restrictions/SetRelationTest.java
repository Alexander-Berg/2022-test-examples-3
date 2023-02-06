package ru.yandex.market.loyalty.core.trigger.restrictions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class SetRelationTest {
    private static final Object DUMMY = new Object();

    @Test
    public void allIncludedInSetSuccess() {
        assertFitness(true, SetRelation.ALL_INCLUDED_IN_SET, ImmutableSet.of(1, 2), ImmutableSet.of(1, 2, 3));
    }

    @Test
    public void allIncludedInSetFail() {
        assertFitness(false, SetRelation.ALL_INCLUDED_IN_SET, ImmutableSet.of(1, 2, 3), ImmutableSet.of(1, 2));
    }

    @Test
    public void atLeastOneIncludedInSetSuccess() {
        assertFitness(true, SetRelation.AT_LEAST_ONE_INCLUDED_IN_SET, ImmutableSet.of(1, 2), ImmutableSet.of(2, 3));
    }

    @Test
    public void atLeastOneIncludedInSetFail() {
        assertFitness(false, SetRelation.AT_LEAST_ONE_INCLUDED_IN_SET, ImmutableSet.of(1, 2), ImmutableSet.of(3, 4));
    }

    @Test
    public void allFromSetShouldBeIncludedSuccess() {
        assertFitness(true, SetRelation.ALL_FROM_SET_SHOULD_BE_INCLUDED, ImmutableSet.of(1, 2, 3), ImmutableSet.of(1,
                2));
    }

    @Test
    public void allFromSetShouldBeIncludedFail() {
        assertFitness(false, SetRelation.ALL_FROM_SET_SHOULD_BE_INCLUDED, ImmutableSet.of(1, 2), ImmutableSet.of(1, 2
                , 3));
    }

    @Test
    public void nothingIncludedInSetSuccess() {
        assertFitness(true, SetRelation.NOTHING_INCLUDED_IN_SET, ImmutableSet.of(1, 2), ImmutableSet.of(3, 4));
    }

    @Test
    public void nothingIncludedInSetFail() {
        assertFitness(false, SetRelation.NOTHING_INCLUDED_IN_SET, ImmutableSet.of(1, 2), ImmutableSet.of(2, 3));
    }

    @Test
    public void equalSetsSuccess() {
        assertFitness(true, SetRelation.EQUAL_SETS, ImmutableSet.of(1, 2), ImmutableSet.of(1, 2));
    }

    @Test
    public void equalSetsFail() {
        assertFitness(false, SetRelation.EQUAL_SETS, ImmutableSet.of(1, 2), ImmutableSet.of(2, 3));
    }

    private static <T> void assertFitness(boolean expected, SetRelation setRelation, Set<T> i, Set<T> r) {
        Map<T, ?> iMap = i.stream().collect(ImmutableMap.toImmutableMap(key -> key, key -> DUMMY));
        assertEquals(expected, !setRelation.fit(iMap, r, t -> true).isEmpty());
        if (setRelation != SetRelation.NOTHING_INCLUDED_IN_SET) {
            assertFalse(!setRelation.fit(iMap, r, t -> false).isEmpty());
        }
    }
}
