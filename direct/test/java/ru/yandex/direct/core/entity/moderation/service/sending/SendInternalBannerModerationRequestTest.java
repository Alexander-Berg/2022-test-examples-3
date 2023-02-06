package ru.yandex.direct.core.entity.moderation.service.sending;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.InternalModerationInfo;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.client.model.ClientFlags;
import ru.yandex.direct.core.entity.moderation.model.AspectRatio;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.InternalBannerRequestData;
import ru.yandex.direct.core.entity.moderation.model.internalad.InternalBannerModerationRequest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewInternalBannerInfo;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestNewInternalBanners.fullInternalBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class SendInternalBannerModerationRequestTest {

    private static final long DEFAULT_VERSION = 1L;

    @Autowired
    private Steps steps;

    @Autowired
    private InternalBannerSender internalBannerSender;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private int shard;
    private ClientInfo clientInfo;
    private AdGroupInfo adGroupInfo;
    private InternalBanner banner;

    @Before
    public void before() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        shard = clientInfo.getShard();

        CampaignInfo campaignInfo =
                steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        NewInternalBannerInfo bannerInfo =
                steps.internalBannerSteps().createModeratedInternalBanner(adGroupInfo, BannerStatusModerate.READY);
        banner = bannerInfo.getBanner();
    }


    @Test
    public void makeInternalBannerModerationRequests_RequestDataIsCorrect() {
        var requests = makeInternalBannerModerationRequests(shard, singletonList(banner.getId()));
        assumeThat(requests, hasSize(1));

        InternalBannerRequestData actual = requests.get(0).getData();

        InternalBannerRequestData expected = getExpectedBannerData();
        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeInternalBannerModerationRequests_RequestDataIsCorrectForBannerWithImageAndLink() {
        String title = "TITLE " + RandomStringUtils.randomAlphanumeric(11);
        String link = "https://direct.yandex.ru/dna/grid/campaigns";
        BannerImageFormat bannerImageFormat = steps.bannerSteps().createImageAdImageFormat(clientInfo);
        banner = createModeratedBannerWithImageAndUrl(title, link, bannerImageFormat);

        var requests = makeInternalBannerModerationRequests(shard, singletonList(banner.getId()));
        assumeThat(requests, hasSize(1));

        InternalBannerRequestData actual = requests.get(0).getData();

        String imageUrl = generateImageUrl(bannerImageFormat.getAvatarHost(), bannerImageFormat.getAvatarNamespace(),
                bannerImageFormat.getMdsGroupId(), bannerImageFormat.getImageHash(), "orig");
        InternalBannerRequestData expected = getExpectedBannerData()
                .withImageAlt(null)
                .withImage(imageUrl)
                .withImageAspects(new AspectRatio(bannerImageFormat.getWidth(), bannerImageFormat.getHeight()))
                .withLink(link)
                .withTitle1(title);

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void moderateOneInternalBanner_ClientWithAsapFlag_CheckRequestData() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientInfo.getClientId());
        steps.clientOptionsSteps()
                .setClientFlags(shard, clientInfo.getClientId(), ClientFlags.AS_SOON_AS_POSSIBLE.getTypedValue());

        var requests = makeInternalBannerModerationRequests(shard, singletonList(banner.getId()));
        assumeThat(requests, hasSize(1));

        InternalBannerRequestData actual = requests.get(0).getData();

        InternalBannerRequestData expected = getExpectedBannerData();
        expected.setAsSoonAsPossible(true);

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeInternalBannerModerationRequests_MetaIsCorrect() {
        var requests = makeInternalBannerModerationRequests(shard, singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat(requests.get(0).getType().getValue(), equalTo("internal_banner"));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(banner.getCampaignId());
        expected.setAdGroupId(banner.getAdGroupId());
        expected.setBannerId(banner.getId());
        expected.setClientId(clientInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setBsBannerId(banner.getBsBannerId());
        expected.setVersionId(DEFAULT_VERSION);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void trySendNotModeratedBanner_ExpectIllegalStateException() {
        var bannerInfo = steps.internalBannerSteps().createInternalBanner(adGroupInfo, BannerStatusModerate.READY,
                TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1,
                TemplateResourceRepositoryMockUtils.TEMPLATE_1_RESOURCE_1_REQUIRED);
        InternalBanner notModeratedBanner = bannerInfo.getBanner();

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(format("Unexpected templateId %d for moderation internal banner",
                notModeratedBanner.getTemplateId()));

        internalBannerSender.send(shard, List.of(notModeratedBanner.getId()), e -> System.currentTimeMillis(),
                el -> null, sender -> {});
    }

    @Test
    public void trySenModeratedBannerWithoutModerationInfo_ExpectIllegalStateException() {
        var bannerInfo = steps.internalBannerSteps().createInternalBanner(adGroupInfo, BannerStatusModerate.READY,
                TemplatePlaceRepositoryMockUtils.PLACE_3_TEMPLATE_1,
                TemplateResourceRepositoryMockUtils.TEMPLATE_7_RESOURCE);
        InternalBanner moderatedBannerWithoutModerationInfo = bannerInfo.getBanner();

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(format("moderationInfo must not be null for banner %d",
                moderatedBannerWithoutModerationInfo.getId()));

        internalBannerSender.send(shard, List.of(moderatedBannerWithoutModerationInfo.getId()),
                e -> System.currentTimeMillis(), el -> null, sender -> {});
    }

    private InternalBannerRequestData getExpectedBannerData() {
        InternalBannerRequestData data = new InternalBannerRequestData()
                .withGeo("225")
                .withObjectName(banner.getDescription())
                .withTicketUrl(banner.getModerationInfo().getTicketUrl())
                .withAdditionalInformation(banner.getModerationInfo().getCustomComment())
                .withIsSecretAd(banner.getModerationInfo().getIsSecretAd())
                .withTemplateId(banner.getTemplateId())
                .withImageAlt(banner.getTemplateVariables().get(0).getInternalValue());
        data.setUserFlags(emptyList());
        return data;
    }

    private List<InternalBannerModerationRequest> makeInternalBannerModerationRequests(int shard, List<Long> bids) {
        Consumer<List<InternalBannerModerationRequest>> sender = Mockito.mock(Consumer.class);
        ArgumentCaptor<List<InternalBannerModerationRequest>> requestsCaptor = ArgumentCaptor.forClass(List.class);

        internalBannerSender.send(shard, bids, e -> System.currentTimeMillis(), el -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }

    private static String generateImageUrl(BannerImageFormat.AvatarHost avatarHost,
                                           BannerImageFormat.AvatarNamespace namespace,
                                           Long mdsGroupId, String imageHash, String formatId) {
        return String.format("https://%s/get-%s/%s/%s/%s", avatarHost.getHost(), namespace.getValue(), mdsGroupId,
                imageHash, formatId);
    }

    private InternalBanner createModeratedBannerWithImageAndUrl(String title, String link,
                                                                BannerImageFormat bannerImageFormat) {
        InternalBanner internalBanner = fullInternalBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withStatusModerate(BannerStatusModerate.READY)
                .withTemplateId(TemplatePlaceRepositoryMockUtils.PLACE_3_TEMPLATE_2)
                .withTemplateVariables(List.of(
                        new TemplateVariable()
                                .withTemplateResourceId(TemplateResourceRepositoryMockUtils.TEMPLATE_9_RESOURCE_IMAGE)
                                .withInternalValue(bannerImageFormat.getImageHash()),
                        new TemplateVariable()
                                .withTemplateResourceId(TemplateResourceRepositoryMockUtils.TEMPLATE_9_RESOURCE_LINK)
                                .withInternalValue(link),
                        new TemplateVariable()
                                .withTemplateResourceId(TemplateResourceRepositoryMockUtils.TEMPLATE_9_RESOURCE_TITLE1)
                                .withInternalValue(title)
                ))
                .withModerationInfo(new InternalModerationInfo()
                        .withIsSecretAd(true)
                        .withStatusShowAfterModeration(true)
                        .withTicketUrl("https://st.yandex-team.ru/LEGAL-113"));

        NewInternalBannerInfo bannerInfo =
                steps.internalBannerSteps().createInternalBanner(new NewInternalBannerInfo()
                        .withAdGroupInfo(adGroupInfo)
                        .withBanner(internalBanner));
        return bannerInfo.getBanner();
    }

}
