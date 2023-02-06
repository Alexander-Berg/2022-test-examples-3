package ru.yandex.direct.grid.processing.service.banner;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
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
import ru.yandex.direct.core.entity.banner.model.BannerImageOpts;
import ru.yandex.direct.core.entity.banner.model.BannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
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
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo;
import ru.yandex.direct.core.testing.repository.TestBannerImageFormatRepository;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TrustedRedirectSteps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesFormatsImageType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.GdBannerButton;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CPI_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_HREF;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualSearchStrategy;
import static ru.yandex.direct.dbschema.ppc.enums.BannerImagesFormatsImageType.image_ad;
import static ru.yandex.direct.dbschema.ppc.enums.BannerImagesFormatsImageType.regular;
import static ru.yandex.direct.feature.FeatureName.SINGLE_IMAGE_AD_TO_BS;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.constants.GdButtonAction.DOWNLOAD;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGraphQlServiceAddImageAdsTest {
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    addedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String ADD_ADS_MUTATION = "addAds";
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
    public BannerTypedRepository bannerTypedRepository;

    @Autowired
    private TestBannerImageFormatRepository testBannerImageFormatRepository;

    @Autowired
    private BannerImagePoolRepository bannerImagePoolRepository;

    @Autowired
    public AggregatorDomainsRepository aggregatorDomainsRepository;

    @Autowired
    public TestTargetingCategoriesRepository testTargetingCategoriesRepository;

    @Autowired
    public TestBannerImageRepository testBannerImageRepository;

    private int shard;
    private User operator;
    private Long turboLandingId;
    private ClientInfo clientInfo;
    private ClientId clientId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        clientId = clientInfo.getClientId();
        OldBannerTurboLanding bannerTurboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId);
        turboLandingId = bannerTurboLanding.getId();

        steps.trustedRedirectSteps().addValidCounters();
    }

    @After
    public void after() {
        steps.trustedRedirectSteps().deleteTrusted();
    }

    @Test
    public void addBanners_aggregatorDomain() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        GdAddAd gdUpdateAd = getGdImageAd(adGroupInfo)
                .withHref(VK_TEST_PUBLIC_HREF);

        GdAddAds gdAddAds = createAddRequest(gdUpdateAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();
        assertThat(getAggregatorDomain(bannerId)).isEqualTo(VK_TEST_PUBLIC_AGGREGATOR_DOMAIN);
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
    public void addBanners_notAggregatorDomain(AdGroupType adGroupType) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveAdGroupByType(adGroupType, clientInfo);
        GdAddAd gdUpdateAd = getGdImageAd(adGroupInfo)
                .withHref(adGroupType == AdGroupType.MOBILE_CONTENT ? HREF_FOR_MOBILE_CONTENT : HREF);

        GdAddAds gdAddAds = createAddRequest(gdUpdateAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    @Parameters(method = "parametersForAddBanners")
    @TestCaseName("{0}")
    public void addBanners_withoutHref(AdGroupType adGroupType) {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DESKTOP_LANDING, true);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveAdGroupByType(adGroupType, clientInfo);
        GdAddAd gdUpdateAd = getGdImageAd(adGroupInfo)
                .withTurbolandingId(adGroupType != AdGroupType.MOBILE_CONTENT ? turboLandingId : null);

        GdAddAds gdAddAds = createAddRequest(gdUpdateAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    @Test
    @Parameters(method = "parametersForAddBanners")
    @TestCaseName("{0}")
    public void addBanners_withTextsLogoAndButton(AdGroupType adGroupType) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveAdGroupByType(adGroupType, clientInfo);
        String imageHash = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo()).getImageHash();

        GdAddAd gdUpdateAd = getGdImageAd(adGroupInfo)
                .withHref(adGroupType == AdGroupType.MOBILE_CONTENT ? HREF_FOR_MOBILE_CONTENT : HREF)
                .withTitle(TITLE)
                .withTitleExtension(TITLE_EXTENSION)
                .withBody(BODY)
                .withLogoImageHash(imageHash)
                .withButton(new GdBannerButton().withAction(DOWNLOAD).withHref(HREF));

        GdAddAds gdAddAds = createAddRequest(gdUpdateAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();

        ImageBanner actualBanner = (ImageBanner) bannerTypedRepository
                .getTyped(adGroupInfo.getShard(), singleton(bannerId)).get(0);
        assertThat(actualBanner.getTitle()).isEqualTo(TITLE);
        assertThat(actualBanner.getTitleExtension()).isEqualTo(TITLE_EXTENSION);
        assertThat(actualBanner.getBody()).isEqualTo(BODY);
        assertThat(actualBanner.getLogoImageHash()).isEqualTo(imageHash);
        assertThat(actualBanner.getButtonAction()).isEqualTo(ButtonAction.DOWNLOAD);
        assertThat(actualBanner.getButtonHref()).isEqualTo(HREF);
    }

    /**
     * Проверка ошибки валидации при попытке создать баннер в РМП группе с некорректной трекинговой ссылкой
     */
    @Test
    public void addBanners_WithWrongHrefForMobileAdGroup_TrackingSystemDomainNotSupported() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        GdAddAd gdImageAd = getGdImageAd(adGroupInfo)
                .withHref("http://not_valid_href.com");
        GdAddAds gdAddAds = createAddRequest(gdImageAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                List.of(new GdDefect().withCode(BannerDefectIds.Gen.THIS_TRACKING_SYSTEM_DOMAIN_NOT_SUPPORTED.getCode())
                        .withPath("adAddItems[0].href")));
        GdAddAdsPayload expectedPayload = new GdAddAdsPayload()
                .withAddedAds(singletonList(null))
                .withValidationResult(expectedValidationResult);

        assertThat(addAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
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
     * Проверяем добавление баннера без трекинговой ссылки
     */
    @Test
    @Parameters(method = "requiredTrackingUrlByStrategyParameters")
    @TestCaseName("[{index}]: {0} -> баннер создан: {3}")
    public void addAd_WithoutTrackingUrl(@SuppressWarnings("unused") String description,
                                         DbStrategy dbStrategy,
                                         CampaignsPlatform campaignsPlatform,
                                         boolean expectSuccess) {
        MobileContentCampaignInfo campaignInfo = createMobileContentCampaign((DbStrategy) dbStrategy
                .withPlatform(campaignsPlatform));
        campaignInfo.withCampaign(new Campaign().withId(campaignInfo.getCampaignId()));

        AdGroupInfo adGroupInfo = steps.adGroupSteps()
                .createActiveAdGroupByType(AdGroupType.MOBILE_CONTENT, campaignInfo);

        GdAddAds gdAddAds = createAddRequest(getGdImageAd(adGroupInfo));

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);

        if (expectSuccess) {
            assertThat(addAdsPayload.getValidationResult()).isNull();
            Long bannerId = addAdsPayload.getAddedAds().get(0).getId();
            assertThat(getAggregatorDomain(bannerId)).isNull();
        } else {
            GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                    List.of(new GdDefect()
                            .withCode(DefectIds.CANNOT_BE_NULL.getCode())
                            .withPath("adAddItems[0].href")));
            GdAddAdsPayload expectedPayload = new GdAddAdsPayload()
                    .withAddedAds(singletonList(null))
                    .withValidationResult(expectedValidationResult);

            assertThat(addAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
        }
    }

    public static Object[] parametersForBannersOpts() {
        return new Object[][]{
                {true},
                {false},
        };
    }

    /**
     * Проверка добавления флага SINGLE_AD_TO_BS в banner_images.opts при
     * добавлении баннера с включенной новой схемой отправки в БК
     */
    @Test
    @Parameters(method = "parametersForBannersOpts")
    @TestCaseName("feature: {0}")
    public void addBanners_CheckSingleAdToBsFlag(boolean singleImageAdToBs) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);

        steps.featureSteps().addClientFeature(clientId, SINGLE_IMAGE_AD_TO_BS, singleImageAdToBs);

        GdAddAd gdUpdateAd = getGdTextImageAd(adGroupInfo)
                .withHref(VK_TEST_PUBLIC_HREF);
        GdAddAds gdAddAds = createAddRequest(gdUpdateAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(addAdsPayload.getValidationResult())
                .as("Валидация создания баннера")
                .isNull();

        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();
        BannerWithBannerImage actualBanner = (BannerWithBannerImage) bannerTypedRepository
                .getTyped(adGroupInfo.getShard(), singleton(bannerId)).get(0);
        if (singleImageAdToBs) {
            soft.assertThat(actualBanner.getOpts())
                    .as("Доп. флаг баннера")
                    .isNotNull()
                    .containsOnly(BannerImageOpts.SINGLE_AD_TO_BS);
        } else {
            soft.assertThat(actualBanner.getOpts())
                    .as("Доп. флаг баннера")
                    .isNull();
        }
        soft.assertAll();
    }

    private GdAddAd getGdImageAd(AdGroupInfo adGroupInfo) {
        return getGdTextImageAd(adGroupInfo, GdAdType.IMAGE_AD)
                .withImageCreativeHash(createBannerImageHash(image_ad));
    }

    private GdAddAd getGdTextImageAd(AdGroupInfo adGroupInfo) {
        return getGdTextImageAd(adGroupInfo, GdAdType.TEXT)
                .withTextBannerImageHash(createBannerImageHash(regular));
    }

    private GdAddAd getGdTextImageAd(AdGroupInfo adGroupInfo, GdAdType gdAdType) {
        return new GdAddAd()
                .withAdType(gdAdType)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withIsMobile(adGroupInfo.getAdGroupType() == AdGroupType.MOBILE_CONTENT)
                .withBody(BODY)
                .withTitle(TITLE);
    }

    private String getAggregatorDomain(Long bannerId) {
        return aggregatorDomainsRepository.getAggregatorDomains(shard, List.of(bannerId)).get(bannerId);
    }

    private GdAddAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(ADD_ADS_MUTATION);
        return convertValue(data.get(ADD_ADS_MUTATION), GdAddAdsPayload.class);
    }

    private GdAddAds createAddRequest(GdAddAd... gdUpdateAd) {
        return new GdAddAds()
                .withSaveDraft(true)
                .withAdAddItems(List.of(gdUpdateAd));
    }

    private String createQuery(GdAddAds gdUpdateAds) {
        return String.format(QUERY_TEMPLATE, ADD_ADS_MUTATION, graphQlSerialize(gdUpdateAds));
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
