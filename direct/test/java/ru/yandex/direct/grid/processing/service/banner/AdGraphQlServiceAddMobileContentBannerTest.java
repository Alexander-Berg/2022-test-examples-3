package ru.yandex.direct.grid.processing.service.banner;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
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

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction;
import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.trustedredirects.model.RedirectType;
import ru.yandex.direct.core.entity.trustedredirects.model.TrustedRedirects;
import ru.yandex.direct.core.entity.trustedredirects.repository.TrustedRedirectsRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdAction;
import ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdAgeLabel;
import ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdFeature;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddMobileContentAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddMobileContentAds;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.validation.result.DefectIds;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CPI_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualSearchStrategy;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdAction.DOWNLOAD;
import static ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdAgeLabel.AGE_6;
import static ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdFeature.ICON;
import static ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdFeature.PRICE;
import static ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdFeature.RATING;
import static ru.yandex.direct.grid.processing.model.banner.GdMobileContentAdFeature.RATING_VOTES;
import static ru.yandex.direct.grid.processing.service.banner.converter.AdMutationDataConverter.toGdMobileContentAdAgeLabel;
import static ru.yandex.direct.grid.processing.service.banner.converter.AdMutationDataConverter.toNewMobileContentPrimaryAction;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGraphQlServiceAddMobileContentBannerTest {
    private static final String TRACKING_URL = "http://app.adjust.com/newnewnew?aaa=555";
    private static final String IMPRESSION_URL = "http://view.adjust.com/impression/newnewnew?aaa=555";
    private static final String MAIL_RU_IMPRESSION_URL =
            "https://trk.mail.ru/i/uh0jz9?mt_gaid={google_aid}&logid={LOGID}";
    private static final String IMPRESSION_URL_WITH_ANOTHER_TRACKER_ID =
            "http://view.adjust.com/impression/aaaaaa?aaa=555";
    private static final String MUTATION_NAME = "addMobileContentAds";
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
            + "    addedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

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
    private UserRepository userRepository;
    @Autowired
    private TrustedRedirectsRepository trustedRedirectsRepository;
    @Autowired
    private BannerService bannerService;

    private ClientInfo clientInfo;
    private User operator;
    private Long adGroupId;
    private Long creativeCanvasId;
    private String imageHash;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operator = userRepository.fetchByUids(clientInfo.getShard(), singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        adGroupId = adGroupInfo.getAdGroupId();

        imageHash = steps.bannerSteps().createWideImageFormat(clientInfo).getImageHash();

        creativeCanvasId = steps.creativeSteps().getNextCreativeId();
        creativeCanvasId = steps.creativeSteps()
                .addDefaultVideoAdditionCreative(adGroupInfo.getClientInfo(), creativeCanvasId).getCreativeId();

        trustedRedirectsRepository.addTrustedDomain(
                new TrustedRedirects().withDomain("app.adjust.com").withRedirectType(RedirectType.MOBILE_APP_COUNTER));
        trustedRedirectsRepository.addTrustedDomain(
                new TrustedRedirects().withDomain("view.adjust.com").withRedirectType(RedirectType.MOBILE_APP_IMPRESSION_COUNTER));
        trustedRedirectsRepository.addTrustedDomain(
                new TrustedRedirects().withDomain("trk.mail.ru").withRedirectType(RedirectType.MOBILE_APP_IMPRESSION_COUNTER));
    }

    @After
    public void after() {
        steps.trustedRedirectSteps().deleteTrusted();
    }

    @Test
    public void addAd_whenSaveDraft() {
        GdAddMobileContentAd gdAddAd = getBannerToSend(adGroupId, DOWNLOAD, AGE_6, Map.of(ICON, true), IMPRESSION_URL);

        GdAddAdsPayload gdAddAdsPayload = sendRequest(gdAddAd, true);
        checkBanner(gdAddAdsPayload, getExpectedBanner(adGroupId));
    }

    @Test
    public void addAd_whenSaveDraft_NullImpressionUrl() {
        GdAddMobileContentAd gdAddAd = getBannerToSend(adGroupId, DOWNLOAD, AGE_6, Map.of(ICON, true), null);

        GdAddAdsPayload gdAddAdsPayload = sendRequest(gdAddAd, true);
        checkBanner(gdAddAdsPayload, getExpectedBanner(adGroupId).withImpressionUrl(null));
    }

    public static Object[] actionParameters() {
        return StreamEx.of(GdMobileContentAdAction.values())
                .map(action -> new Object[]{action, toNewMobileContentPrimaryAction(action)})
                .toArray();
    }

    @Test
    @Parameters(method = "actionParameters")
    @TestCaseName("[{index}] {0} -> {1}")
    public void addAd_WithDifferentActions(GdMobileContentAdAction gdAction,
                                           NewMobileContentPrimaryAction expectedAction) {
        GdAddMobileContentAd gdAddAd = getBannerToSend(adGroupId, gdAction, AGE_6, Map.of(ICON, true), IMPRESSION_URL);

        GdAddAdsPayload gdAddAdsPayload = sendRequest(gdAddAd, false);
        checkBanner(gdAddAdsPayload, getExpectedBanner(adGroupId)
                .withPrimaryAction(expectedAction));
    }

    public static Object[] ageLabelParameters() {
        return StreamEx.of(GdMobileContentAdAgeLabel.values())
                .map(ageLabel -> new Object[]{ageLabel, new BannerFlags()
                        .with(BannerFlags.AGE, toGdMobileContentAdAgeLabel(ageLabel))})
                .toArray();
    }

    @Test
    @Parameters(method = "ageLabelParameters")
    @TestCaseName("[{index}] {0}")
    public void addAd_WithDifferentAges(GdMobileContentAdAgeLabel gdAge, BannerFlags expectedAgeBannerFlag) {
        GdAddMobileContentAd gdAddAd = getBannerToSend(adGroupId, DOWNLOAD, gdAge, Map.of(ICON, true), IMPRESSION_URL);

        GdAddAdsPayload gdAddAdsPayload = sendRequest(gdAddAd, false);
        checkBanner(gdAddAdsPayload, getExpectedBanner(adGroupId)
                .withFlags(expectedAgeBannerFlag));
    }

    public static Object[][] featureParameters() {
        return new Object[][]{
                {"[ICON, RATING_VOTES, PRICE, RATING]",
                        Map.of(ICON, true, RATING_VOTES, true, PRICE, true, RATING, true),
                        Map.of(NewReflectedAttribute.ICON, true, NewReflectedAttribute.RATING_VOTES, true,
                                NewReflectedAttribute.PRICE, true, NewReflectedAttribute.RATING, true)},
                {"[ICON]",
                        Map.of(ICON, true, RATING_VOTES, false, PRICE, false, RATING, false),
                        Map.of(NewReflectedAttribute.ICON, true, NewReflectedAttribute.RATING_VOTES, false,
                                NewReflectedAttribute.PRICE, false, NewReflectedAttribute.RATING, false)},
                {"[RATING_VOTES]",
                        Map.of(ICON, false, RATING_VOTES, true, PRICE, false, RATING, false),
                        Map.of(NewReflectedAttribute.ICON, false, NewReflectedAttribute.RATING_VOTES, true,
                                NewReflectedAttribute.PRICE, false, NewReflectedAttribute.RATING, false)},
                {"[PRICE]",
                        Map.of(ICON, false, RATING_VOTES, false, PRICE, true, RATING, false),
                        Map.of(NewReflectedAttribute.ICON, false, NewReflectedAttribute.RATING_VOTES, false,
                                NewReflectedAttribute.PRICE, true, NewReflectedAttribute.RATING, false)},
                {"[RATING]",
                        Map.of(ICON, false, RATING_VOTES, false, PRICE, false, RATING, true),
                        Map.of(NewReflectedAttribute.ICON, false, NewReflectedAttribute.RATING_VOTES, false,
                                NewReflectedAttribute.PRICE, false, NewReflectedAttribute.RATING, true)},
                {"[]",
                        Map.of(ICON, false, RATING_VOTES, false, PRICE, false, RATING, false),
                        Map.of(NewReflectedAttribute.ICON, false, NewReflectedAttribute.RATING_VOTES, false,
                                NewReflectedAttribute.PRICE, false, NewReflectedAttribute.RATING, false)}
        };
    }

    @Test
    @Parameters(method = "featureParameters")
    @TestCaseName("[{index}]: разрешенные к показу атрибуты {0}")
    public void addAd_WithDifferentFeatures(@SuppressWarnings("unused") String description,
                                            Map<GdMobileContentAdFeature, Boolean> gdFeatures,
                                            Map<NewReflectedAttribute, Boolean> expectedFeatures) {
        GdAddMobileContentAd gdAddAd = getBannerToSend(adGroupId, DOWNLOAD, AGE_6, gdFeatures, IMPRESSION_URL);

        GdAddAdsPayload gdAddAdsPayload = sendRequest(gdAddAd, false);
        checkBanner(gdAddAdsPayload, getExpectedBanner(adGroupId)
                .withReflectedAttributes(expectedFeatures));
    }

    @Test
    public void addAd_WithImpressionUrl() {
        GdAddMobileContentAd gdAddAd = getBannerToSend(adGroupId, DOWNLOAD, AGE_6, Map.of(ICON, true), IMPRESSION_URL);

        GdAddAdsPayload gdAddAdsPayload = sendRequest(gdAddAd, false);
        checkBanner(gdAddAdsPayload, getExpectedBanner(adGroupId));
    }

    @Test
    public void addAd_WithoutImpressionUrl() {
        GdAddMobileContentAd gdAddAd = getBannerToSend(adGroupId, DOWNLOAD, AGE_6, Map.of(ICON, true), null);

        GdAddAdsPayload gdAddAdsPayload = sendRequest(gdAddAd, false);
        checkBanner(gdAddAdsPayload, getExpectedBanner(adGroupId).withImpressionUrl(null));
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
     * Проверяем добавление РМП баннера без трекинговой ссылки
     */
    @Test
    @Parameters(method = "requiredTrackingUrlByStrategyParameters")
    @TestCaseName("[{index}]: {0} -> баннер создан: {3}")
    public void addAd_WithoutTrackingUrl(@SuppressWarnings("unused") String description,
                                         DbStrategy dbStrategy,
                                         CampaignsPlatform campaignsPlatform,
                                         boolean expectSuccess) {
        dbStrategy.setPlatform(campaignsPlatform);
        var campaignInfo = createMobileContentCampaign(dbStrategy);
        AdGroup adGroup = steps.adGroupSteps().saveAdGroup(activeMobileAppAdGroup(campaignInfo.getId()), clientInfo);

        GdAddMobileContentAd gdAddAd = getBannerToSend(adGroup.getId(), DOWNLOAD, AGE_6, Map.of(ICON, true), null)
                .withTrackingUrl(null);

        GdAddAdsPayload gdAddAdsPayload = sendRequest(gdAddAd, false);
        if (expectSuccess) {
            checkBanner(gdAddAdsPayload, getExpectedBanner(adGroup.getId())
                    .withImpressionUrl(null)
                    .withHref(null));
        } else {
            GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                    List.of(new GdDefect()
                            .withCode(DefectIds.CANNOT_BE_NULL.getCode())
                            .withPath("adAddItems[0].href")));
            GdAddAdsPayload expectedPayload = new GdAddAdsPayload()
                    .withAddedAds(singletonList(null))
                    .withValidationResult(expectedValidationResult);

            assertThat(gdAddAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
        }
    }

    @Test
    public void addAd_WithIncorrectImpressionUrlTrackingSystem() {
        GdAddMobileContentAd gdAddAd = getBannerToSend(adGroupId, DOWNLOAD, AGE_6, Map.of(ICON, true),
                MAIL_RU_IMPRESSION_URL);

        GdAddAdsPayload gdAddAdsPayload = sendRequest(gdAddAd, false);
        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                List.of(new GdDefect()
                        .withCode(BannerDefectIds.Gen.INCORRECT_IMPRESSION_URL_TRACKING_SYSTEM.getCode())
                        .withPath("adAddItems[0].impressionUrl")));
        GdAddAdsPayload expectedPayload = new GdAddAdsPayload()
                .withAddedAds(singletonList(null))
                .withValidationResult(expectedValidationResult);

        assertThat(gdAddAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void addAd_WithIncorrectImpressionUrlTrackerId() {
        GdAddMobileContentAd gdAddAd = getBannerToSend(adGroupId, DOWNLOAD, AGE_6, Map.of(ICON, true),
                IMPRESSION_URL_WITH_ANOTHER_TRACKER_ID);

        GdAddAdsPayload gdAddAdsPayload = sendRequest(gdAddAd, false);
        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                List.of(new GdDefect()
                        .withCode(BannerDefectIds.Gen.INCORRECT_IMPRESSION_URL_TRACKER_ID.getCode())
                        .withPath("adAddItems[0].impressionUrl")));
        GdAddAdsPayload expectedPayload = new GdAddAdsPayload()
                .withAddedAds(singletonList(null))
                .withValidationResult(expectedValidationResult);

        assertThat(gdAddAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    private GdAddMobileContentAd getBannerToSend(Long adGroupId, GdMobileContentAdAction gdAction,
                                                 GdMobileContentAdAgeLabel gdAge,
                                                 Map<GdMobileContentAdFeature, Boolean> gdFeatures,
                                                 String impressionUrl) {
        return new GdAddMobileContentAd()
                .withAdGroupId(adGroupId)
                .withBody("TEXT")
                .withTitle("TITLE")
                .withAction(gdAction)
                .withAdType(GdAdType.MOBILE_CONTENT)
                .withAgeLabel(gdAge)
                .withTrackingUrl(TRACKING_URL)
                .withImpressionUrl(impressionUrl)
                .withFeatures(gdFeatures)
                .withCreativeId(creativeCanvasId)
                .withAdImageHash(imageHash);
    }


    private MobileAppBanner getExpectedBanner(Long adGroupId) {
        return new MobileAppBanner()
                .withAdGroupId(adGroupId)
                .withBody("TEXT")
                .withTitle("TITLE")
                .withPrimaryAction(NewMobileContentPrimaryAction.DOWNLOAD)
                .withFlags(new BannerFlags().with(BannerFlags.AGE, Age.AGE_6))
                .withHref(TRACKING_URL)
                .withImpressionUrl(IMPRESSION_URL)
                .withReflectedAttributes(Map.of(
                        NewReflectedAttribute.ICON, true,
                        NewReflectedAttribute.RATING_VOTES, false,
                        NewReflectedAttribute.PRICE, false,
                        NewReflectedAttribute.RATING, false))
                .withImageHash(imageHash);
    }

    private GdAddAdsPayload sendRequest(GdAddMobileContentAd gdAddAd, boolean saveDraft) {
        GdAddMobileContentAds gdAddAds = new GdAddMobileContentAds()
                .withSaveDraft(saveDraft)
                .withAdAddItems(singletonList(gdAddAd));

        String query = String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(gdAddAds));
        return processQueryAndGetResult(query);
    }

    private GdAddAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return convertValue(data.get(MUTATION_NAME), GdAddAdsPayload.class);
    }

    private void checkBanner(GdAddAdsPayload gdAddAdsPayload, MobileAppBanner expectedBanner) {
        validateAddSuccessful(gdAddAdsPayload);
        List<Long> bannerIds = mapList(gdAddAdsPayload.getAddedAds(), GdAddAdPayloadItem::getId);

        List<BannerWithSystemFields> actualBanners = bannerService.getBannersByIds(bannerIds);
        BannerWithSystemFields actualBanner = Iterables.getFirst(actualBanners, null);
        checkNotNull(actualBanner, "banner not found");

        assertThat(actualBanner).as("баннер")
                .is(matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    private void validateAddSuccessful(GdAddAdsPayload actualGdAddAdsPayload) {
        assertThat(actualGdAddAdsPayload.getValidationResult()).isNull();
    }

    private MobileContentCampaignInfo createMobileContentCampaign(DbStrategy dbStrategy) {
        return steps.mobileContentCampaignSteps().createCampaign(clientInfo,
                defaultMobileContentCampaignWithSystemFields(clientInfo)
                        .withStrategy(dbStrategy));
    }
}
