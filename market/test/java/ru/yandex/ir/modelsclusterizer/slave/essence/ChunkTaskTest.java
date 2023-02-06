package ru.yandex.ir.modelsclusterizer.slave.essence;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.ir.modelsclusterizer.be.ClusterBlank;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffer;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffersGroup;
import ru.yandex.ir.modelsclusterizer.core.distance.DistanceMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static ru.yandex.ir.modelsclusterizer.slave.essence.ChunkTask.findNearestMatchedOffersInCluster;

public class ChunkTaskTest {
    private List<FormalizedOffer> offers;
    private DistanceMatrix distanceMatrix;
    private List<ClusterBlank> clusterBlanks;

    @Before
    public void init() {
        offers = Arrays.asList(
            buildFormalizedOffer("offer1", "good1", 0L),
            buildFormalizedOffer("offer2", "good2", 1001L),
            buildFormalizedOffer("offer3", "good3", 1002L)
        );

        DistanceMatrix.OfferwiseDistance offerwiseDistance = new DistanceMatrix.OfferwiseDistance();
        offerwiseDistance.setDistanceMatrix(new double[][] {
            {0.0, -5.0, -2.0},
            {-5.0, 0.0, -4.0},
            {-2.0, -4.0, 0.0}
        });
        long clusterId = 1L;
        FormalizedOffersGroup formalizedOffersGroup = buildFormalizedOffersGroup(clusterId);
        offerwiseDistance.prepareOffersChunk(Arrays.asList(formalizedOffersGroup), offers);
        distanceMatrix = new DistanceMatrix(new double[1][1], offerwiseDistance);

        clusterBlanks = new ArrayList<>();
        clusterBlanks.add(new ClusterBlank(clusterId, new int[1], Arrays.asList(formalizedOffersGroup), 0.0));
    }

    @Test
    public void findNearestMatchedOffersInClusterTest() {
        findNearestMatchedOffersInCluster(distanceMatrix, clusterBlanks, offers);
        assertArrayEquals(clusterBlanks.get(0).getNearestOffersPositions(), new int[] {1, 1, 2});
    }

    private FormalizedOffersGroup buildFormalizedOffersGroup(long clusterId) {
        FormalizedOffersGroup.Builder builder = new FormalizedOffersGroup.Builder();
        builder.setFixed(true)
            .setClusterId(clusterId);
        for (int i = 0; i < offers.size(); i++) {
            builder.addPinned(i);
            if (offers.get(i).getMatchedId() > 0) {
                builder.addRelatedGuruModel(offers.get(i).getMatchedId());
            }
        }
        return builder.build();
    }

    private FormalizedOffer buildFormalizedOffer(String id, String goodId, Long matchedId) {
        return FormalizedOffer.newBuilder()
            .setOfferIds(Arrays.asList(id))
            .setMatchedId(matchedId)
            .setClassifierGoodId(goodId)
            .build();
    }
}
