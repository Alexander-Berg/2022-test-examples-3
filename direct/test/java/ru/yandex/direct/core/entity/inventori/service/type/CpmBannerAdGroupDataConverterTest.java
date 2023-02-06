package ru.yandex.direct.core.entity.inventori.service.type;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.inventori.model.request.AudienceGroup;
import ru.yandex.direct.inventori.model.request.BlockSize;
import ru.yandex.direct.inventori.model.request.CryptaGroup;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.MainPageTrafficType;
import ru.yandex.direct.inventori.model.request.MobileOsType;
import ru.yandex.direct.inventori.model.request.PlatformCorrections;
import ru.yandex.direct.inventori.model.request.Target;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_INTERESTS_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_AUDIENCE_UPPER_BOUND;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDesktop;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestCreatives.fullCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class CpmBannerAdGroupDataConverterTest {

    private ClientId clientId = ClientId.fromLong(1L);
    private Long campaignId = 2L;
    private Long adGroupId = 3L;
    private Long creativeId = 6L;
    private Long bannerId = 7L;

    private AdGroupDataConverter converter = new AdGroupDataConverter();

    @Test
    public void convertAdGroupDataToInventoriTarget_AllData() {
        AdGroupData adGroupData = defaultAdGroupDataBuilder().build();

        Target target = converter.convertAdGroupDataToInventoriTarget(adGroupData);
        Target expectedTarget = getDefaultExpectedTarget();

        assertThat(target).is(matchedBy(beanDiffer(expectedTarget)));
    }

    @Test
    public void convertAdGroupDataToInventoriTarget_NoAdGroupsPremiumTrue() {
        var pricePackage = new PricePackage().withTargetingsFixed(
                new TargetingsFixed().withAllowPremiumDesktopCreative(true));
        AdGroupData adGroupData = defaultAdGroupDataBuilder()
                .withMainPageTrafficType(MainPageTrafficType.DESKTOP)
                .withBannerIds(emptyList())
                .withPricePackage(pricePackage)
                .build();

        Target target = converter.convertAdGroupDataToInventoriTarget(adGroupData);
        Target expectedTarget = getDefaultExpectedTarget()
                .withBlockSizes(singletonList(new BlockSize(1836, 572)));

        assertThat(target).is(matchedBy(beanDiffer(expectedTarget)));
    }

    @Test
    public void convertAdGroupDataToInventoriTarget_NoAdGroupsPremiumFalse() {
        var pricePackage = new PricePackage().withTargetingsFixed(
                new TargetingsFixed().withAllowPremiumDesktopCreative(false));
        AdGroupData adGroupData = defaultAdGroupDataBuilder()
                .withMainPageTrafficType(MainPageTrafficType.DESKTOP)
                .withBannerIds(emptyList())
                .withPricePackage(pricePackage)
                .build();

        Target target = converter.convertAdGroupDataToInventoriTarget(adGroupData);
        Target expectedTarget = getDefaultExpectedTarget()
                .withBlockSizes(singletonList(new BlockSize(1456, 180)));

        assertThat(target).is(matchedBy(beanDiffer(expectedTarget)));
    }

    @Test
    public void testWithoutFrontendDataWithoutExcludedBsCategories() {
        AdGroupData adGroupData = defaultAdGroupDataBuilder().build();
        assertThat(new AdGroupDataConverter().convertAdGroupDataToInventoriTarget(adGroupData).getExcludedBsCategories() == null);
    }

    @Test
    public void testWithFrontendDataWithExcludedBsCategories() {
        FrontendData frontendData = new FrontendData();
        frontendData.withExcludedBsCategories(List.of("brand-safety-categories:1", "brand-safety-categories:2", "brand-safety-categories:10"));
        AdGroupData adGroupData = defaultAdGroupDataBuilder().withFrontendData(frontendData).build();
        List<String> expected = List.of("931:1", "931:2", "931:10");
        assertThat(expected.equals(
                new AdGroupDataConverter().convertAdGroupDataToInventoriTarget(adGroupData).getExcludedBsCategories()));
    }

    private AdGroupData.Builder defaultAdGroupDataBuilder() {
        Campaign campaign = defaultCampaign(clientId, campaignId);
        CpmBannerAdGroup adGroup = defaultAdGroup(campaignId, adGroupId);
        Creative creative = defaultCreative(clientId, creativeId);
        Goal interestGoal = createGoal(CRYPTA_INTERESTS_UPPER_BOUND - 1, "618", "1");
        Goal audienceGoal = createGoal(METRIKA_AUDIENCE_UPPER_BOUND - 1);
        RetargetingCondition retargetingConditions = defaultRetargetingCondition(asList(interestGoal, audienceGoal));
        BidModifierDesktop bidModifierDesktop = defaultBidModifierDesktop(campaignId);
        BidModifierMobile bidModifierMobile = defaultBidModifierMobile(campaignId);
        var pricePackage = new PricePackage().withTargetingsFixed(
                new TargetingsFixed().withAllowPremiumDesktopCreative(false));
        return AdGroupData.builder()
                .withCampaign(campaign)
                .withAdGroup(adGroup)
                .withBannerIds(singletonList(bannerId))
                .withCreativesByBannerId(ImmutableMap.of(bannerId, creative))
                .withRetargetingConditions(singletonList(retargetingConditions))
                .withGoalIdToCryptaGoalMapping(ImmutableMap.of(CRYPTA_INTERESTS_UPPER_BOUND - 1, interestGoal))
                .withBidModifierDesktop(bidModifierDesktop)
                .withBidModifierMobile(bidModifierMobile)
                .withMainPageTrafficType(MainPageTrafficType.ALL)
                .withPageBlocks(emptyList())
                .withExcludedPageBlocks(emptyList())
                .withPricePackage(pricePackage);
    }

    private Campaign defaultCampaign(ClientId clientId, Long campaignId) {
        return new Campaign()
                .withId(campaignId)
                .withClientId(clientId.asLong())
                .withType(CampaignType.CPM_BANNER)
                .withGeo(singleton(100))
                .withAllowedDomains(singletonList("mail.ru"))
                .withAllowedSsp(singletonList("google.ru"))
                .withDisabledDomains(singleton("yandex.ru"));
    }

    private CpmBannerAdGroup defaultAdGroup(Long campaignId, Long adGroupId) {
        return activeCpmBannerAdGroup(campaignId)
                .withId(adGroupId)
                .withGeo(singletonList(200L));
    }

    private Creative defaultCreative(ClientId clientId, Long creativeId) {
        return fullCreative(clientId, creativeId)
                .withWidth(1920L)
                .withHeight(1080L);
    }

    private OldCpmBanner defaultBanner(Long campaignId, Long adGroupId, Long creativeId, Long bannerId) {
        return activeCpmBanner(campaignId, adGroupId, creativeId)
                .withId(bannerId);
    }

    private BidModifierDesktop defaultBidModifierDesktop(Long campaignId) {
        return createDefaultBidModifierDesktop(campaignId)
                .withDesktopAdjustment(new BidModifierDesktopAdjustment().withPercent(120));
    }

    private BidModifierMobile defaultBidModifierMobile(Long campaignId) {
        return createDefaultBidModifierMobile(campaignId)
                .withMobileAdjustment(new BidModifierMobileAdjustment()
                        .withPercent(130)
                        .withOsType(OsType.ANDROID));
    }

    private RetargetingCondition defaultRetargetingCondition(List<Goal> goals) {
        Rule rule = new Rule();
        rule.withType(RuleType.ALL)
                .withGoals(goals);
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition
                .withType(ConditionType.interests)
                .withRules(singletonList(rule));
        return retargetingCondition;
    }

    private Goal createGoal(Long id) {
        return createGoal(id, null, null);
    }

    private Goal createGoal(Long id, String keyword, String keywordValue) {
        Goal goal = new Goal();
        goal.withId(id)
                .withKeyword(keyword)
                .withKeywordValue(keywordValue);
        return goal;
    }

    private Target getDefaultExpectedTarget() {
        return new Target()
                .withAdGroupId(adGroupId)
                .withGroupType(GroupType.BANNER)
                .withDomains(Set.of("mail.ru"))
                .withSsp(Set.of("google.ru"))
                .withExcludedDomains(singleton("yandex.ru"))
                .withCryptaGroups(singletonList(new CryptaGroup(asSet("618:1", "618:2"))))
                .withAudienceGroups(singletonList(new AudienceGroup(AudienceGroup.GroupingType.ALL,
                        singleton(METRIKA_AUDIENCE_UPPER_BOUND - 1 + ""))))
                .withRegions(singleton(200))
                .withBlockSizes(singletonList(new BlockSize(1920, 1080)))
                .withPlatformCorrections(PlatformCorrections.builder().withDesktop(120).withMobile(130)
                        .withMobileOsType(MobileOsType.ANDROID).build());

    }
}
