package ru.yandex.direct.core.entity.banner.type.creative;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperation;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.repository.TestCreativeRepository;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.core.testing.stub.CanvasClientStub;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.result.MassResult;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultVideoAddition;
import static ru.yandex.direct.core.testing.data.TestNewCpmIndoorBanners.clientCpmIndoorBanner;
import static ru.yandex.direct.core.testing.data.TestNewMobileAppBanners.clientMobileAppBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithCreativeAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Autowired
    private TestCreativeRepository testCreativeRepository;

    @Autowired
    protected CanvasClientStub canvasClientStub;

    @Before
    public void before() {
        steps.trustedRedirectSteps().addValidCounters();
    }

    @Test
    public void validCreativeIdForCpmIndoorBanner() {
        adGroupInfo = steps.adGroupSteps().createDefaultCpmIndoorAdGroup();
        var creative = steps.creativeSteps()
                .addDefaultCpmIndoorVideoCreative(adGroupInfo.getClientInfo());
        var banner = clientCpmIndoorBanner(creative.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        CpmIndoorBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), equalTo(creative.getCreativeId()));
    }

    @Test
    public void validCreativeIdForTwoCpmIndoorBanner() {
        adGroupInfo = steps.adGroupSteps().createDefaultCpmIndoorAdGroup();
        var creativeId = steps.creativeSteps()
                .addDefaultCpmIndoorVideoCreative(adGroupInfo.getClientInfo())
                .getCreativeId();

        var banner1 = clientCpmIndoorBanner(creativeId)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        var banner2 = clientCpmIndoorBanner(creativeId)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        BannersAddOperation operation = createOperation(List.of(banner1, banner2), false);
        MassResult<Long> result = operation.prepareAndApply();
        assumeThat(result, isFullySuccessful());

        Long id1 = result.get(0).getResult();
        Long id2 = result.get(1).getResult();

        CpmIndoorBanner actualBanner1 = getBanner(id1);
        CpmIndoorBanner actualBanner2 = getBanner(id2);
        assertThat(actualBanner1.getCreativeId(), equalTo(creativeId));
        assertThat(actualBanner2.getCreativeId(), equalTo(creativeId));
    }

    @Test
    public void addCpmIndoorBanner_DraftCampaign_SaveDraftTest_CreativeStatusModerateNew() {
        var campaignInfo = steps.campaignSteps().createCampaign(newCpmBannerCampaign(null, null)
                .withStatusModerate(StatusModerate.NEW));
        adGroupInfo = steps.adGroupSteps().createDraftCpmIndoorAdGroup(campaignInfo);
        var creative = steps.creativeSteps().addDefaultCpmIndoorVideoCreative(adGroupInfo.getClientInfo());
        CpmIndoorBanner banner = clientCpmIndoorBanner(creative.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());
        Long id = prepareAndApplyValid(banner, true);
        CpmIndoorBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeStatusModerate(),
                equalTo(BannerCreativeStatusModerate.NEW));
    }

    @Test
    public void addCpmIndoorBanner_DraftCampaign_NoSaveDraftTest_CreativeStatusModerateNew() {
        var campaignInfo = steps.campaignSteps().createCampaign(newCpmBannerCampaign(null, null)
                .withStatusModerate(StatusModerate.NEW));
        adGroupInfo = steps.adGroupSteps().createDraftCpmIndoorAdGroup(campaignInfo);
        var creative = steps.creativeSteps().addDefaultCpmIndoorVideoCreative(adGroupInfo.getClientInfo());
        CpmIndoorBanner banner = clientCpmIndoorBanner(creative.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());
        Long id = prepareAndApplyValid(banner, false);
        CpmIndoorBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeStatusModerate(),
                equalTo(BannerCreativeStatusModerate.NEW));
    }

    @Test
    public void addCpmIndoorBanner_ActiveCampaign_SaveDraftTest_CreativeStatusModerateNew() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmIndoorAdGroup();
        var creative = steps.creativeSteps().addDefaultCpmIndoorVideoCreative(adGroupInfo.getClientInfo());
        CpmIndoorBanner banner = clientCpmIndoorBanner(creative.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());
        Long id = prepareAndApplyValid(banner, true);
        CpmIndoorBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeStatusModerate(),
                equalTo(BannerCreativeStatusModerate.NEW));
    }

    @Test
    public void addCpmIndoorBanner_ActiveCampaign_NoSaveDraftTest_CreativeStatusModerateReady() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmIndoorAdGroup();
        var creative = steps.creativeSteps().addDefaultCpmIndoorVideoCreative(adGroupInfo.getClientInfo());
        CpmIndoorBanner banner = clientCpmIndoorBanner(creative.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());
        Long id = prepareAndApplyValid(banner, false);
        CpmIndoorBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeStatusModerate(),
                equalTo(BannerCreativeStatusModerate.READY));
    }

    @Test
    public void validCreativeForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());
        Long expectedCreativeId = creativeInfo.getCreativeId();

        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withCreativeId(expectedCreativeId);

        Long id = prepareAndApplyValid(banner);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), equalTo(expectedCreativeId));
        assertThat(actualBanner.getShowTitleAndBody(), equalTo(false));
    }

    @Test
    public void addTextBannerWithCreativeId_VideoAdditionSync_DataIsSaved() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        long creativeId = testCreativeRepository.getNextCreativeId();
        canvasClientStub.addCreatives(Collections.singletonList(creativeId));

        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withCreativeId(creativeId);

        Long id = prepareAndApplyValid(banner);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), equalTo(creativeId));
    }

    @Test
    public void addTextBannerWithCreativeId_ShowTitleAndBody_IsSaved() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);

        long creativeId = testCreativeRepository.getNextCreativeId();
        canvasClientStub.addCreatives(Collections.singletonList(creativeId));

        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withCreativeId(creativeId)
                .withShowTitleAndBody(true);

        Long id = prepareAndApplyValid(banner);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), equalTo(creativeId));
        assertThat(actualBanner.getShowTitleAndBody(), equalTo(true));
    }

    @Test
    public void addMobileAppBanner_ActiveCampaign_NoSaveDraftTest_CreativeStatusModerateReady() {
        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup();
        var creative = steps.creativeSteps().addDefaultVideoAdditionCreative(adGroupInfo.getClientInfo());
        MobileAppBanner banner = clientMobileAppBanner()
                .withHref("https://trusted1.com")
                .withImpressionUrl("https://trusted.impression.com/impression")
                .withCreativeId(creative.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());
        Long id = prepareAndApplyValid(banner, false);
        MobileAppBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeStatusModerate(),
                equalTo(BannerCreativeStatusModerate.READY));
    }
}
