package ru.yandex.direct.core.entity.moderation.service.sending;

import java.util.Collections;
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

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.moderation.model.AspectRatio;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.cpm.audio.CpmAudioBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.cpm.audio.CpmAudioBannerRequestData;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmAudioBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusmoderate;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.model.BaseModerationData.ASAP_PROPERTY_NAME;
import static ru.yandex.direct.core.entity.moderation.service.sending.CpmAudioBannerSender.CPM_AUDIO_DEFAULT_HEIGHT;
import static ru.yandex.direct.core.entity.moderation.service.sending.CpmAudioBannerSender.CPM_AUDIO_DEFAULT_WIDTH;
import static ru.yandex.direct.core.testing.data.TestBanners.DEFAULT_BS_BANNER_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmAudioBanner;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmAudioAddition;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmAudioAdGroup;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendAudioModerationRequestTest {

    private static final long DEFAULT_VERSION = 2L;


    @Autowired
    TestBannerCreativeRepository testBannerCreativeRepository;

    @Autowired
    TestModerationRepository testModerationRepository;

    @Autowired
    Steps steps;

    @Autowired
    CpmAudioBannerSender cpmAudioBannerSender;

    private CpmAudioBannerInfo bannerInfo;
    private Creative creative;
    private int shard;
    private ClientId clientId;

    @Before
    public void before() {
        AdGroupInfo adGroupInfo =
                steps.adGroupSteps().createAdGroup(new AdGroupInfo()
                        .withAdGroup(activeCpmAudioAdGroup(null).withGeo(Collections.singletonList(Region.MOSCOW_REGION_ID)))
                        .withClientInfo(new ClientInfo().withClient(defaultClient())));
        bannerInfo = steps.bannerSteps().createActiveCpmAudioBanner(
                activeCpmAudioBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), null)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.READY)
                        .withStatusBsSynced(StatusBsSynced.NO),
                adGroupInfo);

        creative = defaultCpmAudioAddition(adGroupInfo.getClientId(), null);
        steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());

        testBannerCreativeRepository.linkBannerWithCreative(bannerInfo, creative.getId());

        testModerationRepository.createBannerVersion(adGroupInfo.getShard(), bannerInfo.getBannerId(), DEFAULT_VERSION);

        shard = adGroupInfo.getShard();
        clientId = adGroupInfo.getClientId();
    }

    @Test
    public void moderateOneCpmAudioBanner_CheckRequestData() {
        BannerModerationMeta expectedRequestMeta = getExpectedBannerModerationMeta(bannerInfo);

        CpmAudioBannerModerationRequest request = makeCpmAudioModerationRequests(bannerInfo);
        BannerModerationMeta requestMeta = request.getMeta();

        assertThat("мета соответствует ожиданию", requestMeta, beanDiffer(expectedRequestMeta));

        CpmAudioBannerRequestData expectedRequestData = getExpectedCpmAudioBannerRequestData(bannerInfo, creative);
        CpmAudioBannerRequestData requestData = request.getData();

        assertThat("data соответствует ожиданию", requestData, beanDiffer(expectedRequestData));
    }

    @Test
    public void moderateOneCpmAudioBanner_ClientWithAsapFlag_CheckRequestData() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        BannerModerationMeta expectedRequestMeta = getExpectedBannerModerationMeta(bannerInfo);

        CpmAudioBannerModerationRequest request = makeCpmAudioModerationRequests(bannerInfo);
        BannerModerationMeta requestMeta = request.getMeta();

        assertThat("мета соответствует ожиданию", requestMeta, beanDiffer(expectedRequestMeta));

        CpmAudioBannerRequestData expectedRequestData = getExpectedCpmAudioBannerRequestData(bannerInfo, creative);
        expectedRequestData.setAsSoonAsPossible(true);
        CpmAudioBannerRequestData requestData = request.getData();

        assertThat("data соответствует ожиданию", requestData, beanDiffer(expectedRequestData));
    }

    @Test
    public void moderateOneCpmAudioBanner_ClientWithNoAsapFlag_NoAsapInRequest() {
        CpmAudioBannerModerationRequest request = makeCpmAudioModerationRequests(bannerInfo);

        assertThat(toJson(request), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void moderateOneCpmAudioBanner_CheckRequestData_WrongCreative() {
        testBannerCreativeRepository.deleteByCreativeIds(bannerInfo.getShard(), creative.getId());
        List<VideoFormat> creativeFormats = Collections.singletonList(new VideoFormat()
                .withType("video/mp4")
                .withUrl("http://yandex.ru/someVideo.mp4"));
        creative = defaultCpmAudioAddition(bannerInfo.getClientId(), null)
                .withAdditionalData(new AdditionalData().withFormats(creativeFormats));
        steps.creativeSteps().createCreative(creative, bannerInfo.getClientInfo());
        testBannerCreativeRepository.linkBannerWithCreative(bannerInfo, creative.getId());

        CpmAudioBannerModerationRequest request = makeCpmAudioModerationRequests(bannerInfo);
        CpmAudioBannerRequestData requestData = request.getData();
        assertEquals(CPM_AUDIO_DEFAULT_WIDTH, requestData.getAspectRatio().getWidth());
        assertEquals(CPM_AUDIO_DEFAULT_HEIGHT, requestData.getAspectRatio().getHeight());
    }

    @Test
    public void moderateOneCpmAudioBanner_CheckDataInDb() {
        makeCpmAudioModerationRequests(bannerInfo);

        long bannerVersion =
                testModerationRepository.getBannerVersion(bannerInfo.getShard(), bannerInfo.getBannerId());
        assertEquals(DEFAULT_VERSION + 1, bannerVersion);

        assertEquals(BannersStatusmoderate.Sent,
                testModerationRepository.getStatusModerate(bannerInfo.getShard(), bannerInfo.getBannerId()));
    }

    private BannerModerationMeta getExpectedBannerModerationMeta(CpmAudioBannerInfo bannerInfo) {
        BannerModerationMeta meta = new BannerModerationMeta();
        meta.setClientId(bannerInfo.getClientId().asLong());
        meta.setCampaignId(bannerInfo.getCampaignId());
        meta.setAdGroupId(bannerInfo.getAdGroupId());
        meta.setBannerId(bannerInfo.getBannerId());
        meta.setUid(bannerInfo.getUid());
        meta.setVersionId(DEFAULT_VERSION + 1);
        meta.setBsBannerId(DEFAULT_BS_BANNER_ID);
        return meta;
    }

    private CpmAudioBannerRequestData getExpectedCpmAudioBannerRequestData(CpmAudioBannerInfo bannerInfo,
                                                                           Creative creative) {
        CpmAudioBannerRequestData data = new CpmAudioBannerRequestData();
        data.setDomain(bannerInfo.getBanner().getDomain());
        data.setHref(bannerInfo.getBanner().getHref());
        data.setCreativeId(bannerInfo.getBanner().getCreativeId());
        data.setCreativePreviewUrl(creative.getPreviewUrl());
        data.setLivePreviewUrl(creative.getLivePreviewUrl());
        data.setDuration(creative.getDuration());
        data.setAspectRatio(new AspectRatio(CPM_AUDIO_DEFAULT_WIDTH, CPM_AUDIO_DEFAULT_HEIGHT));
        data.setGeo(String.valueOf(Region.RUSSIA_REGION_ID));
        data.setUserFlags(emptyList());
        return data;
    }

    private CpmAudioBannerModerationRequest makeCpmAudioModerationRequests(CpmAudioBannerInfo bannerInfo) {
        return makeCpmAudioModerationRequests(Collections.singletonList(bannerInfo)).get(0);
    }

    private List<CpmAudioBannerModerationRequest> makeCpmAudioModerationRequests(List<CpmAudioBannerInfo> bannerInfo) {
        Consumer<List<CpmAudioBannerModerationRequest>> sender = Mockito.mock(Consumer.class);
        ArgumentCaptor<List<CpmAudioBannerModerationRequest>> requestsCaptor = ArgumentCaptor.forClass(List.class);


        cpmAudioBannerSender.send(bannerInfo.get(0).getShard(), mapList(bannerInfo,
                AbstractBannerInfo::getBannerId), e -> System.currentTimeMillis(), o -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }
}
