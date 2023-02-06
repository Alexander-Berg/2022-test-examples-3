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
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.banner.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;

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
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendContentPromotionCollectionModerationVerdictRequestTest {

    private static final String COLLECTION_METADATA_FILENAME = "collection_serp_data_example.json";
    private static final Long BANNER_VERSION = 3L;

    @Autowired
    private Steps steps;

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private TestContentPromotionBanners testContentPromotionBanners;

    private int shard;
    private ContentPromotionBannerInfo bannerInfo;
    private UserInfo userInfo;
    private ContentPromotionContent contentPromotionCollection;
    private long contentPromotionCollectionId;

    @Before
    public void before() throws IOException {
        var adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(COLLECTION);

        String metadataJson = IOUtils.toString(getClass().getResourceAsStream(COLLECTION_METADATA_FILENAME), UTF_8);
        contentPromotionCollectionId = contentPromotionRepository.insertContentPromotion(
                adGroupInfo.getClientId(),
                new ContentPromotionContent()
                        .withUrl("https://yandex.ru/collections/user/skpel/proekty-odnoetazhnykh-domov-iz-brusa/")
                        .withMetadata(metadataJson)
                        .withType(ContentPromotionContentType.COLLECTION)
                        .withIsInaccessible(false)
                        .withExternalId("externalId"));
        contentPromotionCollection = contentPromotionRepository.getContentPromotion(adGroupInfo.getClientId(),
                singletonList(contentPromotionCollectionId)).get(0);

        bannerInfo = steps.contentPromotionBannerSteps()
                .createBanner(adGroupInfo, contentPromotionCollection,
                        testContentPromotionBanners.fullContentPromoCollectionBanner(null, null));

        steps.bannerModerationVersionSteps().addBannerModerationVersion(bannerInfo.getShard(), bannerInfo.getBannerId(),
                BANNER_VERSION);

        shard = bannerInfo.getShard();

        userInfo = bannerInfo.getClientInfo().getChiefUserInfo();
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_RequestDataIsCorrect() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionCollectionId, true)));

        assumeThat(requests, hasSize(1));

        ContentPromotionModerationVerdictRequest actual = requests.get(0);

        ContentPromotionModerationVerdictRequest expected = new ContentPromotionModerationVerdictRequest();
        expected.setUnixtime(actual.getUnixtime());
        expected.setCircuit("content_accessibility_checker");
        expected.setService(ModerationServiceNames.DIRECT_SERVICE);
        expected.setType(ModerationObjectType.getEnumByValue("content_promotion_collection"));

        assertThat("Вернулись правильные данные", actual,
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_MetaIsCorrect() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionCollectionId, true)));

        assertThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(bannerInfo.getCampaignId());
        expected.setAdGroupId(bannerInfo.getAdGroupId());
        expected.setBannerId(bannerInfo.getBannerId());
        expected.setClientId(bannerInfo.getClientId().asLong());
        expected.setUid(userInfo.getUid());
        expected.setVersionId(BANNER_VERSION);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_InaccessibleCollection_ModerationVerdictIsCorrect() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionCollectionId, true)));

        assumeThat(requests, hasSize(1));

        ContentPromotionModerationVerdict actual = requests.get(0).getVerdict();
        ContentPromotionModerationVerdict expected = getContentPromotionModerationVerdict(true);

        assertThat("Вернулся правильный вердикт", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_AccessibleCollection_ModerationVerdictIsCorrect() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionCollectionId, false)));

        assumeThat(requests, hasSize(1));

        ContentPromotionModerationVerdict actual = requests.get(0).getVerdict();
        ContentPromotionModerationVerdict expected = getContentPromotionModerationVerdict(false);

        assertThat("Вернулся правильный вердикт", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_MultipleAccessibilityData_InaccessibleVerdictSent() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        asList(new ContentPromotionAccessibilityData(contentPromotionCollectionId, false),
                                new ContentPromotionAccessibilityData(contentPromotionCollectionId, true)));

        assumeThat(requests, hasSize(1));

        ContentPromotionModerationVerdict actual = requests.get(0).getVerdict();
        ContentPromotionModerationVerdict expected = getContentPromotionModerationVerdict(true);

        assertThat("Вернулся правильный вердикт", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_MultipleAccessibilityData_AccessibleVerdictSent() {
        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        asList(new ContentPromotionAccessibilityData(contentPromotionCollectionId, true),
                                new ContentPromotionAccessibilityData(contentPromotionCollectionId, false)));

        assumeThat(requests, hasSize(1));

        ContentPromotionModerationVerdict actual = requests.get(0).getVerdict();
        ContentPromotionModerationVerdict expected = getContentPromotionModerationVerdict(false);

        assertThat("Вернулся правильный вердикт", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_InaccessibleVideo_ModerationVersionNotChanged() {
        Long bannerId = bannerInfo.getBannerId();

        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionCollectionId, true)));

        assertThat(requests, hasSize(1));

        Map<Long, Long> moderationVersionByBannerId =
                steps.bannerModerationVersionSteps().getBannerModerationVersionByBannerId(shard,
                        singletonList(bannerInfo.getBannerId()));

        assertThat(moderationVersionByBannerId.get(bannerId), is(BANNER_VERSION));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_InaccessibleVideo_ModerationStatusesNotChanged() {
        Long bannerId = bannerInfo.getBannerId();

        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionCollectionId, true)));

        assertThat(requests, hasSize(1));

        List<OldBanner> banners = bannerRepository.getBanners(shard, singletonList(bannerId));
        assertThat(banners, hasSize(1));
        OldBanner banner = banners.get(0);

        assertThat(banner.getStatusModerate(), is(OldBannerStatusModerate.YES));
        assertThat(banner.getStatusPostModerate(), is(OldBannerStatusPostModerate.YES));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_NoBannerLinkedToCollection_NoRequestsSent() {

        Long anotherContentPromotionCollectionId = contentPromotionRepository.insertContentPromotion(
                bannerInfo.getClientId(),
                new ContentPromotionContent()
                        .withUrl("https://yandex.ru/collections/user/skpel/proekty-odnoetazhnykh-domov-iz-brusa/")
                        .withType(ContentPromotionContentType.COLLECTION)
                        .withIsInaccessible(false)
                        .withExternalId("anotherExternalId"));

        Consumer<List<ContentPromotionModerationVerdictRequest>> sender = Mockito.mock(Consumer.class);
        ArgumentCaptor<List<ContentPromotionModerationVerdictRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        moderationService.makeContentPromotionAccessibilityVerdictsRequests(shard,
                singletonList(new ContentPromotionAccessibilityData(anotherContentPromotionCollectionId, false)),
                sender);

        Mockito.verify(sender, Mockito.never()).accept(requestsCaptor.capture());
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_TwoBannersLinkedToCollection_BothBeenInModeration_TwoRequestsSent() {
        var anotherContentPromotionBannerInfo =
                steps.contentPromotionBannerSteps().createDefaultBanner(contentPromotionCollection);

        steps.bannerModerationVersionSteps().addBannerModerationVersion(anotherContentPromotionBannerInfo.getShard(),
                anotherContentPromotionBannerInfo.getBannerId(),
                BANNER_VERSION + 3);

        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionCollectionId, true)));

        assertThat(requests, hasSize(2));
    }

    @Test
    public void makeContentPromotionAccessibilityVerdictsRequests_TwoBannersLinkedToCollection_OneBeenInModeration_OneRequestSent() {
        steps.contentPromotionBannerSteps().createDefaultBanner(contentPromotionCollection);

        List<ContentPromotionModerationVerdictRequest> requests =
                makeContentPromotionAccessibilityVerdictsRequests(shard,
                        singletonList(new ContentPromotionAccessibilityData(contentPromotionCollectionId, true)));

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
