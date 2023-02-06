package ru.yandex.direct.core.copyentity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupsAddOperationTestBase;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceBannerInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.fullPerformanceBanner;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("unchecked")
public class CopyOperationSmartAdGroupSameClientDifferentCampaignTest extends AdGroupsAddOperationTestBase {

    @Autowired
    private CopyOperationFactory factory;

    @Autowired
    private CopyOperationAssert asserts;

    @Autowired
    private AdGroupWithBannersService adGroupWithBannersService;

    private Long uid;
    private ClientId clientId;
    private ClientInfo clientInfo;

    private Long campaignIdFrom;
    private CampaignInfo campaignInfoFrom;
    private CampaignInfo campaignInfoTo;

    private Long campaignIdTo;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();

        clientId = clientInfo.getClientId();

        campaignInfoFrom = steps.campaignSteps().createCampaign(
                activePerformanceCampaign(clientId, clientInfo.getUid())
                        .withEmail("test@yandex-team.ru"),
                clientInfo);
        campaignIdFrom = campaignInfoFrom.getCampaignId();

        campaignInfoTo = steps.campaignSteps().createCampaign(
                activePerformanceCampaign(clientId, clientInfo.getUid())
                        .withEmail("test@yandex-team.ru"),
                clientInfo);

        campaignIdTo = campaignInfoTo.getCampaignId();

        asserts.init(clientId, clientId, uid);

    }

    @Test
    public void adGroupWithMinusKeywordsId() {
        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activePerformanceAdGroup(campaignIdFrom, feedId)
                        .withMinusKeywords(List.of("полет", "навигатора")),
                campaignInfoFrom);

        MassResult<Long> result = createAddOperation(Applicability.FULL, List.of(adGroupInfo.getAdGroup()), uid,
                clientId, geoTree, shard, true)
                .prepareAndApply();
        assertThat(result, isFullySuccessful());
        AdGroup addedAdGroup = actualAdGroup(result.get(0).getResult());

        var xerox = factory.build(copyConfig(addedAdGroup.getId()));

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        asserts.assertEntitiesAreCopied(AdGroup.class, copiedAdGroupIds, List.of(addedAdGroup), COPIED);
    }

    @Test
    public void campaignWithTooManyAdGroups() {
        final int adGroupsToCopy = 510;
        final int adGroupsAlreadyInCampaign = 540;
        final int maxAdGroupsInCampaign = 1000;

        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();
        List<AdGroupInfo> adGroupInfosFrom = IntStreamEx.range(0, adGroupsToCopy)
                .mapToObj(i -> new AdGroupInfo()
                        .withAdGroup(activePerformanceAdGroup(campaignIdFrom, feedId)
                                .withName("adGroup " + i))
                        .withCampaignInfo(campaignInfoFrom))
                .toList();
        steps.adGroupSteps().createAdGroups(adGroupInfosFrom);

        List<AdGroupInfo> adGroupInfosTo = IntStreamEx.range(0, adGroupsAlreadyInCampaign)
                .mapToObj(i -> new AdGroupInfo()
                        .withAdGroup(activePerformanceAdGroup(campaignIdTo, feedId)
                                .withName("adGroup " + i))
                        .withCampaignInfo(campaignInfoTo))
                .toList();
        steps.adGroupSteps().createAdGroups(adGroupInfosTo);
        List<Long> adGroupIds = StreamEx.of(adGroupInfosFrom).map(AdGroupInfo::getAdGroupId).toList();

        var xerox = factory.build(copyConfig(adGroupIds));

        var copyResult = xerox.copy();

        final int adGroupsThatDoesntFitInCampaign = adGroupsAlreadyInCampaign + adGroupsToCopy
                - maxAdGroupsInCampaign;
        assertThat(copyResult.getMassResult().getValidationResult().flattenErrors().size(),
                is(adGroupsThatDoesntFitInCampaign));
    }

    @Test
    public void bsRarelyLoadedGroup_bsRarelyLoadedStatusIsNotCopied() {
        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activePerformanceAdGroup(campaignIdFrom, feedId)
                        .withBsRarelyLoaded(true),
                campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();
        steps.adGroupSteps().setBsRarelyLoaded(shard, adGroupIdFrom, true);
        assertThat(actualAdGroup(adGroupIdFrom).getBsRarelyLoaded(), is(true));

        var xerox = factory.build(copyConfig(adGroupIdFrom));

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var adGroup = actualAdGroup(copiedAdGroupIds.get(0));
        assertThat(adGroup.getBsRarelyLoaded(), is(false));
    }

    @Test
    public void bannerWithStatusModerateReady() {
        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();
        var adGroupInfo =
                steps.adGroupSteps().createAdGroup(activePerformanceAdGroup(campaignIdFrom, feedId), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        Creative creative = defaultPerformanceCreative(clientId, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        Long creativeId = creativeInfo.getCreativeId();

        var bannerInfo = steps.performanceBannerSteps().createPerformanceBanner(
                new NewPerformanceBannerInfo()
                        .withBanner(fullPerformanceBanner(campaignIdFrom, adGroupIdFrom, creativeId)
                                .withStatusModerate(BannerStatusModerate.READY))
                        .withAdGroupInfo(adGroupInfo));

        var xerox = factory.build(copyConfig(adGroupIdFrom));

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo.getBanner()), COPIED);
    }

    @Test
    public void adGroupWithLastChange() {
        LocalDateTime previousDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();
        var adGroupInfo =
                steps.adGroupSteps().createAdGroup(activePerformanceAdGroup(campaignIdFrom, feedId)
                        .withLastChange(previousDate), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        var xerox = factory.build(copyConfig(adGroupIdFrom));

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var adGroup = actualAdGroup(copiedAdGroupIds.get(0));
        assertThat(adGroup.getLastChange(), is(not(equalTo(previousDate))));
    }

    private AdGroup actualAdGroup(Long adGroupId) {
        return adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(adGroupId)).get(0);
    }

    private CopyConfig copyConfig(Long copyId) {
        return copyConfig(List.of(copyId));
    }

    private CopyConfig copyConfig(List<Long> copyIds) {
        return copyConfig(copyIds, campaignIdFrom, campaignIdTo);
    }

    private CopyConfig copyConfig(List<Long> copyIds, Long idFrom, Long idTo) {
        return CopyEntityTestUtils.adGroupCopyConfig(clientInfo, copyIds, idFrom, idTo, uid);
    }
}
