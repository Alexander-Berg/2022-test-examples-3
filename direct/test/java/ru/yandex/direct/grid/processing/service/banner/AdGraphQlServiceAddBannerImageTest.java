package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.model.old.OldDynamicBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.DynamicBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdImage;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceAddBannerImageTest {
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String UPDATE_ADS_MUTATION_NAME = "addAdImage";
    private static final GraphQlTestExecutor.TemplateMutation<GdAddAdImage, GdUpdateAdsPayload>
            UPDATE_ADS_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(UPDATE_ADS_MUTATION_NAME, MUTATION_TEMPLATE,
                    GdAddAdImage.class, GdUpdateAdsPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    public OldBannerRepository bannerRepository;

    private AdGroupInfo defaultAdGroup;
    private TextBannerInfo textAd;
    private Long textAdId;
    private User operator;
    private ClientInfo clientInfo;
    private String imageHash;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        textAd = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        textAdId = textAd.getBannerId();
        operator = UserHelper.getUser(clientInfo.getClient());

        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), clientInfo);
        OldBannerImage bannerImage = steps.bannerSteps().createBannerImage(bannerInfo).getBannerImage();
        imageHash = bannerImage.getImageHash();

        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void addBannerImageToOneTextAdWithoutImage_Success() {
        GdAddAdImage input = new GdAddAdImage()
                .withAdIds(singletonList(textAdId))
                .withAdImageHash(imageHash);

        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        validateUpdateSuccessful(gdUpdateAdsPayload, textAdId);

        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(textAd.getShard(), singletonList(textAdId)).get(0);

        assertThat(actualBanner.getBannerImage()).isNotNull();
        assertThat(actualBanner.getBannerImage().getImageHash()).isEqualTo(imageHash);
    }

    @Test
    public void addBannerImageToOneDynamicAdWithoutImage_Success() {
        AdGroupInfo dynamicTextAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
        DynamicBannerInfo dynamicBanner = steps.bannerSteps().createActiveDynamicBanner(dynamicTextAdGroup);
        Long dynamicBannerId = dynamicBanner.getBannerId();

        GdAddAdImage input = new GdAddAdImage()
                .withAdIds(singletonList(dynamicBannerId))
                .withAdImageHash(imageHash);

        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        validateUpdateSuccessful(gdUpdateAdsPayload, dynamicBannerId);

        var shard = dynamicBanner.getShard();
        OldDynamicBanner actualBanner =
                (OldDynamicBanner) bannerRepository.getBanners(shard, List.of(dynamicBannerId)).get(0);

        assertThat(actualBanner.getBannerImage()).isNotNull();
        assertThat(actualBanner.getBannerImage().getImageHash()).isEqualTo(imageHash);
    }

    @Test
    public void addBannerImageToOneTextAdWithImage_Success() {
        steps.bannerSteps().createBannerImage(textAd);

        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), clientInfo);
        OldBannerImage bannerImage = steps.bannerSteps().createBannerImage(bannerInfo).getBannerImage();

        String newImageHash = bannerImage.getImageHash();

        GdAddAdImage input = new GdAddAdImage()
                .withAdIds(singletonList(textAdId))
                .withAdImageHash(newImageHash);

        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);
        validateUpdateSuccessful(gdUpdateAdsPayload, textAdId);

        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(textAd.getShard(), singletonList(textAdId)).get(0);

        assertThat(actualBanner.getBannerImage()).isNotNull();
        assertThat(actualBanner.getBannerImage().getImageHash()).isEqualTo(newImageHash);
    }

    @Test
    public void addNullBannerImageToOneTextAdWithImage_Deleted() {
        steps.bannerSteps().createBannerImage(textAd);

        GdAddAdImage input = new GdAddAdImage()
                .withAdIds(singletonList(textAdId))
                .withAdImageHash(null);

        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);
        validateUpdateSuccessful(gdUpdateAdsPayload, textAdId);

        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(textAd.getShard(), singletonList(textAdId)).get(0);

        assertThat(actualBanner.getBannerImage()).isNull();
    }

    @Test
    public void addImageToTextAdAndCpmAd_TextAdSaved() {
        CpmBannerInfo cpmAd =
                steps.bannerSteps().createActiveCpmVideoBanner(clientInfo);

        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), clientInfo);
        OldBannerImage bannerImage = steps.bannerSteps().createBannerImage(bannerInfo).getBannerImage();

        String newImageHash = bannerImage.getImageHash();

        GdAddAdImage input = new GdAddAdImage()
                .withAdIds(asList(textAdId, cpmAd.getBannerId()))
                .withAdImageHash(newImageHash);

        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                singletonList(new GdDefect().withCode("BannerDefectIds.Gen.INCONSISTENT_BANNER_TYPE")
                        .withPath("adUpdateItems[1]")));
        GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(singletonList(new GdUpdateAdPayloadItem().withId(textAdId)))
                .withValidationResult(expectedValidationResult);
        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));

        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(textAd.getShard(), singletonList(textAdId)).get(0);

        assertThat(actualBanner.getBannerImage()).isNotNull();
        assertThat(actualBanner.getBannerImage().getImageHash()).isEqualTo(newImageHash);
    }

    @Test
    public void addImageToTextAdAndCpmAd_SelectTypeNoneSaved() {
        CpmBannerInfo cpmAd =
                steps.bannerSteps().createActiveCpmVideoBanner(clientInfo);

        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), clientInfo);
        OldBannerImage bannerImage = steps.bannerSteps().createBannerImage(bannerInfo).getBannerImage();

        String newImageHash = bannerImage.getImageHash();

        GdAddAdImage input = new GdAddAdImage()
                .withAdIds(asList(textAdId, cpmAd.getBannerId()))
                .withAdImageHash(newImageHash);

        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                List.of(new GdDefect()
                        .withCode("BannerDefectIds.Gen.INCONSISTENT_BANNER_TYPE")
                        .withPath("adUpdateItems[1]")
                ));
        GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(List.of(new GdUpdateAdPayloadItem().withId(textAdId)))
                .withValidationResult(expectedValidationResult);
        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));

        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(textAd.getShard(), singletonList(textAdId)).get(0);

        assertThat(actualBanner.getBannerImage()).isNotNull();
        assertThat(actualBanner.getBannerImage().getImageHash()).isEqualTo(newImageHash);
    }

    @Test
    public void addEmptyBannerImageToOneTextAdWithoutImage_Error() {
        GdAddAdImage input = new GdAddAdImage()
                .withAdIds(singletonList(textAdId))
                .withAdImageHash("");

        ExecutionResult result = processor.doMutation(UPDATE_ADS_MUTATION, input, operator);

        List<GraphQLError> graphQLErrors = result.getErrors();

        assertThat(graphQLErrors).isNotEmpty();
    }

    private void validateUpdateSuccessful(GdUpdateAdsPayload gdUpdateAdsPayload, Long... bannerId) {
        List<GdUpdateAdPayloadItem> updatedAds =
                StreamEx.of(bannerId).map(id -> new GdUpdateAdPayloadItem().withId(id)).toList();
        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(updatedAds);

        assertThat(gdUpdateAdsPayload)
                .is(matchedBy(beanDiffer(expectedGdUpdateAdsPayload)));
    }

}
