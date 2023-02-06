package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.moderation.model.AspectRatio;
import ru.yandex.direct.core.entity.moderation.model.BannerLink;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.cpm.canvas.CanvasBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.cpm.canvas.CanvasBannerRequestData;
import ru.yandex.direct.core.entity.moderation.model.mobile_content.MobileAppModerationData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.ImageCreativeBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesAdgroupType;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.model.BaseModerationData.ASAP_PROPERTY_NAME;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.MANUAL;
import static ru.yandex.direct.core.entity.moderation.model.cpm.canvas.CanvasModerationInfoConverter.toCanvasModerationInfo;
import static ru.yandex.direct.core.entity.moderation.service.sending.CanvasBannerSender.INITIAL_VERSION;
import static ru.yandex.direct.core.testing.data.TestBanners.ANOTHER_DEFAULT_BS_BANNER_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageCreativeBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendMobileCanvasModerationRequestTest {

    @Autowired
    private Steps steps;

    @Autowired
    private MobileCanvasBannerSender canvasBannerSender;

    private ClientId clientId;
    private int shard;
    private ImageCreativeBannerInfo bannerInfo;
    private ClientInfo clientInfo;
    private CreativeInfo creativeInfo;
    private OldBannerTurboLanding turbolanding;
    private MobileContentAdGroup mobileContentAdGroup;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreative(clientInfo, steps.creativeSteps().getNextCreativeId());
        turbolanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientInfo.getClientId());

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        mobileContentAdGroup = (MobileContentAdGroup) adGroupInfo.getAdGroup();


        bannerInfo = steps.bannerSteps().createActiveImageCreativeBanner(
                activeImageCreativeBanner(campaignInfo.getCampaignId(), null, creativeInfo.getCreativeId())
                        .withTurboLandingId(turbolanding.getId())
                        .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.READY)
                        .withStatusModerate(OldBannerStatusModerate.READY),
                adGroupInfo
        );
    }

    @Test
    public void makeCanvasModerationRequests_RequestDataIsCorrect() {
        List<CanvasBannerModerationRequest> requests =
                makeMobileCanvasModerationRequests(shard,
                        singletonList(bannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CanvasBannerRequestData actual = requests.get(0).getData();

        CanvasBannerRequestData expected = new CanvasBannerRequestData();
        expected.setHref(bannerInfo.getBanner().getHref());
        expected.setCreativeId(creativeInfo.getCreativeId());
        expected.setPreviewUrl(creativeInfo.getCreative().getPreviewUrl());
        expected.setDomain(bannerInfo.getBanner().getDomain());
        expected.setAspectRatio(
                new AspectRatio(creativeInfo.getCreative().getWidth(), creativeInfo.getCreative().getHeight())
        );
        expected.setModerationInfo(toCanvasModerationInfo(creativeInfo.getCreative().getModerationInfo()));
        expected.setParametrizedHref(bannerInfo.getBanner().getHref());
        expected.setLinks(singletonList(
                new BannerLink()
                        .setHref(bannerInfo.getBanner().getHref())
                        .setParametrizedHref(bannerInfo.getBanner().getHref())
                        .setMobileHref(turbolanding.getUrl())
        ));
        expected.setGeo("225");
        expected.setUserFlags(emptyList());
        expected.setMobileContentModerationData(getExpectedMobileAppModerationData());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeCanvasModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<CanvasBannerModerationRequest> requests =
                makeMobileCanvasModerationRequests(shard,
                        singletonList(bannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CanvasBannerRequestData actual = requests.get(0).getData();

        CanvasBannerRequestData expected = new CanvasBannerRequestData();
        expected.setHref(bannerInfo.getBanner().getHref());
        expected.setCreativeId(creativeInfo.getCreativeId());
        expected.setPreviewUrl(creativeInfo.getCreative().getPreviewUrl());
        expected.setDomain(bannerInfo.getBanner().getDomain());
        expected.setAspectRatio(
                new AspectRatio(creativeInfo.getCreative().getWidth(), creativeInfo.getCreative().getHeight())
        );
        expected.setModerationInfo(toCanvasModerationInfo(creativeInfo.getCreative().getModerationInfo()));
        expected.setParametrizedHref(bannerInfo.getBanner().getHref());
        expected.setLinks(singletonList(
                new BannerLink()
                        .setHref(bannerInfo.getBanner().getHref())
                        .setParametrizedHref(bannerInfo.getBanner().getHref())
                        .setMobileHref(turbolanding.getUrl())
        ));
        expected.setGeo("225");
        expected.setAsSoonAsPossible(true);
        expected.setUserFlags(emptyList());
        expected.setMobileContentModerationData(getExpectedMobileAppModerationData());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeCanvasModerationRequests_ClientWithNoAsapFlag_NoAsapInRequest() {
        List<CanvasBannerModerationRequest> requests =
                makeMobileCanvasModerationRequests(shard, singletonList(bannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CanvasBannerRequestData actual = requests.get(0).getData();

        assertThat("В запросе нет параметра asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeCanvasBannerModerationRequests_MetaIsCorrect() {
        List<CanvasBannerModerationRequest> requests =
                makeMobileCanvasModerationRequests(shard,
                        singletonList(bannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(bannerInfo.getCampaignId());
        expected.setAdGroupId(bannerInfo.getAdGroupId());
        expected.setBannerId(bannerInfo.getBannerId());
        expected.setClientId(bannerInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID);
        expected.setVersionId(INITIAL_VERSION);
        expected.setBannerType(BannersBannerType.image_ad);
        expected.setCampaignType(CampaignsType.mobile_content);
        expected.setAdgroupType(PhrasesAdgroupType.mobile_content);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeCanvasBannerModerationRequests_ManualOnly() {

        steps.bannerSteps().addBannerReModerationFlag(shard, bannerInfo.getBannerId(),
                List.of(RemoderationType.BANNER));

        List<CanvasBannerModerationRequest> requests =
                makeMobileCanvasModerationRequests(shard,
                        singletonList(bannerInfo.getBannerId()));

        assertFalse(steps.bannerSteps().isBannerReModerationFlagPresent(shard, bannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        assertEquals(requests.get(0).getWorkflow(), MANUAL);

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(bannerInfo.getCampaignId());
        expected.setAdGroupId(bannerInfo.getAdGroupId());
        expected.setBannerId(bannerInfo.getBannerId());
        expected.setClientId(bannerInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID);
        expected.setVersionId(INITIAL_VERSION);
        expected.setBannerType(BannersBannerType.image_ad);
        expected.setCampaignType(CampaignsType.mobile_content);
        expected.setAdgroupType(PhrasesAdgroupType.mobile_content);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    private List<CanvasBannerModerationRequest> makeMobileCanvasModerationRequests(int shard,
                                                                                   List<Long> bids) {
        Consumer<List<CanvasBannerModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<CanvasBannerModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        canvasBannerSender.send(shard, bids, e -> System.currentTimeMillis(), e -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }


    private MobileAppModerationData getExpectedMobileAppModerationData() {
        MobileAppModerationData data = new MobileAppModerationData();

        data.setStoreContentId(mobileContentAdGroup.getMobileContent().getStoreContentId());
        data.setAppHref(mobileContentAdGroup.getStoreUrl());
        data.setMobileContentId(mobileContentAdGroup.getMobileContentId());
        data.setBundleId(mobileContentAdGroup.getMobileContent().getBundleId());

        return data;
    }
}
