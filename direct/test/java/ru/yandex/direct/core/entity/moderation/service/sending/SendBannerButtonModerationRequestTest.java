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

import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerButtonStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerButtonModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerButtonRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
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
public class SendBannerButtonModerationRequestTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BannerButtonSender bannerButtonSender;

    @Autowired
    private TestModerationRepository testModerationRepository;

    private int shard;
    private ClientInfo clientInfo;
    private ClientId clientId;

    private CampaignInfo campaignInfo;
    private OldCpmBanner banner;

    @Before
    public void before() throws IOException {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultOverlayCreative(clientInfo, creativeId);

        OldCpmBanner banner =
                activeCpmVideoBanner(campaignInfo.getCampaignId(), null, creativeId)
                        .withButtonAction(ButtonAction.BUY)
                        .withButtonCaption("caption")
                        .withButtonHref("https://yandex.ru")
                        .withButtonStatusModerate(OldBannerButtonStatusModerate.READY);
        CpmBannerInfo cpmBannerInfo = steps.bannerSteps()
                .createActiveCpmVideoBanner(banner, clientInfo);

        this.banner = cpmBannerInfo.getBanner();
    }

    @Test
    public void makeBannerButtonModerationRequests_RequestDataIsCorrect() {
        List<BannerButtonModerationRequest> requests =
                makeBannerButtonModerationRequests(shard, singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        BannerButtonRequestData actual = requests.get(0).getData();

        BannerButtonRequestData expected = new BannerButtonRequestData();
        expected.setButtonText(banner.getButtonCaption());
        expected.setHref(banner.getButtonHref());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeBannerButtonModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<BannerButtonModerationRequest> requests = makeBannerButtonModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        BannerButtonRequestData actual = requests.get(0).getData();
        BannerButtonRequestData expected = new BannerButtonRequestData();

        expected.setButtonText(banner.getButtonCaption());
        expected.setHref(banner.getButtonHref());

        expected.setAsSoonAsPossible(true);

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeBannerButtonModerationRequests_ClientWithNoAsapFlag_NoAsapFlagInRequest() {
        List<BannerButtonModerationRequest> requests = makeBannerButtonModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        BannerButtonRequestData actual = requests.get(0).getData();
        assertThat("Данные не содержат флага asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeBannerButtonModerationRequests_MetaIsCorrect() {
        List<BannerButtonModerationRequest> requests =
                makeBannerButtonModerationRequests(shard,
                        singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        BannerAssetModerationMeta actual = requests.get(0).getMeta();

        BannerAssetModerationMeta expected = new BannerAssetModerationMeta();
        expected.setCampaignId(banner.getCampaignId());
        expected.setAdGroupId(banner.getAdGroupId());
        expected.setBannerId(banner.getId());
        expected.setClientId(clientInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setVersionId(50000);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }


    private List<BannerButtonModerationRequest> makeBannerButtonModerationRequests(int shard, List<Long> bids) {
        Consumer<List<BannerButtonModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<BannerButtonModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);


        bannerButtonSender.send(shard, bids, e -> System.currentTimeMillis(), el -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }

    @Test
    public void makeBannerButtonModerationRequests_WithPreModeration() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER_LOGO));

        List<BannerButtonModerationRequest> requests = makeBannerButtonModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(MANUAL));
    }

    @Test
    public void makeBannerButtonModerationRequests_WithAutoAccept() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER_LOGO));

        List<BannerButtonModerationRequest> requests = makeBannerButtonModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(AUTO_ACCEPT));
    }

    @Test
    public void makeBannerButtonModerationRequests_WithNoFlags() {
        List<BannerButtonModerationRequest> requests = makeBannerButtonModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(COMMON));
    }

}
