package ru.yandex.direct.grid.processing.service.goal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import graphql.ExecutionResult;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.mobileapp.model.MobileGoalConversions;
import ru.yandex.direct.core.entity.mobilegoals.repository.MobileGoalsStatisticRepository;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalsSuggestion;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.goal.mutation.GdMetrikaGoalsByCounter;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetMetrikaGoalsByCounterIdsGraphQlServiceTest {

    private static final String QUERY_TEMPLATE = ""
            + "query {\n" +
            "    getMetrikaGoalsByCounter(input: ${input}) {\n" +
            "      goals {\n" +
            "        id\n" +
            "        counterId\n" +
            "      }\n" +
            "      top1GoalId\n" +
            "      validationResult {\n" +
            "        errors {\n" +
            "          code\n" +
            "          path\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "}";

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;
    @Autowired
    private MetrikaGoalsService metrikaGoalsService;
    @Autowired
    private MobileGoalsStatisticRepository mobileGoalsStatisticRepository;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private GridGraphQLContext context;
    private ClientInfo clientInfo;
    private Long campaignId;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        User user = userService.getUser(clientInfo.getUid());
        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);

        campaignId = steps.campaignSteps().createActiveCampaign(clientInfo).getCampaignId();

        var clientId = Objects.requireNonNull(clientInfo.getClientId());
        steps.featureSteps().addClientFeature(clientId, FeatureName.ENABLE_SUGGESTION_FOR_RECOMMENDED_GOALS, true);
    }

    @After
    public void after() {
        reset(metrikaGoalsService);
    }

    @Test
    public void get_success() {
        int firstCounterId = RandomNumberUtils.nextPositiveInteger();
        int firstGoalId = RandomNumberUtils.nextPositiveInteger();
        metrikaClientStub.addUserCounter(clientInfo.getUid(), firstCounterId);
        metrikaClientStub.addCounterGoal(firstCounterId, new CounterGoal().withId(firstGoalId));

        int secondCounterId = RandomNumberUtils.nextPositiveInteger();
        int secondGoalId = RandomNumberUtils.nextPositiveInteger();
        metrikaClientStub.addCounterGoal(secondCounterId, new CounterGoal().withId(secondGoalId));

        var goalsFromMetrika = Set.of((Goal) (new Goal().withId((long) firstGoalId).withCounterId(firstCounterId)));

        ArgumentCaptor<Collection<Long>> availableCounterIds = ArgumentCaptor.forClass(Collection.class);
        doReturn(goalsFromMetrika).when(metrikaGoalsService).getMetrikaGoalsByCounters(
                anyLong(),
                any(),
                availableCounterIds.capture(),
                any(),
                any(),
                any(),
                anyBoolean(),
                anyBoolean()
        );

        doReturn(new GoalsSuggestion()
                .withSortedGoalsToSuggestion(List.copyOf(goalsFromMetrika))
                .withTop1GoalId(null))
                .when(metrikaGoalsService)
                .getGoalsSuggestion(Mockito.any(), eq(goalsFromMetrika));

        var input = new GdMetrikaGoalsByCounter()
                .withCampaignId(campaignId)
                .withCampaignType(GdCampaignType.TEXT)
                .withCounterIds(List.of((long) firstCounterId, (long) secondCounterId));
        ExecutionResult result = executeRequest(input);
        Map<Object, Object> data = result.getData();
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(data).isNotEmpty();
            List<Map<String, Object>> goals = getDataValue(data, "getMetrikaGoalsByCounter/goals");
            sa.assertThat(goals).hasSize(1);
            Map<String, Object> firstGoal = goals.get(0);
            sa.assertThat(firstGoal.get("id")).isEqualTo((long) firstGoalId);
            sa.assertThat(firstGoal.get("counterId")).isEqualTo(firstCounterId);
            sa.assertThat(availableCounterIds.getValue()).hasSize(1);
            sa.assertThat(availableCounterIds.getValue().toArray()[0]).isEqualTo((long) firstCounterId);

            Map<String, Object> vr = getDataValue(data, "getMetrikaGoalsByCounter/validationResult/errors/0");
            sa.assertThat(vr.get("code")).isEqualTo("CampaignDefectIds.Gen.METRIKA_COUNTER_IS_UNAVAILABLE");
            sa.assertThat(vr.get("path")).isEqualTo("counterIds[1]");
        });
    }

    @Test
    public void getMetrikaGoalsByCounterWithRecommendedGoals() {
        int counterId = RandomNumberUtils.nextPositiveInteger();

        // у цели не должен быть тип MOBILE, т.е. id не из [1_900_000_000L .. 2_000_000_000L]
        int goalId = RandomNumberUtils.nextPositiveInteger(1_900_000_000);

        Goal goal = initTestDataForGoalsSuggestion(counterId, goalId, false);

        long visitsCount = RandomNumberUtils.nextPositiveInteger(10000) + 20;
        metrikaClientStub.addConversionVisitsCountToGoalIdForTwoWeeks(counterId, (long) goalId, visitsCount);

        doReturn(new GoalsSuggestion()
                .withSortedGoalsToSuggestion(List.of(goal))
                .withTop1GoalId(goal.getId()))
                .when(metrikaGoalsService).getGoalsSuggestion(Mockito.any(), eq(Set.of(
                        (Goal) new Goal()
                                .withId((long) goalId)
                                .withMetrikaCounterGoalType(MetrikaCounterGoalType.EMAIL)
                                .withConversionVisitsCount(visitsCount))));

        var input = new GdMetrikaGoalsByCounter()
                .withCampaignId(campaignId)
                .withCampaignType(GdCampaignType.TEXT)
                .withCounterIds(List.of((long) counterId));
        ExecutionResult result = executeRequest(input);
        Map<Object, Object> data = result.getData();

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(data).isNotEmpty();
            List<Map<String, Object>> resultGoals = getDataValue(data, "getMetrikaGoalsByCounter/goals");
            sa.assertThat(resultGoals).hasSize(1);

            Long top1GoalId = getDataValue(data, "getMetrikaGoalsByCounter/top1GoalId");
            sa.assertThat(top1GoalId).isEqualTo(goalId);
        });
    }

    @Test
    public void getMetrikaGoalsByCounterWithRecommendedGoals_withGoalTypeIsMobile() {
        int counterId = RandomNumberUtils.nextPositiveInteger();

        // goal type is MOBILE
        int goalId = 1_900_000_000 + RandomNumberUtils.nextPositiveInteger(100_000_000);

        Goal goal = initTestDataForGoalsSuggestion(counterId, goalId, true);

        long visitsCount = RandomNumberUtils.nextPositiveInteger(10000) + 20;
        doReturn(List.of(new MobileGoalConversions(goalId, visitsCount, 0, 0)))
                .when(mobileGoalsStatisticRepository).getEventsStats(eq(Set.of((long) goalId)), Mockito.anyInt());


        doReturn(new GoalsSuggestion()
                .withSortedGoalsToSuggestion(List.of(goal))
                .withTop1GoalId(goal.getId()))
                .when(metrikaGoalsService).getGoalsSuggestion(Mockito.any(), eq(Set.of(
                        (Goal) new Goal()
                                .withId((long) goalId)
                                .withMetrikaCounterGoalType(MetrikaCounterGoalType.EMAIL)
                                .withConversionVisitsCount(visitsCount)
                                .withIsMobileGoal(true)
                                .withHasRevenue(false))));

        var input = new GdMetrikaGoalsByCounter()
                .withCampaignId(campaignId)
                .withCampaignType(GdCampaignType.TEXT)
                .withCounterIds(List.of((long) counterId));
        ExecutionResult result = executeRequest(input);
        Map<Object, Object> data = result.getData();

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(data).isNotEmpty();
            List<Map<String, Object>> resultGoals = getDataValue(data, "getMetrikaGoalsByCounter/goals");
            sa.assertThat(resultGoals).hasSize(1);

            Long top1GoalId = getDataValue(data, "getMetrikaGoalsByCounter/top1GoalId");
            sa.assertThat(top1GoalId).isEqualTo(goalId);
        });
    }

    private Goal initTestDataForGoalsSuggestion(int counterId, int goalId, boolean isMobileGoal) {
        metrikaClientStub.addUserCounter(clientInfo.getUid(), counterId);

        CounterGoal counterGoal = new CounterGoal().withId(goalId).withType(CounterGoal.Type.EMAIL);
        metrikaClientStub.addCounterGoal(counterId, counterGoal);

        Goal goal = (Goal) new Goal()
                .withId((long) goalId)
                .withMetrikaCounterGoalType(MetrikaCounterGoalType.EMAIL);

        if (isMobileGoal) {
            goal.setIsMobileGoal(true);
        }

        doReturn(Set.of(goal))
                .when(metrikaGoalsService).getMetrikaGoalsByCounters(
                        anyLong(), any(), anyCollection(), anySet(), any(), any(), anyBoolean(), anyBoolean());

        doReturn(Set.of(goal))
                .when(metrikaGoalsService).getAvailableMetrikaGoalsForClient(anyLong(), any(), anySet());

        return goal;

    }

    private ExecutionResult executeRequest(GdMetrikaGoalsByCounter input) {
        String query = StrSubstitutor.replace(
                QUERY_TEMPLATE, Map.of("input", GraphQlJsonUtils.graphQlSerialize(input))
        );

        ExecutionResult result = processor.processQuery(null, query, null, context);
        GraphQLUtils.logErrors(result.getErrors());
        assertThat(result.getErrors()).isEmpty();
        return result;
    }
}
