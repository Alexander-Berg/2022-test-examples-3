package ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion;

import ru.yandex.direct.core.entity.adgroup.container.ComplexContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomBidModifierDemographics;
import static ru.yandex.direct.core.testing.data.TestBanners.VALID_CONTENT_PROMOTION_ID;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoBanner;

public class ComplexContentPromotionAdGroupTestData {

    public static ComplexContentPromotionAdGroup emptyAdGroup(Long campaignId) {
        return new ComplexContentPromotionAdGroup().withAdGroup(
                fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO).withCampaignId(campaignId));
    }

    public static ComplexContentPromotionAdGroup contentPromotionAdGroupWithKeywords(Long campaignId) {
        return new ComplexContentPromotionAdGroup()
                .withAdGroup(fullContentPromotionAdGroup(campaignId, ContentPromotionAdgroupType.VIDEO))
                .withBanners(singletonList(
                        clientContentPromoBanner(VALID_CONTENT_PROMOTION_ID)
                                .withHref(null).withDomain(null)))
                .withKeywords(singletonList(defaultKeyword()))
                .withComplexBidModifier(
                        new ComplexBidModifier().withDemographyModifier(randomBidModifierDemographics()));
    }

    public static ComplexContentPromotionAdGroup contentPromotionAdGroupWithComplexBidModifier(Long campaignId) {
        return contentPromotionAdGroupWithKeywords(campaignId)
                .withComplexBidModifier(
                        new ComplexBidModifier().withDemographyModifier(randomBidModifierDemographics()));
    }
}
