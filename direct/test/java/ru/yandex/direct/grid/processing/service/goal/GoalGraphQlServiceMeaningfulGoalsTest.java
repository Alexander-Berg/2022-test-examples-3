package ru.yandex.direct.grid.processing.service.goal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.goal.GdMeaningfulGoalContainer;
import ru.yandex.direct.grid.processing.model.goal.GdMeaningfulGoalsContainer;
import ru.yandex.direct.grid.processing.model.goal.GoalSubtype;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdCampaignType;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class GoalGraphQlServiceMeaningfulGoalsTest {

    private static final String QUERY_TEMPLATE = ""
            + "query {\n" +
            "  client(searchBy: {\n" +
            "    login: \"${login}\"\n" +
            "  }) {\n" +
            "    meaningfulGoals(input: ${input}) {\n" +
            "      campaignGoals{\n" +
            "        campaignId\n" +
            "        goals {\n" +
            "          id\n" +
            "          domain\n" +
            "          name\n" +
            "          type\n" +
            "          metrikaGoalType\n" +
            "          subtype\n" +
            "          counterId\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    public static final long CAMPAIGN_ID = 1L;
    public static final int COUNTER_ID = 123;
    private static final CampaignType CAMPAIGN_TYPE = CampaignType.TEXT;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignGoalsService campaignGoalsService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    private GridGraphQLContext context;
    private UserInfo userInfo;

    @Before
    public void setUp() {
        userInfo = steps.userSteps().createUser(generateNewUser());

        metrikaClientStub.addUserCounter(userInfo.getUid(), COUNTER_ID);

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
    }

    private void prepareCampaignsGoalsServiceToReturn(Goal goal) {
        doReturn(Map.of(CAMPAIGN_ID, singleton(goal))).when(campaignGoalsService)
                .getAvailableGoalsForCampaignId(
                        eq(userInfo.getUid()),
                        eq(userInfo.getClientId()),
                        anyMap(),
                        eq(null)
                );
    }

    @Test
    public void meaningfulGoals_success() {
        // Prepare
        long goalId = 1L;
        MetrikaCounterGoalType goalType = MetrikaCounterGoalType.URL;
        GoalSubtype subtype = GoalSubtype.METRIKA_GOAL_ID;
        Goal goal = (Goal) new Goal()
                .withId(goalId)
                .withDomain("ya.ru")
                .withName(RandomStringUtils.randomAlphabetic(5) + 1)
                .withType(GoalType.GOAL)
                .withMetrikaCounterGoalType(goalType)
                .withSubtype(subtype.getTypedValue())
                .withCounterId(COUNTER_ID);
        prepareCampaignsGoalsServiceToReturn(goal);

        // Execute
        ExecutionResult result = executeRequest();
        Map<Object, Object> data = result.getData();

        // Check
        Map<String, Object> expected = Map.of(
                "client", Map.of(
                        "meaningfulGoals", Map.of(
                                "campaignGoals", List.of(Map.of(
                                        "campaignId", CAMPAIGN_ID,
                                        "goals", List.of(Map.of(
                                                "id", goal.getId(),
                                                "domain", goal.getDomain(),
                                                "name", goal.getName(),
                                                "type", goal.getType().name(),
                                                "metrikaGoalType", goal.getMetrikaCounterGoalType().name(),
                                                "subtype", subtype.name(),
                                                "counterId", goal.getCounterId()
                                        ))
                                ))
                        )
                )
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @NotNull
    private ExecutionResult executeRequest() {
        GdMeaningfulGoalContainer goalContainer =
                new GdMeaningfulGoalContainer()
                        .withCampaignId(CAMPAIGN_ID)
                        .withCampaignType(toGdCampaignType(CAMPAIGN_TYPE))
                        .withCounterIds(singleton((long) COUNTER_ID));
        GdMeaningfulGoalsContainer goalsContainer = new GdMeaningfulGoalsContainer()
                .withMeaningfulGoalContainers(Collections.singletonList(goalContainer));

        String query = StrSubstitutor.replace(
                QUERY_TEMPLATE, Map.of(
                        "login", context.getOperator().getLogin(),
                        "input", GraphQlJsonUtils.graphQlSerialize(goalsContainer)
                ));

        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());
        assertThat(result.getErrors()).isEmpty();
        return result;
    }
}
