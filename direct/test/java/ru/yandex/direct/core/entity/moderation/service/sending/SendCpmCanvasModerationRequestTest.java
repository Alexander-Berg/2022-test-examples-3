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

import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.moderation.model.AspectRatio;
import ru.yandex.direct.core.entity.moderation.model.BannerLink;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.cpm.canvas.CanvasBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.cpm.canvas.CanvasBannerRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
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
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendCpmCanvasModerationRequestTest {

    @Autowired
    private Steps steps;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private CpmCanvasBannerSender canvasBannerSender;

    private int shard;
    private OldBannerTurboLanding turbolanding;
    private CpmBannerInfo cpmBannerInfo;
    private ClientInfo clientInfo;
    private CreativeInfo creativeInfo;
    private ClientId clientId;

    @Before
    public void before() throws IOException {

        clientInfo = steps.clientSteps().createDefaultClient();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo);
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);
        long adGroupId = adGroupInfo.getAdGroupId();
        long campaignId = campaignInfo.getCampaignId();

        creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreative(clientInfo, steps.creativeSteps().getNextCreativeId());

        turbolanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientInfo.getClientId());
        cpmBannerInfo = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(campaignId, adGroupId, creativeInfo.getCreativeId())
                        .withTurboLandingId(turbolanding.getId())
                        .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.READY),
                clientInfo);

        shard = cpmBannerInfo.getShard();
        clientId = cpmBannerInfo.getClientId();
        bannerRepository.updateStatusModerate(shard, singletonList(cpmBannerInfo.getBannerId()), OldBannerStatusModerate.READY);
    }

    @Test
    public void makeCanvasModerationRequests_RequestDataIsCorrect() {
        List<CanvasBannerModerationRequest> requests =
                makeCpmCanvasModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CanvasBannerRequestData actual = requests.get(0).getData();

        CanvasBannerRequestData expected = new CanvasBannerRequestData();
        expected.setHref(cpmBannerInfo.getBanner().getHref());
        expected.setCreativeId(creativeInfo.getCreativeId());
        expected.setPreviewUrl(creativeInfo.getCreative().getPreviewUrl());
        expected.setDomain(cpmBannerInfo.getBanner().getDomain());
        expected.setAspectRatio(
                new AspectRatio(creativeInfo.getCreative().getWidth(), creativeInfo.getCreative().getHeight())
        );
        expected.setModerationInfo(toCanvasModerationInfo(creativeInfo.getCreative().getModerationInfo()));
        expected.setParametrizedHref(cpmBannerInfo.getBanner().getHref());
        expected.setLinks(singletonList(
                new BannerLink()
                        .setHref(cpmBannerInfo.getBanner().getHref())
                        .setParametrizedHref(cpmBannerInfo.getBanner().getHref())
                        .setMobileHref(turbolanding.getUrl())
        ));
        expected.setGeo("225");
        expected.setUserFlags(emptyList());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeCanvasModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<CanvasBannerModerationRequest> requests =
                makeCpmCanvasModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CanvasBannerRequestData actual = requests.get(0).getData();

        CanvasBannerRequestData expected = new CanvasBannerRequestData();
        expected.setHref(cpmBannerInfo.getBanner().getHref());
        expected.setCreativeId(creativeInfo.getCreativeId());
        expected.setPreviewUrl(creativeInfo.getCreative().getPreviewUrl());
        expected.setDomain(cpmBannerInfo.getBanner().getDomain());
        expected.setAspectRatio(
                new AspectRatio(creativeInfo.getCreative().getWidth(), creativeInfo.getCreative().getHeight())
        );
        expected.setModerationInfo(toCanvasModerationInfo(creativeInfo.getCreative().getModerationInfo()));
        expected.setParametrizedHref(cpmBannerInfo.getBanner().getHref());
        expected.setLinks(singletonList(
                new BannerLink()
                        .setHref(cpmBannerInfo.getBanner().getHref())
                        .setParametrizedHref(cpmBannerInfo.getBanner().getHref())
                        .setMobileHref(turbolanding.getUrl())
        ));
        expected.setGeo("225");
        expected.setAsSoonAsPossible(true);
        expected.setUserFlags(emptyList());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeCanvasModerationRequests_ClientWithNoAsapFlag_NoAsapInRequest() {
        List<CanvasBannerModerationRequest> requests =
                makeCpmCanvasModerationRequests(shard, singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CanvasBannerRequestData actual = requests.get(0).getData();

        assertThat("В запросе нет параметра asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeCanvasBannerModerationRequests_MetaIsCorrect() {
        List<CanvasBannerModerationRequest> requests =
                makeCpmCanvasModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(cpmBannerInfo.getCampaignId());
        expected.setAdGroupId(cpmBannerInfo.getAdGroupId());
        expected.setBannerId(cpmBannerInfo.getBannerId());
        expected.setClientId(cpmBannerInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID);
        expected.setVersionId(INITIAL_VERSION);
        expected.setBannerType(BannersBannerType.cpm_banner);
        expected.setCampaignType(CampaignsType.cpm_banner);
        expected.setAdgroupType(PhrasesAdgroupType.cpm_banner);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeCanvasBannerModerationRequests_ManualOnly() {

        steps.bannerSteps().addBannerReModerationFlag(shard, cpmBannerInfo.getBannerId(),
                List.of(RemoderationType.BANNER));

        List<CanvasBannerModerationRequest> requests =
                makeCpmCanvasModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));

        assertFalse(steps.bannerSteps().isBannerReModerationFlagPresent(shard, cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        assertEquals(requests.get(0).getWorkflow(), MANUAL);

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(cpmBannerInfo.getCampaignId());
        expected.setAdGroupId(cpmBannerInfo.getAdGroupId());
        expected.setBannerId(cpmBannerInfo.getBannerId());
        expected.setClientId(cpmBannerInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID);
        expected.setVersionId(INITIAL_VERSION);
        expected.setBannerType(BannersBannerType.cpm_banner);
        expected.setCampaignType(CampaignsType.cpm_banner);
        expected.setAdgroupType(PhrasesAdgroupType.cpm_banner);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    private List<CanvasBannerModerationRequest> makeCpmCanvasModerationRequests(int shard,
                                                                                List<Long> bids) {
        Consumer<List<CanvasBannerModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<CanvasBannerModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        canvasBannerSender.send(shard, bids, e -> System.currentTimeMillis(), e -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }

}
