package ru.yandex.direct.core.entity.banner.type.sitelink;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.draftTextAdgroup;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithSitelinksAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    private ClientInfo clientInfo;
    private SitelinkSetInfo sitelinkSetInfo;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        clientInfo = adGroupInfo.getClientInfo();
        sitelinkSetInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);
    }

    @Test
    public void addTextBanner_DraftCampaign_SaveDraftTest_StatusSitelinksModerateNew() {
        var campaignInfo = steps.campaignSteps().createCampaign(newTextCampaign(null, null)
                .withStatusModerate(StatusModerate.NEW), clientInfo);
        adGroupInfo = steps.adGroupSteps().createAdGroup(draftTextAdgroup(null), campaignInfo);
        TextBanner banner = clientBanner();
        Long id = prepareAndApplyValid(banner, true);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getSitelinksSetId(), equalTo(sitelinkSetInfo.getSitelinkSetId()));
        assertThat(actualBanner.getStatusSitelinksModerate(), equalTo(BannerStatusSitelinksModerate.NEW));
    }

    @Test
    public void addTextBanner_DraftCampaign_NoSaveDraftTest_StatusSitelinksModerateNew() {
        var campaignInfo = steps.campaignSteps().createCampaign(newTextCampaign(null, null)
                .withStatusModerate(StatusModerate.NEW), clientInfo);
        adGroupInfo = steps.adGroupSteps().createAdGroup(draftTextAdgroup(null), campaignInfo);
        TextBanner banner = clientBanner();
        Long id = prepareAndApplyValid(banner, false);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getSitelinksSetId(), equalTo(sitelinkSetInfo.getSitelinkSetId()));
        assertThat(actualBanner.getStatusSitelinksModerate(), equalTo(BannerStatusSitelinksModerate.NEW));
    }

    @Test
    public void addTextBanner_ActiveCampaign_SaveDraftTest_StatusSitelinksModerateNew() {
        TextBanner banner = clientBanner();
        Long id = prepareAndApplyValid(banner, true);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getSitelinksSetId(), equalTo(sitelinkSetInfo.getSitelinkSetId()));
        assertThat(actualBanner.getStatusSitelinksModerate(), equalTo(BannerStatusSitelinksModerate.NEW));
    }

    @Test
    public void addTextBanner_ActiveCampaign_NoSaveDraftTest_StatusSitelinksModerateReady() {
        TextBanner banner = clientBanner();
        Long id = prepareAndApplyValid(banner, false);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getSitelinksSetId(), equalTo(sitelinkSetInfo.getSitelinkSetId()));
        assertThat(actualBanner.getStatusSitelinksModerate(), equalTo(BannerStatusSitelinksModerate.READY));
    }

    @Test
    public void addTextBanner_NullSitelinksSetId_StatusSitelinksModerateNew() {
        TextBanner banner = clientBanner()
                .withSitelinksSetId(null);
        Long id = prepareAndApplyValid(banner, false);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getSitelinksSetId(), nullValue());
        assertThat(actualBanner.getStatusSitelinksModerate(), equalTo(BannerStatusSitelinksModerate.NEW));
    }

    private TextBanner clientBanner() {
        return clientTextBanner()
                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId())
                .withAdGroupId(adGroupInfo.getAdGroupId());
    }
}
