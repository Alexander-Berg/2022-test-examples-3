package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matchers;
import org.jooq.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddSmartAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddSmartAds;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.creativeNotFound;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultPerformanceAdGroup;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceAddSmartAdsTest {

    private static final String MUTATION_NAME = "addSmartAds";
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

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private OldBannerRepository bannerRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private TestClientRepository testClientRepository;

    private User operator;
    private int shard;
    private Long creativeId;
    private Long adGroupId;
    private Long campaignId;
    private Long feedId;
    private ClientId clientId;
    private Client client;
    private ClientInfo clientInfo;


    @Before
    public void before() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        adGroupId = adGroupInfo.getAdGroupId();
        campaignId = adGroupInfo.getCampaignId();
        feedId = adGroupInfo.getFeedId();
        clientId = adGroupInfo.getClientId();

        clientInfo = adGroupInfo.getClientInfo();
        shard = clientInfo.getShard();
        client = clientInfo.getClient();
        operator = UserHelper.getUser(client);
        TestAuthHelper.setDirectAuthentication(operator);

        Creative creative = defaultPerformanceCreative(null, null);
        creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();
    }

    @Test
    public void addBanners_whenSaveDraft() {
        GdAddAdsPayload gdAddAdsPayload = addBanner(adGroupId, creativeId, true);
        validateAddSuccessful(gdAddAdsPayload);

        Long bannerId = gdAddAdsPayload.getAddedAds().get(0).getId();

        OldPerformanceBanner expectedBanner = new OldPerformanceBanner()
                .withCreativeId(creativeId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanners_whenForceModerate() {
        GdAddAdsPayload gdAddAdsPayload = addBanner(adGroupId, creativeId, false);
        validateAddSuccessful(gdAddAdsPayload);

        Long bannerId = gdAddAdsPayload.getAddedAds().get(0).getId();

        OldPerformanceBanner expectedBanner = new OldPerformanceBanner()
                .withCreativeId(creativeId)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);

        checkBanner(bannerId, expectedBanner);
    }

    @Test
    public void addBanners_whenAnotherClientCreative() {
        Creative creative = defaultPerformanceCreative(null, null);
        Long anotherClientCreativeId = steps.creativeSteps().createCreative(creative, null).getCreativeId();

        GdAddAdsPayload gdAddAdsPayload = addBanner(adGroupId, anotherClientCreativeId, true);

        GdValidationResult expectedValidationResult = toGdValidationResult(
                path(field(GdAddSmartAds.AD_ADD_ITEMS), index(0), field(GdAddSmartAd.CREATIVE_ID)),
                creativeNotFound()
        ).withWarnings(null);

        GdAddAdsPayload expectedGdAddAdsPayload = new GdAddAdsPayload()
                .withAddedAds(singletonList(null))
                .withValidationResult(expectedValidationResult);

        assertThat(gdAddAdsPayload).is(matchedBy(beanDiffer(expectedGdAddAdsPayload)));

        List<Banner> banners = bannerTypedRepository.getBannersByGroupIds(shard, singletonList(adGroupId));
        assertThat(banners).isEmpty();
    }


    @Test
    public void addBanners_success_whenAdGroupContainsTheCrimeaCriativeInRussiaAndClientFromRussia() {
        //Подготавливаем и проверяем исходное состояние
        assumeThat(client.getCountryRegionId(), Matchers.is(Region.RUSSIA_REGION_ID));
        PerformanceAdGroup crimeaAdGroup = defaultPerformanceAdGroup(campaignId, feedId)
                .withGeo(singletonList(Region.SIMFEROPOL_REGION_ID));
        Configuration configuration = dslContextProvider.ppc(shard).configuration();
        adGroupRepository.addAdGroups(configuration, clientId, singletonList(crimeaAdGroup));
        Long crimeaAdGroupId = crimeaAdGroup.getId();
        Creative russiaCreative = defaultPerformanceCreative(clientId, null)
                .withSumGeo(singletonList(Region.RUSSIA_REGION_ID));
        steps.creativeSteps().createCreative(russiaCreative, clientInfo);
        Long russiaCreativeId = russiaCreative.getId();

        //Выполняем запрос
        GdAddAdsPayload payload = addBanner(crimeaAdGroupId, russiaCreativeId, true);

        //Проверяем результат
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(payload.getValidationResult()).isNull();
            soft.assertThat(payload.getAddedAds().get(0)).isNotNull();
        });
    }

    @Test
    public void addBanners_failure_whenAdGroupContainsTheCrimeaCriativeInRussiaAndClientFromUkraine() {
        //Подготавливаем исходное состояние
        PerformanceAdGroup crimeaAdGroup = defaultPerformanceAdGroup(campaignId, feedId)
                .withGeo(singletonList(Region.SIMFEROPOL_REGION_ID));
        Configuration configuration = dslContextProvider.ppc(shard).configuration();
        adGroupRepository.addAdGroups(configuration, clientId, singletonList(crimeaAdGroup));
        Long crimeaAdGroupId = crimeaAdGroup.getId();
        Creative russiaCreative = defaultPerformanceCreative(clientId, null)
                .withSumGeo(singletonList(Region.RUSSIA_REGION_ID));
        assumeThat(russiaCreative.getStatusModerate(),
                is(ru.yandex.direct.core.entity.creative.model.StatusModerate.YES));
        steps.creativeSteps().createCreative(russiaCreative, clientInfo);
        Long russiaCreativeId = russiaCreative.getId();
        testClientRepository.setClientRegionId(shard, clientId, Region.UKRAINE_REGION_ID);

        //Ожидаемый результат
        GdAddAdsPayload expectedPayload = new GdAddAdsPayload()
                .withAddedAds(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(singletonList(new GdDefect()
                                .withCode("BannerDefectIds.Gen.INCONSISTENT_CREATIVE_GEO_TO_ADGROUP_GEO")
                                .withPath("adAddItems[0].creativeId"))));

        //Выполняем запрос
        GdAddAdsPayload payload = addBanner(crimeaAdGroupId, russiaCreativeId, true);

        //Проверяем результат
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }


    private GdAddAdsPayload addBanner(Long adGroupId, Long creativeId, boolean saveDraft) {
        GdAddSmartAds gdAddSmartAds = new GdAddSmartAds()
                .withSaveDraft(saveDraft)
                .withAdAddItems(singletonList(
                        new GdAddSmartAd()
                                .withAdGroupId(adGroupId)
                                .withCreativeId(creativeId)));

        String query = createQuery(gdAddSmartAds);
        return processQueryAndGetResult(query);
    }

    private String createQuery(GdAddSmartAds gdAddSmartAds) {
        return String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(gdAddSmartAds));
    }

    private GdAddAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return convertValue(data.get(MUTATION_NAME), GdAddAdsPayload.class);
    }

    private void validateAddSuccessful(GdAddAdsPayload actualGdAddAdsPayload) {
        assertThat(actualGdAddAdsPayload.getValidationResult()).isNull();
    }

    private void checkBanner(Long bannerId, OldBanner expectedBanner) {
        OldPerformanceBanner actualBanner = getPerformanceBanner(bannerId);
        assertThat(actualBanner).is(matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    private OldPerformanceBanner getPerformanceBanner(Long bannerId) {
        return (OldPerformanceBanner) bannerRepository.getBanners(shard, singletonList(bannerId)).get(0);
    }
}
