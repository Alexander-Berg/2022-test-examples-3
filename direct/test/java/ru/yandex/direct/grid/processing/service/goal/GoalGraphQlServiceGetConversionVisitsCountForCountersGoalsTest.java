package ru.yandex.direct.grid.processing.service.goal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class GoalGraphQlServiceGetConversionVisitsCountForCountersGoalsTest {
    private static final int COUNTER_ID = 123;
    private static final long GOAL_ID = RandomNumberUtils.nextPositiveInteger();

    private static final String QUERY_TEMPLATE = ""
            + "query {\n" +
            "  client(searchBy: {\n" +
            "    login: \"${login}\"\n" +
            "  }) {\n" +
            "    conversionVisitsCountForCountersGoals(input: ${input}) {\n" +
            "      goalsConversionVisitsCount {" +
            "        id" +
            "        conversionVisitsCount" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Autowired
    Steps steps;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private MetrikaGoalsService metrikaGoalsService;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    private GridGraphQLContext context;
    private UserInfo userInfo;

    @Before
    public void before() {
        userInfo = steps.userSteps().createUser(generateNewUser());

        metrikaClientStub.addUserCounter(userInfo.getUid(), COUNTER_ID);

        Goal goal = new Goal();
        goal.withName("goal1")
                .withId(GOAL_ID)
                .withType(GoalType.GOAL)
                .withCounterId(COUNTER_ID);

        doReturn(Set.of(goal)).
                when(metrikaGoalsService).getAvailableMetrikaGoalsForClient(eq(userInfo.getUid()),
                        eq(userInfo.getClientId()),
                        any());

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
    }

    @Test
    public void getGoalsConversionVisitsCount() {
        long visitsCount = 156L;
        metrikaClientStub.addCounterGoal(COUNTER_ID, new CounterGoal()
                .withId((int) GOAL_ID)
                .withType(CounterGoal.Type.URL));
        metrikaClientStub.addConversionVisitsCountToGoalIdForTwoWeeks(COUNTER_ID, GOAL_ID,
                visitsCount);
        ExecutionResult result = executeRequest(List.of(COUNTER_ID));
        Map<Object, Object> expected = Map.of("client",
                Map.of("conversionVisitsCountForCountersGoals",
                        Map.of("goalsConversionVisitsCount", List.of(Map.of("id", GOAL_ID, "conversionVisitsCount",
                                visitsCount)))));
        Map<Object, Object> data = result.getData();
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getGoalsConversionVisitsCountWithEmptyCounters() {
        long visitsCount = 156L;
        metrikaClientStub.addCounterGoal(COUNTER_ID, new CounterGoal()
                .withId((int) GOAL_ID)
                .withType(CounterGoal.Type.URL));
        metrikaClientStub.addConversionVisitsCountToGoalIdForTwoWeeks(COUNTER_ID, GOAL_ID,
                visitsCount);
        ExecutionResult result = executeRequest(Collections.emptyList());
        Map<Object, Object> expected = Map.of("client",
                Map.of("conversionVisitsCountForCountersGoals",
                        Map.of("goalsConversionVisitsCount", List.of(Map.of("id", GOAL_ID, "conversionVisitsCount",
                                visitsCount)))));
        Map<Object, Object> data = result.getData();
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @NotNull
    private ExecutionResult executeRequest(List<Integer> counterIds) {

        String query = StrSubstitutor.replace(
                QUERY_TEMPLATE, Map.of(
                        "login", context.getOperator().getLogin(),
                        "input", GraphQlJsonUtils.graphQlSerialize(counterIds)
                ));

        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());
        assertThat(result.getErrors()).isEmpty();
        return result;
    }
}
