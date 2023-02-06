package ru.yandex.direct.core.entity.metrika.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsPpcDictRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;


@CoreTest
@RunWith(SpringRunner.class)
public class MetrikaGoalsServiceGetByCounterTest {

    @Autowired
    private MetrikaGoalsService metrikaGoalsService;

    @Autowired
    private RetargetingGoalsPpcDictRepository retargetingGoalsPpcDictRepository;

    @Autowired
    private Steps steps;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClient;

    private ClientId clientId;
    private Long operatorUid;
    private Long campaignId;
    private CampaignType campaignType;

    private static Long metrikaCounterId = 1234L;
    private static Long metrikaGoalId = 1234L;

    private static final Long UNAVAILABLE_COUNTER_ID = 111L;
    private static final Long UNAVAILABLE_GOAL_ID = 1111L;

    public static final CounterGoal.Type COUNTER_GOAL_TYPE = CounterGoal.Type.NUMBER;

    @Before
    public void before() {
        UserInfo defaultUser = steps.userSteps().createDefaultUser();
        var textCampaign = steps.campaignSteps().createActiveTextCampaign(defaultUser.getClientInfo());
        campaignId = textCampaign.getCampaignId();
        campaignType = textCampaign.getCampaign().getType();
        clientId = defaultUser.getClientId();
        operatorUid = defaultUser.getUid();

        metrikaCounterId++;
        metrikaGoalId++;
        metrikaClient.addUserCounter(defaultUser.getUid(), metrikaCounterId.intValue());
        metrikaClient.addCounterGoal(metrikaCounterId.intValue(), new CounterGoal()
                .withId(metrikaGoalId.intValue())
                .withType(COUNTER_GOAL_TYPE));

        metrikaClient.addUnavailableCounter(UNAVAILABLE_COUNTER_ID);
        metrikaClient.addCounterGoal(UNAVAILABLE_COUNTER_ID.intValue(), new CounterGoal()
                .withId(UNAVAILABLE_GOAL_ID.intValue())
                .withType(COUNTER_GOAL_TYPE)
                .withSource(CounterGoal.Source.AUTO));
    }

    @Test
    public void Get_Success() {
        Set<Goal> actualGoals = metrikaGoalsService.getMetrikaGoalsByCounter(operatorUid, clientId,
                List.of(metrikaCounterId), campaignId, campaignType);

        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void Get_Success_CheckVisitCount() {
        long visitsCount = 19L;
        metrikaClient.addConversionVisitsCountToGoalIdForTwoWeeks(metrikaCounterId.intValue(), metrikaGoalId,
                visitsCount);

        Set<Goal> actualGoals = metrikaGoalsService.getMetrikaGoalsByCounter(operatorUid, clientId,
                List.of(metrikaCounterId), campaignId, campaignType);

        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void Get_WithoutCampaign_Success() {
        Set<Goal> actualGoals = metrikaGoalsService.getMetrikaGoalsByCounter(operatorUid, clientId,
                List.of(metrikaCounterId), null, campaignType);

        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void Get_Success_MetrikaGoalsPpcdictNotUpdatedWhenEmpty() {
        // проверяем, что в METRIKA_GOALS нет целей для кампании
        List<Goal> goalIdsFromMetrikaGoals =
                retargetingGoalsPpcDictRepository.getMetrikaGoalsFromPpcDict(List.of(metrikaGoalId));
        assertThat(goalIdsFromMetrikaGoals).isEmpty();

        // проверяем что получили корректную цель из метрики
        Set<Goal> actualGoals = metrikaGoalsService.getMetrikaGoalsByCounter(operatorUid, clientId,
                List.of(metrikaCounterId), campaignId, campaignType);
        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));

        goalIdsFromMetrikaGoals = retargetingGoalsPpcDictRepository.getMetrikaGoalsFromPpcDict(List.of(metrikaGoalId));
        assertThat(goalIdsFromMetrikaGoals).isEmpty();
    }

    @Test
    public void Get_Success_MetrikaGoalsPpcdictNotUpdatedWhenExisted() {
        // проверяем случай, когда METRIKA_GOALS уже есть цель из метрики
        addToMetrikaGoalsPpcdictRepository(metrikaGoalId);
        List<Goal> goalIdsFromMetrikaGoals =
                retargetingGoalsPpcDictRepository.getMetrikaGoalsFromPpcDict(List.of(metrikaGoalId));
        assertThat(goalIdsFromMetrikaGoals).isNotEmpty();

        // проверяем что получили корректную цель из метрики
        Set<Goal> actualGoals = metrikaGoalsService.getMetrikaGoalsByCounter(operatorUid, clientId,
                List.of(metrikaCounterId), campaignId, campaignType);
        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));

        // проверяем, что в METRIKA_GOALS есть цель, которую получили из метрики
        goalIdsFromMetrikaGoals = retargetingGoalsPpcDictRepository.getMetrikaGoalsFromPpcDict(List.of(metrikaGoalId));
        assertThat(goalIdsFromMetrikaGoals).isNotEmpty();

        Goal expectedPpcDictGoal = (Goal) new Goal()
                .withId(metrikaGoalId)
                .withMetrikaCounterGoalType(MetrikaCounterGoalType.NUMBER);

        assertThat(goalIdsFromMetrikaGoals.get(0)).is(matchedBy(beanDiffer(expectedPpcDictGoal)
                .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void Get_UnavailableGoals_Success() {
        Set<Goal> actualGoals = metrikaGoalsService.getMetrikaGoalsByCounters(
                operatorUid,
                clientId,
                List.of(metrikaCounterId, UNAVAILABLE_COUNTER_ID),
                Set.of(),
                Map.of(campaignId, campaignType),
                new ForCampaignType(campaignType),
                false,
                false
        );

        assertThat(actualGoals).hasSize(2);
        Set<Long> actualGoalIds = listToSet(actualGoals, Goal::getId);
        assertThat(actualGoalIds).containsExactlyInAnyOrder(metrikaGoalId, UNAVAILABLE_GOAL_ID);
    }

    private void addToMetrikaGoalsPpcdictRepository(long goalId) {
        Set<Goal> goals = Set.of((Goal) new Goal()
                .withId(goalId)
                .withMetrikaCounterGoalType(MetrikaCounterGoalType.NUMBER));

        retargetingGoalsPpcDictRepository.addMetrikaGoalsToPpcDict(goals);
    }
}
