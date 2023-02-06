package ru.yandex.direct.grid.core.util;

import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdGroupFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.CampaignFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.CampaignGoalFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.DynamicTargetFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.FetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.OfferFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.PackageStrategyFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.RetargetingFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.ShowConditionFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.SmartFilterFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.WalletFetchedFieldsResolver;

public class FetchedFieldsResolverCoreUtil {
    public static FetchedFieldsResolver buildFetchedFieldsResolver(boolean defaultValue) {
        return new FetchedFieldsResolver()
                .withCampaign(buildCampaignFetchedFieldsResolver(defaultValue))
                .withCampaignGoal(buildCampaignGoalFetchedFieldsResolver(defaultValue))
                .withAd(buildAdFetchedFieldsResolver(defaultValue))
                .withAdGroup(buildAdGroupFetchedFieldsResolver(defaultValue))
                .withDynamicTarget(buildDynamicTargetFetchedFieldsResolver(defaultValue))
                .withShowCondition(buildShowConditionFetchedFieldsResolver(defaultValue))
                .withRetargeting(buildRetargetingFetchedFieldsResolver(defaultValue))
                .withSmartFilter(buildSmartFilterFetchedFieldsResolver(defaultValue))
                .withOffer(buildOfferFetchedFieldsResolver(defaultValue))
                .withWallet(buildWalletFetchedFieldsResolver(defaultValue))
                .withPackageStrategy(buildPackageStrategyFetchedFieldsResolver(defaultValue));
    }

    public static CampaignFetchedFieldsResolver buildCampaignFetchedFieldsResolver(boolean defaultValue) {
        return new CampaignFetchedFieldsResolver()
                .withRecommendations(defaultValue)
                .withStats(defaultValue)
                .withCacheKey(defaultValue);
    }

    public static CampaignGoalFetchedFieldsResolver buildCampaignGoalFetchedFieldsResolver(boolean defaultValue) {
        return new CampaignGoalFetchedFieldsResolver()
                .withDomain(defaultValue);
    }

    public static AdFetchedFieldsResolver buildAdFetchedFieldsResolver(boolean defaultValue) {
        return new AdFetchedFieldsResolver()
                .withRecommendations(defaultValue)
                .withStats(defaultValue)
                .withStatsByDays(defaultValue)
                .withCacheKey(defaultValue);
    }

    public static AdGroupFetchedFieldsResolver buildAdGroupFetchedFieldsResolver(boolean defaultValue) {
        return new AdGroupFetchedFieldsResolver()
                .withRecommendations(defaultValue)
                .withStats(defaultValue)
                .withCacheKey(defaultValue)
                .withTags(defaultValue);
    }

    public static ShowConditionFetchedFieldsResolver buildShowConditionFetchedFieldsResolver(boolean defaultValue) {
        return new ShowConditionFetchedFieldsResolver()
                .withAuctionData(defaultValue)
                .withPokazometrData(defaultValue)
                .withStats(defaultValue)
                .withCacheKey(defaultValue);
    }

    @SuppressWarnings("WeakerAccess")
    public static RetargetingFetchedFieldsResolver buildRetargetingFetchedFieldsResolver(boolean defaultValue) {
        return new RetargetingFetchedFieldsResolver()
                .withStats(defaultValue)
                .withRetargetingCondition(defaultValue)
                .withCacheKey(defaultValue);
    }

    @SuppressWarnings("WeakerAccess")
    public static DynamicTargetFetchedFieldsResolver buildDynamicTargetFetchedFieldsResolver(boolean defaultValue) {
        return new DynamicTargetFetchedFieldsResolver()
                .withStats(defaultValue)
                .withGoalStats(defaultValue);
    }

    @SuppressWarnings("WeakerAccess")
    public static SmartFilterFetchedFieldsResolver buildSmartFilterFetchedFieldsResolver(boolean defaultValue) {
        return new SmartFilterFetchedFieldsResolver()
                .withStats(defaultValue);
    }

    @SuppressWarnings("WeakerAccess")
    public static OfferFetchedFieldsResolver buildOfferFetchedFieldsResolver(boolean defaultValue) {
        return new OfferFetchedFieldsResolver()
                .withStats(defaultValue)
                .withCacheKey(defaultValue);
    }

    @SuppressWarnings("WeakerAccess")
    public static WalletFetchedFieldsResolver buildWalletFetchedFieldsResolver(boolean defaultValue) {
        return new WalletFetchedFieldsResolver()
                .withPaidDaysLeft(defaultValue);
    }

    @SuppressWarnings("WeakerAccess")
    public static PackageStrategyFetchedFieldsResolver buildPackageStrategyFetchedFieldsResolver(boolean defaultValue) {
        return new PackageStrategyFetchedFieldsResolver()
                .withCacheKey(defaultValue)
                .withStats(defaultValue);
    }
}
