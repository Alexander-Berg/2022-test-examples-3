package ru.yandex.ir.clusterer.hierarchy;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffersGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author mkrasnoperov
 */
public class MarketLogicUnionManagerTest {
    private void clusterizeAndCheck(double[][] weights, List<FormalizedOffersGroup> elements, int[][] expected) {

        HierarchicalClustering<ClusterMeta> clustering = new HierarchicalClustering<>(
            MarketLogicUnionManager.create(weights, elements)
        );
        List<int[]> clusters = clustering.clusterizeAndGetIndexes();
        ClusterTestUtils.assertSameClusterSet(Arrays.asList(expected), clusters);

    }

    @Test
    public void checkFringe() {
        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(createFormalizedOffersGroup(true, 0, 100));
        elements.add(createFormalizedOffersGroup(true, 1, 101));
        elements.add(createFormalizedOffersGroup(false, 2, 102));
        elements.add(createFormalizedOffersGroup(false, 3, 103));
        elements.add(createFormalizedOffersGroup(false, 4, 104));
        elements.add(createFormalizedOffersGroup(false, 5, 105));
        clusterizeAndCheck(
            new double[][]{
                {0.0, -100.0, -3, -2, -1, 0.0},
                {-100.0, 0.0, -2.5, -2, -1, 0.0},
                {-3, -2.5, 0.0, -0.5, 0, 0},
                {-2, -2, -0.5, 0.0, 0, 0},
                {2, -1, 0, 0, 0.0, -101},
                {0, 0, 0, 0, -101, 0.0},
            },
            elements, new int[][]{{0, 2, 3}, {1, 4, 5}}
        );
    }

    @Test
    public void checkFringe2() {
        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(createFormalizedOffersGroup(true, 0, 100));
        elements.add(createFormalizedOffersGroup(true, 1, 101));
        elements.add(createFormalizedOffersGroup(false, 2, 102));
        elements.add(createFormalizedOffersGroup(false, 3, 103));
        elements.add(createFormalizedOffersGroup(false, 4, 104));
        elements.add(createFormalizedOffersGroup(false, 5, 105));
        clusterizeAndCheck(
            new double[][]{
                {0.0, -100.0, -3, -2, 2, -3},
                {-100.0, 0.0, -2.5, -2, 0, 0.0},
                {-3, -2.5, 0.0, -0.5, 0, 0},
                {-2, -2, -0.5, 0.0, 0, 0},
                {2, 0, 0, 0, 0.0, -101},
                {-3, 0, 0, 0, -101, 0.0},
            },
            elements, new int[][]{{0, 2, 3, 4, 5}, {1}}
        );
    }

    @Test
    public void checkCandidateTwoNotFixedTest() {
        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(createFormalizedOffersGroup(false, 0, 100));
        elements.add(createFormalizedOffersGroup(false, 1, 101));
        clusterizeAndCheck(
            new double[][]{
                {0.0, -2.0},
                {-2.0, 0.0},
            },
            elements, new int[][]{{0, 1}}
        );
    }

    @Test
    public void checkCandidateOneNotFixedTest() {
        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(createFormalizedOffersGroup(true, 0, 100));
        elements.add(createFormalizedOffersGroup(false, 1, 101));
        clusterizeAndCheck(
            new double[][]{
                {0.0, -2.0},
                {-2.0, 0.0},
            },
            elements, new int[][]{{0, 1}}
        );
    }

    @Test
    public void checkCandidateTwoFixedOneFreeTest() {
        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(createFormalizedOffersGroup(true, 0, 100));
        elements.add(createFormalizedOffersGroup(true, 1, 101));
        elements.add(createFormalizedOffersGroup(false, 2, 102));
        clusterizeAndCheck(
            new double[][]{
                {0.0, -3.0, -1.0},
                {-3.0, 0.0, -2.0},
                {-1.0, -2.0, 0.0},
            },
            elements, new int[][]{{0}, {1, 2}}
        );
    }

    @Test
    public void checkCandidateTwoFixedOneFree2Test() {
        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(createFormalizedOffersGroup(true, 0, 100));
        elements.add(createFormalizedOffersGroup(true, 1, 101));
        elements.add(createFormalizedOffersGroup(false, 2, 102));
        clusterizeAndCheck(
            new double[][]{
                {0.0, -3.0, 1.0},
                {-3.0, 0.0, 2.0},
                {1.0, 2.0, 0.0},
            },
            elements, new int[][]{{0}, {1}, {2}}
        );
    }

    @Test
    public void checkCandidateThrownTest() {
        FormalizedOffersGroup.Builder builder = new FormalizedOffersGroup.Builder();
        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(builder.addPinned(0).addPinned(1).addThrowed(3).setClusterId(100).buildFixed());
        builder.clear();
        elements.add(builder.addPinned(3).setClusterId(101).buildFixed());
        clusterizeAndCheck(
            new double[][]{
                {0.0, -2.0},
                {-2.0, 0.0},
            },
            elements, new int[][]{{0}, {1}}
        );
    }

    @Test
    public void checkCandidateWithoutThrownTest() {
        FormalizedOffersGroup.Builder builder = new FormalizedOffersGroup.Builder();

        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(builder.addPinned(0).addPinned(1).addThrowed(2).setClusterId(100).buildFixed());
        builder.clear();
        elements.add(builder.addPinned(3).setClusterId(101).buildFixed());
        clusterizeAndCheck(
            new double[][]{
                {0.0, -2.0},
                {-2.0, 0.0},
            },
            elements, new int[][]{{0}, {1}}
        );
    }

    @Test
    public void checkTailClusteringSimple1Test() {
        FormalizedOffersGroup.Builder builder = new FormalizedOffersGroup.Builder();

        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(builder.addPinned(0).addRelatedGuruModel(1001).build());
        builder.clear();
        elements.add(builder.addPinned(1).addRelatedGuruModel(1001).build());
        // ожидаем, что даже отнесение офферов к одной гуру-модели не служит причиной объединения
        // Наружу эти кластера всеравно не опубликованы, а нам позволяет увереннее кластеризовать.
        clusterizeAndCheck(
            new double[][]{
                {0.0, 2.0},
                {2.0, 0.0},
            },
            elements, new int[][]{{0}, {1}}
        );
    }

    @Test
    public void checkTailClusteringSimple2Test() {
        FormalizedOffersGroup.Builder builder = new FormalizedOffersGroup.Builder();

        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(builder.addPinned(2).addRelatedGuruModel(1001).build());
        builder.clear();
        elements.add(builder.addPinned(3).addPinned(4).addRelatedGuruModel(1001).build());
        clusterizeAndCheck(
            new double[][]{
                {0.0, -2.0},
                {-2.0, 0.0},
            },
            elements, new int[][]{{0, 1}}
        );
    }


    @Test
    @Ignore
    public void checkTailClusteringFixedVsGuruTest() {
        FormalizedOffersGroup.Builder builder = new FormalizedOffersGroup.Builder();

        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(builder.addPinned(2).addPinned(3).addRelatedGuruModel(1001).build());
        builder.clear();
        elements.add(builder.addPinned(4).setClusterId(101).buildFixed());
        clusterizeAndCheck(
            new double[][]{
                {0.0, -2.0},
                {-2.0, 0.0},
            },
            elements, new int[][]{{0, 1}}
        );
    }

    @Test
    @Ignore
    public void checkTailClusteringGuruVsNoGuruTest() {
        FormalizedOffersGroup.Builder builder = new FormalizedOffersGroup.Builder();

        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(builder.addPinned(2).addPinned(3).addRelatedGuruModel(1001).build());
        builder.clear();
        elements.add(builder.addPinned(4).build());
        clusterizeAndCheck(
            new double[][]{
                {0.0, -2.0},
                {-2.0, 0.0},
            },
            elements, new int[][]{{0, 1}}
        );
    }

    @Test
    public void checkTailClusteringGuruNoConflictTest() {
        FormalizedOffersGroup.Builder builder = new FormalizedOffersGroup.Builder();

        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(builder.addPinned(2).addPinned(3).addRelatedGuruModel(1001).addRelatedGuruModel(1002).build());
        builder.clear();
        elements.add(builder.addPinned(4).addRelatedGuruModel(1001).build());
        clusterizeAndCheck(
            new double[][]{
                {0.0, -2.0},
                {-2.0, 0.0},
            },
            elements, new int[][]{{0, 1}}
        );
    }

    @Test
    public void checkTailClusteringGuruConflict1Test() {
        FormalizedOffersGroup.Builder builder = new FormalizedOffersGroup.Builder();

        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(builder.addPinned(2).addPinned(3).addRelatedGuruModel(1001).addRelatedGuruModel(1002).build());
        builder.clear();
        elements.add(builder.addPinned(4).addPinned(5).addRelatedGuruModel(1001).addRelatedGuruModel(1003).build());
        clusterizeAndCheck(
            new double[][]{
                {0.0, -2.0},
                {-2.0, 0.0},
            },
            elements, new int[][]{{0}, {1}}
        );
    }

    @Test
    @Ignore
    public void checkTailClusteringUndefinedBehaviourConflict1Test() {
        // Если у нас уже есть кластер в котором есть офферы из разных гуру-карточек, то мы можем использовать разные
        // стратегии объединения. Например, объединять с другими группами, которые содержат офферы из этих же
        // гуру-карточек. Но тогда возникает ситуация, когда мы две группы вначале не можем объединить (см группы 2 и 3)
        // А после объединения одной из них с первой, теоретически уже можем объединить. Но в силу использования
        // текущей реализации иерархической кластеризации мы поддерживаем контракт:
        // Если две группы нельзя склеить на шаге X, то и в дальнейшем нельзя склеить их или же объединения
        // групп, содержащие эти группы.

        // Тест демонстративный. Пока заигнорил.
        FormalizedOffersGroup.Builder builder = new FormalizedOffersGroup.Builder();

        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(builder.addPinned(2).addPinned(3).addRelatedGuruModel(1001).addRelatedGuruModel(1002).build());
        builder.clear();
        elements.add(builder.addPinned(4).addRelatedGuruModel(1001).build());
        builder.clear();
        elements.add(builder.addPinned(5).addRelatedGuruModel(1002).build());
        clusterizeAndCheck(
            new double[][]{
                {0.0, -2.0, -2.0},
                {-2.0, 0.0, -3.0},
                {-2.0, -3.0, 0.0},
            },
            elements, new int[][]{{0, 1}}
        );
    }

    @Test
    public void checkTailClusteringBigKeepGuruIdTest() {
        FormalizedOffersGroup.Builder builder = new FormalizedOffersGroup.Builder();

        List<FormalizedOffersGroup> elements = new ArrayList<>();
        elements.add(builder.addPinned(2).addPinned(3).addRelatedGuruModel(1001).build());
        builder.clear();
        elements.add(builder.addPinned(4).addRelatedGuruModel(1001).build());
        builder.clear();
        elements.add(builder.addPinned(5).build());
        clusterizeAndCheck(
            new double[][]{
                {0.0, -5.0, -2.0},
                {-5.0, 0.0, 0.0},
                {-2.0, 0.0, 0.0},
            },
            elements, new int[][]{{0, 1, 2}}
        );
    }

    private FormalizedOffersGroup createFormalizedOffersGroup(
        boolean fixed, int formalizedOffersPosition, long clusterId
    ) {
        return new FormalizedOffersGroup.Builder()
            .setFixed(fixed)
            .addPinned(formalizedOffersPosition)
            .setClusterId(clusterId)
            .build();

    }

}