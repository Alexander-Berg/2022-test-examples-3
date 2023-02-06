package ru.yandex.direct.core.entity.inventori.service.type;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.creative.model.Creative;
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
import ru.yandex.direct.inventori.model.request.MobileOsType;
import ru.yandex.direct.inventori.model.request.PlatformCorrections;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.request.VideoCreative;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_INTERESTS_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_AUDIENCE_UPPER_BOUND;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDesktop;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestCreatives.fullCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmVideoAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class CpmVideoAdGroupDataConverterTest {

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
        Target expectedTarget = new Target()
                .withAdGroupId(adGroupId)
                .withGroupType(GroupType.VIDEO)
                .withExcludedDomains(singleton("video.yandex.ru"))
                .withCryptaGroups(singletonList(new CryptaGroup(asSet("618:1", "618:2"))))
                .withAudienceGroups(singletonList(new AudienceGroup(AudienceGroup.GroupingType.ALL,
                        singleton(METRIKA_AUDIENCE_UPPER_BOUND - 1 + ""))))
                .withRegions(singleton(200))
                .withVideoCreatives(singletonList(new VideoCreative(5000, new BlockSize(200, 300), Sets.newSet(
                        new BlockSize(16, 9)
                ))))
                .withPlatformCorrections(PlatformCorrections.builder().withDesktop(120).withMobile(130)
                        .withMobileOsType(MobileOsType.ANDROID).build());

        assertThat(target).is(matchedBy(beanDiffer(expectedTarget)));
    }

    private AdGroupData.Builder defaultAdGroupDataBuilder() {
        Campaign campaign = defaultCampaign(clientId, campaignId);
        CpmVideoAdGroup adGroup = defaultAdGroup(campaignId, adGroupId);
        Creative creative = defaultCreative(clientId, creativeId);
        Goal interestGoal = createGoal(CRYPTA_INTERESTS_UPPER_BOUND - 1, "618", "1");
        Goal audienceGoal = createGoal(METRIKA_AUDIENCE_UPPER_BOUND - 1);
        RetargetingCondition retargetingConditions = defaultRetargetingCondition(asList(interestGoal, audienceGoal));
        BidModifierDesktop bidModifierDesktop = defaultBidModifierDesktop(campaignId);
        BidModifierMobile bidModifierMobile = defaultBidModifierMobile(campaignId);

        return AdGroupData.builder()
                .withCampaign(campaign)
                .withAdGroup(adGroup)
                .withBannerIds(singletonList(bannerId))
                .withCreativesByBannerId(ImmutableMap.of(bannerId, creative))
                .withRetargetingConditions(singletonList(retargetingConditions))
                .withGoalIdToCryptaGoalMapping(ImmutableMap.of(CRYPTA_INTERESTS_UPPER_BOUND - 1, interestGoal))
                .withBidModifierDesktop(bidModifierDesktop)
                .withBidModifierMobile(bidModifierMobile)
                .withPageBlocks(emptyList())
                .withExcludedPageBlocks(emptyList());
    }

    private Campaign defaultCampaign(ClientId clientId, Long campaignId) {
        return new Campaign()
                .withId(campaignId)
                .withClientId(clientId.asLong())
                .withType(CampaignType.CPM_BANNER)
                .withGeo(singleton(100))
                .withDisabledDomains(singleton("yandex.ru"))
                .withDisabledVideoPlacements(singletonList("video.yandex.ru"));
    }

    private CpmVideoAdGroup defaultAdGroup(Long campaignId, Long adGroupId) {
        return activeCpmVideoAdGroup(campaignId)
                .withId(adGroupId)
                .withGeo(singletonList(200L));
    }

    private Creative defaultCreative(ClientId clientId, Long creativeId) {
        return fullCreative(clientId, creativeId)
                .withDuration(5L);
    }

    private OldCpmBanner defaultBanner(Long campaignId, Long adGroupId, Long creativeId, Long bannerId) {
        return activeCpmVideoBanner(campaignId, adGroupId, creativeId)
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
}
