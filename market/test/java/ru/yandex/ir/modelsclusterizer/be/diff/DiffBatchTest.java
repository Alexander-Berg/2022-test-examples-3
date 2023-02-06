package ru.yandex.ir.modelsclusterizer.be.diff;

import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.ir.modelsclusterizer.ClusterizerConstants;
import ru.yandex.ir.modelsclusterizer.be.CategoryInfo;
import ru.yandex.ir.modelsclusterizer.be.Cluster;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffer;
import ru.yandex.ir.modelsclusterizer.be.RepresentativeOfferSet;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author mkrasnoperov
 */
public class DiffBatchTest {
    private CategoryInfo categoryInfo;

    @Before
    public void init() {
        categoryInfo = Mockito.mock(CategoryInfo.class);
    }

    private FormalizedOffer buildFormalizedOffer(List<String> ids, String goodId, Long matchedId) {
        return FormalizedOffer.newBuilder()
            .setOfferIds(ids)
            .setMatchedId(matchedId)
            .setClassifierGoodId(goodId)
            .build();
    }

    private Cluster createCluster(CategoryInfo categoryInfo, long pinnedClusterId,
                                 List<FormalizedOffer> formalizedOffers) {
        SetMultimap<String, String> goodIdToMagicIdOfferMap = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        for (FormalizedOffer offer : formalizedOffers) {
            for (String magicId : offer.getOfferIds()) {
                goodIdToMagicIdOfferMap.put(offer.getClassifierGoodId(), magicId);
            }
        }

        return new Cluster.Builder()
            .setContent(
                new Cluster.ClusterContent.Builder()
                    .setName("Offer name")
                    .setDescr("Offer description")
                    .setVendor(1)
                    .setGoodIdToMagicIdOffersMap(goodIdToMagicIdOfferMap)
                    .setRepresentativeOfferSet(RepresentativeOfferSet.REPRESENTATIVE_OFFER_DISABLED_ENTRY)
                    .setParameters(Collections.emptyList())
                    .build()
            )
            .setRuntimeContext(
                new Cluster.ClusterRuntimeContext.Builder()
                    .setCategoryInfo(categoryInfo)
                    .setSessionId("0")
                    .setFormalizedOffers(formalizedOffers)
                    .setDuplicateRate(0.0)
                    .setPinnedClusterId(pinnedClusterId)
                    .build()
            )
            .build();
    }

    @Test
    public void tailClusteringBuilderSimple() {
        List<FormalizedOffer> newClusterOffers = Arrays.asList(
            buildFormalizedOffer(Collections.singletonList("offer1"), "good1", 1001L)
        );

        Cluster freshCluster = createCluster(categoryInfo,
            ClusterizerConstants.DEFAULT_PINNED_CLUSTER_ID,
            newClusterOffers);
        freshCluster.getContent().unpublish();
        freshCluster.setPersistenceData(
            new Cluster.ClusterPersistenceData.Builder()
                .setClusterIdO(OptionalLong.of(101L))
                .setNewDeveloperProperties(
                    ModelStorage.ClusterizerDeveloperProperties.newBuilder()
                        .setSessionId("sessionId_1")
                        .build()
                )
                .build()
        );
        freshCluster.setPersistenceDataAsNewCluster(
            new Cluster.ClusterPersistenceData.Builder()
                .setNewDeveloperProperties(
                    ModelStorage.ClusterizerDeveloperProperties.newBuilder()
                        .setSessionId("sessionId_1")
                        .setCreatedAsRelativeToGuru(true)
                        .build()
                )
                .build()
        );

        DiffChunk chunk = new DiffChunk.Builder().addClusterToUpdate(freshCluster).build();
        DiffBatch.TailClusteringFiltrationBuilder diffBatchBuilder = new DiffBatch.TailClusteringFiltrationBuilder();
        diffBatchBuilder.addChunk(chunk);
        DiffBatch diffBatch = diffBatchBuilder.build();
        assertFalse(diffBatch.getDeleteDiffList().get(0).getStrongRelation().hasCreateId());
        assertTrue(diffBatch.getDeleteDiffList().get(0).getStrongRelation().hasId());
        assertEquals(diffBatch.getDeleteDiffList().get(0).getStrongRelation().getId(), 1001L);
        assertTrue(diffBatch.getCreateDiffList().get(0)
            .getClusterizerDeveloperProperties().getCreatedAsRelativeToGuru());
        assertEquals(diffBatch.getUpdateDiffList().size(), 0);
    }

    @Test
    public void tailClusteringBuilderNoAction() {
        // Не создавать transition, если кластеру соответствуют 2 разных гуру карточки
        List<FormalizedOffer> newClusterOffers = Arrays.asList(
            buildFormalizedOffer(Collections.singletonList("offer1"), "good1", 1001L),
            buildFormalizedOffer(Collections.singletonList("offer2"), "good2", 1002L)
        );

        Cluster freshCluster = createCluster(categoryInfo,
            ClusterizerConstants.DEFAULT_PINNED_CLUSTER_ID,
            newClusterOffers);
        freshCluster.getContent().unpublish();
        freshCluster.setPersistenceData(
            new Cluster.ClusterPersistenceData.Builder()
                .setClusterIdO(OptionalLong.of(101L))
                .setNewDeveloperProperties(
                    ModelStorage.ClusterizerDeveloperProperties.newBuilder()
                        .setSessionId("sessionId_1")
                        .build()
                )
                .build()
        );
        freshCluster.setPersistenceDataAsNewCluster(
            new Cluster.ClusterPersistenceData.Builder()
                .setNewDeveloperProperties(
                    ModelStorage.ClusterizerDeveloperProperties.newBuilder()
                        .setSessionId("sessionId_1")
                        .setCreatedAsRelativeToGuru(true)
                        .build()
                )
                .build()
        );

        DiffChunk chunk = new DiffChunk.Builder().addClusterToUpdate(freshCluster).build();
        DiffBatch.TailClusteringFiltrationBuilder diffBatchBuilder = new DiffBatch.TailClusteringFiltrationBuilder();
        diffBatchBuilder.addChunk(chunk);
        DiffBatch diffBatch = diffBatchBuilder.build();
        assertEquals(diffBatch.getCreateDiffList().size(), 0);
        assertEquals(diffBatch.getUpdateDiffList().size(), 0);
        assertEquals(diffBatch.getDeleteDiffList().size(), 0);
    }

    @Test
    public void transitionToGuruInsteadOfCreatingTest() {
        DiffChunk.Builder chunkBuilder = new DiffChunk.Builder();

        List<FormalizedOffer> newClusterOffers = Arrays.asList(
            buildFormalizedOffer(Collections.singletonList("offer1"), "good1", 1001L)
        );

        Cluster freshCluster = createCluster(categoryInfo,
            ClusterizerConstants.DEFAULT_PINNED_CLUSTER_ID,
            newClusterOffers);
        freshCluster.getContent().unpublish();
        freshCluster.setPersistenceData(
            new Cluster.ClusterPersistenceData.Builder()
                .setNewDeveloperProperties(
                    ModelStorage.ClusterizerDeveloperProperties.newBuilder()
                        .setSessionId("sessionId_1")
                        .setCreatedAsRelativeToGuru(true)
                        .build()
                )
                .build()
        );
        chunkBuilder.addClusterToCreate(freshCluster);
        chunkBuilder.addDeleteModelDiff(
            new DeleteModelRawDiff.Builder()
                .setClusterId(15)
                .setStrongRelation(new OffersMoveRelation(freshCluster, 1))
                .addRelation(new OffersMoveRelation(freshCluster, 1))
                .build()
        );

        DiffChunk chunk = chunkBuilder.build();
        assertEquals(chunk.getDeleteDiffs().size(), 1);
        DiffBatch.Builder diffBatchBuilder = new DiffBatch.Builder();
        diffBatchBuilder.addChunk(chunk);
        DiffBatch diffBatch = diffBatchBuilder.build();
        assertEquals(1001L, diffBatch.getDeleteDiffList().get(0).getStrongRelation().getId());
        assertFalse(diffBatch.getDeleteDiffList().get(0).getStrongRelation().hasCreateId());
        assertEquals(1, diffBatch.getCreateDiffList().size());

    }
}