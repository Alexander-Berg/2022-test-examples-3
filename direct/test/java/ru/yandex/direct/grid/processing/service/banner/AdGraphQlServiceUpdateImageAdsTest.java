package ru.yandex.direct.grid.processing.service.banner;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerImageOpts;
import ru.yandex.direct.core.entity.banner.model.BannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.old.Image;
import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusImageModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.entity.image.model.BannerImageFromPool;
import ru.yandex.direct.core.entity.image.model.BannerImageSource;
import ru.yandex.direct.core.entity.image.repository.BannerImagePoolRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo;
import ru.yandex.direct.core.testing.repository.TestBannerImageFormatRepository;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TrustedRedirectSteps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesFormatsImageType;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesStatusshow;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.GdBannerButton;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CPI_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_HREF;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualSearchStrategy;
import static ru.yandex.direct.feature.FeatureName.SINGLE_IMAGE_AD_TO_BS;
import static ru.yandex.direct.feature.FeatureName.SINGLE_IMAGE_AD_TO_BS_AFTER_UPDATE_IMAGE;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.constants.GdButtonAction.DOWNLOAD;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGraphQlServiceUpdateImageAdsTest {

    private static final String MUTATION_NAME = "updateAds";
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
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
    private static final String TURBOLANDING_HREF_PARAMS = "turbolandingHrefParams";
    private static final String HREF = "https://yandex.ru";
    private static final String HREF_FOR_MOBILE_CONTENT = "http://" + TrustedRedirectSteps.DOMAIN;
    private static final String TITLE = "title";
    private static final String TITLE_EXTENSION = "title extension";
    private static final String BODY = "body";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private BannerTypedRepository bannerRepository;
    @Autowired
    private TestBannerImageFormatRepository testBannerImageFormatRepository;
    @Autowired
    private BannerImagePoolRepository bannerImagePoolRepository;
    @Autowired
    UserRepository userRepository;

    @Autowired
    public BannerTypedRepository bannerTypedRepository;

    @Autowired
    public AggregatorDomainsRepository aggregatorDomainsRepository;

    @Autowired
    public TestBannerImageRepository testBannerImageRepository;

    private User operator;
    private int shard;
    private ClientInfo clientInfo;
    private Long turboLandingId;
    private ClientId clientId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        OldBannerTurboLanding bannerTurboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId);
        turboLandingId = bannerTurboLanding.getId();

        steps.trustedRedirectSteps().addValidCounters();
    }

    @After
    public void after() {
        steps.trustedRedirectSteps().deleteTrusted();
    }

    @Test
    public void updateBanners_aggregatorDomain() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();

        String imageHash = createBannerImageHash(BannerImagesFormatsImageType.image_ad);
        Image image = new Image().withImageHash(imageHash).withStatusModerate(StatusImageModerate.YES);
        OldImageHashBanner banner = activeImageHashBanner(campaignId, adGroupId).withImage(image);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload =
                updateImageBanner(AdGroupType.BASE, bannerId, imageHash, null, VK_TEST_PUBLIC_HREF);
        validateAddSuccessful(gdUpdateAdsPayload);

        BannerWithHref actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getHref()).isEqualTo(VK_TEST_PUBLIC_HREF);
        assertThat(getAggregatorDomain(bannerId)).isEqualTo(VK_TEST_PUBLIC_AGGREGATOR_DOMAIN);
    }

    @Test
    public void updateBannersWithFlags() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();

        String imageHash = createBannerImageHash(BannerImagesFormatsImageType.image_ad);
        Image image = new Image().withImageHash(imageHash).withStatusModerate(StatusImageModerate.YES);
        var flags = BannerFlags.fromSource("medicine");
        OldImageHashBanner banner = activeImageHashBanner(campaignId, adGroupId).withImage(image)
                .withFlags(flags);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload =
                updateImageBanner(AdGroupType.BASE, bannerId, imageHash, null, VK_TEST_PUBLIC_HREF);
        validateAddSuccessful(gdUpdateAdsPayload);

        var actualBanner = bannerRepository.getStrictly(shard, singletonList(bannerId), ImageBanner.class).get(0);
        assertThat(actualBanner.getFlags()).is(matchedBy(beanDiffer(flags)));
    }

    public static Object[] parametersForAddBanners() {
        return new Object[][]{
                {AdGroupType.BASE},
                {AdGroupType.MOBILE_CONTENT},
        };
    }

    @Test
    @Parameters(method = "parametersForAddBanners")
    @TestCaseName("{0}")
    public void updateBanners_notAggregatorDomain(AdGroupType adGroupType) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveAdGroupByType(adGroupType, clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();

        String imageHash = createBannerImageHash(BannerImagesFormatsImageType.image_ad);
        Image image = new Image().withImageHash(imageHash).withStatusModerate(StatusImageModerate.YES);
        OldImageHashBanner banner = activeImageHashBanner(campaignId, adGroupId).withImage(image);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        String href = adGroupType == AdGroupType.MOBILE_CONTENT ? HREF_FOR_MOBILE_CONTENT : HREF;
        GdUpdateAdsPayload gdUpdateAdsPayload = updateImageBanner(adGroupType, bannerId, imageHash, null, href);
        validateAddSuccessful(gdUpdateAdsPayload);

        BannerWithHref actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getHref()).isEqualTo(href);
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    @Parameters(method = "parametersForAddBanners")
    @TestCaseName("{0}")
    public void updateBanners_withoutHref(AdGroupType adGroupType) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveAdGroupByType(adGroupType, clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();

        String imageHash = createBannerImageHash(BannerImagesFormatsImageType.image_ad);
        Image image = new Image().withImageHash(imageHash).withStatusModerate(StatusImageModerate.YES);
        OldImageHashBanner banner = activeImageHashBanner(campaignId, adGroupId).withImage(image);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();
        GdUpdateAdsPayload gdUpdateAdsPayload = updateImageBanner(
                adGroupType, bannerId, imageHash, turboLandingId, null);
        validateAddSuccessful(gdUpdateAdsPayload);

        BannerWithHref actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getHref()).isNull();
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    @Parameters(method = "parametersForAddBanners")
    @TestCaseName("{0}")
    public void updateBanners_withTextsLogoAndButton(AdGroupType adGroupType) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveAdGroupByType(adGroupType, clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();

        String imageHash = createBannerImageHash(BannerImagesFormatsImageType.image_ad);
        Image image = new Image().withImageHash(imageHash).withStatusModerate(StatusImageModerate.YES);
        String logoImageHash = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo()).getImageHash();
        OldImageHashBanner banner = activeImageHashBanner(campaignId, adGroupId).withImage(image);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAd gdAddAd = new GdUpdateAd()
                .withAdType(GdAdType.IMAGE_AD)
                .withId(bannerId)
                .withImageCreativeHash(imageHash)
                .withHref(adGroupType == AdGroupType.MOBILE_CONTENT ? HREF_FOR_MOBILE_CONTENT : HREF)
                .withIsMobile(adGroupType == AdGroupType.MOBILE_CONTENT)
                .withTitle(TITLE)
                .withTitleExtension(TITLE_EXTENSION)
                .withBody(BODY)
                .withLogoImageHash(logoImageHash)
                .withButton(new GdBannerButton().withAction(DOWNLOAD).withHref(HREF));

        GdUpdateAds gdUpdateCpmAds = createUpdateRequest(gdAddAd);
        String query = createQuery(gdUpdateCpmAds);
        GdUpdateAdsPayload gdUpdateAdsPayload = processQueryAndGetResult(query);
        validateAddSuccessful(gdUpdateAdsPayload);

        ImageBanner actualBanner = (ImageBanner) bannerTypedRepository
                .getTyped(adGroupInfo.getShard(), singleton(bannerId)).get(0);
        assertThat(actualBanner.getTitle()).isEqualTo(TITLE);
        assertThat(actualBanner.getTitleExtension()).isEqualTo(TITLE_EXTENSION);
        assertThat(actualBanner.getBody()).isEqualTo(BODY);
        assertThat(actualBanner.getLogoImageHash()).isEqualTo(logoImageHash);
        assertThat(actualBanner.getButtonAction()).isEqualTo(ButtonAction.DOWNLOAD);
        assertThat(actualBanner.getButtonHref()).isEqualTo(HREF);
    }

    /**
     * Проверка ошибки валидации при попытке обновить баннер в РМП группе с некорректной трекинговой ссылкой
     */
    @Test
    public void updateBanners_WithWrongHrefForMobileAdGroup_TrackingSystemDomainNotSupported() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();

        String imageHash = createBannerImageHash(BannerImagesFormatsImageType.image_ad);
        Image image = new Image().withImageHash(imageHash).withStatusModerate(StatusImageModerate.YES);
        OldImageHashBanner banner = activeImageHashBanner(campaignId, adGroupId).withImage(image);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload = updateImageBanner(AdGroupType.MOBILE_CONTENT,
                bannerId, imageHash, null, "http://not_valid_href.com");

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                List.of(new GdDefect().withCode(BannerDefectIds.Gen.THIS_TRACKING_SYSTEM_DOMAIN_NOT_SUPPORTED.getCode())
                        .withPath("adUpdateItems[0].href")));
        GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(singletonList(null))
                .withValidationResult(expectedValidationResult);

        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    public static Object[] requiredTrackingUrlByStrategyParameters() {
        return StreamEx.of(CampaignsPlatform.values())
                .map(platform -> List.of(
                        new Object[]{"AUTOBUDGET with " + platform + " platform and with default goalId",
                                autobudgetStrategy(null, null, DEFAULT_CPI_GOAL_ID)
                                        .withAutobudget(CampaignsAutobudget.YES), platform,
                                platform == CampaignsPlatform.CONTEXT},
                        new Object[]{"AUTOBUDGET with " + platform + " platform and without goalId",
                                autobudgetStrategy(null, null, null)
                                        .withAutobudget(CampaignsAutobudget.YES), platform,
                                true},
                        new Object[]{"AUTOBUDGET with " + platform + " platform and without default goalId",
                                autobudgetStrategy(null, null, 5L)
                                        .withAutobudget(CampaignsAutobudget.YES), platform,
                                true},
                        new Object[]{"DEFAULT_CPI_GOAL_ID with " + platform + " platform and with default goalId",
                                averageCpiStrategy(DEFAULT_CPI_GOAL_ID)
                                        .withAutobudget(CampaignsAutobudget.YES), platform,
                                platform == CampaignsPlatform.CONTEXT},
                        new Object[]{"DEFAULT_CPI_GOAL_ID with " + platform + " platform and without default goalId",
                                averageCpiStrategy(5L)
                                        .withAutobudget(CampaignsAutobudget.YES), platform,
                                platform == CampaignsPlatform.CONTEXT},
                        new Object[]{"DEFAULT with " + platform + " platform", manualSearchStrategy(), platform,
                                true}))
                .flatMap(Collection::stream)
                .toArray();
    }

    /**
     * Проверяем обновление баннера без трекинговой ссылки
     */
    @Test
    @Parameters(method = "requiredTrackingUrlByStrategyParameters")
    @TestCaseName("[{index}]: {0} -> баннер создан: {3}")
    public void updateAd_WithoutTrackingUrl(@SuppressWarnings("unused") String description,
                                            DbStrategy dbStrategy,
                                            CampaignsPlatform campaignsPlatform,
                                            boolean expectSuccess) {
        MobileContentCampaignInfo campaignInfo = createMobileContentCampaign((DbStrategy) dbStrategy
                .withPlatform(campaignsPlatform));
        campaignInfo.withCampaign(new Campaign().withId(campaignInfo.getCampaignId()));

        AdGroupInfo adGroupInfo = steps.adGroupSteps()
                .createActiveAdGroupByType(AdGroupType.MOBILE_CONTENT, campaignInfo);

        String imageHash = createBannerImageHash(BannerImagesFormatsImageType.image_ad);
        Image image = new Image()
                .withImageHash(imageHash)
                .withStatusModerate(StatusImageModerate.YES);
        OldImageHashBanner banner = activeImageHashBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withImage(image);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();
        GdUpdateAdsPayload gdUpdateAdsPayload =
                updateImageBanner(AdGroupType.MOBILE_CONTENT, bannerId, imageHash, null, null);

        if (expectSuccess) {
            validateAddSuccessful(gdUpdateAdsPayload);

            BannerWithHref actualBanner = getBanner(bannerId);
            assertThat(actualBanner.getHref()).isNull();
            assertThat(getAggregatorDomain(bannerId)).isNull();
        } else {
            GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                    List.of(new GdDefect()
                            .withCode(DefectIds.CANNOT_BE_NULL.getCode())
                            .withPath("adUpdateItems[0].href")));
            GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                    .withUpdatedAds(singletonList(null))
                    .withValidationResult(expectedValidationResult);

            assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
        }
    }

    public static Object[] parametersForBannersOpts() {
        return new Object[][]{
                {Set.of(BannerImageOpts.SINGLE_AD_TO_BS), true},
                {Set.of(BannerImageOpts.SINGLE_AD_TO_BS), false},
                {emptySet(), true},
                {emptySet(), false},
                {null, true},
                {null, false},
        };
    }

    /**
     * Проверка что при обновлении флаги banner_images.opts не изменяются, если раньше уже была картинка
     */
    @Test
    @Parameters(method = "parametersForBannersOpts")
    @TestCaseName("opts: {0}, feature: {1}")
    public void updateBanners_CheckSingleAdToBsFlag(
            Set<BannerImageOpts> bannerImageOpts,
            boolean singleImageAdToBSEnabled
    ) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        steps.featureSteps().addClientFeature(clientId, SINGLE_IMAGE_AD_TO_BS, singleImageAdToBSEnabled);

        String imageHash = createBannerImageHash(BannerImagesFormatsImageType.regular);

        // Создаем баннер с картиночной версией в banner_images
        var bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo);
        var bannerImageFormat = steps.bannerSteps().createBannerImageFormat(clientInfo);
        var bannerImage = defaultBannerImage(bannerInfo.getBannerId(), bannerImageFormat.getImageHash());
        testBannerImageRepository.addBannerImages(shard, List.of(bannerImage));
        Long bannerId = bannerImage.getBannerId();

        // Добавляем флаг для картиночной версии
        testBannerImageRepository.updateImageOpts(shard, bannerId, bannerImageOpts);

        GdUpdateAdsPayload gdUpdateAdsPayload =
                updateTextBanner(AdGroupType.BASE, bannerId, imageHash, null, VK_TEST_PUBLIC_HREF);
        validateAddSuccessful(gdUpdateAdsPayload);

        BannerWithBannerImage actualBanner = (BannerWithBannerImage) bannerTypedRepository
                .getTyped(shard, singleton(bannerId)).get(0);
        assertThat(actualBanner.getOpts())
                .as("Доп. флаг баннера не изменился")
                .isEqualTo(bannerImageOpts);
    }

    public static Object[] featueParameters() {
        return new Object[][]{
                {true},
                {false},
        };
    }

    /**
     * Проверка что при обновлении выставляется флаг banner_images.opts если раньше у баннера никогда не было картинки
     * и только при включенной фиче
     */
    @Test
    @Parameters(method = "featueParameters")
    @TestCaseName("feature: {0}")
    public void updateBanners_AddVeryFirstImage(boolean singleImageAdToBs) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        steps.featureSteps().addClientFeature(clientId, SINGLE_IMAGE_AD_TO_BS, singleImageAdToBs);
        steps.featureSteps().addClientFeature(clientId, SINGLE_IMAGE_AD_TO_BS_AFTER_UPDATE_IMAGE, singleImageAdToBs);

        String imageHash = createBannerImageHash(BannerImagesFormatsImageType.regular);


        var bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo);
        var bannerId = bannerInfo.getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload =
                updateTextBanner(AdGroupType.BASE, bannerId, imageHash, null, VK_TEST_PUBLIC_HREF);
        validateAddSuccessful(gdUpdateAdsPayload);

        var expectBannerImageOpts = singleImageAdToBs ? Set.of(BannerImageOpts.SINGLE_AD_TO_BS) : null;

        BannerWithBannerImage actualBanner = (BannerWithBannerImage) bannerTypedRepository
                .getTyped(shard, singleton(bannerId)).get(0);
        assertThat(actualBanner.getOpts())
                .as("Доп. флаг баннера не изменился")
                .isEqualTo(expectBannerImageOpts);
    }

    /**
     * Проверка что при обновлении не изменяется флаг banner_images.opts если раньше у баннера была картинка, но
     * сейчас она удалена
     */
    @Test
    @Parameters(method = "parametersForBannersOpts")
    @TestCaseName("feature: {0}")
    public void updateBanners_AddImageInsteadOfDeleted(
            Set<BannerImageOpts> bannerImageOpts,
            boolean singleImageAdToBs
    ) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        steps.featureSteps().addClientFeature(clientId, SINGLE_IMAGE_AD_TO_BS, singleImageAdToBs);
        steps.featureSteps().addClientFeature(clientId, SINGLE_IMAGE_AD_TO_BS_AFTER_UPDATE_IMAGE, singleImageAdToBs);

        String imageHash = createBannerImageHash(BannerImagesFormatsImageType.regular);

        // Создаем баннер с картиночной версией в banner_images
        var bannerImageFormat = steps.bannerSteps().createBannerImageFormat(clientInfo);
        var bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo);
        var bannerImage = defaultBannerImage(bannerInfo.getBannerId(), bannerImageFormat.getImageHash());
        testBannerImageRepository.addBannerImages(shard, List.of(bannerImage));
        Long bannerId = bannerImage.getBannerId();

        // Добавляем флаг для картиночной версии
        testBannerImageRepository.updateImageOpts(shard, bannerId, bannerImageOpts);
        testBannerImageRepository.updateStatusShow(shard, List.of(bannerId), BannerImagesStatusshow.No);

        GdUpdateAdsPayload gdUpdateAdsPayload =
                updateTextBanner(AdGroupType.BASE, bannerId, imageHash, null, VK_TEST_PUBLIC_HREF);
        validateAddSuccessful(gdUpdateAdsPayload);

        BannerWithBannerImage actualBanner = (BannerWithBannerImage) bannerTypedRepository
                .getTyped(shard, singleton(bannerId)).get(0);
        assertThat(actualBanner.getOpts())
                .as("Доп. флаг баннера не изменился")
                .isEqualTo(bannerImageOpts);
    }

    private String getAggregatorDomain(Long bannerId) {
        return aggregatorDomainsRepository.getAggregatorDomains(shard, List.of(bannerId)).get(bannerId);
    }

    private GdUpdateAdsPayload updateImageBanner(AdGroupType adGroupType, Long bannerId, String imageHash,
                                                 Long turboLandingId, String href) {
        GdUpdateAd gdUpdateAd = getGdUpdateAd(adGroupType, bannerId, turboLandingId, href, GdAdType.IMAGE_AD)
                .withImageCreativeHash(imageHash);
        return updateBanner(gdUpdateAd);
    }

    private GdUpdateAdsPayload updateTextBanner(AdGroupType adGroupType, Long bannerId, String imageHash,
                                                Long turboLandingId, String href) {
        GdUpdateAd gdUpdateAd = getGdUpdateAd(adGroupType, bannerId, turboLandingId, href, GdAdType.TEXT)
                .withTextBannerImageHash(imageHash);
        return updateBanner(gdUpdateAd);
    }

    private GdUpdateAd getGdUpdateAd(AdGroupType adGroupType, Long bannerId, Long turboLandingId, String href,
                                     GdAdType gdAdType) {
        GdUpdateAd gdAddAd = new GdUpdateAd()
                .withAdType(gdAdType)
                .withId(bannerId)
                .withHref(href)
                .withIsMobile(adGroupType == AdGroupType.MOBILE_CONTENT)
                .withTitle(TITLE)
                .withBody(BODY);

        if (turboLandingId != null && adGroupType != AdGroupType.MOBILE_CONTENT) {
            gdAddAd.withTurbolandingId(turboLandingId)
                    .withTurbolandingHrefParams(TURBOLANDING_HREF_PARAMS);
        }
        return gdAddAd;
    }

    private GdUpdateAdsPayload updateBanner(GdUpdateAd gdUpdateAd) {
        GdUpdateAds gdUpdateCpmAds = createUpdateRequest(gdUpdateAd);
        String query = createQuery(gdUpdateCpmAds);
        return processQueryAndGetResult(query);
    }

    private String createQuery(GdUpdateAds gdUpdateCpmAds) {
        return String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(gdUpdateCpmAds));
    }

    private GdUpdateAds createUpdateRequest(GdUpdateAd gdAddAd) {
        return new GdUpdateAds()
                .withSaveDraft(true)
                .withAdUpdateItems(singletonList(gdAddAd));
    }

    private GdUpdateAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return convertValue(data.get(MUTATION_NAME), GdUpdateAdsPayload.class);
    }

    private void validateAddSuccessful(GdUpdateAdsPayload actualGdUpdateAdsPayload) {
        assertThat(actualGdUpdateAdsPayload.getValidationResult()).isNull();
    }

    private BannerWithHref getBanner(Long bannerId) {
        return bannerRepository.getStrictly(shard, singletonList(bannerId), BannerWithHref.class).get(0);
    }

    private String createBannerImageHash(BannerImagesFormatsImageType bannerImagesFormatsImageType) {
        ClientId clientId = clientInfo.getClientId();
        String imageHash = random(22, true, true);
        BannerImageFromPool bannerImageFromPool = new BannerImageFromPool()
                .withImageHash(imageHash)
                .withCreateTime(LocalDateTime.now())
                .withSource(BannerImageSource.DIRECT)
                .withClientId(clientId.asLong());
        bannerImagePoolRepository.addOrUpdateImagesToPool(shard, clientId, List.of(bannerImageFromPool));
        testBannerImageFormatRepository.create(shard, imageHash, bannerImagesFormatsImageType);
        return imageHash;
    }

    private MobileContentCampaignInfo createMobileContentCampaign(DbStrategy dbStrategy) {
        return steps.mobileContentCampaignSteps().createCampaign(clientInfo,
                defaultMobileContentCampaignWithSystemFields(clientInfo)
                        .withStrategy(dbStrategy));
    }
}
