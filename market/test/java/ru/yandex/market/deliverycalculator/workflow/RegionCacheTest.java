package ru.yandex.market.deliverycalculator.workflow;

import java.util.Optional;
import java.util.stream.StreamSupport;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ReflectionAssertMatcher;
import ru.yandex.market.deliverycalculator.model.Region;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;
import ru.yandex.market.deliverycalculator.workflow.util.collection.TreeNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionCacheTest extends FunctionalTest {

    @Autowired
    private RegionCache tested;

    @Test
    @DbUnitDataSet(before = "regions.csv")
    void testCorrectCacheLoad() {
        TreeNode<Region> regionTreeNode = tested.getRegionNode(213);

        assertNotNull(regionTreeNode);
        Iterable<TreeNode<Region>> regionsTree = regionTreeNode::ancestorsIterator;
        assertEquals(5, StreamSupport.stream(regionsTree.spliterator(), false)
                .count());
        MatcherAssert.assertThat(regionTreeNode.getValue(), new ReflectionAssertMatcher<>(createMoscowRegion()));
    }

    @Test
    void testFindMaxGranularity_emptyDatabase() {
        Optional<Integer> maxGranularity = tested.findMaxGranularity();

        assertEquals(Optional.empty(), maxGranularity);
    }

    @Test
    @DbUnitDataSet(before = "getRegionsGranularity.before.csv")
    void testFindMaxGranularity_granularityCalculated() {
        Optional<Integer> maxGranularity = tested.findMaxGranularity();

        assertTrue(maxGranularity.isPresent());
        assertEquals(10, maxGranularity.get());
    }

    private Region createMoscowRegion() {
        return new Region(213, "Москва", 6, 1);
    }

}
