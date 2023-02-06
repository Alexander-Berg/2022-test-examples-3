package ru.yandex.direct.core.entity.adgroup.service.complex;

import java.math.BigDecimal;
import java.util.List;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.testing.data.TestRelevanceMatches;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.model.ModelProperty;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingUtils.convertRetargetingsToTargetInterests;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientBidModifierDemographics;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestGroups.clientTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultClientKeyword;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.clientImageBannerWithCreative;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.clientImageBannerWithImage;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.data.TestOfferRetargetingsKt.getDefaultOfferRetargeting;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.defaultSitelinkSet;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.core.testing.data.TestVcards.vcardUserFields;

@SuppressWarnings({"unused", "SameParameterValue"})
public class ComplexTextAdGroupTestData {
    public static final Currency CURRENCY = Currencies.getCurrency(CurrencyCode.RUB);

    // empty adGroup

    public static ComplexTextAdGroup emptyAdGroupForAdd(Long campaignId) {
        return emptyTextAdGroup(null, campaignId);
    }

    public static ComplexTextAdGroup emptyAdGroupForUpdate(Long adGroupId) {
        return emptyTextAdGroup(adGroupId, null);
    }

    private static ComplexTextAdGroup emptyTextAdGroup(Long adGroupId, Long campaignId) {
        return new ComplexTextAdGroup()
                .withAdGroup(clientTextAdGroup(campaignId).withId(adGroupId));
    }

    // empty adGroup with custom show conditions
    public static <M> ComplexTextAdGroup emptyAdGroupWithModelForAdd(Long campaignId, M model,
                                                                     ModelProperty<ComplexTextAdGroup, M> modelProperty) {
        return emptyAdGroupWithModel(null, campaignId, model, modelProperty);
    }

    public static <M> ComplexTextAdGroup emptyAdGroupWithModelForUpdate(Long adGroupId, M model,
                                                                   ModelProperty<ComplexTextAdGroup, M> modelProperty) {
        return emptyAdGroupWithModel(adGroupId, null, model, modelProperty);
    }

    private static <M> ComplexTextAdGroup emptyAdGroupWithModel(Long adGroupId, Long campaignId, M model,
                                                                ModelProperty<ComplexTextAdGroup, M> modelProperty) {
        var adGroup = emptyTextAdGroup(adGroupId, campaignId);
        modelProperty.set(adGroup, model);
        return adGroup;
    }

    // full adGroup with text banner

    public static ComplexTextAdGroup fullAdGroupForAdd(Long campaignId, Long retConditionId) {
        return fullTextAdGroup(null, campaignId, retConditionId);
    }

    public static ComplexTextAdGroup fullAdGroupForUpdate(Long adGroupId, Long retConditionId) {
        return fullTextAdGroup(adGroupId, null, retConditionId);
    }

    public static ComplexTextAdGroup fullTextAdGroup(Long adGroupId, Long campaignId, Long retConditionId) {
        return fullTextAdGroup(adGroupId, campaignId, retConditionId,
                singletonList(fullTextBanner(null)));
    }

    // full adGroup with image hash banner

    public static ComplexTextAdGroup fullAdGroupWithImageHashBannerForAdd(Long campaignId,
                                                                          Long retConditionId, String imageHash) {
        return fullAdGroupWithImageHashBanner(null, campaignId, retConditionId, imageHash);
    }

    public static ComplexTextAdGroup fullAdGroupWithImageHashBannerForUpdate(Long adGroupId,
                                                                             Long retConditionId, String imageHash) {
        return fullAdGroupWithImageHashBanner(adGroupId, null, retConditionId, imageHash);
    }

    public static ComplexTextAdGroup fullAdGroupWithImageHashBanner(Long adGroupId, Long campaignId,
                                                                    Long retConditionId, String imageHash) {
        return fullTextAdGroup(adGroupId, campaignId, retConditionId,
                singletonList(imageHashBanner(null, imageHash)));
    }

    // full adGroup with image creative banner

    public static ComplexTextAdGroup fullAdGroupWithImageCreativeBannerForAdd(Long campaignId,
                                                                              Long retConditionId, Long creativeId) {
        return fullAdGroupWithImageCreativeBanner(null, campaignId, retConditionId, creativeId);
    }

    public static ComplexTextAdGroup fullAdGroupWithImageCreativeBannerForUpdate(Long adGroupId,
                                                                                 Long retConditionId, Long creativeId) {
        return fullAdGroupWithImageCreativeBanner(adGroupId, null, retConditionId, creativeId);
    }

    public static ComplexTextAdGroup fullAdGroupWithImageCreativeBanner(Long adGroupId, Long campaignId,
                                                                        Long retConditionId, Long creativeId) {
        return fullTextAdGroup(adGroupId, campaignId, retConditionId,
                singletonList(imageCreativeBanner(null, creativeId)));
    }

    // full adGroup with custom banners

    public static ComplexTextAdGroup fullTextAdGroup(Long adGroupId, Long campaignId,
                                                     Long retConditionId, List<ComplexBanner> complexBanners) {
        return new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaignId).withId(adGroupId))
                .withComplexBanners(complexBanners)
                .withKeywords(singletonList(defaultKeyword()))
                .withRelevanceMatches(singletonList(defaultRelevanceMatch()))
                .withOfferRetargetings(singletonList(defaultOfferRetargeting()))
                .withTargetInterests(singletonList(randomPriceRetargeting(retConditionId)))
                .withComplexBidModifier(randomComplexBidModifierMobile());
    }

    // image hash banner

    public static ComplexBanner imageHashBannerForAdd(String imageHash) {
        return imageHashBanner(null, imageHash);
    }

    public static ComplexBanner imageHashBanner(Long bannerId, String imageHash) {
        return new ComplexBanner()
                .withBanner(clientImageBannerWithImage(imageHash).withId(bannerId));
    }

    // image creative banner

    public static ComplexBanner imageCreativeBannerForAdd(Long creativeId) {
        return imageCreativeBanner(null, creativeId);
    }

    public static ComplexBanner imageCreativeBanner(Long bannerId, Long creativeId) {
        return new ComplexBanner()
                .withBanner(clientImageBannerWithCreative(creativeId).withId(bannerId));
    }

    // text banner

    public static ComplexTextBanner emptyBannerForAdd() {
        return new ComplexTextBanner()
                .withBanner(clientTextBanner().withId(null));
    }

    public static ComplexTextBanner emptyBanner(Long bannerId) {
        return new ComplexTextBanner()
                .withBanner(clientTextBanner().withId(bannerId));
    }

    public static ComplexTextBanner bannerWithSitelinkSet(Long bannerId) {
        ComplexTextBanner banner = emptyBanner(bannerId)
                .withSitelinkSet(defaultSitelinkSet(null));
        String bannerHref = "https://yandex.ru";
        banner.getBanner().withHref(bannerHref);
        banner.getSitelinkSet().getSitelinks().get(0).withHref(bannerHref + "/company");
        banner.getSitelinkSet().getSitelinks().get(1).withHref(bannerHref + "/company/press_releases/2018");
        banner.getSitelinkSet().getSitelinks().get(2).withHref(bannerHref + "/company/researches");
        return banner;
    }

    public static ComplexTextBanner fullTextBannerForAdd() {
        return fullTextBanner(null);
    }

    public static ComplexTextBanner fullTextBanner(Long bannerId) {
        return bannerWithSitelinkSet(bannerId).withVcard(fullVcard());
    }

    public static ComplexTextBanner bannerWithVcard(Long bannerId) {
        return emptyBanner(bannerId).withVcard(vcardUserFields(null));
    }

    // keyword

    public static Keyword randomPhraseKeywordForAdd() {
        return randomPhraseKeywordForUpdate(null);
    }

    public static Keyword randomPhraseKeywordForUpdate(Long keywordId) {
        return defaultClientKeyword()
                .withId(keywordId)
                .withPhrase(randomAlphanumeric(10));
    }

    // relevance match

    public static RelevanceMatch defaultRelevanceMatch() {
        return TestRelevanceMatches.defaultRelevanceMatch();
    }

    // offer retargeting

    public static OfferRetargeting defaultOfferRetargeting() {
        return getDefaultOfferRetargeting();
    }

    // retargeting

    public static TargetInterest randomPriceRetargeting(Long retConditionId) {
        Retargeting retargeting = defaultRetargeting(null, null, retConditionId)
                .withPriceContext(BigDecimal.valueOf(nextLong(100, CURRENCY.getMaxPrice().longValue())));
        return convertRetargetingsToTargetInterests(singletonList(retargeting), emptyList()).get(0);
    }

    public static TargetInterest randomCpmPriceRetargeting(Long retConditionId) {
        Retargeting retargeting = defaultRetargeting(null, null, retConditionId)
                .withPriceContext(BigDecimal.valueOf(nextLong(100, CURRENCY.getMaxCpmPrice().longValue())));
        return convertRetargetingsToTargetInterests(singletonList(retargeting), emptyList()).get(0);
    }

    // bid modifiers

    public static ComplexBidModifier randomComplexBidModifierMobile() {
        return new ComplexBidModifier()
                .withMobileModifier(randomBidModifierMobile());
    }

    public static BidModifierMobile randomBidModifierMobile() {
        return createDefaultClientBidModifierMobile(null)
                .withType(BidModifierType.MOBILE_MULTIPLIER)
                .withMobileAdjustment(new BidModifierMobileAdjustment()
                        .withPercent(nextInt(50, 1300)));
    }

    public static ComplexBidModifier randomComplexBidModifierDemographics() {
        return new ComplexBidModifier()
                .withDemographyModifier(randomBidModifierDemographics());
    }

    public static BidModifierDemographics randomBidModifierDemographics() {
        BidModifierDemographicsAdjustment adjustment = new BidModifierDemographicsAdjustment()
                .withAge(AgeType._18_24)
                .withGender(GenderType.MALE)
                .withPercent(nextInt(1, 1300));
        return createDefaultClientBidModifierDemographics(null)
                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                .withDemographicsAdjustments(singletonList(adjustment));
    }

    public static ComplexBidModifier randomComplexBidModifierMobileAndDemographics() {
        return new ComplexBidModifier()
                .withMobileModifier(randomBidModifierMobile())
                .withDemographyModifier(randomBidModifierDemographics());
    }
}
