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

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.regions.Region.BY_REGION_ID;
import static ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(Parameterized.class)
public class PerformanceBannerUpdateCreativeModerationTest extends PerformanceBannerUpdateTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final List<Long> EXPECTED_CREATIVE_GEO = asList(RUSSIA_REGION_ID, KAZAKHSTAN_REGION_ID);
    private static final List<Long> AD_GROUP_GEO = List.of(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID,
            KAZAKHSTAN_REGION_ID);

    @Autowired
    public Steps steps;

    @Autowired
    private CreativeRepository creativeRepository;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BannerStatusModerate bannerStatusModerate;

    @Parameterized.Parameter(2)
    public CreativeUpdateStrategy creativeUpdateStrategy;

    @Parameterized.Parameter(3)
    public ModerationMode moderationMode;

    // в expected полях null значит не изменился
    @Parameterized.Parameter(4)
    public StatusModerate expectedCreativeStatusModerate;

    @Parameterized.Parameter(5)
    public List<Long> expectedCreativeSumGeo;

    enum CreativeUpdateStrategy {
        KEEP_OLD_CREATIVE,
        UPDATE_CREATIVE,
        UPDATE_TO_CREATIVE_MODERATE_YES
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "updateBanner_CreativeStatus_ModeForceSaveDraft_CreativeIdDoesNotChange",
                        BannerStatusModerate.YES,
                        CreativeUpdateStrategy.KEEP_OLD_CREATIVE,
                        ModerationMode.FORCE_SAVE_DRAFT,
                        null,
                        null
                },
                {
                        "updateBanner_CreativeStatus_ModeForceModerate_bannerWasNotDraft_CreativeIdDoesNotChange",
                        BannerStatusModerate.YES,
                        CreativeUpdateStrategy.KEEP_OLD_CREATIVE,
                        ModerationMode.FORCE_MODERATE,
                        null,
                        null
                },
                {
                        "updateBanner_CreativeStatus_ModeForceModerate_bannerWasDraft_CreativeIdDoesNotChange",
                        BannerStatusModerate.NEW,
                        CreativeUpdateStrategy.KEEP_OLD_CREATIVE,
                        ModerationMode.FORCE_MODERATE,
                        CREATIVE_MODERATE_READY,
                        null
                },
                {
                        "updateBanner_CreativeStatus_ModeForceSaveDraft_CreativeIdChanges",
                        BannerStatusModerate.YES,
                        CreativeUpdateStrategy.UPDATE_CREATIVE,
                        ModerationMode.FORCE_SAVE_DRAFT,
                        null,
                        EXPECTED_CREATIVE_GEO
                },
                {
                        "updateBanner_CreativeStatus_ModeForceModerate_bannerWasNotDraft_CreativeIdChanges",
                        BannerStatusModerate.YES,
                        CreativeUpdateStrategy.UPDATE_CREATIVE,
                        ModerationMode.FORCE_MODERATE,
                        CREATIVE_MODERATE_READY,
                        EXPECTED_CREATIVE_GEO
                },
                {
                        "updateBanner_CreativeStatus_ModeForceModerate_bannerWasDraft_CreativeIdChanges",
                        BannerStatusModerate.NEW,
                        CreativeUpdateStrategy.UPDATE_CREATIVE,
                        ModerationMode.FORCE_MODERATE,
                        CREATIVE_MODERATE_READY,
                        EXPECTED_CREATIVE_GEO
                },
                {
                        "updateBanner_dontUpdateCreativesGeoAndStatusModerate_whenCreativeStatusModerateIsYes",
                        BannerStatusModerate.NEW,
                        CreativeUpdateStrategy.UPDATE_TO_CREATIVE_MODERATE_YES,
                        ModerationMode.FORCE_MODERATE,
                        null,
                        null
                },
        });
    }

    private CampaignInfo campaignInfo;
    private Integer shard;
    private Creative oldCreative;
    private Creative newCreative;

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        ClientInfo clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();
        oldCreative = createCreativeWithStatusModerateAndGeo(clientInfo, CREATIVE_MODERATE_NEW,
                asList(RUSSIA_REGION_ID, BY_REGION_ID));

        Creative draftCreative = createCreativeWithStatusModerateAndGeo(clientInfo, CREATIVE_MODERATE_NEW,
                singletonList(BY_REGION_ID));

        Creative notDraftCreative = createCreativeWithStatusModerateAndGeo(clientInfo, CREATIVE_MODERATE_YES,
                asList(RUSSIA_REGION_ID, KAZAKHSTAN_REGION_ID, BY_REGION_ID));

        switch (creativeUpdateStrategy) {
            case KEEP_OLD_CREATIVE:
                newCreative = oldCreative;
                break;
            case UPDATE_CREATIVE:
                newCreative = draftCreative;
                break;
            case UPDATE_TO_CREATIVE_MODERATE_YES:
                newCreative = notDraftCreative;
                break;
        }
    }

    @Test
    public void testParametrized() {
        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(activePerformanceAdGroup(null, feedId)
                .withGeo(AD_GROUP_GEO), campaignInfo);
        bannerInfo = steps.performanceBannerSteps().createPerformanceBanner(
                performanceBanner(adGroupInfo, oldCreative, bannerStatusModerate, 0L));

        updateBannerCreativeId(newCreative.getId());

        StatusModerate statusModerate = expectedCreativeStatusModerate == null ?
                newCreative.getStatusModerate() : expectedCreativeStatusModerate;
        List<Long> sumGeo = expectedCreativeSumGeo == null ? newCreative.getSumGeo() : expectedCreativeSumGeo;
        checkCreativeStatusModerateAndGeo(adGroupInfo.getClientId(), newCreative.getId(), statusModerate, sumGeo);
    }

    private void updateBannerCreativeId(Long creativeId) {
        ModelChanges<PerformanceBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(),
                PerformanceBanner.class)
                .process(creativeId, PerformanceBanner.CREATIVE_ID);
        prepareAndApplyValid(List.of(modelChanges), moderationMode);
    }

    private void checkCreativeStatusModerateAndGeo(
            ClientId clientId,
            Long creativeId,
            ru.yandex.direct.core.entity.creative.model.StatusModerate expectedStatusModerate,
            List<Long> expectedCreativeGeo) {
        Creative actualCreative = creativeRepository
                .getCreatives(shard, clientId, singletonList(creativeId)).get(0);

        assertSoftly(softly -> {
            softly.assertThat(actualCreative.getStatusModerate()).isEqualTo(expectedStatusModerate);
            softly.assertThat(actualCreative.getSumGeo())
                    .is(matchedBy(containsInAnyOrder(expectedCreativeGeo.toArray())));
        });
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

}
