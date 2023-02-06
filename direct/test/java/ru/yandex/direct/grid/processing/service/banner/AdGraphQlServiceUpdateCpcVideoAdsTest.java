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
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.old.OldCpcVideoBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TrustedRedirectSteps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CPI_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_HREF;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpcVideoBanner;
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
public class AdGraphQlServiceUpdateCpcVideoAdsTest {

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
    private BannerTypedRepository bannerRepository;
    @Autowired
    UserRepository userRepository;

    @Autowired
    public AggregatorDomainsRepository aggregatorDomainsRepository;

    private User operator;
    private int shard;
    private ClientInfo clientInfo;
    private Long turboLandingId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();
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
        long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId).getCreativeId();

        OldCpcVideoBanner banner = activeCpcVideoBanner(campaignId, adGroupId, creativeId);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload =
                updateBanner(AdGroupType.BASE, bannerId, creativeId, null, VK_TEST_PUBLIC_HREF);
        validateAddSuccessful(gdUpdateAdsPayload);

        BannerWithHref actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getHref()).isEqualTo(VK_TEST_PUBLIC_HREF);
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
    public void updateBanners_notAggregatorDomain(AdGroupType adGroupType) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveAdGroupByType(adGroupType, clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();
        long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId);

        OldCpcVideoBanner banner = activeCpcVideoBanner(campaignId, adGroupId, creativeId);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        String href = adGroupType == AdGroupType.MOBILE_CONTENT ? HREF_FOR_MOBILE_CONTENT : HREF;
        GdUpdateAdsPayload gdUpdateAdsPayload =
                updateBanner(adGroupType, bannerId, creativeId, null, href);
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
        long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId);

        OldCpcVideoBanner banner = activeCpcVideoBanner(campaignId, adGroupId, creativeId);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload =
                updateBanner(adGroupType, bannerId, creativeId,
                        adGroupType != AdGroupType.MOBILE_CONTENT ? turboLandingId : null, null);
        validateAddSuccessful(gdUpdateAdsPayload);

        BannerWithHref actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getHref()).isNull();
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    /**
     * Проверка ошибки валидации при попытке обновить баннер в РМП группе с некорректной трекинговой ссылкой
     */
    @Test
    public void updateBanners_WithWrongHrefForMobileAdGroup_TrackingSystemDomainNotSupported() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();
        long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId);

        OldCpcVideoBanner banner = activeCpcVideoBanner(campaignId, adGroupId, creativeId);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload = updateBanner(AdGroupType.MOBILE_CONTENT,
                bannerId, creativeId, null, "http://not_valid_href.com");

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                List.of(new GdDefect().withCode(BannerDefectIds.Gen.THIS_TRACKING_SYSTEM_DOMAIN_NOT_SUPPORTED.getCode())
                        .withPath("adUpdateItems[0].href")));
        GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(singletonList(null))
                .withValidationResult(expectedValidationResult);

        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    /**
     * Проверка ошибки валидации при попытке обновить баннер в РМП группе с турболендингом
     */
    @Test
    public void updateBanners_InMobileContentWithTurboLanding() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps()
                .createActiveAdGroupByType(AdGroupType.MOBILE_CONTENT, clientInfo);
        long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId);

        OldCpcVideoBanner banner =
                activeCpcVideoBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload =
                updateBanner(AdGroupType.MOBILE_CONTENT, bannerId, creativeId, turboLandingId, null);

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                List.of(new GdDefect()
                        .withCode(CampaignDefectIds.Gen.INCONSISTENT_CAMPAIGN_TYPE.getCode())
                        .withPath("adUpdateItems[0]")));
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

        long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpcVideoCreative(clientInfo, creativeId);

        OldCpcVideoBanner banner =
                activeCpcVideoBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId);
        Long bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GdUpdateAdsPayload gdUpdateAdsPayload =
                updateBanner(AdGroupType.MOBILE_CONTENT, bannerId, creativeId, null, null);

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

    private String getAggregatorDomain(Long bannerId) {
        return aggregatorDomainsRepository.getAggregatorDomains(shard, List.of(bannerId)).get(bannerId);
    }

    private GdUpdateAdsPayload updateBanner(AdGroupType adGroupType, Long bannerId, Long creativeId,
                                            Long turboLandingId, String href) {
        GdUpdateAd gdAddAd = new GdUpdateAd()
                .withAdType(GdAdType.CPC_VIDEO)
                .withId(bannerId)
                .withCreativeId(creativeId)
                .withHref(href)
                .withIsMobile(adGroupType == AdGroupType.MOBILE_CONTENT)
                .withTitle(TITLE)
                .withBody(BODY);

        if (turboLandingId != null) {
            gdAddAd.withTurbolandingId(turboLandingId)
                    .withTurbolandingHrefParams(TURBOLANDING_HREF_PARAMS);
        }

        GdUpdateAds gdUpdateCpmAds = new GdUpdateAds()
                .withSaveDraft(true)
                .withAdUpdateItems(singletonList(gdAddAd));

        String query = String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(gdUpdateCpmAds));
        return processQueryAndGetResult(query);
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

    private MobileContentCampaignInfo createMobileContentCampaign(DbStrategy dbStrategy) {
        return steps.mobileContentCampaignSteps().createCampaign(clientInfo,
                defaultMobileContentCampaignWithSystemFields(clientInfo)
                        .withStrategy(dbStrategy));
    }
}
