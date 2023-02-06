package ru.yandex.market.loyalty.core.service.report;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.loyalty.core.model.CategoryTree;
import ru.yandex.market.loyalty.core.model.CategoryTreeRecord;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.collect.ImmutableSet.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

public class RecommendationsServiceUtilsTest {
    private CategoryTree categoryTree;

    @Before
    public void setUp() {
        categoryTree = new CategoryTree(
                Arrays.asList(
                        new CategoryTreeRecord(0, null, ""),
                        new CategoryTreeRecord(1, 0L, ""),
                        new CategoryTreeRecord(2, 0L, ""),
                        new CategoryTreeRecord(3, 0L, ""),
                        new CategoryTreeRecord(4, 1L, ""),
                        new CategoryTreeRecord(5, 1L, ""),
                        new CategoryTreeRecord(6, 2L, ""),
                        new CategoryTreeRecord(7, 2L, ""),
                        new CategoryTreeRecord(8, 2L, ""),
                        new CategoryTreeRecord(9, 4L, ""),
                        new CategoryTreeRecord(10, 9L, "")
                )
        );
    }

    @Test
    public void test() {
        final Set<Long> leafHids = RecommendationsServiceUtils.getLeafHids(
                of(0L), categoryTree);

        assertThat(
                leafHids,
                containsInAnyOrder(
                        equalTo(3L),
                        equalTo(5L),
                        equalTo(6L),
                        equalTo(7L),
                        equalTo(8L),
                        equalTo(10L)
                )
        );
    }

    @Test
    public void testInvertHidsReturningLeafHids() {
        final Set<Long> leafHids = RecommendationsServiceUtils.invertHidsReturningLeafHids(
                of(2L), categoryTree);

        assertThat(
                leafHids,
                containsInAnyOrder(
                        equalTo(3L),
                        equalTo(5L),
                        equalTo(10L)
                )
        );
    }

    @Test
    public void testGetHigherLevelHids() {
        final Set<Long> leafHids = RecommendationsServiceUtils.getHigherLevelHids(
                new TreeSet<>(of(5L, 6L, 7L, 8L)), 2, categoryTree);

        assertThat(
                leafHids,
                containsInAnyOrder(
                        equalTo(1L),
                        equalTo(2L)
                )
        );
    }

    @Test
    public void testGetHigherLevelHids2() {
        final Set<Long> leafHids = RecommendationsServiceUtils.getHigherLevelHids(
                new TreeSet<>(of(6L, 7L, 8L)), 2, categoryTree);

        assertThat(
                leafHids,
                containsInAnyOrder(
                        equalTo(2L)
                )
        );
    }

    @Test
    public void testGetHigherLevelHids3() {
        final Set<Long> leafHids = RecommendationsServiceUtils.getHigherLevelHids(
                new TreeSet<>(of(5L, 10L)), 2, categoryTree);

        assertThat(
                leafHids,
                containsInAnyOrder(
                        equalTo(5L),
                        equalTo(10L)
                )
        );
    }

    @Test
    public void testGetHigherLevelHids4() {
        final Set<Long> leafHids = RecommendationsServiceUtils.getHigherLevelHids(
                new TreeSet<>(of(5L, 10L)), 1, categoryTree);

        assertThat(
                leafHids,
                containsInAnyOrder(
                        equalTo(1L)
                )
        );
    }

    @Test
    public void testGetHigherLevelHids5() {
        final Set<Long> leafHids = RecommendationsServiceUtils.getHigherLevelHids(
                new TreeSet<>(of(3L, 6L, 7L, 8L)), 2, categoryTree);

        assertThat(
                leafHids,
                containsInAnyOrder(
                        equalTo(2L),
                        equalTo(3L)
                )
        );
    }

}
