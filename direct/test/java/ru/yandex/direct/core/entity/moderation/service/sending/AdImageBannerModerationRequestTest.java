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

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;
import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.ad_image.AdImageBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.ad_image.AdImageBannerRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.AUTO_ACCEPT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.COMMON;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.MANUAL;
import static ru.yandex.direct.core.entity.moderation.service.sending.AbstractAdImageBannerSender.INITIAL_VERSION;
import static ru.yandex.direct.core.testing.data.TestBanners.THIRD_DEFAULT_BS_BANNER_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdImageBannerModerationRequestTest {

    @Autowired
    private Steps steps;

    @Autowired
    private AdImageBannerSender adImageBannerSender;

    @Autowired
    private TestModerationRepository testModerationRepository;


    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldImageHashBanner banner;
    private BannerImageFormat bannerImageFormat;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        shard = clientInfo.getShard();

        banner = (OldImageHashBanner) steps.bannerSteps()
                .createBanner(activeImageHashBanner(campaignInfo.getCampaignId(), null)
                                .withStatusModerate(OldBannerStatusModerate.READY)
                                .withBody("TestBody")
                                .withTitle("TestTitle")
                                .withTitleExtension("TestTitleExt"),
                        clientInfo
                )
                .getBanner();

        bannerImageFormat = testModerationRepository.addBannerImageFormat(shard, banner.getImage().getImageHash(),
                new ImageSize().withHeight(100).withWidth(100));
    }

    @Test
    public void makeAdImageBannerModerationRequests_RequestDataIsCorrect() {
        List<AdImageBannerModerationRequest> requests =
                makeAdImageBannerModerationRequests(shard, singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        AdImageBannerRequestData actual = requests.get(0).getData();

        AdImageBannerRequestData expected = new AdImageBannerRequestData();

        expected.setParametrizedHref(banner.getHref());
        expected.setHref(banner.getHref());
        expected.setHost("avatars.mdst.yandex.net");
        expected.setImageHash(bannerImageFormat.getImageHash());
        expected.setMdsGroupId(bannerImageFormat.getMdsGroupId().longValue());
        expected.setNamespace("direct-picture");
        expected.setBody("TestBody");
        expected.setTitle("TestTitle");
        expected.setTitleExtension("TestTitleExt");
        expected.setGeo("225");
        expected.setUserFlags(emptyList());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeAdImageBannerModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientInfo.getClientId());
        steps.clientOptionsSteps().setClientFlags(shard, clientInfo.getClientId(), "as_soon_as_possible");

        List<AdImageBannerModerationRequest> requests = makeAdImageBannerModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        AdImageBannerRequestData actual = requests.get(0).getData();

        assertThat("Вернулись правильные данные", actual.getAsSoonAsPossible(), Matchers.is(true));
    }

    @Test
    public void makeAdImageBannerModerationRequests_WithPreModeration() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER));

        List<AdImageBannerModerationRequest> requests = makeAdImageBannerModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(MANUAL));
    }

    @Test
    public void makeAdImageBannerModerationRequests_WithAutoAccept() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER));

        List<AdImageBannerModerationRequest> requests = makeAdImageBannerModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(AUTO_ACCEPT));
    }

    @Test
    public void makeAdImageBannerModerationRequests_WithNoFlags() {

        List<AdImageBannerModerationRequest> requests = makeAdImageBannerModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(COMMON));
    }

    @Test
    public void makeAdImageBannerModerationRequests_MetaIsCorrect() {
        List<AdImageBannerModerationRequest> requests =
                makeAdImageBannerModerationRequests(shard,
                        singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat(requests.get(0).getType().getValue(), equalTo("image_ad_sm"));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(banner.getCampaignId());
        expected.setAdGroupId(banner.getAdGroupId());
        expected.setBannerId(banner.getId());
        expected.setClientId(clientInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setBsBannerId(THIRD_DEFAULT_BS_BANNER_ID);
        expected.setVersionId(INITIAL_VERSION);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    private List<AdImageBannerModerationRequest> makeAdImageBannerModerationRequests(int shard, List<Long> bids) {
        Consumer<List<AdImageBannerModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<AdImageBannerModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        adImageBannerSender.send(shard, bids, e -> System.currentTimeMillis(), el -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }

}
