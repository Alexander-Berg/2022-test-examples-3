package ru.yandex.direct.core.entity.banner.type.performance;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.regions.Region.BY_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;

@CoreTest
@RunWith(Parameterized.class)
public class PerformanceBannerUpdateAdGroupModerationTest extends PerformanceBannerUpdateTest {

    private static final ru.yandex.direct.core.entity.adgroup.model.StatusModerate ADGROUP_MODERATE_NEW =
            ru.yandex.direct.core.entity.adgroup.model.StatusModerate.NEW;
    private static final ru.yandex.direct.core.entity.adgroup.model.StatusModerate ADGROUP_MODERATE_YES =
            ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES;

    private static final ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate ADGROUP_POST_MODERATE_YES =
            ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES;
    private static final ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate ADGROUP_POST_MODERATE_NO =
            ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.NO;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate CAMPAIGN_MODERATE_NEW =
            ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate.NEW;
    private static final ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate CAMPAIGN_MODERATE_NO =
            ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate.NO;
    private static final ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate CAMPAIGN_MODERATE_YES =
            ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate.YES;

    @Autowired
    public Steps steps;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public StatusModerate statusModerate;

    @Parameterized.Parameter(2)
    public StatusPostModerate statusPostModerate;

    @Parameterized.Parameter(3)
    public ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate campaignStatusModerate;

    @Parameterized.Parameter(4)
    public StatusBsSynced statusBsSynced;

    @Parameterized.Parameter(5)
    public BannerStatusModerate bannerStatusModerate;

    @Parameterized.Parameter(6)
    public Long bsBannerId;

    @Parameterized.Parameter(7)
    public ModerationMode moderationMode;

    @Parameterized.Parameter(8)
    public StatusModerate exptectedStatusModerate;

    @Parameterized.Parameter(9)
    public StatusPostModerate exptectedStatusPostModerate;

    @Parameterized.Parameter(10)
    public CampaignStatusModerate expectedCampaignStatusModerate;

    @Parameterized.Parameter(11)
    public StatusBsSynced expectedStatusBsSynced;

    @Parameterized.Parameter(12)
    public StatusBLGenerated expectedStatusBLGenerated;

    private static final Long DEFAULT_BS_BANNER_ID = 23458732L;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "updateBanner_updateAdGroupStatuses",
                        ADGROUP_MODERATE_NEW,
                        ADGROUP_POST_MODERATE_NO,
                        CAMPAIGN_MODERATE_YES,
                        StatusBsSynced.SENDING,
                        BannerStatusModerate.NEW,
                        DEFAULT_BS_BANNER_ID,

                        ModerationMode.FORCE_MODERATE,

                        ADGROUP_MODERATE_YES,
                        ADGROUP_POST_MODERATE_YES,
                        CampaignStatusModerate.YES,
                        StatusBsSynced.NO,
                        StatusBLGenerated.PROCESSING
                },
                {
                        // в обычной ситуации мы делаем campaignRepository.sendRejectedCampaignsToModerate
                        // но для performance банера такого быть не должно, кампания должна остаться rejected
                        "updateBanner_updateAdGroupStatuses_InRejectedCampaign",
                        ADGROUP_MODERATE_NEW,
                        ADGROUP_POST_MODERATE_NO,
                        CAMPAIGN_MODERATE_NO,
                        StatusBsSynced.SENDING,
                        BannerStatusModerate.NEW,
                        DEFAULT_BS_BANNER_ID,

                        ModerationMode.FORCE_MODERATE,

                        ADGROUP_MODERATE_YES,
                        ADGROUP_POST_MODERATE_YES,
                        CampaignStatusModerate.NO,
                        StatusBsSynced.NO,
                        StatusBLGenerated.PROCESSING
                },
                {
                        "updateBanner_dontUpdateAdGroupStatuses_whenForceSaveDraftMode",
                        ADGROUP_MODERATE_NEW,
                        ADGROUP_POST_MODERATE_NO,
                        CAMPAIGN_MODERATE_YES,
                        StatusBsSynced.SENDING,
                        BannerStatusModerate.NEW,
                        DEFAULT_BS_BANNER_ID,

                        ModerationMode.FORCE_MODERATE,

                        ADGROUP_MODERATE_YES,
                        ADGROUP_POST_MODERATE_YES,
                        CampaignStatusModerate.YES,
                        StatusBsSynced.NO,
                        StatusBLGenerated.PROCESSING
                },
                {
                        "updateBanner_dontUpdateAdGroupStatuses_whenCampaignIsDraft",
                        ADGROUP_MODERATE_NEW,
                        ADGROUP_POST_MODERATE_NO,
                        CAMPAIGN_MODERATE_NEW,
                        StatusBsSynced.SENDING,
                        BannerStatusModerate.NEW,
                        DEFAULT_BS_BANNER_ID,

                        ModerationMode.FORCE_MODERATE,

                        ADGROUP_MODERATE_NEW,
                        ADGROUP_POST_MODERATE_NO,
                        CampaignStatusModerate.NEW,
                        StatusBsSynced.SENDING,
                        StatusBLGenerated.NO
                },
                {
                        "updateBanner_dontUpdateAdGroupStatuses_whenBannerWasNotDraft",
                        ADGROUP_MODERATE_NEW,
                        ADGROUP_POST_MODERATE_NO,
                        CAMPAIGN_MODERATE_YES,
                        StatusBsSynced.SENDING,
                        BannerStatusModerate.YES,
                        DEFAULT_BS_BANNER_ID,

                        ModerationMode.FORCE_MODERATE,

                        ADGROUP_MODERATE_NEW,
                        ADGROUP_POST_MODERATE_NO,
                        CampaignStatusModerate.YES,
                        StatusBsSynced.SENDING,
                        StatusBLGenerated.NO
                },
                {
                        "updateBanner_dontUpdateAdGroupStatuses_whenAdGroupWasNotDraft",
                        ADGROUP_MODERATE_YES,
                        ADGROUP_POST_MODERATE_YES,
                        CAMPAIGN_MODERATE_YES,
                        StatusBsSynced.YES,
                        BannerStatusModerate.NEW,
                        DEFAULT_BS_BANNER_ID,

                        ModerationMode.FORCE_MODERATE,

                        ADGROUP_MODERATE_YES,
                        ADGROUP_POST_MODERATE_YES,
                        CampaignStatusModerate.YES,
                        StatusBsSynced.YES,
                        StatusBLGenerated.PROCESSING
                },
                {
                        "updateBanner_updateAdGroupStatuses_whenBannerNeverSentToBs",
                        ADGROUP_MODERATE_YES,
                        ADGROUP_POST_MODERATE_YES,
                        CAMPAIGN_MODERATE_YES,
                        StatusBsSynced.YES,
                        BannerStatusModerate.YES,
                        0L,

                        ModerationMode.FORCE_MODERATE,

                        ADGROUP_MODERATE_YES,
                        ADGROUP_POST_MODERATE_YES,
                        CampaignStatusModerate.YES,
                        StatusBsSynced.NO,
                        StatusBLGenerated.NO
                }
        });
    }

    private CampaignInfo campaignInfo;
    private ClientInfo clientInfo;
    private Integer shard;
    private Creative oldCreative;
    private Long newCreativeId;
    private Long feedId;

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createCampaign(
                activePerformanceCampaign(null, null).withStatusModerate(campaignStatusModerate));
        clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();
        oldCreative = createCreativeWithStatusModerateAndGeo(CREATIVE_MODERATE_NEW,
                asList(RUSSIA_REGION_ID, BY_REGION_ID));

        Creative newCreative = createCreativeWithStatusModerateAndGeo(CREATIVE_MODERATE_NEW,
                singletonList(BY_REGION_ID));
        newCreativeId = newCreative.getId();

        feedId = steps.feedSteps().createDefaultFeed().getFeedId();
    }

    @Test
    public void testParametrized() {
        PerformanceAdGroup adGroup = activePerformanceAdGroup(null, feedId)
                .withStatusModerate(statusModerate)
                .withStatusPostModerate(statusPostModerate)
                .withStatusBsSynced(statusBsSynced)
                .withStatusBLGenerated(StatusBLGenerated.NO);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);
        bannerInfo = steps.performanceBannerSteps().createPerformanceBanner(
                performanceBanner(adGroupInfo, oldCreative, bannerStatusModerate, bsBannerId));

        updateBannerCreativeId(newCreativeId);

        PerformanceAdGroup expectedAdGroup = new PerformanceAdGroup()
                .withStatusModerate(exptectedStatusModerate)
                .withStatusPostModerate(exptectedStatusPostModerate)
                .withStatusBsSynced(expectedStatusBsSynced)
                .withStatusBLGenerated(expectedStatusBLGenerated);

        checkAdGroup(adGroupInfo.getAdGroupId(), expectedAdGroup);

        Campaign actualCampaign = campaignRepository.getCampaigns(campaignInfo.getShard(),
                singletonList(campaignInfo.getCampaignId())).get(0);
        assertThat(actualCampaign.getStatusModerate(), equalTo(expectedCampaignStatusModerate));
    }

    private void updateBannerCreativeId(Long creativeId) {
        ModelChanges<PerformanceBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(),
                PerformanceBanner.class)
                .process(creativeId, PerformanceBanner.CREATIVE_ID);
        prepareAndApplyValid(List.of(modelChanges), moderationMode);
    }

    private Creative createCreativeWithStatusModerateAndGeo(
            ru.yandex.direct.core.entity.creative.model.StatusModerate statusModerate,
            List<Long> sumGeo) {
        Creative creative = defaultPerformanceCreative(null, null)
                .withStatusModerate(statusModerate)
                .withSumGeo(sumGeo);
        return steps.creativeSteps().createCreative(creative, clientInfo).getCreative();
    }

    private void checkAdGroup(Long adGroupId, PerformanceAdGroup expectedAdGroup) {
        PerformanceAdGroup actualAdGroup = (PerformanceAdGroup) adGroupRepository
                .getAdGroups(shard, singletonList(adGroupId)).get(0);

        assertThat(actualAdGroup, beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields()));
    }

}
