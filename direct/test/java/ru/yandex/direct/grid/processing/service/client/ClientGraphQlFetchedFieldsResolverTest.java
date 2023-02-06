package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;

import graphql.ExecutionResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.CampaignFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.FetchedFieldsResolver;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.data.TestGdAds;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetFilter;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetsContainer;
import ru.yandex.direct.grid.processing.model.goal.GdGoalsContainer;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.offer.GdOfferFilter;
import ru.yandex.direct.grid.processing.model.offer.GdOffersContainer;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingFilter;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingsContainer;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionFilter;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionsContainer;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterFilter;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFiltersContainer;
import ru.yandex.direct.grid.processing.model.strategy.query.GdPackageStrategiesContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.DynamicTargetTestDataUtils;
import ru.yandex.direct.grid.processing.util.GoalTestDataUtils;
import ru.yandex.direct.grid.processing.util.OfferTestDataUtils;
import ru.yandex.direct.grid.processing.util.RetargetingTestDataUtils;
import ru.yandex.direct.grid.processing.util.ShowConditionTestDataUtils;
import ru.yandex.direct.grid.processing.util.SmartFilterTestDataUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil.buildCampaignFetchedFieldsResolver;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.getDefaultPackageStrategiesContainerInput;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientGraphQlFetchedFieldsResolverTest {

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Test
    public void testFetchedFieldsResolver_noClientInRequest() {
        String queryTemplate = "{\n"
                + "  constants{\n"
                + "    currencyConstants{\n"
                + "      code\n"
                + "    }\n"
                + "  }\n"
                + "}\n";
        assertThat(processAndGetActual(queryTemplate)).isNull();
    }

    @Test
    public void testFetchedFieldsResolver_allFalse() {
        String queryTemplate = "{\n"
                + "  client(searchBy: {login: \"%s\"}) {\n"
                + "    campaigns(input: %s) {\n"
                + "      rowset {\n"
                + "        id\n"
                + "      }\n"
                + "    }\n"
                + "    campaignGoals(input: %s) {\n"
                + "      rowset {\n"
                + "        id\n"
                + "      }\n"
                + "    }\n"
                + "    adGroups(input: %s) {\n"
                + "      rowset {\n"
                + "        id\n"
                + "      }\n"
                + "    }\n"
                + "    ads(input: %s) {\n"
                + "      rowset {\n"
                + "        id\n"
                + "      }\n"
                + "    }\n"
                + "    showConditions(input: %s) {\n"
                + "      rowset {\n"
                + "        ... on GdKeyword {\n"
                + "          keyword\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "    offers(input: %s) {\n"
                + "      rowset {\n"
                + "        id {\n"
                + "          businessId\n"
                + "          shopId\n"
                + "          offerYabsId\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}\n";
        FetchedFieldsResolver expected = FetchedFieldsResolverCoreUtil.buildFetchedFieldsResolver(false);
        FetchedFieldsResolver actual = processAndGetActual(queryTemplate);
        assertThat(actual).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testFetchedFieldsResolver_allTrue() {
        String queryTemplate = "{\n"
                + "  client(searchBy: {login: \"%s\"}) {\n"
                + "    campaigns(input: %s) {\n"
                + "      cacheKey\n"
                + "      rowset {\n"
                + "        recommendations{\n"
                + "          isApplicable\n"
                + "        }\n"
                + "        stats {\n"
                + "          avgClickCost\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "    campaignGoals(input: %s) {\n"
                + "      rowset {\n"
                + "        domain\n"
                + "      }\n"
                + "    }\n"
                + "    adGroups(input: %s) {\n"
                + "      cacheKey\n"
                + "      rowset {\n"
                + "        recommendations{\n"
                + "          isApplicable\n"
                + "        }\n"
                + "        tags {\n"
                + "          id\n"
                + "        }\n"
                + "        stats {\n"
                + "          avgClickCost\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "    ads(input: %s) {\n"
                + "      cacheKey\n"
                + "      rowset {\n"
                + "        recommendations{\n"
                + "          isApplicable\n"
                + "        }\n"
                + "        stats {\n"
                + "          avgClickCost\n"
                + "        }\n"
                + "        statsByDays {\n"
                + "          avgClickCost\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "    showConditions(input: %s) {\n"
                + "      cacheKey\n"
                + "      rowset {\n"
                + "        stats {\n"
                + "          avgClickCost\n"
                + "        }\n"
                + "        ... on GdKeyword {\n"
                + "          auctionData {\n"
                + "            auctionDataItems {\n"
                + "              amnestyPrice\n"
                + "            }\n"
                + "          }\n"
                + "          pokazometerData {\n"
                + "            allCostsAndClicks {\n"
                + "              clicks\n"
                + "            }\n"
                + "          }\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "    retargetings(input: %s) {\n"
                + "      cacheKey\n"
                + "      rowset {\n"
                + "        retargetingCondition{\n"
                + "          name\n"
                + "        }\n"
                + "        stats {\n"
                + "          avgClickCost\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "    dynamicAdTargets(input: %s) {\n"
                + "      cacheKey\n"
                + "      rowset {\n"
                + "        name\n"
                + "        stats {\n"
                + "          avgClickCost\n"
                + "        }\n"
                + "        goalStats {\n"
                + "          goalId\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "    smartFilters(input: %s) {\n"
                + "      cacheKey\n"
                + "      rowset {\n"
                + "        name\n"
                + "        stats {\n"
                + "          avgClickCost\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "    offers(input: %s) {\n"
                + "      cacheKey\n"
                + "      rowset {\n"
                + "        name\n"
                + "        stats {\n"
                + "          avgClickCost\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "    wallets {\n"
                + "      paidDaysLeft"
                + "    }\n"
                + "    strategies(input: %s) {\n"
                + "        cacheKey\n"
                + "        totalCount\n"
                + "        rowset {\n"
                + "          name\n"
                + "          stats {\n"
                + "             avgClickCost\n"
                + "          }\n"
                + "        }\n"
                + "      }"
                + "  }\n"
                + "}\n";
        FetchedFieldsResolver expected = FetchedFieldsResolverCoreUtil.buildFetchedFieldsResolver(true);
        FetchedFieldsResolver actual = processAndGetActual(queryTemplate);
        assertThat(actual).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testFetchedFieldsResolver_WithInlineFragmentAndFragmentSpread() {
        String queryTemplate = "{\n"
                + "  client(searchBy: {login: \"%s\"}) {\n"
                + "    campaigns(input: %s) {\n"
                + "      ... on GdCampaignsContext {\n"
                + "        totalCount\n"
                + "        cacheKey\n"
                + "      }\n"
                + "      rowset {\n"
                + "        ...campaignsRowFragment\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}\n"
                + "fragment campaignsRowFragment on GdCampaign {\n"
                + "  id\n"
                + "  stats {\n"
                + "    avgClickCost\n"
                + "  }\n"
                + "}";

        CampaignFetchedFieldsResolver campaignFetchedFieldsResolver =
                buildCampaignFetchedFieldsResolver(false)
                        .withCacheKey(true)
                        .withStats(true);
        FetchedFieldsResolver expected = FetchedFieldsResolverCoreUtil.buildFetchedFieldsResolver(false)
                .withCampaign(campaignFetchedFieldsResolver);
        assertThat(processAndGetActual(queryTemplate)).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testFetchedFieldsResolver_WithDeepInlineFragmentAndDeepFragmentSpread() {
        String queryTemplate = "{\n"
                + "  client(searchBy: {login: \"%s\"}) {\n"
                + "    campaigns(input: %s) {\n"
                + "      ... on GdCampaignsContext {\n"
                + "        totalCount\n"
                + "        ...campaignsContextFragment\n"
                + "      }\n"
                + "      rowset {\n"
                + "        ...campaignsRowFragment\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}\n"
                + "fragment campaignsContextFragment on GdCampaignsContext {\n"
                + "  cacheKey\n"
                + "}"
                + "fragment campaignsRowFragment on GdCampaign {\n"
                + "  id\n"
                + "  ... on GdCampaign {\n"
                + "     stats {\n"
                + "         avgClickCost\n"
                + "     }\n"
                + "  }\n"
                + "}";

        CampaignFetchedFieldsResolver campaignFetchedFieldsResolver =
                buildCampaignFetchedFieldsResolver(false)
                        .withCacheKey(true)
                        .withStats(true);
        FetchedFieldsResolver expected = FetchedFieldsResolverCoreUtil.buildFetchedFieldsResolver(false)
                .withCampaign(campaignFetchedFieldsResolver);
        assertThat(processAndGetActual(queryTemplate)).is(matchedBy(beanDiffer(expected)));
    }

    private FetchedFieldsResolver processAndGetActual(String queryTemplate) {
        GdCampaignsContainer campaignsContainer =
                CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        GdGoalsContainer campaignGoalsContainer =
                GoalTestDataUtils.getDefaultGdGoalsContainerInput();

        GdAdGroupsContainer adGroupsContainer =
                AdGroupTestDataUtils.getDefaultGdAdGroupsContainer()
                        .withOrderBy(Collections.emptyList())
                        .withFilter(new GdAdGroupFilter().withCampaignIdIn(Collections.emptySet()));

        GdAdsContainer adsContainer = TestGdAds.getDefaultGdAdsContainer()
                .withOrderBy(Collections.emptyList())
                .withFilter(new GdAdFilter().withCampaignIdIn(Collections.emptySet()));
        GdShowConditionsContainer showConditionsContainer =
                ShowConditionTestDataUtils.getDefaultGdShowConditionsContainer()
                        .withOrderBy(Collections.emptyList())
                        .withFilter(new GdShowConditionFilter().withCampaignIdIn(Collections.emptySet()));

        GdRetargetingsContainer retargetingsContainer =
                RetargetingTestDataUtils.getDefaultGdRetargetingsContainer()
                        .withOrderBy(Collections.emptyList())
                        .withFilter(new GdRetargetingFilter()
                                .withCampaignIdIn(Collections.emptySet())
                                .withAdGroupIdIn(Collections.emptySet()));

        GdDynamicAdTargetsContainer dynamicAdTargetsContainer =
                DynamicTargetTestDataUtils.getDefaultGdDynamicAdTargetsContainer()
                        .withFilter(new GdDynamicAdTargetFilter()
                                .withCampaignIdIn(Collections.emptySet())
                                .withAdGroupIdIn(Collections.emptySet()));

        GdSmartFiltersContainer smartFiltersContainer =
                SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
                        .withFilter(new GdSmartFilterFilter()
                                .withCampaignIdIn(Collections.emptySet())
                                .withAdGroupIdIn(Collections.emptySet()));

        GdOffersContainer offersContainer =
                OfferTestDataUtils.getDefaultGdOffersContainer()
                        .withFilter(new GdOfferFilter()
                                .withCampaignIdIn(Collections.emptySet())
                                .withAdGroupIdIn(Collections.emptySet()));

        GdPackageStrategiesContainer packageStrategiesContainer =
                getDefaultPackageStrategiesContainerInput();

        UserInfo userInfo = userSteps.createUser(generateNewUser());
        GridGraphQLContext context = new GridGraphQLContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);

        String query = String.format(queryTemplate, userInfo.getUser().getLogin(),
                graphQlSerialize(campaignsContainer),
                graphQlSerialize(campaignGoalsContainer),
                graphQlSerialize(adGroupsContainer),
                graphQlSerialize(adsContainer),
                graphQlSerialize(showConditionsContainer),
                graphQlSerialize(retargetingsContainer),
                graphQlSerialize(dynamicAdTargetsContainer),
                graphQlSerialize(smartFiltersContainer),
                graphQlSerialize(offersContainer),
                graphQlSerialize(packageStrategiesContainer));

        ExecutionResult er = processor.processQuery(null, query, null, context);

        return context.getFetchedFieldsReslover();
    }
}
