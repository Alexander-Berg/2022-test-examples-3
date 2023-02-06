package ru.yandex.direct.grid.processing.service.banner.contentpromotion;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddContentPromotionAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddContentPromotionAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdContentPromotion;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.fromSerpData;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.realLifeCollection;
import static ru.yandex.direct.core.testing.data.TestContentPromotionVideos.fromVideoBannerAsContent;
import static ru.yandex.direct.core.testing.data.TestContentPromotionVideos.realLifeVideoBanner;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.CONTENT_TYPE_NOT_MATCHES_ADGROUP_CONTENT_TYPE;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.TestUtils.randomName;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceAddContentPromotionAdsTest {

    private static final String OBJECT_NOT_FOUND = "DefectIds.OBJECT_NOT_FOUND";

    @Autowired
    private Steps steps;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    @Autowired
    private OldBannerRepository bannerRepository;

    private static final String MUTATION_NAME = "addContentPromotionAds";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    addedAds {\n"
            + "      id\n"
            + "    }\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdAddContentPromotionAds, GdAddAdsPayload> ADD_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdAddContentPromotionAds.class, GdAddAdsPayload.class);

    private Integer shard;
    private User operator;

    private Long videoAdGroupId;
    private Long collectionAdGroupId;

    private Long videoContentPromotionId;
    private Long collectionContentPromotionId;

    @Before
    public void before() throws IOException {
        ClientInfo clientInfo =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(Region.KAZAKHSTAN_REGION_ID));
        ClientId clientId = clientInfo.getClientId();

        shard = clientInfo.getShard();
        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        AdGroupInfo videoAdGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(campaignInfo, VIDEO);
        AdGroupInfo collectionAdGroupInfo =
                steps.adGroupSteps().createDefaultContentPromotionAdGroup(campaignInfo, COLLECTION);
        videoAdGroupId = videoAdGroupInfo.getAdGroupId();
        collectionAdGroupId = collectionAdGroupInfo.getAdGroupId();

        videoContentPromotionId = contentPromotionRepository.insertContentPromotion(clientId,
                fromVideoBannerAsContent(realLifeVideoBanner(), clientId));
        collectionContentPromotionId = contentPromotionRepository.insertContentPromotion(clientId,
                fromSerpData(realLifeCollection(), clientId));
    }

    @Test
    public void addContentPromotionAds_ContentPromotionVideo_Success() {
        String title = randomName("", 10);
        String description = randomName("", 10);
        String visitUrl = "https://ya.ru";

        GdAddContentPromotionAds gdAddContentPromotionAds = new GdAddContentPromotionAds()
                .withSaveDraft(true)
                .withAdAddItems(singletonList(new GdAddContentPromotionAd()
                        .withAdGroupId(videoAdGroupId)
                        .withAdType(GdAdType.CONTENT_PROMOTION)
                        .withTitle(title)
                        .withDescription(description)
                        .withVisitUrl(visitUrl)
                        .withContentPromotion(
                                new GdContentPromotion().withContentPromotionId(videoContentPromotionId))));

        GdAddAdsPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddContentPromotionAds, operator);
        validateResponseSuccessful(payload);

        assumeThat(payload.getAddedAds(), iterableWithSize(1));

        Long bannerId = payload.getAddedAds().get(0).getId();
        List<OldBanner> actualBanners = bannerRepository.getBanners(shard, singletonList(bannerId));
        assumeThat(actualBanners, iterableWithSize(1));

        OldContentPromotionBanner actualBanner = (OldContentPromotionBanner) actualBanners.get(0);
        OldContentPromotionBanner expectedBanner = new OldContentPromotionBanner()
                .withBannerType(OldBannerType.CONTENT_PROMOTION)
                .withAdGroupId(videoAdGroupId)
                .withTitle(title)
                .withBody(description)
                .withContentPromotionId(videoContentPromotionId)
                .withVisitUrl(visitUrl)
                .withHref("https://www.youtube.com/watch?v=0hCBBnZI2AU");

        assertThat(actualBanner).is(
                matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addContentPromotionAds_ContentPromotionVideo_NoVisitUrl_Success() {
        String title = randomName("", 10);
        String description = randomName("", 10);

        GdAddContentPromotionAds gdAddContentPromotionAds = new GdAddContentPromotionAds()
                .withSaveDraft(true)
                .withAdAddItems(singletonList(new GdAddContentPromotionAd()
                        .withAdGroupId(videoAdGroupId)
                        .withAdType(GdAdType.CONTENT_PROMOTION)
                        .withTitle(title)
                        .withDescription(description)
                        .withContentPromotion(
                                new GdContentPromotion().withContentPromotionId(videoContentPromotionId))));

        GdAddAdsPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddContentPromotionAds, operator);
        validateResponseSuccessful(payload);

        assumeThat(payload.getAddedAds(), iterableWithSize(1));

        Long bannerId = payload.getAddedAds().get(0).getId();
        List<OldBanner> actualBanners = bannerRepository.getBanners(shard, singletonList(bannerId));
        assumeThat(actualBanners, iterableWithSize(1));

        OldContentPromotionBanner actualBanner = (OldContentPromotionBanner) actualBanners.get(0);

        assertThat(actualBanner.getVisitUrl()).isNull();
    }

    @Test
    public void addContentPromotionAds_ContentPromotionCollection_Success() {
        String visitUrl = "https://ya.ru";

        GdAddContentPromotionAds gdAddContentPromotionAds = new GdAddContentPromotionAds()
                .withSaveDraft(true)
                .withAdAddItems(singletonList(new GdAddContentPromotionAd()
                        .withAdGroupId(collectionAdGroupId)
                        .withAdType(GdAdType.CONTENT_PROMOTION)
                        .withVisitUrl("https://ya.ru")
                        .withContentPromotion(
                                new GdContentPromotion().withContentPromotionId(collectionContentPromotionId))));

        GdAddAdsPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddContentPromotionAds, operator);
        validateResponseSuccessful(payload);

        assumeThat(payload.getAddedAds(), iterableWithSize(1));

        Long bannerId = payload.getAddedAds().get(0).getId();
        List<OldBanner> actualBanners = bannerRepository.getBanners(shard, singletonList(bannerId));
        assumeThat(actualBanners, iterableWithSize(1));

        OldContentPromotionBanner actualBanner = (OldContentPromotionBanner) actualBanners.get(0);
        OldContentPromotionBanner expectedBanner = new OldContentPromotionBanner()
                .withBannerType(OldBannerType.CONTENT_PROMOTION)
                .withAdGroupId(collectionAdGroupId)
                .withContentPromotionId(collectionContentPromotionId)
                .withVisitUrl(visitUrl)
                .withHref("https://l7test.yandex.ru/collections/user/yakudzablr/tupye-kartinochki/");

        assertThat(actualBanner).is(
                matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addContentPromotionAds_InvalidAdGroupId_ValidationError() {
        GdAddContentPromotionAds gdAddContentPromotionAds = new GdAddContentPromotionAds()
                .withSaveDraft(true)
                .withAdAddItems(singletonList(new GdAddContentPromotionAd()
                        .withAdGroupId(-1L)
                        .withAdType(GdAdType.CONTENT_PROMOTION)
                        .withTitle(randomName("", 10))
                        .withDescription(randomName("", 10))
                        .withVisitUrl("https://ya.ru")
                        .withContentPromotion(
                                new GdContentPromotion().withContentPromotionId(videoContentPromotionId))));

        ExecutionResult result = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddContentPromotionAds, operator);

        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    public void addContentPromotionAds_InvalidContentPromotionId_ValidationError() {
        GdAddContentPromotionAds gdAddContentPromotionAds = new GdAddContentPromotionAds()
                .withSaveDraft(true)
                .withAdAddItems(singletonList(new GdAddContentPromotionAd()
                        .withAdGroupId(videoAdGroupId)
                        .withAdType(GdAdType.CONTENT_PROMOTION)
                        .withTitle(randomName("", 10))
                        .withDescription(randomName("", 10))
                        .withVisitUrl("https://ya.ru")
                        .withContentPromotion(
                                new GdContentPromotion().withContentPromotionId(-1L))));

        ExecutionResult result = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddContentPromotionAds, operator);
        Map<String, Object> data = result.getData();
        GdAddAdsPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdAddAdsPayload.class);

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(payload.getAddedAds()).hasSize(1);
        softAssertions.assertThat(payload.getAddedAds()).containsExactlyElementsOf(singletonList(null));
        softAssertions.assertThat(payload.getValidationResult().getErrors()).hasSize(1);
        softAssertions.assertThat(payload.getValidationResult().getErrors().get(0).getCode())
                .isEqualTo(OBJECT_NOT_FOUND);
        softAssertions.assertThat(payload.getValidationResult().getErrors().get(0).getPath())
                .isEqualTo("adAddItems[0].contentPromotionId");
        softAssertions.assertAll();
    }

    @Test
    public void addContentPromotionAds_WrongContentPromotionType_ValidationError() {
        GdAddContentPromotionAds gdAddContentPromotionAds = new GdAddContentPromotionAds()
                .withSaveDraft(true)
                .withAdAddItems(singletonList(new GdAddContentPromotionAd()
                        .withAdGroupId(collectionAdGroupId)
                        .withAdType(GdAdType.CONTENT_PROMOTION)
                        .withVisitUrl("https://ya.ru")
                        .withContentPromotion(
                                new GdContentPromotion().withContentPromotionId(videoContentPromotionId))));

        ExecutionResult result = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddContentPromotionAds, operator);
        Map<String, Object> data = result.getData();
        GdAddAdsPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdAddAdsPayload.class);

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(payload.getAddedAds()).hasSize(1);
        softAssertions.assertThat(payload.getAddedAds()).containsExactlyElementsOf(singletonList(null));
        softAssertions.assertThat(payload.getValidationResult().getErrors()).hasSize(1);
        softAssertions.assertThat(payload.getValidationResult().getErrors().get(0).getCode())
                .isEqualTo(CONTENT_TYPE_NOT_MATCHES_ADGROUP_CONTENT_TYPE);
        softAssertions.assertAll();
    }
}
