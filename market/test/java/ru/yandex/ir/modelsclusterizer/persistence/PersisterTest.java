package ru.yandex.ir.modelsclusterizer.persistence;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.ir.modelsclusterizer.ClusterizerConstants;
import ru.yandex.ir.modelsclusterizer.be.CategoryInfo;
import ru.yandex.ir.modelsclusterizer.be.Cluster;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffer;
import ru.yandex.ir.modelsclusterizer.be.OldClusterMetadata;
import ru.yandex.ir.modelsclusterizer.be.RepresentativeOfferSet;
import ru.yandex.ir.modelsclusterizer.be.diff.DiffChunk;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Evgeniya Yakovleva, <a href="mailto:ragvena@yandex-team.ru"/>
 */
public class PersisterTest {
    private CategoryInfo categoryInfo;
    private Persister persister;

    @Before
    public void init() {
        categoryInfo = Mockito.mock(CategoryInfo.class);
        persister = new Persister();
    }

    //Если новый кластер собрался из старого с pinned и других кластеров.
    @Test
    public void makeDiffChunkTest() {
        // новый кластер, который имитирует объединение кластера с pinned и кластера без pinned.
        // кластер с pinned имеет id 1 и состав {1}
        // кластер без pinned имеет состав {2, 3 ,4}
        SetMultimap<String, String> goodIdToMagicId = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        goodIdToMagicId.put("1", "1");
        goodIdToMagicId.put("1", "2");
        goodIdToMagicId.put("1", "3");
        goodIdToMagicId.put("1", "4");
        Cluster freshCluster = buildCluster(categoryInfo, 42, goodIdToMagicId, 1);


        Set<Cluster> freshClusters = new HashSet<>();
        freshClusters.add(freshCluster);

        // заполняем историю
        Long2ObjectMap<OldClusterMetadata> oldClustersById = new Long2ObjectLinkedOpenHashMap<>();
        oldClustersById.put(1, getOldClusterMetadata(1, new HashSet<String>() {{
            add("1");
        }}));

        oldClustersById.put(2, getOldClusterMetadata(2, new HashSet<String>() {{
            add("2");
            add("3");
            add("4");
        }}));

        LongList parentList = new LongArrayList();
        parentList.add(1);
        parentList.add(2);


        DiffChunk actual = persister.makeDiffChunk(oldClustersById, parentList, freshClusters);
        assertEquals(actual.getClustersToUpdate().get(0).getPersistenceData().getClusterIdO().getAsLong(), 1);
        assertTrue(actual.getDeleteDiffs().get(0).getStrongRelation().getHeir() == freshCluster);
    }

    @Test
    public void mergeClusterizerDeveloperPropertiesUpdateClustersTest() {
        SetMultimap<String, String> goodIdToMagicId = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        goodIdToMagicId.put("1", "1");
        goodIdToMagicId.put("1", "2");

        Cluster freshCluster = createCluster(categoryInfo,
            ClusterizerConstants.DEFAULT_PINNED_CLUSTER_ID,
            goodIdToMagicId);
        Cluster pinnedCluster = createCluster(categoryInfo, 1, goodIdToMagicId);

        ModelStorage.ClusterizerDeveloperProperties oldClusterDeveloperProperties =
            ModelStorage.ClusterizerDeveloperProperties.newBuilder()
                .setDuplicatesRate(0.5)
                .setSessionId("0.5")
                .setCreatedAsRelativeToGuru(true)
                .build();

        Long2ObjectMap<OldClusterMetadata> oldClusterMetadatas = new Long2ObjectLinkedOpenHashMap<>();
        oldClusterMetadatas.put(1, getOldClusterMetadata(1, new HashSet<String>() {{
            add("1");
            add("2");
        }}, oldClusterDeveloperProperties));

        List<DiffChunk> diffChunks = persister.enrichHistory(oldClusterMetadatas, Lists.newArrayList(freshCluster));
        assertEquals(1, diffChunks.size());
        assertEquals(1, diffChunks.get(0).getClustersToUpdate().size());
        assertEquals(freshCluster.getPersistenceData().getNewDeveloperProperties().getCreatedAsRelativeToGuru(),
            oldClusterDeveloperProperties.getCreatedAsRelativeToGuru());
        assertEquals(freshCluster.getPersistenceData().getNewDeveloperProperties().getDuplicatesRate(),
            0.1, 1e-9);

        List<DiffChunk> diffChunksForPinned = persister.enrichHistory(oldClusterMetadatas,
            Lists.newArrayList(pinnedCluster));
        assertEquals(1, diffChunksForPinned.size());
        assertEquals(1, diffChunksForPinned.get(0).getClustersToUpdate().size());
        assertEquals(pinnedCluster.getPersistenceData().getNewDeveloperProperties().getCreatedAsRelativeToGuru(),
            oldClusterDeveloperProperties.getCreatedAsRelativeToGuru());
    }

    private FormalizedOffer buildFormalizedOffer(List<String> ids, String goodId, Long matchedId) {
        return FormalizedOffer.newBuilder()
            .setOfferIds(ids)
            .setMatchedId(matchedId)
            .setClassifierGoodId(goodId)
            .build();
    }

    @Test
    public void publishNewClustersWithoutMatchedOffersTest() {
        List<FormalizedOffer> newClusterOffers = Arrays.asList(
            buildFormalizedOffer(Collections.singletonList("offer1"), "good1", 0L)
        );

        Cluster freshCluster = createCluster(categoryInfo,
            ClusterizerConstants.DEFAULT_PINNED_CLUSTER_ID,
            newClusterOffers);


        assertEquals(freshCluster.getContent().isPublished(), true);


        Long2ObjectMap<OldClusterMetadata> oldClusterMetadatas = new Long2ObjectLinkedOpenHashMap<>();

        List<DiffChunk> diffChunks = persister.enrichHistory(oldClusterMetadatas, Lists.newArrayList(freshCluster));
        assertEquals(diffChunks.get(0).getClustersToCreate().get(0).getContent().isPublished(), true);
    }

    @Test
    public void unpublishNewClustersWithMatchedOffersTest() {
        List<FormalizedOffer> newClusterOffers = Arrays.asList(
            buildFormalizedOffer(Collections.singletonList("offer1"), "good1", 0L),
            buildFormalizedOffer(Collections.singletonList("offer2"), "good2", 1001L)
        );

        Cluster freshCluster = createCluster(categoryInfo,
            ClusterizerConstants.DEFAULT_PINNED_CLUSTER_ID,
            newClusterOffers);

        assertEquals(freshCluster.getContent().isPublished(), false);

        Long2ObjectMap<OldClusterMetadata> oldClusterMetadatas = new Long2ObjectLinkedOpenHashMap<>();

        List<DiffChunk> diffChunks = persister.enrichHistory(oldClusterMetadatas, Lists.newArrayList(freshCluster));
        assertEquals(diffChunks.get(0).getClustersToCreate().get(0).getContent().isPublished(), false);
        assertEquals(diffChunks.get(0).getClustersToCreate().get(0).getPersistenceData()
            .getNewDeveloperProperties().getCreatedAsRelativeToGuru(), true);
    }

    @Test
    public void publishUpdatingClustersWithMatchedOffersFromHBaseTest() {
        FormalizedOffer offer = buildFormalizedOffer(Collections.singletonList("offer2"), "good2", 1001L);
        offer.setFullyFromHBase(new HashSet<>(Collections.singletonList("offer2")));
        List<FormalizedOffer> newClusterOffers = Arrays.asList(
            offer
        );

        Cluster freshCluster = createCluster(categoryInfo,
            ClusterizerConstants.DEFAULT_PINNED_CLUSTER_ID,
            newClusterOffers);



        assertEquals(freshCluster.getContent().isPublished(), true);


        Long2ObjectMap<OldClusterMetadata> oldClusterMetadatas = new Long2ObjectLinkedOpenHashMap<>();

        oldClusterMetadatas.put(117, getOldClusterMetadata(117, new HashSet<String>() {{
            add("offer2");
        }}));

        List<DiffChunk> diffChunks = persister.enrichHistory(oldClusterMetadatas, Lists.newArrayList(freshCluster));
        assertEquals(diffChunks.get(0).getClustersToUpdate().get(0).getContent().isPublished(), true);
        assertEquals(diffChunks.get(0).getClustersToUpdate().get(0).getPersistenceData()
            .getClusterIdO().getAsLong(), 117L);
    }

    @Test
    public void unpublishUpdatingClustersWithMatchedOffersTest() {
        List<FormalizedOffer> newClusterOffers = Arrays.asList(
            buildFormalizedOffer(Collections.singletonList("offer1"), "good1", 0L),
            buildFormalizedOffer(Collections.singletonList("offer2"), "good2", 1001L)
        );

        Cluster freshCluster = createCluster(categoryInfo,
            ClusterizerConstants.DEFAULT_PINNED_CLUSTER_ID,
            newClusterOffers);

        assertEquals(freshCluster.getContent().isPublished(), false);

        Long2ObjectMap<OldClusterMetadata> oldClusterMetadatas = new Long2ObjectLinkedOpenHashMap<>();

        oldClusterMetadatas.put(117, getOldClusterMetadata(117, new HashSet<String>() {{
            add("offer1");
            add("offer2");
        }}));

        List<DiffChunk> diffChunks = persister.enrichHistory(oldClusterMetadatas, Lists.newArrayList(freshCluster));
        assertEquals(diffChunks.get(0).getClustersToUpdate().get(0).getContent().isPublished(), false);
        assertEquals(diffChunks.get(0).getClustersToUpdate().get(0).getPersistenceData()
            .getClusterIdO().getAsLong(), 117L);
        assertEquals(diffChunks.get(0).getClustersToUpdate().get(0).getPersistenceData()
            .getNewDeveloperProperties().getCreatedAsRelativeToGuru(), false);

    }

    @Test
    public void multipleMatchedOffersInClusterTest() {
        List<FormalizedOffer> newClusterOffers = Arrays.asList(
            buildFormalizedOffer(Collections.singletonList("offer1"), "good1", 1002L),
            buildFormalizedOffer(Collections.singletonList("offer2"), "good2", 1001L)
        );

        Cluster freshCluster = createCluster(categoryInfo,
            ClusterizerConstants.DEFAULT_PINNED_CLUSTER_ID,
            newClusterOffers);

        assertEquals(freshCluster.getContent().isPublished(), false);

        Long2ObjectMap<OldClusterMetadata> oldClusterMetadatas = new Long2ObjectLinkedOpenHashMap<>();

        oldClusterMetadatas.put(117, getOldClusterMetadata(117, new HashSet<String>() {{
            add("offer1");
            add("offer2");
        }}));

        List<DiffChunk> diffChunks = persister.enrichHistory(oldClusterMetadatas, Lists.newArrayList(freshCluster));
        assertEquals(diffChunks.get(0).getClustersToUpdate().get(0).getContent().isPublished(), false);
        assertEquals(diffChunks.get(0).getClustersToUpdate().get(0).getPersistenceData()
            .getClusterIdO().getAsLong(), 117L);
        assertEquals(diffChunks.get(0).getClustersToUpdate().get(0).getPersistenceData()
            .getNewDeveloperProperties().getCreatedAsRelativeToGuru(), false);

    }

    @Test
    public void mergeClusterizerDeveloperPropertiesCreateClustersTest() {
        SetMultimap<String, String> goodIdToMagicId = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        goodIdToMagicId.put("1", "1");
        goodIdToMagicId.put("1", "2");

        Cluster freshCluster = createCluster(categoryInfo,
                                             ClusterizerConstants.DEFAULT_PINNED_CLUSTER_ID,
                                             goodIdToMagicId);

        ModelStorage.ClusterizerDeveloperProperties oldClusterDeveloperProperties =
            ModelStorage.ClusterizerDeveloperProperties.newBuilder()
                .setDuplicatesRate(0.5)
                .setSessionId("0.5")
                .build();
        Long2ObjectMap<OldClusterMetadata> oldClusterMetadatas = new Long2ObjectLinkedOpenHashMap<>();
        oldClusterMetadatas.put(1, getOldClusterMetadata(1, new HashSet<String>() {{
            add("7");
            add("8");
        }}, oldClusterDeveloperProperties));

        List<DiffChunk> diffChunks = persister.enrichHistory(oldClusterMetadatas,
                                                                       Lists.newArrayList(freshCluster));
        assertEquals(1, diffChunks.size());
        assertEquals(1, diffChunks.get(0).getClustersToCreate().size());
        assertTrue(freshCluster.getPersistenceData().getNewDeveloperProperties() != null);
        assertNotEquals(freshCluster.getPersistenceData().getNewDeveloperProperties(), oldClusterDeveloperProperties);
    }

    private Cluster createCluster(CategoryInfo categoryInfo, long pinnedClusterId,
                                 SetMultimap<String, String> goodIdToMagicIdOfferMap) {
        return buildCluster(categoryInfo, 1, Collections.emptyList(), goodIdToMagicIdOfferMap, pinnedClusterId, 0.1);
    }

    private Cluster createCluster(CategoryInfo categoryInfo, long pinnedClusterId,
                                 List<FormalizedOffer> formalizedOffers) {
        SetMultimap<String, String> goodIdToMagicIdOfferMap = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        for (FormalizedOffer offer : formalizedOffers) {
            for (String magicId : offer.getOfferIds()) {
                goodIdToMagicIdOfferMap.put(offer.getClassifierGoodId(), magicId);
            }
        }
        return buildCluster(categoryInfo, 1, formalizedOffers, goodIdToMagicIdOfferMap, pinnedClusterId, 0.1);
    }

    private OldClusterMetadata getOldClusterMetadata(long id, Set<String> offerIds,
                                                     ModelStorage.ClusterizerDeveloperProperties properties) {
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(id).setClusterizerDeveloperProperties(properties)
            .addAllClusterizerOfferIds(offerIds).build();

        return new OldClusterMetadata(model, false);
    }

    private OldClusterMetadata getOldClusterMetadata(long id, Set<String> offerIds) {
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(id)
            .addAllClusterizerOfferIds(offerIds).build();

        return new OldClusterMetadata(model, false);
    }


    public static Cluster buildCluster(CategoryInfo categoryInfo, long vendorId,
                                       SetMultimap<String, String> goodIdToMagicId,
                                       long pinnedClusterId) {
        return buildCluster(categoryInfo, vendorId, Collections.emptyList(), goodIdToMagicId, pinnedClusterId, 0.0);
    }

    public static Cluster buildCluster(CategoryInfo categoryInfo, long vendorId,
                                       List<FormalizedOffer> formalizedOffers,
                                       SetMultimap<String, String> goodIdToMagicId,
                                       long pinnedClusterId, double duplicateRate) {

        return new Cluster.Builder()
            .setRuntimeContext(
                new Cluster.ClusterRuntimeContext.Builder()
                    .setCategoryInfo(categoryInfo)
                    .setSessionId("0")
                    .setFormalizedOffers(formalizedOffers)
                    .setDuplicateRate(duplicateRate)
                    .setPinnedClusterId(pinnedClusterId)
                    .build()
            )
            .setContent(
                new Cluster.ClusterContent.Builder()
                    .setVendor(vendorId)
                    .setGoodIdToMagicIdOffersMap(goodIdToMagicId)
                    .setRepresentativeOfferSet(RepresentativeOfferSet.REPRESENTATIVE_OFFER_DISABLED_ENTRY)
                    .build()
            )
            .build();
    }
}
