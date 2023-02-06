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

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.model.BannerLink;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.cpm.video.CpmVideoBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.cpm.video.CpmVideoBannerRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.Steps;
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
import static ru.yandex.direct.core.testing.data.TestBanners.ANOTHER_DEFAULT_BS_BANNER_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendCpmVideoBannerModerationRequestTest {

    @Autowired
    private Steps steps;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private CpmVideoBannerSender cpmVideoBannerSender;

    private int shard;
    private CpmBannerInfo cpmBannerInfo;
    private ClientInfo clientInfo;
    private CreativeInfo creativeInfo;
    private ClientId clientId;

    @Before
    public void before() throws IOException {
        ppcPropertiesSupport.set("cpm_video_moderation_rate", "100");

        // when(ppcPropertiesSupport.get("")).thenReturn("100");

        clientInfo = steps.clientSteps().createDefaultClient();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup(clientInfo);
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);
        long adGroupId = adGroupInfo.getAdGroupId();
        long campaignId = campaignInfo.getCampaignId();
        creativeInfo = steps.creativeSteps()
                .addDefaultCpmVideoAdditionCreative(clientInfo, steps.creativeSteps().getNextCreativeId());

        cpmBannerInfo = steps.bannerSteps().createActiveCpmVideoBanner(
                activeCpmVideoBanner(campaignId, adGroupId, creativeInfo.getCreativeId()),
                clientInfo);

        shard = cpmBannerInfo.getShard();
        clientId = cpmBannerInfo.getClientId();
        bannerRepository.updateStatusModerate(shard, singletonList(cpmBannerInfo.getBannerId()), OldBannerStatusModerate.READY);
    }

    @Test
    public void makeCpmVideoModerationRequests_RequestDataIsCorrect() {
        List<CpmVideoBannerModerationRequest> requests =
                makeCpmVideoBannerModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CpmVideoBannerRequestData actual = requests.get(0).getData();

        CpmVideoBannerRequestData expected = new CpmVideoBannerRequestData();
        expected.setHref(cpmBannerInfo.getBanner().getHref());
        expected.setCreativeId(creativeInfo.getCreativeId());
        expected.setPreviewUrl(creativeInfo.getCreative().getPreviewUrl());
        expected.setDomain(cpmBannerInfo.getBanner().getDomain());
        expected.setBody(cpmBannerInfo.getBanner().getBody());
        expected.setTitle(cpmBannerInfo.getBanner().getTitle());
        expected.setTitleExtension(cpmBannerInfo.getBanner().getTitleExtension());
        expected.setGeo("225");
        expected.setUserFlags(emptyList());
        expected.setLinks(List.of(
                new BannerLink()
                        .setHref(cpmBannerInfo.getBanner().getHref())
                        .setParametrizedHref(cpmBannerInfo.getBanner().getHref())
        ));

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeCpmVideoModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<CpmVideoBannerModerationRequest> requests =
                makeCpmVideoBannerModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CpmVideoBannerRequestData actual = requests.get(0).getData();

        CpmVideoBannerRequestData expected = new CpmVideoBannerRequestData();
        expected.setHref(cpmBannerInfo.getBanner().getHref());
        expected.setCreativeId(creativeInfo.getCreativeId());
        expected.setPreviewUrl(creativeInfo.getCreative().getPreviewUrl());
        expected.setDomain(cpmBannerInfo.getBanner().getDomain());
        expected.setBody(cpmBannerInfo.getBanner().getBody());
        expected.setTitle(cpmBannerInfo.getBanner().getTitle());
        expected.setTitleExtension(cpmBannerInfo.getBanner().getTitleExtension());
        expected.setGeo("225");
        expected.setAsSoonAsPossible(true);
        expected.setUserFlags(emptyList());
        expected.setLinks(List.of(
                new BannerLink()
                        .setHref(cpmBannerInfo.getBanner().getHref())
                        .setParametrizedHref(cpmBannerInfo.getBanner().getHref())
        ));

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeCpmVideoModerationRequests_ClientWithNoAsapFlag_NoAsapInRequest() {
        List<CpmVideoBannerModerationRequest> requests =
                makeCpmVideoBannerModerationRequests(shard, singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CpmVideoBannerRequestData actual = requests.get(0).getData();

        assertThat("В запросе нет параметра asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeCpmVideoBannerModerationRequests_MetaIsCorrect() {
        List<CpmVideoBannerModerationRequest> requests =
                makeCpmVideoBannerModerationRequests(shard,
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
        expected.setVersionId(64);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeCpmVideoBannerModerationRequests_ManualOnly() {

        steps.bannerSteps().addBannerReModerationFlag(shard, cpmBannerInfo.getBannerId(),
                List.of(RemoderationType.BANNER));

        List<CpmVideoBannerModerationRequest> requests =
                makeCpmVideoBannerModerationRequests(shard,
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
        expected.setVersionId(64);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    private List<CpmVideoBannerModerationRequest> makeCpmVideoBannerModerationRequests(int shard,
                                                                                       List<Long> bids) {
        Consumer<List<CpmVideoBannerModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<CpmVideoBannerModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        cpmVideoBannerSender.send(shard, bids, e -> System.currentTimeMillis(), e -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }

}
