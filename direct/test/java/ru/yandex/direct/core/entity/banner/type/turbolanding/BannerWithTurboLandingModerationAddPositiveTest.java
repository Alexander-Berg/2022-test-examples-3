package ru.yandex.direct.core.entity.banner.type.turbolanding;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.draftTextAdgroup;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboLandingModerationAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void addTextBanner_DraftCampaign_SaveDraftTest_TurboLandingStatusModerateNew() {
        var campaignInfo = steps.campaignSteps().createCampaign(newTextCampaign(null, null)
                .withStatusModerate(StatusModerate.NEW));
        adGroupInfo = steps.adGroupSteps().createAdGroup(draftTextAdgroup(null), campaignInfo);
        var turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(adGroupInfo.getClientId());
        var banner = clientTextBanner()
                .withTurboLandingId(turboLanding.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId());
        Long id = prepareAndApplyValid(banner, true);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getTurboLandingStatusModerate(), equalTo(BannerTurboLandingStatusModerate.NEW));
    }

    @Test
    public void addTextBanner_DraftCampaign_NoSaveDraftTest_TurboLandingStatusModerateNew() {
        var campaignInfo = steps.campaignSteps().createCampaign(newTextCampaign(null, null)
                .withStatusModerate(StatusModerate.NEW));
        adGroupInfo = steps.adGroupSteps().createAdGroup(draftTextAdgroup(null), campaignInfo);
        var turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(adGroupInfo.getClientId());
        var banner = clientTextBanner()
                .withTurboLandingId(turboLanding.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner, false);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getTurboLandingStatusModerate(), equalTo(BannerTurboLandingStatusModerate.NEW));
    }

    @Test
    public void addTextBanner_ActiveCampaign_SaveDraftTest_TurboLandingStatusModerateNew() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        var turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(adGroupInfo.getClientId());
        var banner = clientTextBanner()
                .withTurboLandingId(turboLanding.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner, true);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getTurboLandingStatusModerate(), equalTo(BannerTurboLandingStatusModerate.NEW));
    }

    @Test
    public void addTextBanner_ActiveCampaign_NoSaveDraftTest_TurboLandingStatusModerateReady() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        var turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(adGroupInfo.getClientId());
        var banner = clientTextBanner()
                .withTurboLandingId(turboLanding.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner, false);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getTurboLandingStatusModerate(), equalTo(BannerTurboLandingStatusModerate.READY));
    }
}
