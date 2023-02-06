package ru.yandex.market.replenishment.autoorder.utils;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TruckSplitterTest {

    @Test
    public void simpleSplitTest() {
        simpleTest(2.1, 2.3, null);
    }

    @Test
    public void hugeSplitTest() {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(1L, 100L, 3, 3));
        recommendations.add(create(2L, 200L, 2, 3));

        var splitter = new TruckSplitter(recommendations, 2.5, null, null, true);
        splitter.splitRecommendations();
        var splitted = splitter.getSplittedRecommendations();

        assertEquals(4, splitted.size());

        assertEquals(1, splitted.get(0).size());
        assertRecommendation(100L, 2, 2, splitted.get(0).get(0));

        assertEquals(1, splitted.get(1).size());
        assertRecommendation(100L, 1, 1, splitted.get(1).get(0));

        assertEquals(1, splitted.get(2).size());
        assertRecommendation(200L, 1, 2, splitted.get(2).get(0));

        assertEquals(1, splitted.get(3).size());
        assertRecommendation(200L, 1, 1, splitted.get(3).get(0));
    }

    @Test
    public void simpleNoSplitTest() {
        splitWholeTest(null, 2.3, null);
    }

    @Test
    public void splitByWeightTest() {
        simpleTest(2.5, Double.MAX_VALUE, null);
    }

    @Test
    public void splitByWeightTest2() {
        simpleTest(2.5, null, null);
    }

    @Test
    public void splitByVolumeWithShipmentQuantumTest() {
        simpleQuantumTest(null, 7., null, 3L);
    }

    @Test
    public void splitByVolumeTest() {
        simpleTest(Double.MAX_VALUE, 2.5, null);
    }

    @Test
    public void splitByVolumeTest2() {
        simpleTest(null, 2.5, null);
    }

    @Test
    public void splitByOrderSum() {
        simpleTest(null, null, 200L);
    }

    @Test
    public void splitByOrderSum2() {
        simpleTest(2.1, 2.3, 200L);
    }

    @Test
    public void exceptionTest() {
        UserWarningException exception = assertThrows(UserWarningException.class, () ->
                simpleTest(0.9, 100., null));
        assertThat(exception.getMessage(), startsWith("Даже единица товара для MSKU 100 не помещается в грузовик"));
    }

    @Test
    public void splitSeveralTimeTest() {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(1L, 100L, 4, 7));

        var splitter = new TruckSplitter(recommendations, 3.1, null, null, false);
        splitter.splitRecommendations();
        var splitted = splitter.getSplittedRecommendations();

        assertEquals(3, splitted.size());

        assertEquals(1, splitted.get(0).size());
        assertRecommendation(100L, 1, 3, splitted.get(0).get(0));

        assertEquals(1, splitted.get(1).size());
        assertRecommendation(100L, 2, 3, splitted.get(1).get(0));

        assertEquals(1, splitted.get(2).size());
        assertRecommendation(100L, 1, 1, splitted.get(2).get(0));
    }

    @Test
    public void notSplitTest() {
        notSplitTest(7., 7.);
    }

    @Test
    public void notSplitTest2() {
        notSplitTest(null, null);
    }

    @Test
    public void splitWithZeroRecommendations() {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(1L, 100L, 0, 0));
        recommendations.add(create(2L, 200L, 4, 7));
        recommendations.add(create(3L, 300L, 1, 0));

        var splitter = new TruckSplitter(recommendations, 3.1, null, null, false);
        splitter.splitRecommendations();
        var splitted = splitter.getSplittedRecommendations();

        assertEquals(3, splitted.size());

        assertEquals(3, splitted.get(0).size());
        assertRecommendation(200L, 1, 3, splitted.get(0).get(0));
        assertRecommendation(100L, 0, 0, splitted.get(0).get(1));
        assertRecommendation(300L, 1, 0, splitted.get(0).get(2));

        assertEquals(1, splitted.get(1).size());
        assertRecommendation(200L, 2, 3, splitted.get(1).get(0));

        assertEquals(1, splitted.get(2).size());
        assertRecommendation(200L, 1, 1, splitted.get(2).get(0));
    }

    @Test
    public void splitOnlyZeroRecommendations() {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(1L, 100L, 0, 0));
        recommendations.add(create(2L, 200L, 1, 0));
        recommendations.add(create(3L, 300L, 2, 0));

        var splitter = new TruckSplitter(recommendations, 3.1, 8.1, null, false);
        splitter.splitRecommendations();
        var splitted = splitter.getSplittedRecommendations();

        assertEquals(1, splitted.size());

        assertEquals(3, splitted.get(0).size());
        assertRecommendation(100L, 0, 0, splitted.get(0).get(0));
        assertRecommendation(200L, 1, 0, splitted.get(0).get(1));
        assertRecommendation(300L, 2, 0, splitted.get(0).get(2));
    }

    @Test
    public void splitRecommendationsWithNullWeightOrVolume() {
        List<RecommendationNew> recommendations = new ArrayList<>();
        var r = create(1L, 100L, 0, 10);
        r.setWeight(null);
        recommendations.add(r);

        r = create(2L, 200L, 10, 10);
        r.setLength(null);
        recommendations.add(r);

        r = create(3L, 300L, 5, 10);
        r.setWidth(null);
        recommendations.add(r);

        r = create(4L, 400L, 10, 5);
        r.setHeight(null);
        recommendations.add(r);

        var splitter = new TruckSplitter(recommendations, 1000., 1000., null, false);
        splitter.splitRecommendations();
        var splitted = splitter.getSplittedRecommendations();

        assertEquals(1, splitted.size());

        assertEquals(4, splitted.get(0).size());
        assertRecommendation(100L, 0, 10, splitted.get(0).get(0));
        assertRecommendation(200L, 10, 10, splitted.get(0).get(1));
        assertRecommendation(300L, 5, 10, splitted.get(0).get(2));
        assertRecommendation(400L, 10, 5, splitted.get(0).get(3));
    }

    private void simpleTest(Double weight, Double volume, Long orderSum) {
        simpleTest(weight, volume, orderSum, false);
    }

    private void simpleTest(Double weight, Double volume, Long orderSum, boolean oneMskuOneTruck) {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(1L, 100L, 1, 1));
        recommendations.add(create(2L, 200L, 3, 3));

        var splitter = new TruckSplitter(recommendations, weight, volume, orderSum, oneMskuOneTruck);
        splitter.splitRecommendations();
        var splitted = splitter.getSplittedRecommendations();

        assertEquals(2, splitted.size());

        assertEquals(2, splitted.get(0).size());
        assertRecommendation(100L, 1, 1, splitted.get(0).get(0));
        assertRecommendation(200L, 1, 1, splitted.get(0).get(1));

        assertEquals(1, splitted.get(1).size());
        assertRecommendation(200L, 2, 2, splitted.get(1).get(0));
    }

    private void splitWholeTest(Double weight, Double volume, Long orderSum) {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(1L, 100L, 1, 1));
        recommendations.add(create(2L, 200L, 2, 2));

        var splitter = new TruckSplitter(recommendations, weight, volume, orderSum, true);
        splitter.splitRecommendations();
        var splitted = splitter.getSplittedRecommendations();

        assertEquals(2, splitted.size());

        assertEquals(1, splitted.get(0).size());
        assertRecommendation(100L, 1, 1, splitted.get(0).get(0));

        assertEquals(1, splitted.get(1).size());
        assertRecommendation(200L, 2, 2, splitted.get(1).get(0));
    }

    private void simpleQuantumTest(Double weight, Double volume, Long orderSum, Long quantum) {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(1L, 100L, 4, 4, quantum));
        recommendations.add(create(2L, 200L, 4, 4, quantum));

        var splitter = new TruckSplitter(recommendations, weight, volume, orderSum, true);
        splitter.splitRecommendations();
        var splitted = splitter.getSplittedRecommendations();

        assertEquals(2, splitted.size());

        assertEquals(1, splitted.get(0).size());
        assertRecommendation(100L, 4, 4, splitted.get(0).get(0));

        assertEquals(1, splitted.get(1).size());
        assertRecommendation(200L, 4, 4, splitted.get(1).get(0));
    }

    private void notSplitTest(Double weight, Double volume) {
        List<RecommendationNew> recommendations = new ArrayList<>();
        recommendations.add(create(1L, 100L, 1, 3));
        recommendations.add(create(2L, 100L, 2, 2));
        recommendations.add(create(3L, 200L, 3, 1));

        var splitter = new TruckSplitter(recommendations, weight, volume, null, false);
        splitter.splitRecommendations();
        var splitted = splitter.getSplittedRecommendations();

        assertEquals(1, splitted.size());

        assertEquals(3, splitted.get(0).size());
        assertRecommendation(100L, 1, 3, splitted.get(0).get(0));
        assertRecommendation(100L, 2, 2, splitted.get(0).get(1));
        assertRecommendation(200L, 3, 1, splitted.get(0).get(2));
    }

    private RecommendationNew create(long id, long msku, int qty, int adj) {
        var r = new RecommendationNew();
        r.setId(id);
        r.setMsku(msku);
        r.setPurchQty(qty);
        r.setAdjustedPurchQty(adj);
        r.setWeight(1000L); // weight = 1 кг
        r.setWidth(100L);
        r.setHeight(100L);
        r.setLength(100L); // volume = 1 м^3
        r.setPurchaseResultPrice(100.);
        return r;
    }

    private RecommendationNew create(long id, long msku, int qty, int adj, long quantum) {
        var r = new RecommendationNew();
        r.setId(id);
        r.setMsku(msku);
        r.setPurchQty(qty);
        r.setAdjustedPurchQty(adj);
        r.setWeight(100L); // weight = 1 кг
        r.setWidth(100L);
        r.setHeight(100L);
        r.setLength(100L); // volume = 1 м^3
        r.setPurchaseResultPrice(100.);
        r.setShipmentQuantum(quantum);
        return r;
    }

    private static void assertRecommendation(long msku, int qty, int adj, RecommendationNew r) {
        assertEquals(msku, r.getMsku());
        assertEquals(qty, r.getPurchQty());
        assertEquals(adj, r.getAdjustedPurchQty());
    }
}
