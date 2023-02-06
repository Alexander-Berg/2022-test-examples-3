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
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerRepository;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.cpm.in_banner.InBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.cpm.in_banner.InBannerRequestData;
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
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendInBannerModerationRequestTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private CpmInBannerSender inBannerSender;

    private int shard;
    private CpmBannerInfo cpmBannerInfo;
    private ClientInfo clientInfo;
    private CreativeInfo creativeInfo;
    private ClientId clientId;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClientAndUser();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo);
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);
        long adGroupId = adGroupInfo.getAdGroupId();
        long campaignId = campaignInfo.getCampaignId();
        creativeInfo = steps.creativeSteps()
                .addDefaultInBannerCreative(clientInfo, steps.creativeSteps().getNextCreativeId());

        cpmBannerInfo = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(campaignId, adGroupId, creativeInfo.getCreativeId()),
                clientInfo);

        shard = cpmBannerInfo.getShard();
        clientId = cpmBannerInfo.getClientId();
        bannerRepository.moderation.updateStatusModerate(shard, singletonList(cpmBannerInfo.getBannerId()),
                BannerStatusModerate.READY);
    }

    @Test
    public void makeInBannerModerationRequests_RequestDataIsCorrect() {
        List<InBannerModerationRequest> requests =
                makeInBannerModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        InBannerRequestData actual = requests.get(0).getData();

        InBannerRequestData expected = new InBannerRequestData();
        expected.setHref(cpmBannerInfo.getBanner().getHref());
        expected.setCreativeId(creativeInfo.getCreativeId());
        expected.setPreviewUrl(creativeInfo.getCreative().getPreviewUrl());
        expected.setDomain(cpmBannerInfo.getBanner().getDomain());
        expected.setGeo("225");
        expected.setUserFlags(emptyList());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeInBannerModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<InBannerModerationRequest> requests =
                makeInBannerModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        InBannerRequestData actual = requests.get(0).getData();

        InBannerRequestData expected = new InBannerRequestData();
        expected.setHref(cpmBannerInfo.getBanner().getHref());
        expected.setCreativeId(creativeInfo.getCreativeId());
        expected.setPreviewUrl(creativeInfo.getCreative().getPreviewUrl());
        expected.setDomain(cpmBannerInfo.getBanner().getDomain());
        expected.setGeo("225");
        expected.setAsSoonAsPossible(true);
        expected.setUserFlags(emptyList());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeInBannerModerationRequests_ClientWithNoAsapFlag_NoAsapInRequest() {
        List<InBannerModerationRequest> requests =
                makeInBannerModerationRequests(shard, singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        InBannerRequestData actual = requests.get(0).getData();

        assertThat("В запросе нет параметра asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeInBannerBannerModerationRequests_MetaIsCorrect() {
        List<InBannerModerationRequest> requests =
                makeInBannerModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(cpmBannerInfo.getCampaignId());
        expected.setAdGroupId(cpmBannerInfo.getAdGroupId());
        expected.setBannerId(cpmBannerInfo.getBannerId());
        expected.setClientId(cpmBannerInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setVersionId(1);
        expected.setBsBannerId(23458732L);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeInBannerBannerModerationRequests_ManualOnly() {

        steps.bannerSteps().addBannerReModerationFlag(shard, cpmBannerInfo.getBannerId(),
                List.of(RemoderationType.BANNER));

        List<InBannerModerationRequest> requests =
                makeInBannerModerationRequests(shard,
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
        expected.setVersionId(1);
        expected.setBsBannerId(23458732L);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    private List<InBannerModerationRequest> makeInBannerModerationRequests(int shard,
                                                                           List<Long> bids) {
        Consumer<List<InBannerModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<InBannerModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        inBannerSender.send(shard, bids, e -> System.currentTimeMillis(), e -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }

}
