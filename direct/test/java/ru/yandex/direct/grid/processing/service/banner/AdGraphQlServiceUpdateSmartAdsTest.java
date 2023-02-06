package ru.yandex.direct.grid.processing.service.banner;

import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateSmartAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateSmartAds;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.creativeNotFound;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceUpdateSmartAdsTest {

    private static final String MUTATION_NAME = "updateSmartAds";
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
            + "    updatedAds {"
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
    private OldBannerRepository bannerRepository;

    private ClientInfo clientInfo;
    private User operator;
    private int shard;
    private Long oldCreativeId;
    private Long bannerId;

    @Before
    public void before() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();

        clientInfo = adGroupInfo.getClientInfo();
        shard = clientInfo.getShard();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        Creative oldCreative = defaultPerformanceCreative(null, null);
        oldCreativeId = steps.creativeSteps().createCreative(oldCreative, clientInfo).getCreativeId();

        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();

        OldPerformanceBanner banner = activePerformanceBanner(campaignId, adGroupId, oldCreativeId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);
        bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();
    }

    @Test
    public void updateBanners_whenSaveDraftAndCreativeIdChanges() {
        Creative newCreative = defaultPerformanceCreative(null, null);
        Long newCreativeId = steps.creativeSteps().createCreative(newCreative, clientInfo).getCreativeId();

        GdUpdateAdsPayload gdUpdateAdsPayload = updateBannerCreativeId(bannerId, newCreativeId, true);

        validateUpdateSuccessful(bannerId, gdUpdateAdsPayload);

        OldPerformanceBanner expectedBanner = new OldPerformanceBanner()
                .withCreativeId(newCreativeId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);

        checkUpdatedBanner(bannerId, expectedBanner);
    }

    @Test
    public void updateBanners_whenForceModerateAndCreativeIdDoesNotChange() {
        GdUpdateAdsPayload gdUpdateAdsPayload = updateBannerCreativeId(bannerId, oldCreativeId, false);

        validateUpdateSuccessful(bannerId, gdUpdateAdsPayload);

        OldPerformanceBanner expectedBanner = new OldPerformanceBanner()
                .withCreativeId(oldCreativeId)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);

        checkUpdatedBanner(bannerId, expectedBanner);
    }

    @Test
    public void updateBanners_whenFlagsAlreadyExist() {
        var flags = BannerFlags.fromSource("medicine");
        steps.bannerSteps().setFlags(clientInfo.getShard(), bannerId, flags);

        Creative newCreative = defaultPerformanceCreative(null, null);
        Long newCreativeId = steps.creativeSteps().createCreative(newCreative, clientInfo).getCreativeId();

        GdUpdateAdsPayload gdUpdateAdsPayload = updateBannerCreativeId(bannerId, newCreativeId, true);

        validateUpdateSuccessful(bannerId, gdUpdateAdsPayload);

        OldPerformanceBanner expectedBanner = new OldPerformanceBanner()
                .withCreativeId(newCreativeId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withFlags(flags)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);

        checkUpdatedBanner(bannerId, expectedBanner);
    }

    @Test
    public void updateBanners_whenAnotherClientCreative() {
        OldPerformanceBanner bannerBeforeUpdate = getPerformanceBanner(bannerId);

        Creative newCreative = defaultPerformanceCreative(null, null);
        Long newCreativeId = steps.creativeSteps().createCreative(newCreative, null).getCreativeId();

        GdUpdateAdsPayload gdUpdateAdsPayload = updateBannerCreativeId(bannerId, newCreativeId, true);

        GdValidationResult expectedValidationResult = toGdValidationResult(
                path(field(GdUpdateSmartAds.AD_UPDATE_ITEMS), index(0), field(GdUpdateSmartAd.CREATIVE_ID)),
                creativeNotFound()
        ).withWarnings(null);

        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(singletonList(null))
                .withValidationResult(expectedValidationResult);

        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedGdUpdateAdsPayload)));

        checkUpdatedBanner(bannerId, bannerBeforeUpdate);
    }

    private GdUpdateAdsPayload updateBannerCreativeId(Long bannerId, Long newCreativeId, boolean saveDraft) {
        GdUpdateSmartAds gdUpdateSmartAds = new GdUpdateSmartAds()
                .withSaveDraft(saveDraft)
                .withAdUpdateItems(singletonList(
                        new GdUpdateSmartAd()
                                .withId(bannerId)
                                .withCreativeId(newCreativeId)));

        String query = createQuery(gdUpdateSmartAds);
        return processQueryAndGetResult(query);
    }

    private String createQuery(GdUpdateSmartAds gdUpdateSmartAds) {
        return String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(gdUpdateSmartAds));
    }

    private GdUpdateAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return convertValue(data.get(MUTATION_NAME), GdUpdateAdsPayload.class);
    }

    private void validateUpdateSuccessful(Long bannerId, GdUpdateAdsPayload actualGdUpdateAdsPayload) {
        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(singletonList(
                        new GdUpdateAdPayloadItem().withId(bannerId)));

        assertThat(actualGdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedGdUpdateAdsPayload)));
    }

    private void checkUpdatedBanner(Long bannerId, OldBanner expectedBanner) {
        OldPerformanceBanner actualBanner = getPerformanceBanner(bannerId);
        assertThat(actualBanner).is(matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    private OldPerformanceBanner getPerformanceBanner(Long bannerId) {
        return (OldPerformanceBanner) bannerRepository.getBanners(shard, singletonList(bannerId)).get(0);
    }
}
