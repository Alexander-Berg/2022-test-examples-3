package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.moderation.model.AspectRatio;
import ru.yandex.direct.core.entity.moderation.model.BannerLink;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow;
import ru.yandex.direct.core.entity.moderation.model.contentpromotion.ContentPromotionBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.contentpromotion.ContentPromotionVideoBannerRequestData;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.moderation.model.BaseModerationData.ASAP_PROPERTY_NAME;
import static ru.yandex.direct.core.testing.data.TestBanners.YET_ANOTHER_DEFAULT_BS_BANNER_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerVideoType;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendContentPromotionVideoBannerModerationRequestTest {

    private static final String VIDEO_METADATA_FILENAME = "content_promotion_video_metadata_example.json";

    @Autowired
    private Steps steps;

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    @Autowired
    private ContentPromotionBannerSender contentPromotionBannerSender;

    private int shard;
    private ContentPromotionBannerInfo contentPromotionBannerInfo;
    private ContentPromotionBannerInfo anotherContentPromotionBannerInfo;
    private ContentPromotionBannerInfo anotherContentPromotionBannerInfo2;
    private Long contentPromotionId;
    private UserInfo userInfo;
    private ClientId clientId;

    @Before
    public void before() throws IOException {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        userInfo = clientInfo.getChiefUserInfo();
        clientId = clientInfo.getClientId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);

        String videoMetadataJson = IOUtils.toString(getClass().getResourceAsStream(VIDEO_METADATA_FILENAME), UTF_8);
        contentPromotionId = contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withExternalId("external_id")
                        .withIsInaccessible(false)
                        .withType(ContentPromotionContentType.VIDEO)
                        .withUrl("https://www.youtube.com/watch?v=TfSJ5MPqrEc")
                        .withPreviewUrl("//avatars.mds.yandex.net/get-vthumb/924554/babef136032ed7566e85a81aff88cece")
                        .withMetadata(videoMetadataJson));

        Long anotherContentPromotionId = contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withExternalId("another_external_id")
                        .withIsInaccessible(false)
                        .withType(ContentPromotionContentType.VIDEO)
                        .withUrl("https://www.youtube.com/watch?v=TfSJ5MPqrEc")
                        .withPreviewUrl(
                                "https://avatars.mds.yandex.net/get-vthumb/924554/babef136032ed7566e85a81aff88cece")
                        .withMetadata(videoMetadataJson));

        Long anotherContentPromotionId2 = contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withExternalId("another_external_id2")
                        .withIsInaccessible(false)
                        .withType(ContentPromotionContentType.VIDEO)
                        .withUrl("https://www.youtube.com/watch?v=TfSJ5MPqrEc")
                        .withPreviewUrl(
                                "http://avatars.mds.yandex.net/get-vthumb/924554/babef136032ed7566e85a81aff88cece")
                        .withMetadata(videoMetadataJson));

        contentPromotionBannerInfo = steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerVideoType(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withStatusPostModerate(OldBannerStatusPostModerate.NO)
                        .withContentPromotionId(contentPromotionId), adGroupInfo);

        anotherContentPromotionBannerInfo = steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerVideoType(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withStatusPostModerate(OldBannerStatusPostModerate.NO)
                        .withContentPromotionId(anotherContentPromotionId), adGroupInfo);

        anotherContentPromotionBannerInfo2 = steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerVideoType(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withStatusPostModerate(OldBannerStatusPostModerate.NO)
                        .withContentPromotionId(anotherContentPromotionId2), adGroupInfo);

        shard = contentPromotionBannerInfo.getShard();
    }

    @Test
    public void makeContentPromotionModerationRequests_VideoType_RequestInfoIsCorrect() {
        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard, singletonList(contentPromotionBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        ContentPromotionBannerModerationRequest actual = requests.get(0);
        ContentPromotionBannerModerationRequest expected = new ContentPromotionBannerModerationRequest();
        expected.setWorkflow(ModerationWorkflow.COMMON);
        expected.setService(ModerationServiceNames.DIRECT_SERVICE);
        expected.setType(ModerationObjectType.getEnumByValue("content_promotion_video"));
        expected.setUnixtime(actual.getUnixtime());

        assertThat(actual, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void makeContentPromotionModerationRequests_VideoType_DataIsCorrect() {
        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard, singletonList(contentPromotionBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        ContentPromotionVideoBannerRequestData actual =
                (ContentPromotionVideoBannerRequestData) requests.get(0).getData();

        ContentPromotionVideoBannerRequestData expected = new ContentPromotionVideoBannerRequestData();
        expected.setHost("youtube.com");
        expected.setCreativeId(contentPromotionId);
        expected.setDuration(1280L);
        expected.setCreativePreviewUrl("https://avatars.mds.yandex" +
                ".net/get-vthumb/924554/babef136032ed7566e85a81aff88cece");
        expected.setTitle(contentPromotionBannerInfo.getBanner().getTitle());
        expected.setBody(contentPromotionBannerInfo.getBanner().getBody());
        expected.setAspectRatio(new AspectRatio(1280, 720));
        expected.setVideoHostingUrl("www.youtube.com/watch?v=TfSJ5MPqrEc");
        expected.setLogin(userInfo.getUser().getLogin());
        expected.setDomain("www.yandex.ru");
        expected.setLinks(singletonList(new BannerLink().setHref("https://www.yandex.ru/")));
        expected.setUserFlags(emptyList());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionModerationRequests_VideoType_AnotherPreviewUrlFormat_PreviewUrlIsCorrect() {
        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard,
                        singletonList(anotherContentPromotionBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        ContentPromotionVideoBannerRequestData actual =
                (ContentPromotionVideoBannerRequestData) requests.get(0).getData();

        assertThat("Вернулось правильное preview url", actual.getCreativePreviewUrl(),
                is("https://avatars.mds.yandex.net/get-vthumb/924554/babef136032ed7566e85a81aff88cece"));
    }

    @Test
    public void makeContentPromotionModerationRequests_VideoType_AnotherPreviewUrlFormat2_PreviewUrlIsCorrect() {
        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard,
                        singletonList(anotherContentPromotionBannerInfo2.getBannerId()));

        assumeThat(requests, hasSize(1));

        ContentPromotionVideoBannerRequestData actual =
                (ContentPromotionVideoBannerRequestData) requests.get(0).getData();

        assertThat("Вернулось правильное preview url", actual.getCreativePreviewUrl(),
                is("http://avatars.mds.yandex.net/get-vthumb/924554/babef136032ed7566e85a81aff88cece"));
    }

    @Test
    public void makeContentPromotionModerationRequests_VideoType_MetaIsCorrect() {
        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard, singletonList(contentPromotionBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(contentPromotionBannerInfo.getCampaignId());
        expected.setAdGroupId(contentPromotionBannerInfo.getAdGroupId());
        expected.setBannerId(contentPromotionBannerInfo.getBannerId());
        expected.setClientId(contentPromotionBannerInfo.getClientId().asLong());
        expected.setUid(userInfo.getUid());
        expected.setBsBannerId(YET_ANOTHER_DEFAULT_BS_BANNER_ID);
        expected.setVersionId(1);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionModerationRequests_VideoType_ClientWithAsapFlag_DataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard, singletonList(contentPromotionBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        ContentPromotionVideoBannerRequestData actual =
                (ContentPromotionVideoBannerRequestData) requests.get(0).getData();

        ContentPromotionVideoBannerRequestData expected = new ContentPromotionVideoBannerRequestData();
        expected.setHost("youtube.com");
        expected.setCreativeId(contentPromotionId);
        expected.setDuration(1280L);
        expected.setCreativePreviewUrl("https://avatars.mds.yandex" +
                ".net/get-vthumb/924554/babef136032ed7566e85a81aff88cece");
        expected.setTitle(contentPromotionBannerInfo.getBanner().getTitle());
        expected.setBody(contentPromotionBannerInfo.getBanner().getBody());
        expected.setAspectRatio(new AspectRatio(1280, 720));
        expected.setVideoHostingUrl("www.youtube.com/watch?v=TfSJ5MPqrEc");
        expected.setLogin(userInfo.getUser().getLogin());
        expected.setDomain("www.yandex.ru");
        expected.setLinks(singletonList(new BannerLink().setHref("https://www.yandex.ru/")));
        expected.setAsSoonAsPossible(true);
        expected.setUserFlags(emptyList());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionModerationRequests_VideoType_ClientWithNoAsapFlag_NoAsapInRequest() {
        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard, singletonList(contentPromotionBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        ContentPromotionVideoBannerRequestData actual =
                (ContentPromotionVideoBannerRequestData) requests.get(0).getData();

        assertThat(toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    private List<ContentPromotionBannerModerationRequest> makeContentPromotionModerationRequests(int shard,
                                                                                                 List<Long> bids) {
        Consumer<List<ContentPromotionBannerModerationRequest>> sender = Mockito.mock(Consumer.class);
        ArgumentCaptor<List<ContentPromotionBannerModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        contentPromotionBannerSender.send(shard, bids, o -> System.currentTimeMillis(), o -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }
}
