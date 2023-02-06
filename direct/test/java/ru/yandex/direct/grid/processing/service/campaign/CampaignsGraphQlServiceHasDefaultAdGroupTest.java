package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignsGraphQlService.HAS_DEFAULT_AD_GROUP_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsGraphQlServiceHasDefaultAdGroupTest {

    private static final String QUERY_TEMPLATE =
            "{\n" +
                    "    client(searchBy: {login: \"%s\"}) {\n" +
                    "    campaigns(input: %s) {\n" +
                    "            rowset {\n" +
                    "                ... on GdPriceCampaign {\n" +
                    "                    hasDefaultAdGroup\n" +
                    "                }\n" +
                    "            }" +
                    "        }\n" +
                    "    }\n" +
                    "}";

    private GdCampaignsContainer campaignsContainer;
    private GridGraphQLContext context;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    private CpmPriceCampaign cpmPriceCampaign;
    private ClientInfo clientInfo;

    @Before
    public void initTestData() {
        UserInfo userInfo = steps.userSteps().createDefaultUser();
        clientInfo = userInfo.getClientInfo();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID, true);

        var pricePackage =
                steps.pricePackageSteps().createApprovedPricePackageWithClients(clientInfo).getPricePackage();

        cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @Test
    @Description("Если нет групп - hasDefaultAdGroup должен быть false")
    public void hasNoAdGroups() {
        ExecutionResult result = testServiceAndGetResult();

        Map<String, Object> data = result.getData();
        assertThat(result.getErrors())
                .isEmpty();
        assertThat(data)
                .is(matchedBy(beanDiffer(getExpectedPayload(Boolean.FALSE))));
    }

    @Test
    @Description("Если есть группа с приоритетом 0 - hasDefaultAdGroup должен быть true")
    public void hasOnlyDefaultAdGroup() {
        CpmYndxFrontpageAdGroup defaultAdGroup = activeCpmYndxFrontpageAdGroup(cpmPriceCampaign.getId())
                .withPriority(0L);
        steps.adGroupSteps().createAdGroupRaw(defaultAdGroup, clientInfo);

        ExecutionResult result = testServiceAndGetResult();

        Map<String, Object> data = result.getData();
        assertThat(result.getErrors())
                .isEmpty();
        assertThat(data)
                .is(matchedBy(beanDiffer(getExpectedPayload(Boolean.TRUE))));
    }

    @Test
    @Description("Если нет группы с приоритетом 0 - hasDefaultAdGroup должен быть false")
    public void hasOnlyCustomAdGroup() {
        CpmYndxFrontpageAdGroup customAdGroup = activeCpmYndxFrontpageAdGroup(cpmPriceCampaign.getId())
                .withPriority(1L);
        steps.adGroupSteps().createAdGroupRaw(customAdGroup, clientInfo);

        ExecutionResult result = testServiceAndGetResult();

        Map<String, Object> data = result.getData();
        assertThat(result.getErrors())
                .isEmpty();
        assertThat(data)
                .is(matchedBy(beanDiffer(getExpectedPayload(Boolean.FALSE))));
    }

    @Test
    @Description("Если есть группа с приоритетом 0 и другие группы - hasDefaultAdGroup должен быть true")
    public void hasDefaultAndCustomAdgroups() {
        CpmYndxFrontpageAdGroup defaultAdGroup = activeCpmYndxFrontpageAdGroup(cpmPriceCampaign.getId())
                .withPriority(0L);
        steps.adGroupSteps().createAdGroupRaw(defaultAdGroup, clientInfo);
        CpmYndxFrontpageAdGroup customAdGroup = activeCpmYndxFrontpageAdGroup(cpmPriceCampaign.getId())
                .withPriority(1L);
        steps.adGroupSteps().createAdGroupRaw(customAdGroup, clientInfo);

        ExecutionResult result = testServiceAndGetResult();

        Map<String, Object> data = result.getData();
        assertThat(result.getErrors())
                .isEmpty();
        assertThat(data)
                .is(matchedBy(beanDiffer(getExpectedPayload(Boolean.TRUE))));
    }

    private ExecutionResult testServiceAndGetResult() {
        campaignsContainer.getFilter().setCampaignIdIn(ImmutableSet.of(cpmPriceCampaign.getId()));
        campaignsContainer.getLimitOffset().withLimit(1).withOffset(0);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());
        return result;
    }

    private static Map<String, Object> getExpectedPayload(Boolean hasDefaultAdGroup) {
        Map<String, Object> row = ImmutableMap.of(HAS_DEFAULT_AD_GROUP_RESOLVER_NAME, hasDefaultAdGroup);

        return ImmutableMap.of(
                "client", ImmutableMap.of(
                        "campaigns", ImmutableMap.of(
                                "rowset", List.of(
                                        row
                                )
                        )
                )
        );
    }

}
