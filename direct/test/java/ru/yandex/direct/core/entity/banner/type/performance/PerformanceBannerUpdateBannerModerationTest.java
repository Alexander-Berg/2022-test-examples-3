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
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.fullPerformanceBanner;
import static ru.yandex.direct.regions.Region.BY_REGION_ID;
import static ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;

@CoreTest
@RunWith(Parameterized.class)
public class PerformanceBannerUpdateBannerModerationTest extends PerformanceBannerUpdateTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final StatusModerate CAMPAIGN_MODERATE_NEW =
            StatusModerate.NEW;

    private static final List<Long> AD_GROUP_GEO = List.of(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID,
            KAZAKHSTAN_REGION_ID);

    @Autowired
    public Steps steps;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BannerStatusModerate bannerStatusModerate;

    @Parameterized.Parameter(2)
    public BannerCreativeStatusModerate creativeStatusModerate;

    @Parameterized.Parameter(3)
    public CreativeUpdateStrategy creativeUpdateStrategy;

    @Parameterized.Parameter(4)
    public ModerationMode moderationMode;

    @Parameterized.Parameter(5)
    public OldBannerStatusModerate exptectedStatusModerate;

    @Parameterized.Parameter(6)
    public OldBannerStatusPostModerate exptectedStatusPostModerate;

    @Parameterized.Parameter(7)
    public OldBannerCreativeStatusModerate exptectedCreativeStatusModerate;

    @Parameterized.Parameter(8)
    public StatusBsSynced expectedStatusBsSynced;

    enum CreativeUpdateStrategy {
        KEEP_OLD_CREATIVE,
        UPDATE_CREATIVE,
        UPDATE_CREATIVE_IN_DRAFT_CAMPAIGN
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "updateBanner_ModeForceModerate_bannerWasDraft",
                        BannerStatusModerate.NEW,
                        BannerCreativeStatusModerate.YES,
                        CreativeUpdateStrategy.UPDATE_CREATIVE,
                        ModerationMode.FORCE_MODERATE,

                        OldBannerStatusModerate.YES,
                        OldBannerStatusPostModerate.YES,
                        OldBannerCreativeStatusModerate.YES,
                        StatusBsSynced.NO
                },
                {
                        "updateBanner_ModeForceModerate_bannerWasDraft_creativeNo",
                        BannerStatusModerate.NEW,
                        BannerCreativeStatusModerate.NO,
                        CreativeUpdateStrategy.UPDATE_CREATIVE,
                        ModerationMode.FORCE_MODERATE,

                        OldBannerStatusModerate.YES,
                        OldBannerStatusPostModerate.YES,
                        OldBannerCreativeStatusModerate.YES,
                        StatusBsSynced.NO
                },
                {
                        "updateBanner_ModeForceModerate_bannerWasDraft_creativeNew",
                        BannerStatusModerate.NEW,
                        BannerCreativeStatusModerate.NEW,
                        CreativeUpdateStrategy.UPDATE_CREATIVE,
                        ModerationMode.FORCE_MODERATE,

                        OldBannerStatusModerate.YES,
                        OldBannerStatusPostModerate.YES,
                        OldBannerCreativeStatusModerate.YES,
                        StatusBsSynced.NO
                },
                {
                        "updateBanner_ModeDefault_bannerWasDraft",
                        BannerStatusModerate.NEW,
                        BannerCreativeStatusModerate.YES,
                        CreativeUpdateStrategy.UPDATE_CREATIVE,
                        ModerationMode.DEFAULT,

                        OldBannerStatusModerate.NEW,
                        OldBannerStatusPostModerate.YES,
                        OldBannerCreativeStatusModerate.YES,
                        StatusBsSynced.NO
                },
                {
                        "updateBanner_ModeForceSaveDraft_bannerWasDraft",
                        BannerStatusModerate.NEW,
                        BannerCreativeStatusModerate.YES,
                        CreativeUpdateStrategy.UPDATE_CREATIVE,
                        ModerationMode.FORCE_SAVE_DRAFT,

                        OldBannerStatusModerate.NEW,
                        OldBannerStatusPostModerate.NO,
                        OldBannerCreativeStatusModerate.NEW,
                        StatusBsSynced.NO
                },
                {
                        "updateBanner_ModeForceModerate_bannerWasNotDraft",
                        BannerStatusModerate.YES,
                        BannerCreativeStatusModerate.YES,
                        CreativeUpdateStrategy.UPDATE_CREATIVE,
                        ModerationMode.FORCE_MODERATE,

                        OldBannerStatusModerate.YES,
                        OldBannerStatusPostModerate.YES,
                        OldBannerCreativeStatusModerate.YES,
                        StatusBsSynced.NO
                },
                {
                        "updateBanner_ModeDefault_bannerWasNotDraft",
                        BannerStatusModerate.YES,
                        BannerCreativeStatusModerate.YES,
                        CreativeUpdateStrategy.UPDATE_CREATIVE,
                        ModerationMode.DEFAULT,

                        OldBannerStatusModerate.YES,
                        OldBannerStatusPostModerate.YES,
                        OldBannerCreativeStatusModerate.YES,
                        StatusBsSynced.NO
                },
                {
                        "updateBanner_ModeForceSaveDraft_bannerWasNotDraft",
                        BannerStatusModerate.YES,
                        BannerCreativeStatusModerate.YES,
                        CreativeUpdateStrategy.UPDATE_CREATIVE,
                        ModerationMode.FORCE_SAVE_DRAFT,

                        OldBannerStatusModerate.NEW,
                        OldBannerStatusPostModerate.NO,
                        OldBannerCreativeStatusModerate.NEW,
                        StatusBsSynced.NO
                },
                {
                        // Если кампания черновик, то и объекты сохраняем как черновики
                        "updateBanner_ModeForceModerate_bannerWasDraft_campaignIsDraft",
                        BannerStatusModerate.NEW,
                        BannerCreativeStatusModerate.YES,
                        CreativeUpdateStrategy.UPDATE_CREATIVE_IN_DRAFT_CAMPAIGN,
                        ModerationMode.FORCE_MODERATE,

                        OldBannerStatusModerate.NEW,
                        OldBannerStatusPostModerate.NO,
                        OldBannerCreativeStatusModerate.NEW,
                        StatusBsSynced.NO
                },
                {
                        "updateBanner_ModeForceModerate_bannerWasNotDraft_CreativeIdDoesNotChange",
                        BannerStatusModerate.YES,
                        BannerCreativeStatusModerate.YES,
                        CreativeUpdateStrategy.KEEP_OLD_CREATIVE,
                        ModerationMode.FORCE_MODERATE,

                        OldBannerStatusModerate.YES,
                        OldBannerStatusPostModerate.YES,
                        OldBannerCreativeStatusModerate.YES,
                        StatusBsSynced.YES
                },
                {
                        "updateBanner_ModeForceModerate_bannerWasDraft_CreativeIdDoesNotChange",
                        BannerStatusModerate.NEW,
                        BannerCreativeStatusModerate.YES,
                        CreativeUpdateStrategy.KEEP_OLD_CREATIVE,
                        ModerationMode.FORCE_MODERATE,

                        OldBannerStatusModerate.YES,
                        OldBannerStatusPostModerate.YES,
                        OldBannerCreativeStatusModerate.YES,
                        StatusBsSynced.NO
                },
                {
                        "updateBanner_ModeForceModerate_bannerWasDraft_CreativeIdDoesNotChange_creativeSent",
                        BannerStatusModerate.NEW,
                        BannerCreativeStatusModerate.SENT,
                        CreativeUpdateStrategy.KEEP_OLD_CREATIVE,
                        ModerationMode.FORCE_MODERATE,

                        OldBannerStatusModerate.YES,
                        OldBannerStatusPostModerate.YES,
                        OldBannerCreativeStatusModerate.YES,
                        StatusBsSynced.NO
                },
                {
                        "updateBanner_ModeForceModerate_bannerWasDraft_CreativeIdDoesNotChange_creativeSending",
                        BannerStatusModerate.NEW,
                        BannerCreativeStatusModerate.SENDING,
                        CreativeUpdateStrategy.KEEP_OLD_CREATIVE,
                        ModerationMode.FORCE_MODERATE,

                        OldBannerStatusModerate.YES,
                        OldBannerStatusPostModerate.YES,
                        OldBannerCreativeStatusModerate.YES,
                        StatusBsSynced.NO
                },
                {
                        // Статусы модерации и statusBsSynced не меняются, если ModerationMode = Default и никакие
                        // проперти не поменялись.
                        "updateBanner_ModeDefault_bannerWasDraft_CreativeIdDoesNotChange",
                        BannerStatusModerate.NEW,
                        BannerCreativeStatusModerate.YES,
                        CreativeUpdateStrategy.KEEP_OLD_CREATIVE,
                        ModerationMode.DEFAULT,

                        OldBannerStatusModerate.NEW,
                        OldBannerStatusPostModerate.YES,
                        OldBannerCreativeStatusModerate.YES,
                        StatusBsSynced.YES
                }
        });
    }

    private CampaignInfo campaignInfo;
    private Integer shard;
    private Creative oldCreative;
    private Long creativeId;

    @Before
    public void before() {
        if (creativeUpdateStrategy == CreativeUpdateStrategy.UPDATE_CREATIVE_IN_DRAFT_CAMPAIGN) {
            campaignInfo = steps.campaignSteps().createCampaign(
                    activePerformanceCampaign(null, null).withStatusModerate(CAMPAIGN_MODERATE_NEW));
        } else {
            campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        }
        ClientInfo clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();
        oldCreative = createCreativeWithStatusModerateAndGeo(clientInfo, CREATIVE_MODERATE_NEW,
                asList(RUSSIA_REGION_ID, BY_REGION_ID));

        Creative newCreative = createCreativeWithStatusModerateAndGeo(clientInfo, CREATIVE_MODERATE_NEW,
                singletonList(BY_REGION_ID));

        switch (creativeUpdateStrategy) {
            case KEEP_OLD_CREATIVE:
                creativeId = oldCreative.getId();
                break;
            case UPDATE_CREATIVE:
            case UPDATE_CREATIVE_IN_DRAFT_CAMPAIGN:
                creativeId = newCreative.getId();
                break;
        }
    }

    @Test
    public void testParametrized() {
        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(activePerformanceAdGroup(null, feedId)
                .withGeo(AD_GROUP_GEO), campaignInfo);
        bannerInfo = steps.performanceBannerSteps().createPerformanceBanner(
                new NewPerformanceBannerInfo()
                        .withCampaignInfo(adGroupInfo.getCampaignInfo())
                        .withAdGroupInfo(adGroupInfo)
                        .withCreativeInfo(new CreativeInfo().withCreative(oldCreative))
                        .withBanner(fullPerformanceBanner(adGroupInfo.getCampaignId(),
                                adGroupInfo.getAdGroupId(),
                                oldCreative.getId())
                                .withStatusModerate(bannerStatusModerate)
                                .withBsBannerId(0L)
                                .withCreativeStatusModerate(creativeStatusModerate)));

        updateBannerCreativeId(creativeId);

        OldPerformanceBanner expectedBanner = new OldPerformanceBanner()
                .withCreativeId(creativeId)
                .withStatusModerate(exptectedStatusModerate)
                .withStatusPostModerate(exptectedStatusPostModerate)
                .withCreativeStatusModerate(exptectedCreativeStatusModerate)
                .withStatusBsSynced(expectedStatusBsSynced);

        checkUpdatedBanner(bannerInfo.getBannerId(), expectedBanner);
    }

    private void updateBannerCreativeId(Long creativeId) {
        ModelChanges<PerformanceBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(),
                PerformanceBanner.class)
                .process(creativeId, PerformanceBanner.CREATIVE_ID);
        prepareAndApplyValid(List.of(modelChanges), moderationMode);
    }

    private Creative createCreativeWithStatusModerateAndGeo(
            ClientInfo clientInfo,
            ru.yandex.direct.core.entity.creative.model.StatusModerate statusModerate,
            List<Long> sumGeo) {
        Creative creative = defaultPerformanceCreative(null, null)
                .withStatusModerate(statusModerate)
                .withSumGeo(sumGeo);
        return steps.creativeSteps().createCreative(creative, clientInfo).getCreative();
    }

    private void checkUpdatedBanner(Long bannerId, OldBanner expectedBanner) {
        OldPerformanceBanner actualBanner = (OldPerformanceBanner) bannerRepository
                .getBanners(shard, singletonList(bannerId)).get(0);

        assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

}
