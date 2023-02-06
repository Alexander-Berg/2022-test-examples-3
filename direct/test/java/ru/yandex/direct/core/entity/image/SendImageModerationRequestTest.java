package ru.yandex.direct.core.entity.image;

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

import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.image.ImageModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.image.ImageRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.entity.moderation.service.sending.ImageSender;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.model.BaseModerationData.ASAP_PROPERTY_NAME;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.AUTO_ACCEPT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.COMMON;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.MANUAL;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendImageModerationRequestTest {

    @Autowired
    private Steps steps;

    @Autowired
    private ImageSender imageSender;

    @Autowired
    private TestModerationRepository testModerationRepository;

    private int shard;
    private ClientInfo clientInfo;
    private ClientId clientId;

    private CampaignInfo campaignInfo;
    private OldTextBanner banner;
    private BannerImageInfo<TextBannerInfo> imageInfo;

    @Before
    public void before() throws IOException {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        TextBannerInfo textBannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null), campaignInfo);

        banner = textBannerInfo.getBanner();

        imageInfo = steps.bannerSteps().createBannerImage(textBannerInfo,
                steps.bannerSteps().createBannerImageFormat(clientInfo),
                defaultBannerImage(banner.getId(), randomAlphanumeric(16)).withBsBannerId(3L)
                        .withStatusModerate(OldStatusBannerImageModerate.READY)
        );

    }

    @Test
    public void makeImageModerationRequests_RequestDataIsCorrect() {
        List<ImageModerationRequest> requests =
                makeImageModerationRequests(shard, singletonList(imageInfo.getBannerImageId()));

        assumeThat(requests, hasSize(1));

        ImageRequestData actual = requests.get(0).getData();

        ImageRequestData expected = new ImageRequestData();
        expected.setName(null);
        expected.setNamespace(imageInfo.getBannerImageFormat().getAvatarNamespace().getValue());
        expected.setImageHash(imageInfo.getBannerImage().getImageHash());
        expected.setHost(imageInfo.getBannerImageFormat().getAvatarHost().getHost());
        expected.setMdsGroupId(imageInfo.getBannerImageFormat().getMdsGroupId().intValue());
        expected.setCampaignType(CampaignType.toSource(campaignInfo.getCampaign().getType()));

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeImageModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<ImageModerationRequest> requests = makeImageModerationRequests(shard,
                singletonList(imageInfo.getBannerImageId()));

        assumeThat(requests, hasSize(1));

        ImageRequestData actual = requests.get(0).getData();
        ImageRequestData expected = new ImageRequestData();

        expected.setName(null);
        expected.setNamespace(imageInfo.getBannerImageFormat().getAvatarNamespace().getValue());
        expected.setImageHash(imageInfo.getBannerImage().getImageHash());
        expected.setHost(imageInfo.getBannerImageFormat().getAvatarHost().getHost());
        expected.setMdsGroupId(imageInfo.getBannerImageFormat().getMdsGroupId().intValue());
        expected.setCampaignType(CampaignType.toSource(campaignInfo.getCampaign().getType()));

        expected.setAsSoonAsPossible(true);

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeImageModerationRequests_ClientWithNoAsapFlag_NoAsapFlagInRequest() {
        List<ImageModerationRequest> requests = makeImageModerationRequests(shard,
                singletonList(imageInfo.getBannerImageId()));

        assumeThat(requests, hasSize(1));

        ImageRequestData actual = requests.get(0).getData();
        assertThat("Данные не содержат флага asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeImageModerationRequests_MetaIsCorrect() {
        List<ImageModerationRequest> requests =
                makeImageModerationRequests(shard,
                        singletonList(imageInfo.getBannerImageId()));

        assumeThat(requests, hasSize(1));

        BannerAssetModerationMeta actual = requests.get(0).getMeta();

        BannerAssetModerationMeta expected = new BannerAssetModerationMeta();
        expected.setCampaignId(banner.getCampaignId());
        expected.setAdGroupId(banner.getAdGroupId());
        expected.setBannerId(banner.getId());
        expected.setClientId(clientInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setVersionId(10000000L);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }


    private List<ImageModerationRequest> makeImageModerationRequests(int shard, List<Long> bids) {
        Consumer<List<ImageModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<ImageModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);


        imageSender.send(shard, bids, e -> System.currentTimeMillis(), el -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }

    @Test
    public void makeImageModerationRequests_WithPreModeration() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER_IMAGE));

        List<ImageModerationRequest> requests = makeImageModerationRequests(shard,
                singletonList(imageInfo.getBannerImageId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(MANUAL));
    }

    @Test
    public void makeImageModerationRequests_WithAutoAccept() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER_IMAGE));

        List<ImageModerationRequest> requests = makeImageModerationRequests(shard,
                singletonList(imageInfo.getBannerImageId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(AUTO_ACCEPT));
    }

    @Test
    public void makeImageModerationRequests_WithNoFlags() {
        List<ImageModerationRequest> requests = makeImageModerationRequests(shard,
                singletonList(imageInfo.getBannerImageId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(COMMON));
    }

}
