package ru.yandex.direct.grid.processing.processor;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionsContainer;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.ShowConditionTestDataUtils.getDefaultGdShowConditionsContainer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

/**
 * Тест, который проверяет, что все сервисы прицепились к процессору корректно. В тестовом запросе должно быть как
 * минимум по одной query из каждого сервиса. Не нужно проверять тут хоть какую-то логику, для этого есть отдельные
 * тесты на сервисы
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GridGraphQLProcessorSmokeTest {
    private static final String QUERY_TEMPLATE = "{\n"
            + "  constants {\n"
            + "    currencyConstants(codes: [" + CurrencyCode.CHF + "]) {\n"
            + "      code\n"
            + "      minDailyBudget\n"
            + "    }\n"
            + "  }\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    info {\n"
            + "      id\n"
            + "      shard\n"
            + "    }\n"
            + "    features {\n"
            + "      hasEcommerce\n"
            + "    }\n"
            + "    chiefLogin\n"
            + "    wallets {\n"
            + "      id\n"
            + "      sum\n"
            + "      currency\n"
            + "    }\n"
            + "    campaigns(input: %s) {\n"
            + "      totalCount\n"
            + "      campaignIds\n"
            + "      rowset {\n"
            + "        id\n"
            + "        index\n"
            + "        name\n"
            + "      }\n"
            + "    }\n"
            + "    adGroups(input: %s) {\n"
            + "      totalCount\n"
            + "    }\n"
            + "    ads(input: %s) {\n"
            + "      totalCount\n"
            + "    }\n"
            + "    showConditions(input: %s) {\n"
            + "      totalCount\n"
            + "    }\n"
            + "  }\n"
            + "  operator {\n"
            + "    info {\n"
            + "      login\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private GdCampaignsContainer campaignsContainer;
    private GdAdGroupsContainer adGroupsContainer;
    private GdAdsContainer adsContainer;
    private GdShowConditionsContainer showConditionsContainer;
    private GridGraphQLContext context;
    private UserInfo userInfo;
    private CampaignInfo campaignInfo;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Before
    public void initTestData() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mock(HttpServletRequest.class)));
        userInfo = userSteps.createUser(generateNewUser());
        campaignInfo = campaignSteps.createActiveCampaign(userInfo.getClientInfo());

        context = ContextHelper.buildContext(userInfo.getUser());
        gridContextProvider.setGridContext(context);

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        ImmutableSet<Long> campaignIdIn = ImmutableSet.of(campaignInfo.getCampaignId());

        adGroupsContainer = getDefaultGdAdGroupsContainer();
        adGroupsContainer.getFilter().setCampaignIdIn(campaignIdIn);

        adsContainer = getDefaultGdAdsContainer();
        adsContainer.getFilter().setCampaignIdIn(campaignIdIn);

        showConditionsContainer = getDefaultGdShowConditionsContainer();
        showConditionsContainer.getFilter().setCampaignIdIn(campaignIdIn);
    }

    @After
    public void afterTest() {
        RequestContextHolder.resetRequestAttributes();
    }


    @Test
    public void testProcessor() {
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer), graphQlSerialize(adGroupsContainer),
                graphQlSerialize(adsContainer), graphQlSerialize(showConditionsContainer)
        );

        ExecutionResult result = processor.processQuery(null, query, null, context);

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = ImmutableMap.of(
                "constants", Collections.singletonMap(
                        "currencyConstants",
                        Collections.singletonList(ImmutableMap.of(
                                "code", CurrencyCode.CHF.toString(),
                                "minDailyBudget", Currencies.getCurrency(CurrencyCode.CHF).getMinDayBudget()
                        ))
                ),
                "client", getExpectedClientData(userInfo, campaignInfo),
                "operator", Collections.singletonMap("info",
                        Collections.singletonMap("login", userInfo.getUser().getLogin()))
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }


    private static Map<String, Object> getExpectedClientData(UserInfo userInfo, CampaignInfo campaignInfo) {
        return ImmutableMap.<String, Object>builder()
                .put("info", ImmutableMap.of(
                        "id", userInfo.getClientInfo().getClientId().asLong(),
                        "shard", userInfo.getShard()))
                .put("features", Collections.singletonMap("hasEcommerce", false))
                .put("chiefLogin", userInfo.getUser().getLogin())
                .put("wallets", Collections.emptyList())
                .put("campaigns", ImmutableMap.<String, Object>builder()
                        .put("totalCount", 1)
                        .put("campaignIds", Collections.singletonList(campaignInfo.getCampaignId()))
                        .put("rowset", Collections.singletonList(ImmutableMap.of(
                                "id", campaignInfo.getCampaignId(),
                                "index", 0,
                                "name", campaignInfo.getCampaign().getName()
                        )))
                        .build())
                .put("adGroups", Collections.singletonMap("totalCount", 0))
                .put("ads", Collections.singletonMap("totalCount", 0))
                .put("showConditions", Collections.singletonMap("totalCount", 0))
                .build();
    }

}
