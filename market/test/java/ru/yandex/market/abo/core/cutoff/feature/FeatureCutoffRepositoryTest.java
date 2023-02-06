package ru.yandex.market.abo.core.cutoff.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.core.feature.model.FeatureCutoffType.MANUAL;
import static ru.yandex.market.core.feature.model.FeatureCutoffType.PINGER;
import static ru.yandex.market.core.feature.model.FeatureType.CROSSDOCK;
import static ru.yandex.market.core.feature.model.FeatureType.DROPSHIP;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE;
import static ru.yandex.market.core.param.model.ParamCheckStatus.FAIL;
import static ru.yandex.market.core.param.model.ParamCheckStatus.SUCCESS;

/**
 * @author artemmz
 * @date 20/03/2020.
 */
class FeatureCutoffRepositoryTest extends EmptyTest {
    private static final long SHOP_ID = 34234256L;
    @Autowired
    FeatureCutoffRepository repository;

    @Test
    void lastFeatureCutoffs() {
        var cutoff1 = cutoff(SUCCESS);
        var cutoff2 = cutoff(FAIL);
        repository.saveAll(List.of(cutoff1, cutoff2));
        flushAndClear();

        List<FeatureCutoff> shopStats = repository.lastShopFeatureCutoffs(
                MARKETPLACE.name(), PINGER.getId());

        assertEquals(1, shopStats.size());
        assertEquals(cutoff2.getStatus(), shopStats.get(0).getStatus());
        assertEquals(SHOP_ID, shopStats.get(0).getShopId());
    }

    @Test
    void lastShopFeatureCutoffs() {
        FeatureCutoff cutoffOld = cutoff(SUCCESS, DROPSHIP, MANUAL);
        FeatureCutoff cutoffDropship = cutoff(FAIL, DROPSHIP, MANUAL);

        FeatureCutoff cutoffOld2 = cutoff(FAIL, MARKETPLACE, MANUAL);
        FeatureCutoff cutoffMarketplace = cutoff(SUCCESS, MARKETPLACE, MANUAL);

        FeatureCutoff cutoffCrossdock = cutoff(FAIL, CROSSDOCK, MANUAL);

        repository.saveAll(List.of(cutoffOld, cutoffDropship, cutoffOld2, cutoffMarketplace, cutoffCrossdock));
        flushAndClear();

        List<FeatureCutoff> lastShopCutoffs = repository.lastShopFeatureCutoffs(SHOP_ID,
                List.of(CROSSDOCK.name(), MARKETPLACE.name(), DROPSHIP.name()),
                MANUAL.getId());

        assertEquals(3, lastShopCutoffs.size());
        assertEquals(Set.of(cutoffCrossdock, cutoffMarketplace, cutoffDropship), new HashSet<>(lastShopCutoffs));
    }

    private static FeatureCutoff cutoff(ParamCheckStatus status) {
        return cutoff(status, MARKETPLACE, PINGER);
    }

    private static FeatureCutoff cutoff(ParamCheckStatus status,
                                        FeatureType featureType,
                                        FeatureCutoffType featureCutoffType) {
        FeatureCutoff cutoff = NoStatusFeatureCutoffBuilder.create(SHOP_ID, featureType, featureCutoffType)
                .withUid(RND.nextLong())
                .build(); // FeatureCutoff.class
        cutoff.setStatus(status);
        cutoff.setOpened(CutoffActionStatus.OK);
        return cutoff;
    }
}
