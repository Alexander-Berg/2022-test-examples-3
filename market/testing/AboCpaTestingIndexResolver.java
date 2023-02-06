package ru.yandex.market.mbi.api.testing;

import java.util.function.LongFunction;

import ru.yandex.market.core.feed.model.FeedSiteType;
import ru.yandex.market.core.moderation.feed.FeedIndexationResults;
import ru.yandex.market.core.moderation.feed.IndexingResultRepository;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.mbi.api.client.entity.moderation.AboCpaTestingIndex;

/**
 * Вычисляет самый доступный для CPA-проверок индекс.
 *
 * @author zoom
 */
public class AboCpaTestingIndexResolver implements LongFunction<AboCpaTestingIndex> {

    private final IndexingResultRepository mainIndexingResultRepository;
    private final IndexingResultRepository sandboxIndexingResultRepository;
    private final ParamService paramService;

    public AboCpaTestingIndexResolver(IndexingResultRepository mainIndexingResultRepository,
                                      IndexingResultRepository sandboxIndexingResultRepository,
                                      ParamService paramService) {
        this.mainIndexingResultRepository = mainIndexingResultRepository;
        this.sandboxIndexingResultRepository = sandboxIndexingResultRepository;
        this.paramService = paramService;
    }

    @Override
    public AboCpaTestingIndex apply(long shopId) {
        FeedIndexationResults sbxResults = sandboxIndexingResultRepository.loadShopResults(shopId);
        if (sbxResults.hasCpaOffers(FeedSiteType.MARKET)) {
            return AboCpaTestingIndex.SANDBOX;
        }
        if (Boolean.TRUE.equals(paramService.getParamBooleanValue(ParamType.SHOP_TESTING, shopId)) &&
                !sbxResults.areEligibleForModeration(FeedSiteType.MARKET)) {
            return AboCpaTestingIndex.SANDBOX_LOADING;
        }
        if (mainIndexingResultRepository.loadShopResults(shopId).hasCpaOffers(FeedSiteType.MARKET)) {
            return AboCpaTestingIndex.MAIN;
        }
        return AboCpaTestingIndex.NONE;
    }
}
