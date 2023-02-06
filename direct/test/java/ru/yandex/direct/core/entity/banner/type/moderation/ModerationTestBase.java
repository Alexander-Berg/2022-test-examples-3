package ru.yandex.direct.core.entity.banner.type.moderation;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithModerationStatuses;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.operation.Operation;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singleton;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.PHRASES;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

public abstract class ModerationTestBase {

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public StatusModerate adGroupStatusModerate;

    @Parameterized.Parameter(2)
    public StatusPostModerate adGroupStatusPostModerate;

    @Parameterized.Parameter(3)
    public CampaignStatusModerate campaignStatusModerate;

    @Parameterized.Parameter(5)
    public BannerStatusModerate expectedBannerStatusModerate;

    @Parameterized.Parameter(6)
    public BannerStatusPostModerate expectedBannerStatusPostModerate;

    @Parameterized.Parameter(7)
    public StatusModerate expectedAdGroupStatusModerate;

    @Parameterized.Parameter(8)
    public StatusPostModerate expectedAdGroupStatusPostModerate;

    @Parameterized.Parameter(9)
    public CampaignStatusModerate expectedCampaignStatusModerate;

    @Autowired
    public AdGroupRepository adGroupRepository;

    @Autowired
    public CampaignRepository campaignRepository;

    @Autowired
    public BannerTypedRepository bannerTypedRepository;

    @Autowired
    public FeatureService featureService;

    @Autowired
    public DslContextProvider dslContextProvider;

    @Autowired
    public Steps steps;

    public AdGroupInfo adGroupInfo;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Test
    public void testModeration() {
        adGroupInfo = createAdGroup();
        setCampaignModerationStatuses();
        setAdGroupModerationStatuses();

        Operation<Long> operation = createOperation(adGroupInfo);

        MassResult<Long> result = operation.prepareAndApply();
        assumeThat(result, isFullySuccessful());

        Long bannerId = result.get(0).getResult();

        SoftAssertions.assertSoftly(assertions -> {
            checkBannerModerationStatuses(adGroupInfo, bannerId, assertions);
            checkAdGroupModerationStatuses(adGroupInfo, assertions);
            checkCampaignModerationStatuses(adGroupInfo, assertions);
        });
    }

    protected abstract AdGroupInfo createAdGroup();

    protected void setCampaignModerationStatuses() {
        dslContextProvider.ppc(adGroupInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_MODERATE, CampaignStatusModerate.toSource(campaignStatusModerate))
                .where(CAMPAIGNS.CID.eq(adGroupInfo.getCampaignId()))
                .execute();
    }

    protected void setAdGroupModerationStatuses() {
        dslContextProvider.ppc(adGroupInfo.getShard())
                .update(PHRASES)
                .set(PHRASES.STATUS_MODERATE, StatusModerate.toSource(adGroupStatusModerate))
                .set(PHRASES.STATUS_POST_MODERATE, StatusPostModerate.toSource(adGroupStatusPostModerate))
                .where(PHRASES.PID.eq(adGroupInfo.getAdGroupId()))
                .execute();
    }

    protected abstract Operation<Long> createOperation(AdGroupInfo adGroupInfo);

    protected void checkBannerModerationStatuses(AdGroupInfo adGroupInfo, Long bannerId, SoftAssertions assertions) {
        Banner banner = bannerTypedRepository.getTyped(adGroupInfo.getShard(),
                singleton(bannerId)).get(0);
        BannerWithModerationStatuses bannerWithModerationStatuses =
                (BannerWithModerationStatuses) banner;
        assertions.assertThat(bannerWithModerationStatuses.getStatusModerate())
                .as("banner.statusModerate")
                .isEqualTo(expectedBannerStatusModerate);
        assertions.assertThat(bannerWithModerationStatuses.getStatusPostModerate())
                .as("banner.statusPostModerate")
                .isEqualTo(expectedBannerStatusPostModerate);
    }

    protected void checkAdGroupModerationStatuses(AdGroupInfo adGroupInfo, SoftAssertions assertions) {
        AdGroup adGroup = adGroupRepository.getAdGroups(
                adGroupInfo.getShard(), singleton(adGroupInfo.getAdGroupId())).get(0);
        assertions.assertThat(adGroup.getStatusModerate())
                .as("adGroup.statusModerate")
                .isEqualTo(expectedAdGroupStatusModerate);
        assertions.assertThat(adGroup.getStatusPostModerate())
                .as("adGroup.statusPostModerate")
                .isEqualTo(expectedAdGroupStatusPostModerate);
    }

    protected void checkCampaignModerationStatuses(AdGroupInfo adGroupInfo, SoftAssertions assertions) {
        Campaign campaign = campaignRepository.getCampaigns(adGroupInfo.getShard(),
                singleton(adGroupInfo.getCampaignId())).get(0);
        assertions.assertThat(campaign.getStatusModerate())
                .as("campaign.statusModerate")
                .isEqualTo(expectedCampaignStatusModerate);
    }
}
