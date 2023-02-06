package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.verdictrequest.contentpromotion.ContentPromotionAccessibilityData;
import ru.yandex.direct.core.entity.moderation.model.verdictrequest.contentpromotion.ContentPromotionModerationVerdict;
import ru.yandex.direct.core.entity.moderation.model.verdictrequest.contentpromotion.ContentPromotionModerationVerdictRequest;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationService;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.moderation.model.verdictrequest.contentpromotion.ContentPromotionModerationVerdict.getContentPromotionModerationVerdict;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerVideoType;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendContentPromotionVideoModerationVerdictRequestTest {

    private static final String VIDEO_METADATA_FILENAME = "content_promotion_video_metadata_example.json";
    private static final Long BANNER_VERSION = 4L;

    @Autowired
    private Steps steps;

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private OldBannerRepository bannerRepository;

    private int shard;
    private ContentPromotionBannerInfo contentPromotionVideoBannerInfo;
    private UserInfo userInfo;
    private Long contentPromotionVideoId;

    @Before
    public void before() throws IOException {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(COLLECTION);

        String metadataJson = IOUtils.toString(getClass().getResourceAsStream(VIDEO_METADATA_FILENAME), UTF_8);
        contentPromotionVideoId =
                contentPromotionRepository.insertContentPromotion(adGroupInfo.getClientId(),
                        new ContentPromotionContent()
                                .withUrl("https://www.youtube.com/watch?v=TfSJ5MPqrEc")
                                .withMetadata(metadataJson)
                                .withType(ContentPromotionContentType.VIDEO)
                                .withIsInaccessible(false)
                                .withExternalId("externalId"));

        contentPromotionVideoBannerInfo = steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerVideoType(null, null)
                        .withContentPromotionId(contentPromotionVideoId)
                        .withStatusModerate(OldBannerStatusModerate.YES)
                        .withStatusPostModerate(OldBannerStatusPostModerate.YES), ContentPromotionAdgroupType.VIDEO);

        steps.bannerModerationVersionSteps().addBannerModerationVersion(contentPromotionVideoBannerInfo.getShard(),
                contentPromotionVideoBannerInfo.getBannerId(),
                BANNER_VERSION);

        shard = contentPromotionVideoBannerInfo.getShard();
        userInfo = contentPromotionVideoBannerInfo.getClientInfo().getChiefUserInfo();
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_RequestDataIsCorrect() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionVideoId, true)));

        assumeThat(requests, hasSize(1));

        ContentPromotionModerationVerdictRequest actual = requests.get(0);

        ContentPromotionModerationVerdictRequest expected = new ContentPromotionModerationVerdictRequest();
        expected.setUnixtime(actual.getUnixtime());
        expected.setCircuit("content_accessibility_checker");
        expected.setService(ModerationServiceNames.DIRECT_SERVICE);
        expected.setType(ModerationObjectType.getEnumByValue("content_promotion_video"));

        assertThat("Вернулись правильные данные", actual,
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_MetaIsCorrect() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionVideoId, true)));

        assertThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(contentPromotionVideoBannerInfo.getCampaignId());
        expected.setAdGroupId(contentPromotionVideoBannerInfo.getAdGroupId());
        expected.setBannerId(contentPromotionVideoBannerInfo.getBannerId());
        expected.setClientId(contentPromotionVideoBannerInfo.getClientId().asLong());
        expected.setUid(userInfo.getUid());
        expected.setVersionId(BANNER_VERSION);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_InaccessibleVideo_ModerationVerdictIsCorrect() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionVideoId, true)));

        assumeThat(requests, hasSize(1));

        ContentPromotionModerationVerdict actual = requests.get(0).getVerdict();
        ContentPromotionModerationVerdict expected = getContentPromotionModerationVerdict(true);

        assertThat("Вернулся правильный вердикт", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_InaccessibleVideo_ModerationVersionNotChanged() {
        Long bannerId = contentPromotionVideoBannerInfo.getBannerId();

        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionVideoId, true)));

        assertThat(requests, hasSize(1));

        Map<Long, Long> moderationVersionByBannerId =
                steps.bannerModerationVersionSteps().getBannerModerationVersionByBannerId(shard,
                        singletonList(contentPromotionVideoBannerInfo.getBannerId()));

        assertThat(moderationVersionByBannerId.get(bannerId), is(BANNER_VERSION));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_InaccessibleVideo_ModerationStatusesNotChanged() {
        Long bannerId = contentPromotionVideoBannerInfo.getBannerId();

        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionVideoId, true)));

        assertThat(requests, hasSize(1));

        List<OldBanner> banners = bannerRepository.getBanners(shard, singletonList(bannerId));
        assertThat(banners, hasSize(1));
        OldBanner banner = banners.get(0);

        assertThat(banner.getStatusModerate(), is(OldBannerStatusModerate.YES));
        assertThat(banner.getStatusPostModerate(), is(OldBannerStatusPostModerate.YES));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_AccessibleVideo_ModerationVerdictIsCorrect() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionVideoId, false)));

        assumeThat(requests, hasSize(1));

        ContentPromotionModerationVerdict actual = requests.get(0).getVerdict();
        ContentPromotionModerationVerdict expected = getContentPromotionModerationVerdict(false);

        assertThat("Вернулся правильный вердикт", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_MultipleAccessibilityData_InaccessibleVerdictSent() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        asList(new ContentPromotionAccessibilityData(contentPromotionVideoId, false),
                                new ContentPromotionAccessibilityData(contentPromotionVideoId, true)));

        assumeThat(requests, hasSize(1));

        ContentPromotionModerationVerdict actual = requests.get(0).getVerdict();
        ContentPromotionModerationVerdict expected = getContentPromotionModerationVerdict(true);

        assertThat("Вернулся правильный вердикт", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_MultipleAccessibilityData_AccessibleVerdictSent() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        asList(new ContentPromotionAccessibilityData(contentPromotionVideoId, true),
                                new ContentPromotionAccessibilityData(contentPromotionVideoId, false)));

        assumeThat(requests, hasSize(1));

        ContentPromotionModerationVerdict actual = requests.get(0).getVerdict();
        ContentPromotionModerationVerdict expected = getContentPromotionModerationVerdict(false);

        assertThat("Вернулся правильный вердикт", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_NoBannerLinkedToVideo_NoRequestsSent() {
        Long anotherContentPromotionVideoId =
                contentPromotionRepository.insertContentPromotion(contentPromotionVideoBannerInfo.getClientId(),
                        new ContentPromotionContent()
                                .withUrl("https://www.youtube.com/watch?v=TfSJ5MPqrEc")
                                .withType(ContentPromotionContentType.VIDEO)
                                .withIsInaccessible(false)
                                .withExternalId("anotherExternalId"));

        Consumer<List<ContentPromotionModerationVerdictRequest>> sender = Mockito.mock(Consumer.class);
        ArgumentCaptor<List<ContentPromotionModerationVerdictRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        moderationService.makeContentPromotionAccessibilityVerdictsRequests(shard,
                singletonList(new ContentPromotionAccessibilityData(anotherContentPromotionVideoId, false)), sender);

        Mockito.verify(sender, Mockito.never()).accept(requestsCaptor.capture());
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_TwoBannersLinkedToVideo_BothBeenInModeration_TwoRequestsSent() {
        ContentPromotionBannerInfo anotherContentPromotionBannerInfo =
                steps.bannerSteps().createActiveContentPromotionBanner(activeContentPromotionBannerVideoType(null, null)
                        .withContentPromotionId(contentPromotionVideoId)
                        .withStatusModerate(OldBannerStatusModerate.YES)
                        .withStatusPostModerate(OldBannerStatusPostModerate.YES), ContentPromotionAdgroupType.VIDEO);

        steps.bannerModerationVersionSteps().addBannerModerationVersion(anotherContentPromotionBannerInfo.getShard(),
                anotherContentPromotionBannerInfo.getBannerId(),
                BANNER_VERSION);

        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionVideoId, true)));

        assertThat(requests, hasSize(2));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_TwoBannersLinkedToVideo_OneBeenInModeration_OneRequestSent() {
        steps.bannerSteps().createActiveContentPromotionBanner(activeContentPromotionBannerVideoType(null, null)
                .withContentPromotionId(contentPromotionVideoId)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withStatusPostModerate(OldBannerStatusPostModerate.YES), ContentPromotionAdgroupType.VIDEO);

        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionVideoId, true)));

        assertThat(requests, hasSize(1));
    }

    private List<ContentPromotionModerationVerdictRequest> makeContentPromotionAccessibilityVerdictsRequests(int shard,
                                                                                                             List<ContentPromotionAccessibilityData> requests) {
        Consumer<List<ContentPromotionModerationVerdictRequest>> sender =
                Mockito.mock(Consumer.class);
        ArgumentCaptor<List<ContentPromotionModerationVerdictRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        moderationService.makeContentPromotionAccessibilityVerdictsRequests(shard, requests, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }
}
