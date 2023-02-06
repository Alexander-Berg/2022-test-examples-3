package ru.yandex.direct.grid.processing.service.campaign;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
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
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetAverageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignsGraphQlService.STRATEGY_LEARNING_STATUS;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class CampaignGraphQlConversionStrategyLearningStatusTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        ... on GdTextCampaign {\n"
            + "          strategyLearningStatus {\n"
            + "            status\n"
            + "            restartDate\n"
            + "            conversionCount\n"
            + "            conversionRate\n"
            + "            averageConversion\n"
            + "            statisticCalculationStartTime\n"
            + "          }"
            + "        }"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @Autowired
    public Steps steps;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    private GdCampaignsContainer campaignsContainer;

    private UserInfo userInfo;
    private ClientInfo clientInfo;
    private GridGraphQLContext context;

    @Before
    public void before() {
        userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();

        TextCampaign textCampaign = defaultTextCampaignWithSystemFields(clientInfo);
        textCampaign.withStrategy(defaultAutobudgetAverageCpaStrategy(RandomNumberUtils.nextPositiveLong()));

        steps.textCampaignSteps().createCampaign(clientInfo, textCampaign);

        campaignsContainer.getFilter().setCampaignIdIn(Set.of(textCampaign.getId()));
    }

    @Test
    public void getStrategyLearningStatus() {
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));

        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();

        var expectedStatusData = new HashMap<>();
        expectedStatusData.put(STRATEGY_LEARNING_STATUS, null);

        Map<String, Object> expected = Collections.singletonMap(
                "client", ImmutableMap.of(
                        "campaigns", Map.of(
                                "rowset", Collections.singletonList(
                                        expectedStatusData
                                )
                        )
                )
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

}
