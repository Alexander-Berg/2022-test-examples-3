package ru.yandex.market.replenishment.autoorder.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;

import static org.junit.Assert.assertEquals;

public class RecommendationsUtilsTest {

    @Test
    public void simpleCollapseTest() {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(1L, 100L, 1, 3));
        recommendations.add(create(2L, 100L, 2, 2));
        recommendations.add(create(3L, 200L, 3, 1));

        var idsForDelete = RecommendationsUtils.collapse(recommendations);

        assertEquals(2, recommendations.size());
        assertRecommendation(1L,100L, 3, 5, recommendations.get(0));
        assertRecommendation(3L,200L, 3, 1, recommendations.get(1));

        var expectedSet = new HashSet<Long>();
        expectedSet.add(2L);
        assertEquals(expectedSet, idsForDelete);
    }

    @Test
    public void simpleCollapseTest_WithNulls() {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(null, 100L, 1, 3));
        recommendations.add(create(2L, 100L, 2, 2));
        recommendations.add(create(null, 100L, 0, 1));
        recommendations.add(create(3L, 200L, 3, 1));

        var idsForDelete = RecommendationsUtils.collapse(recommendations);

        assertEquals(2, recommendations.size());
        assertRecommendation(null,100L, 3, 6, recommendations.get(0));
        assertRecommendation(3L,200L, 3, 1, recommendations.get(1));

        var expectedSet = new HashSet<Long>();
        expectedSet.add(2L);
        assertEquals(expectedSet, idsForDelete);
    }

    @Test
    public void simpleCollapseTest_WithOnlyNulls() {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(null, 100L, 1, 3));
        recommendations.add(create(null, 100L, 2, 2));
        recommendations.add(create(null, 100L, 0, 1));
        recommendations.add(create(3L, 200L, 3, 1));

        var idsForDelete = RecommendationsUtils.collapse(recommendations);

        assertEquals(2, recommendations.size());
        assertRecommendation(null,100L, 3, 6, recommendations.get(0));
        assertRecommendation(3L,200L, 3, 1, recommendations.get(1));

        var expectedSet = new HashSet<Long>();
        assertEquals(expectedSet, idsForDelete);
    }

    @Test
    public void notCollapseTest() {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(1L, 100L, 1, 3));
        recommendations.add(create(null, 200L, 2, 2));
        recommendations.add(create(3L, 300L, 3, 1));

        var idsForDelete = RecommendationsUtils.collapse(recommendations);

        assertEquals(3, recommendations.size());
        assertRecommendation(1L,100L, 1, 3, recommendations.get(0));
        assertRecommendation(null,200L, 2, 2, recommendations.get(1));
        assertRecommendation(3L,300L, 3, 1, recommendations.get(2));

        var expectedSet = new HashSet<Long>();
        assertEquals(expectedSet, idsForDelete);
    }

    private RecommendationNew create(Long id, long msku, int qty, int adj) {
        var r = new RecommendationNew();
        r.setId(id);
        r.setMsku(msku);
        r.setPurchQty(qty);
        r.setAdjustedPurchQty(adj);
        return r;
    }

    private static void assertRecommendation(Long id, long msku, int qty, int adj, RecommendationNew r) {
        assertEquals(id, r.getId());
        assertEquals(msku, r.getMsku());
        assertEquals(qty, r.getPurchQty());
        assertEquals(adj, r.getAdjustedPurchQty());
    }
}
