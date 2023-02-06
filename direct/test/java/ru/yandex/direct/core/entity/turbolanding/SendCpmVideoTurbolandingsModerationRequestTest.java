package ru.yandex.direct.core.entity.turbolanding;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.moderation.model.turbolandings.TurbolandingModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.turbolandings.TurbolandingModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.turbolandings.TurbolandingRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.entity.moderation.service.sending.TurbolandingSender;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.model.BaseModerationData.ASAP_PROPERTY_NAME;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.AUTO_ACCEPT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.COMMON;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.MANUAL;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendCpmVideoTurbolandingsModerationRequestTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TurbolandingSender turbolandingSender;

    @Autowired
    private TestModerationRepository testModerationRepository;

    private int shard;
    private CpmBannerInfo cpmBannerInfo;
    private ClientInfo clientInfo;
    private CreativeInfo creativeInfo;
    private TurboLanding turboLanding;
    private ClientId clientId;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClient();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup(clientInfo);
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);
        long adGroupId = adGroupInfo.getAdGroupId();
        long campaignId = campaignInfo.getCampaignId();

        creativeInfo = steps.creativeSteps()
                .addDefaultCpmVideoAdditionCreative(clientInfo, steps.creativeSteps().getNextCreativeId());


        turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientInfo.getClientId());
        OldCpmBanner cpmBanner = activeCpmVideoBanner(campaignId, adGroupId, creativeInfo.getCreativeId());

        cpmBanner.withTurboLandingId(turboLanding.getId());
        cpmBanner.withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.READY);

        cpmBannerInfo = steps.bannerSteps().createActiveCpmVideoBanner(
                cpmBanner,
                clientInfo);

        steps.turboLandingSteps().addBannerToBannerTurbolandingsTableOrUpdate(campaignId, List.of(cpmBanner));

        shard = cpmBannerInfo.getShard();
        clientId = clientInfo.getClientId();
    }

    @Test
    public void makeCpmVideoTurbolandingModerationRequests_RequestDataIsCorrect() {
        List<TurbolandingModerationRequest> requests =
                makeCpmVideoTurbolandingModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        TurbolandingRequestData actual = requests.get(0).getData();

        TurbolandingRequestData expected = new TurbolandingRequestData();
        expected.setHref("https://yandex.ru/turbo?text=vkbn&test=" + turboLanding.getId());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeCpmVideoTurbolandingModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");
        List<TurbolandingModerationRequest> requests =
                makeCpmVideoTurbolandingModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));
        assumeThat(requests, hasSize(1));
        TurbolandingRequestData actual = requests.get(0).getData();
        TurbolandingRequestData expected = new TurbolandingRequestData();
        expected.setHref("https://yandex.ru/turbo?text=vkbn&test=" + turboLanding.getId());
        expected.setAsSoonAsPossible(true);
        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeCpmVideoTurbolandingModerationRequests_ClientWithNoAsapFlag_NoAsapFlagInRequest() {
        List<TurbolandingModerationRequest> requests =
                makeCpmVideoTurbolandingModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));
        assumeThat(requests, hasSize(1));
        TurbolandingRequestData actual = requests.get(0).getData();
        assertThat("Данные не содержат флага asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeCpmVideoTurbolandingModerationRequests_MetaIsCorrect() {
        List<TurbolandingModerationRequest> requests =
                makeCpmVideoTurbolandingModerationRequests(shard,
                        singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        TurbolandingModerationMeta actual = requests.get(0).getMeta();

        TurbolandingModerationMeta expected = new TurbolandingModerationMeta();
        expected.setCampaignId(cpmBannerInfo.getCampaignId());
        expected.setAdGroupId(cpmBannerInfo.getAdGroupId());
        expected.setBannerId(cpmBannerInfo.getBannerId());
        expected.setClientId(cpmBannerInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setVersionId(1600);
        expected.setTlId(turboLanding.getId());

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    private List<TurbolandingModerationRequest> makeCpmVideoTurbolandingModerationRequests(int shard,
                                                                                           List<Long> bids) {
        Consumer<List<TurbolandingModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<TurbolandingModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);


        turbolandingSender.send(shard, bids, e -> System.currentTimeMillis(), el -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }

    @Test
    public void makeTurbolinkModerationRequests_WithPreModeration() {
        testModerationRepository.createReModerationRecord(shard, cpmBannerInfo.getBannerId(),
                Set.of(RemoderationType.DISPLAY_HREFS));

        List<TurbolandingModerationRequest> requests = makeCpmVideoTurbolandingModerationRequests(shard,
                singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(MANUAL));
    }

    @Test
    public void makeTurbolinkModerationRequests_WithAutoAccept() {
        testModerationRepository.createAutoAcceptRecord(shard, cpmBannerInfo.getBannerId(),
                Set.of(AutoAcceptanceType.TURBOLANDING));

        List<TurbolandingModerationRequest> requests = makeCpmVideoTurbolandingModerationRequests(shard,
                singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(AUTO_ACCEPT));
    }

    @Test
    public void makeTurbolinkModerationRequests_WithNoFlags() {

        List<TurbolandingModerationRequest> requests = makeCpmVideoTurbolandingModerationRequests(shard,
                singletonList(cpmBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(COMMON));
    }

}
