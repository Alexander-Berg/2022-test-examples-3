package ru.yandex.ir.modelsclusterizer.slave;

import com.google.common.collect.SetMultimap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.ir.modelsclusterizer.be.CategoryMetadata;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffer;
import ru.yandex.ir.modelsclusterizer.be.OldClusterMetadata;
import ru.yandex.ir.modelsclusterizer.persistence.pinned.BiggestByMappedOffersStrategy;
import ru.yandex.ir.modelsclusterizer.be.OfferToClusterMetadata;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Evgeniya Yakovleva, <a href="mailto:ragvena@yandex-team.ru"/>
 */
public class BiggestByMappedOffersStrategyTest {
    CategoryMetadata categoryMetadata;
    FormalizedOffer.FormalizedOfferBuilder formalizedOfferBuilder;
    BiggestByMappedOffersStrategy strategy;
    SetMultimap<String, OfferToClusterMetadata> pinnedOfferIdToClusterId;
    Long2ObjectMap<OldClusterMetadata> clustersData;

    @Before
    public void init() {
        Long2ObjectMap<Long2ObjectMap<OldClusterMetadata>> structure = new Long2ObjectArrayMap<>();
        clustersData = new Long2ObjectArrayMap<>();

        // mock истории кластеров
        int vendorId = 1;
        long timestamp = System.currentTimeMillis();
        //expired кластера, все оферы положил кластеризатор
        addClusterData(clustersData, 1, timestamp, getClusterizedAddOfferInfo("o1", "o2"));
        addClusterData(clustersData, 5, timestamp, getClusterizedAddOfferInfo("o14", "o15", "o16", "o17"));

        List<ModelStorage.OfferInfo> pinned = new ArrayList<>();
        pinned.add(getOfferInfo("o18", false));
        pinned.add(getOfferInfo("o19", false));
        //expired кластер, оферы припинены вручную
        addClusterData(clustersData, 7, timestamp, pinned);

        //живые кластер, все оферы положил кластеризатор
        addClusterData(clustersData, 2, 0, getClusterizedAddOfferInfo("o3", "o4", "o5"));
        addClusterData(clustersData, 3, 0, getClusterizedAddOfferInfo("o6", "o7", "o8", "o9"));
        addClusterData(clustersData, 4, 0, getClusterizedAddOfferInfo("o10", "o11", "o12", "o13"));
        addClusterData(clustersData, 6, 0, Arrays.asList(ModelStorage.OfferInfo.getDefaultInstance()));
        addClusterData(clustersData, 8, 0, getClusterizedAddOfferInfo("o18", "o19", "o20", "o21"));


        structure.put(vendorId, clustersData);
        categoryMetadata = CategoryMetadata.construct(42, 0, structure, Collections.<String>emptySet());
        pinnedOfferIdToClusterId = categoryMetadata.getTransposeStructureWithMetaData(
            OldClusterMetadata::getPinnedOfferIds
        ).get(vendorId);

        formalizedOfferBuilder = FormalizedOffer.newBuilder().setVendorId(vendorId);

        strategy = new BiggestByMappedOffersStrategy();
    }

    private ModelStorage.OfferInfo getOfferInfo(String offerId, boolean addedByClusterizer) {
        return ModelStorage.OfferInfo.newBuilder().setOfferId(offerId).setUserId(addedByClusterizer ? ParameterValueComposer
            .CLUSTERIZER_USER_ID : 1).build();
    }

    private List<ModelStorage.OfferInfo> getClusterizedAddOfferInfo(String... offerIds) {
        List<ModelStorage.OfferInfo> offerInfos = new ArrayList<>(offerIds.length);
        for (String offerId : offerIds) {
            offerInfos.add(getOfferInfo(offerId, true));
        }
        return offerInfos;
    }

    private void addClusterData(Long2ObjectMap<OldClusterMetadata> clustersData, int clusterId, long expiredAt,
                                List<ModelStorage.OfferInfo> pinnedOfferInfos) {
        clustersData.put(clusterId, initOldClusterMetadata(clusterId, expiredAt, pinnedOfferInfos));
    }

    private OldClusterMetadata initOldClusterMetadata(int clusterId, long expiredAt, List<ModelStorage.OfferInfo>
        pinnedOfferInfos) {

        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder().setId(clusterId)
            .setExpiredDate(expiredAt);
        pinnedOfferInfos.stream().forEach(offerInfo -> builder.addClusterizerOfferIds(offerInfo.getOfferId()));
        builder.addAllPinnedOfferInfos(pinnedOfferInfos);


        return new OldClusterMetadata(builder.build(), false);
    }

    @Test
    public void getBiggestLiveClusterTest() {
        FormalizedOffer formalizedOffer = formalizedOfferBuilder.setOfferIds(
            Arrays.asList(
                "o1", "o2",
                "o10", "o11", "o12", "o13"
            )
        ).build();

        Long actualClusterId = strategy.chosePinnedClusterId(
            formalizedOffer, pinnedOfferIdToClusterId
        );
        assertEquals("getBiggestLiveClusterTest", Long.valueOf(4), actualClusterId);
    }


    @Test
    public void getOldestLiveClusterTest() {
        FormalizedOffer formalizedOffer = formalizedOfferBuilder.setOfferIds(
            Arrays.asList(
                "o1", "o2",
                "o6", "o7",
                "o10", "o11"
            )
        ).build();

        Long actualClusterId = strategy.chosePinnedClusterId(
            formalizedOffer, pinnedOfferIdToClusterId
        );
        assertEquals("getOldestLiveClusterTest", Long.valueOf(3), actualClusterId);
    }

    @Test
    public void getBiggestExpiredClusterTest() {
        FormalizedOffer formalizedOffer = formalizedOfferBuilder.setOfferIds(
            Arrays.asList(
                "o1", "o2",
                "o6", "o7",
                "o14", "o15", "o16", "o17"
            )
        ).build();

        Long actualClusterId = strategy.chosePinnedClusterId(
            formalizedOffer, pinnedOfferIdToClusterId
        );
        assertEquals("getBiggestExpiredClusterTest", Long.valueOf(5), actualClusterId);
    }

    @Test
    public void getOldestExpiredClusterTest() {
        FormalizedOffer formalizedOffer = formalizedOfferBuilder.setOfferIds(
            Arrays.asList(
                "o1", "o2",
                "o14", "o15"
            )
        ).build();

        Long actualClusterId = strategy.chosePinnedClusterId(
            formalizedOffer, pinnedOfferIdToClusterId
        );
        assertEquals("getOldestExpiredClusterTest", Long.valueOf(1), actualClusterId);
    }

    @Test
    public void getManuallyPinnedClusterTest() {
        FormalizedOffer formalizedOffer = formalizedOfferBuilder.setOfferIds(
            Arrays.asList("o18", "o20")
        ).build();

        Long actualClusterId = strategy.chosePinnedClusterId(
            formalizedOffer, pinnedOfferIdToClusterId
        );

        assertEquals("getManuallyPinnedClusterTest", Long.valueOf(7), actualClusterId);
    }

}
