package ru.yandex.direct.core.entity.adgroup.service.complex.cpm;

import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_SPECIFIC;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomComplexBidModifierDemographics;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomComplexBidModifierMobile;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomCpmPriceRetargeting;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.clientNewBannerAdditionalHrefs;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmVideoAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDefaultVideoAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestGroups.clientCpmIndoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.clientCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.clientYndxFrontpageAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForCpmBanner;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;
import static ru.yandex.direct.core.testing.data.TestNewCpmIndoorBanners.fullCpmIndoorBanner;
import static ru.yandex.direct.core.testing.data.TestNewCpmOutdoorBanners.fullCpmOutdoorBanner;

public class ComplexCpmAdGroupTestData {

    public static ComplexCpmAdGroup emptyAdGroup(Long campaignId) {
        return new ComplexCpmAdGroup()
                .withAdGroup(activeCpmBannerAdGroup(campaignId));
    }

    public static ComplexCpmAdGroup cpmBannerAdGroupWithRetargetings(Long campaignId, RetargetingCondition retCondition,
                                                                     Long creativeId) {
        return new ComplexCpmAdGroup()
                .withAdGroup(activeCpmBannerAdGroup(campaignId))
                .withBanners(singletonList(fullCpmBanner(campaignId, null, creativeId)))
                .withTargetInterests(singletonList(randomCpmPriceRetargeting(retCondition.getId())))
                .withRetargetingConditions(singletonList(retCondition));
    }

    public static ComplexCpmAdGroup cpmBannerAdGroupWithKeywords(Long campaignId, Long creativeId) {
        return new ComplexCpmAdGroup()
                .withAdGroup(activeCpmBannerAdGroup(campaignId))
                .withBanners(singletonList(fullCpmBanner(campaignId, null, creativeId)))
                .withKeywords(singletonList(keywordForCpmBanner()))
                .withComplexBidModifier(randomComplexBidModifierMobile());
    }

    public static ComplexCpmAdGroup cpmGeoproductAdGroup(Long campaignId, Long turbolandingId,
                                                         RetargetingCondition retCondition, Long creativeId) {
        return new ComplexCpmAdGroup()
                .withAdGroup(activeCpmGeoproductAdGroup(campaignId))
                .withBanners(singletonList(fullCpmBanner(campaignId, null, creativeId)
                        .withTurboLandingId(turbolandingId)
                        .withHref(null)))
                .withTargetInterests(singletonList(randomCpmPriceRetargeting(retCondition.getId())))
                .withRetargetingConditions(singletonList(retCondition));
    }

    public static ComplexCpmAdGroup cpmVideoAdGroup(Long campaignId, RetargetingCondition retCondition,
                                                    Long creativeId) {
        return new ComplexCpmAdGroup()
                .withAdGroup(activeCpmVideoAdGroup(campaignId))
                .withBanners(singletonList(fullCpmBanner(campaignId, null, creativeId)
                        .withPixels(singletonList(adfoxPixelUrl()))))
                .withTargetInterests(singletonList(randomCpmPriceRetargeting(retCondition.getId())))
                .withRetargetingConditions(singletonList(retCondition));
    }

    public static ComplexCpmAdGroup cpmOutdoorAdGroup(Long campaignId, OutdoorPlacement placement,
                                                      RetargetingCondition retCondition, Long creativeId) {
        return new ComplexCpmAdGroup()
                .withAdGroup(clientCpmOutdoorAdGroup(campaignId, placement))
                .withBanners(singletonList(fullCpmOutdoorBanner(campaignId, null, creativeId)))
                .withTargetInterests(
                        singletonList(randomCpmPriceRetargeting(retCondition == null ? null : retCondition.getId())))
                .withRetargetingConditions(singletonList(retCondition));
    }

    public static ComplexCpmAdGroup cpmIndoorAdGroup(Long campaignId, IndoorPlacement placement,
                                                     RetargetingCondition retCondition, Long creativeId) {
        return new ComplexCpmAdGroup()
                .withAdGroup(clientCpmIndoorAdGroup(campaignId, placement))
                .withBanners(singletonList(fullCpmIndoorBanner(campaignId, null, creativeId)))
                .withTargetInterests(
                        singletonList(randomCpmPriceRetargeting(retCondition == null ? null : retCondition.getId())))
                .withRetargetingConditions(singletonList(retCondition))
                .withComplexBidModifier(randomComplexBidModifierDemographics());
    }

    public static ComplexCpmAdGroup cpmYndxFrontpageAdGroup(Long campaignId, Long creativeId) {
        return new ComplexCpmAdGroup()
                .withAdGroup(activeCpmYndxFrontpageAdGroup(campaignId))
                .withBanners(singletonList(fullCpmBanner(campaignId, null, creativeId)
                        .withPixels(singletonList(adfoxPixelUrl()))));
    }

    public static ComplexCpmAdGroup cpmVideoAdGroupForPriceSales(CpmPriceCampaign campaign) {
        return new ComplexCpmAdGroup()
                .withAdGroup(activeDefaultVideoAdGroupForPriceSales(campaign).withPriority(PRIORITY_SPECIFIC));
    }

    public static ComplexCpmAdGroup cpmYndxFrontpageAdGroupForPriceSales(Long campaignId, Long creativeId,
                                                                         Long priority) {
        return new ComplexCpmAdGroup()
                .withAdGroup(clientYndxFrontpageAdGroupForPriceSales(campaignId)
                        .withPriority(priority))
                .withBanners(singletonList(fullCpmBanner(campaignId, null, creativeId)
                        .withPixels(singletonList(adfoxPixelUrl()))
                        .withAdditionalHrefs(clientNewBannerAdditionalHrefs())));
    }
}
