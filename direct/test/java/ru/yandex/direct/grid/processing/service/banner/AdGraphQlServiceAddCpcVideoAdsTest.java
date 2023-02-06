package ru.yandex.direct.grid.processing.service.banner;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TrustedRedirectSteps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CPI_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_HREF;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualSearchStrategy;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGraphQlServiceAddCpcVideoAdsTest {
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
    private static final String BODY = "body";
    private static final String TITLE = "title";

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
    public AggregatorDomainsRepository aggregatorDomainsRepository;

    private int shard;
    private User operator;
    private Long turboLandingId;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        ClientId clientId = clientInfo.getClientId();
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
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveAdGroupByType(AdGroupType.BASE, clientInfo);
        long creativeId = steps.creativeSteps().getNextCreativeId();
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId);
        GdAddAd gdUpdateAd = defaultGdCpcVideoAd(adGroupInfo, creativeInfo)
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
        long creativeId = steps.creativeSteps().getNextCreativeId();
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId);
        String href = adGroupType == AdGroupType.MOBILE_CONTENT ? HREF_FOR_MOBILE_CONTENT : HREF;
        GdAddAd gdUpdateAd = defaultGdCpcVideoAd(adGroupInfo, creativeInfo)
                .withHref(href);

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
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.DESKTOP_LANDING, true);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveAdGroupByType(adGroupType, clientInfo);
        long creativeId = steps.creativeSteps().getNextCreativeId();
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId);
        GdAddAd gdUpdateAd = defaultGdCpcVideoAd(adGroupInfo, creativeInfo)
                .withTurbolandingId(adGroupType != AdGroupType.MOBILE_CONTENT ? turboLandingId : null);

        GdAddAds gdAddAds = createAddRequest(gdUpdateAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    /**
     * Проверка ошибки валидации при попытке создать баннер в РМП группе с некорректной трекинговой ссылкой
     */
    @Test
    public void addBanners_WithWrongHrefForMobileAdGroup_TrackingSystemDomainNotSupported() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        long creativeId = steps.creativeSteps().getNextCreativeId();
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId);
        GdAddAd gdAddAd = defaultGdCpcVideoAd(adGroupInfo, creativeInfo)
                .withHref("http://not_valid_href.com");

        GdAddAds gdAddAds = createAddRequest(gdAddAd);

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

    /**
     * Проверка ошибки валидации при попытке добавить баннер в РМП группе с турболендингом
     */
    @Test
    public void addBanners_InMobileContentWithTurboLanding() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps()
                .createActiveAdGroupByType(AdGroupType.MOBILE_CONTENT, clientInfo);
        long creativeId = steps.creativeSteps().getNextCreativeId();
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId);
        GdAddAd gdUpdateAd = defaultGdCpcVideoAd(adGroupInfo, creativeInfo)
                .withTurbolandingId(turboLandingId);

        GdAddAds gdAddAds = createAddRequest(gdUpdateAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                List.of(new GdDefect().withCode(CampaignDefectIds.Gen.INCONSISTENT_CAMPAIGN_TYPE.getCode())
                        .withPath("adAddItems[0]")));
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

        long creativeId = steps.creativeSteps().getNextCreativeId();
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId);

        GdAddAds gdAddAds = createAddRequest(defaultGdCpcVideoAd(adGroupInfo, creativeInfo));

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

    private GdAddAds createAddRequest(GdAddAd gdAd) {
        return new GdAddAds()
                .withSaveDraft(true)
                .withAdAddItems(List.of(gdAd));
    }

    private GdAddAd defaultGdCpcVideoAd(AdGroupInfo adGroupInfo, CreativeInfo creativeInfo) {
        return new GdAddAd()
                .withAdType(GdAdType.CPC_VIDEO)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCreativeId(creativeInfo.getCreativeId())
                .withIsMobile(adGroupInfo.getAdGroupType() == AdGroupType.MOBILE_CONTENT)
                .withBody(BODY)
                .withTitle(TITLE);
    }

    private String createQuery(GdAddAds gdUpdateAds) {
        return String.format(QUERY_TEMPLATE, ADD_ADS_MUTATION, graphQlSerialize(gdUpdateAds));
    }

    private MobileContentCampaignInfo createMobileContentCampaign(DbStrategy dbStrategy) {
        return steps.mobileContentCampaignSteps().createCampaign(clientInfo,
                defaultMobileContentCampaignWithSystemFields(clientInfo)
                        .withStrategy(dbStrategy));
    }
}
