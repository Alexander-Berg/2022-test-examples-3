package ru.yandex.ir.modelsclusterizer.utils;

import com.google.common.collect.Multimaps;
import ru.yandex.ir.modelsclusterizer.be.CategoryInfo;
import ru.yandex.ir.modelsclusterizer.be.Cluster;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffer;
import ru.yandex.ir.modelsclusterizer.be.RepresentativeOfferSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Evgeniya Yakovleva
 */
public class ClusterBuilderUtils {
    public static Cluster.Builder getClusterBuilder(CategoryInfo categoryInfo, long vendorId) {
        return new Cluster.Builder()
            .setRuntimeContext(
                new Cluster.ClusterRuntimeContext.Builder()
                    .setCategoryInfo(categoryInfo)
                    .setSessionId("0")
                    .setFormalizedOffers(Collections.<FormalizedOffer>emptyList())
                    .setDuplicateRate(0.)
                .build()
            )
            .setContent(
                new Cluster.ClusterContent.Builder()
                    .setVendor(vendorId)
                    .setGoodIdToMagicIdOffersMap(Multimaps.newSetMultimap(new HashMap<>(), HashSet::new))
                    .setRepresentativeOfferSet(RepresentativeOfferSet.REPRESENTATIVE_OFFER_DISABLED_ENTRY)
                    .build()
            );
    }
}
