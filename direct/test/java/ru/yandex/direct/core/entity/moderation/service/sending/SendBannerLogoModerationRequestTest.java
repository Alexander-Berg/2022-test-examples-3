package ru.yandex.direct.core.entity.moderation.service.sending;

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
import ru.yandex.direct.core.entity.banner.model.old.StatusBannerLogoModerate;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerLogoModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerLogoRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
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
public class SendBannerLogoModerationRequestTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BannerLogoSender bannerLogoSender;

    @Autowired
    private TestModerationRepository testModerationRepository;

    private int shard;
    private ClientInfo clientInfo;
    private ClientId clientId;

    private CampaignInfo campaignInfo;
    private OldCpmBanner banner;
    private BannerImageFormat bannerImageFormat;

    @Before
    public void before() throws IOException {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultOverlayCreative(clientInfo, creativeId);

        bannerImageFormat = steps.bannerSteps().createBannerImageFormat(clientInfo);
        OldCpmBanner banner =
                activeCpmVideoBanner(campaignInfo.getCampaignId(), null, creativeId)
                        .withLogoImageHash(bannerImageFormat.getImageHash())
                        .withLogoStatusModerate(StatusBannerLogoModerate.READY);
        CpmBannerInfo cpmBannerInfo = steps.bannerSteps()
                .createActiveCpmVideoBanner(banner, clientInfo);

        this.banner = cpmBannerInfo.getBanner();
    }

    @Test
    public void makeBannerLogoModerationRequests_RequestDataIsCorrect() {
        List<BannerLogoModerationRequest> requests =
                makeBannerLogoModerationRequests(shard, singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        BannerLogoRequestData actual = requests.get(0).getData();

        BannerLogoRequestData expected = new BannerLogoRequestData();
        expected.setNamespace(bannerImageFormat.getAvatarNamespace().getValue());
        expected.setImageHash(bannerImageFormat.getImageHash());
        expected.setHost(bannerImageFormat.getAvatarHost().getHost());
        expected.setMdsGroupId(bannerImageFormat.getMdsGroupId().intValue());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeBannerLogoModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<BannerLogoModerationRequest> requests = makeBannerLogoModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        BannerLogoRequestData actual = requests.get(0).getData();
        BannerLogoRequestData expected = new BannerLogoRequestData();

        expected.setNamespace(bannerImageFormat.getAvatarNamespace().getValue());
        expected.setImageHash(bannerImageFormat.getImageHash());
        expected.setHost(bannerImageFormat.getAvatarHost().getHost());
        expected.setMdsGroupId(bannerImageFormat.getMdsGroupId().intValue());

        expected.setAsSoonAsPossible(true);

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeBannerLogoModerationRequests_ClientWithNoAsapFlag_NoAsapFlagInRequest() {
        List<BannerLogoModerationRequest> requests = makeBannerLogoModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        BannerLogoRequestData actual = requests.get(0).getData();
        assertThat("Данные не содержат флага asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeBannerLogoModerationRequests_MetaIsCorrect() {
        List<BannerLogoModerationRequest> requests =
                makeBannerLogoModerationRequests(shard,
                        singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        BannerAssetModerationMeta actual = requests.get(0).getMeta();

        BannerAssetModerationMeta expected = new BannerAssetModerationMeta();
        expected.setCampaignId(banner.getCampaignId());
        expected.setAdGroupId(banner.getAdGroupId());
        expected.setBannerId(banner.getId());
        expected.setClientId(clientInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setVersionId(40000);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }


    private List<BannerLogoModerationRequest> makeBannerLogoModerationRequests(int shard, List<Long> bids) {
        Consumer<List<BannerLogoModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<BannerLogoModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);


        bannerLogoSender.send(shard, bids, e -> System.currentTimeMillis(), el -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }

    @Test
    public void makeBannerLogoModerationRequests_WithPreModeration() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER_LOGO));

        List<BannerLogoModerationRequest> requests = makeBannerLogoModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(MANUAL));
    }

    @Test
    public void makeBannerLogoModerationRequests_WithAutoAccept() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER_LOGO));

        List<BannerLogoModerationRequest> requests = makeBannerLogoModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(AUTO_ACCEPT));
    }

    @Test
    public void makeBannerLogoModerationRequests_WithNoFlags() {
        List<BannerLogoModerationRequest> requests = makeBannerLogoModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(COMMON));
    }

}
