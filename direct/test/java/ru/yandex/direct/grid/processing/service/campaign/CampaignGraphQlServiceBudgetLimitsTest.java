package ru.yandex.direct.grid.processing.service.campaign;

import java.time.LocalDate;
import java.util.List;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCpmCampaignDayBudgetLimitsRequestItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignGraphQlServiceBudgetLimitsTest {
    private static final String QUERY_TEMPLATE = "{\n" +
            " client(searchBy: {login: \"%s\"}) {\n" +
            "   cpmCampaignBudgetLimits(input: {\n" +
            "      items: %s" +
            "    }) {\n" +
            "      limits {\n" +
            "        maximumBudget\n" +
            "        minimalBudget\n" +
            "      }\n" +
            "    }\n" +
            "    __typename\n" +
            "  }\n" +
            "}";

    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    private UserInfo defaultUser;

    @Before
    public void before() {
        defaultUser = steps.userSteps().createDefaultUser();
        context = ContextHelper.buildContext(defaultUser.getUser())
                .withFetchedFieldsReslover(null);
    }

    @Test
    public void getCpmCampaignBudgetLimits() {
        GdCpmCampaignDayBudgetLimitsRequestItem item = new GdCpmCampaignDayBudgetLimitsRequestItem()
                .withFinishDate(LocalDate.now().plusDays(10))
                .withStartDate(LocalDate.now().plusDays(1))
                .withIsRestarting(false);
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(List.of(item)));

        ExecutionResult result = processor.processQuery(null, query, null, context);
        assertThat(result.getErrors())
                .isEmpty();
    }

}
