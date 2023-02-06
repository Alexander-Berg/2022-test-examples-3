package ru.yandex.direct.core.entity.banner.type.vcard;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.draftTextAdgroup;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithVcardAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    private UserInfo userInfo;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private VcardInfo vcardInfo;

    @Before
    public void before() {
        userInfo = steps.userSteps().createDefaultUser();
        clientInfo = userInfo.getClientInfo();
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        campaignInfo = adGroupInfo.getCampaignInfo();
        vcardInfo = steps.vcardSteps().createVcard(campaignInfo);
    }

    @Test
    public void addTextBanner_DraftCampaign_SaveDraftTest_VcardStatusModerateNew() {
        campaignInfo = steps.campaignSteps().createCampaign(newTextCampaign(null, null)
                .withStatusModerate(StatusModerate.NEW), clientInfo);
        adGroupInfo = steps.adGroupSteps().createAdGroup(draftTextAdgroup(null), campaignInfo);
        vcardInfo = steps.vcardSteps().createVcard(campaignInfo);

        TextBanner banner = clientBannerWithVcard();
        Long id = prepareAndApplyValid(banner, true);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getVcardId(), equalTo(vcardInfo.getVcardId()));
        assertThat(actualBanner.getVcardStatusModerate(), equalTo(BannerVcardStatusModerate.NEW));
    }

    @Test
    public void addTextBanner_DraftCampaign_NoSaveDraftTest_VcardStatusModerateNew() {
        campaignInfo = steps.campaignSteps().createCampaign(newTextCampaign(null, null)
                .withStatusModerate(StatusModerate.NEW), clientInfo);
        adGroupInfo = steps.adGroupSteps().createAdGroup(draftTextAdgroup(null), campaignInfo);
        vcardInfo = steps.vcardSteps().createVcard(campaignInfo);

        TextBanner banner = clientBannerWithVcard();
        Long id = prepareAndApplyValid(banner, false);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getVcardId(), equalTo(vcardInfo.getVcardId()));
        assertThat(actualBanner.getVcardStatusModerate(), equalTo(BannerVcardStatusModerate.NEW));
    }

    @Test
    public void addTextBanner_ActiveCampaign_SaveDraftTest_VcardStatusModerateNew() {
        TextBanner banner = clientBannerWithVcard();
        Long id = prepareAndApplyValid(banner, true);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getVcardId(), equalTo(vcardInfo.getVcardId()));
        assertThat(actualBanner.getVcardStatusModerate(), equalTo(BannerVcardStatusModerate.NEW));
    }

    @Test
    public void addTextBanner_ActiveCampaign_NoSaveDraftTest_VcardStatusModerateReady() {
        TextBanner banner = clientBannerWithVcard();
        Long id = prepareAndApplyValid(banner, false);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getVcardId(), equalTo(vcardInfo.getVcardId()));
        assertThat(actualBanner.getVcardStatusModerate(), equalTo(BannerVcardStatusModerate.READY));
    }

    @Test
    public void addTextBanner_NullVcardId_VcardStatusModerateNew() {
        TextBanner banner = clientBannerWithVcard()
                .withVcardId(null);
        Long id = prepareAndApplyValid(banner, false);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getVcardId(), nullValue());
        assertThat(actualBanner.getVcardStatusModerate(), equalTo(BannerVcardStatusModerate.NEW));
    }

    private TextBanner clientBannerWithVcard() {
        return clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withVcardId(vcardInfo.getVcardId());
    }
}
