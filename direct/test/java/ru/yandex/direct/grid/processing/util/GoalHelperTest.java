package ru.yandex.direct.grid.processing.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.model.GdGoalStatsFilter;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class GoalHelperTest {
    @Parameterized.Parameter
    public Set<Long> goalIds;

    @Parameterized.Parameter(1)
    public List<GdGoalStatsFilter> goalStatsFilters;

    @Parameterized.Parameter(2)
    public Set<Long> expectedGoalIds;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {null,
                        null,
                        emptySet()},
                {null,
                        Arrays.asList(new GdGoalStatsFilter().withGoalId(1L), new GdGoalStatsFilter().withGoalId(2L)),
                        Sets.newHashSet(1L, 2L)
                },
                {emptySet(),
                        Arrays.asList(new GdGoalStatsFilter().withGoalId(1L), new GdGoalStatsFilter().withGoalId(2L)),
                        Sets.newHashSet(1L, 2L)
                },
                {Sets.newHashSet(1L, 2L),
                        null,
                        Sets.newHashSet(1L, 2L)
                },
                {Sets.newHashSet(1L, 2L),
                        emptyList(),
                        Sets.newHashSet(1L, 2L)
                },
                {Sets.newHashSet(1L, 2L),
                        Arrays.asList(new GdGoalStatsFilter().withGoalId(2L), new GdGoalStatsFilter().withGoalId(3L)),
                        Sets.newHashSet(1L, 2L, 3L)
                },
                {Sets.newHashSet(1L, 2L),
                        Arrays.asList(new GdGoalStatsFilter().withGoalId(null), new GdGoalStatsFilter().withGoalId(3L)),
                        Sets.newHashSet(1L, 2L, 3L)
                },
        });
    }

    @Test
    public void combineGoalIds() {
        Set<Long> resultGoalIds = GoalHelper.combineGoalIds(goalIds, goalStatsFilters);
        assertEquals(expectedGoalIds, resultGoalIds);
    }
}
