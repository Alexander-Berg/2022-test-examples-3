package ru.yandex.direct.core.entity.moderation.service.sending;

import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.moderation.model.BannerLink;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.cpm.video.CpmVideoBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.cpm.video.CpmVideoBannerRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpcVideoBannerInfo;
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
import static ru.yandex.direct.core.testing.data.TestBanners.DEFAULT_BS_BANNER_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

public abstract class AbstractSendCpcVideoBannerModerationRequestTest {
    @Autowired
    protected Steps steps;

    @Autowired
    protected PpcPropertiesSupport ppcPropertiesSupport;

    protected int shard;
    protected ClientInfo clientInfo;
    protected CreativeInfo creativeInfo;
    protected ClientId clientId;
    protected AdGroupInfo adGroupInfo;

    protected CpcVideoBannerInfo cpcBannerInfo;

    protected abstract void init();
    protected abstract BaseCpmVideoBannerSender getSender();

    protected void setExpectedRequestDataFields(CpmVideoBannerRequestData requestData) {
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        creativeInfo = steps.creativeSteps()
                .addDefaultVideoAdditionCreative(clientInfo, steps.creativeSteps().getNextCreativeId());

        init();
    }

    @Test
    public void makeCpcVideoModerationRequests_RequestDataIsCorrect() {
        List<CpmVideoBannerModerationRequest> requests =
                makeCpcVideoBannerModerationRequests(shard,
                        singletonList(cpcBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CpmVideoBannerRequestData actual = requests.get(0).getData();

        CpmVideoBannerRequestData expected = new CpmVideoBannerRequestData();
        expected.setHref(cpcBannerInfo.getBanner().getHref());
        expected.setCreativeId(creativeInfo.getCreativeId());
        expected.setPreviewUrl(creativeInfo.getCreative().getPreviewUrl());
        expected.setDomain(cpcBannerInfo.getBanner().getDomain());
        expected.setBody(cpcBannerInfo.getBanner().getBody());
        expected.setTitle(cpcBannerInfo.getBanner().getTitle());
        expected.setGeo("225");
        expected.setUserFlags(emptyList());
        expected.setModerationInfo(creativeInfo.getCreative().getModerationInfo());
        expected.setLinks(List.of(
                new BannerLink()
                        .setHref(cpcBannerInfo.getBanner().getHref())
                        .setParametrizedHref(cpcBannerInfo.getBanner().getHref())
        ));
        setExpectedRequestDataFields(expected);

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeCpcVideoModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<CpmVideoBannerModerationRequest> requests =
                makeCpcVideoBannerModerationRequests(shard,
                        singletonList(cpcBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CpmVideoBannerRequestData actual = requests.get(0).getData();

        CpmVideoBannerRequestData expected = new CpmVideoBannerRequestData();
        expected.setHref(cpcBannerInfo.getBanner().getHref());
        expected.setCreativeId(creativeInfo.getCreativeId());
        expected.setPreviewUrl(creativeInfo.getCreative().getPreviewUrl());
        expected.setDomain(cpcBannerInfo.getBanner().getDomain());
        expected.setBody(cpcBannerInfo.getBanner().getBody());
        expected.setTitle(cpcBannerInfo.getBanner().getTitle());
        expected.setGeo("225");
        expected.setAsSoonAsPossible(true);
        expected.setUserFlags(emptyList());
        expected.setModerationInfo(creativeInfo.getCreative().getModerationInfo());
        expected.setLinks(List.of(
                new BannerLink()
                        .setHref(cpcBannerInfo.getBanner().getHref())
                        .setParametrizedHref(cpcBannerInfo.getBanner().getHref())
        ));
        setExpectedRequestDataFields(expected);

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeCpcVideoModerationRequests_ClientWithNoAsapFlag_NoAsapInRequest() {
        List<CpmVideoBannerModerationRequest> requests =
                makeCpcVideoBannerModerationRequests(shard, singletonList(cpcBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        CpmVideoBannerRequestData actual = requests.get(0).getData();

        assertThat("В запросе нет параметра asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeCpcVideoBannerModerationRequests_MetaIsCorrect() {
        List<CpmVideoBannerModerationRequest> requests =
                makeCpcVideoBannerModerationRequests(shard,
                        singletonList(cpcBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(cpcBannerInfo.getCampaignId());
        expected.setAdGroupId(cpcBannerInfo.getAdGroupId());
        expected.setBannerId(cpcBannerInfo.getBannerId());
        expected.setClientId(cpcBannerInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setBsBannerId(DEFAULT_BS_BANNER_ID);
        expected.setVersionId(64);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeCpcVideoBannerModerationRequests_ManualOnly() {

        steps.bannerSteps().addBannerReModerationFlag(shard, cpcBannerInfo.getBannerId(),
                List.of(RemoderationType.BANNER));

        List<CpmVideoBannerModerationRequest> requests =
                makeCpcVideoBannerModerationRequests(shard,
                        singletonList(cpcBannerInfo.getBannerId()));

        assertFalse(steps.bannerSteps().isBannerReModerationFlagPresent(shard, cpcBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        assertEquals(requests.get(0).getWorkflow(), MANUAL);

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(cpcBannerInfo.getCampaignId());
        expected.setAdGroupId(cpcBannerInfo.getAdGroupId());
        expected.setBannerId(cpcBannerInfo.getBannerId());
        expected.setClientId(cpcBannerInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setBsBannerId(DEFAULT_BS_BANNER_ID);
        expected.setVersionId(64);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    private List<CpmVideoBannerModerationRequest> makeCpcVideoBannerModerationRequests(int shard,
                                                                                       List<Long> bids) {
        Consumer<List<CpmVideoBannerModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<CpmVideoBannerModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        getSender().send(shard, bids, e -> System.currentTimeMillis(), e -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }
}
