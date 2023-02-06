package ru.yandex.direct.core.entity.metrika.service;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository;
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterWithAdditionalInformation;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsPpcDictRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_ECOMMERCE_BASE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaGoalsServiceGetAvailableMetrikaGoalsForClientTest {

    @Autowired
    private MetrikaGoalsService metrikaGoalsService;

    @Autowired
    private MetrikaCampaignRepository metrikaCampaignRepository;

    @Autowired
    private RetargetingGoalsPpcDictRepository retargetingGoalsPpcDictRepository;

    @Autowired
    private Steps steps;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;


    private int shard;
    private ClientId clientId;
    private Long campaignId;
    private Long operatorUid;
    private MetrikaCounterWithAdditionalInformation metrikaCounter;

    private static Long metrikaCounterId = 1234L;
    private static Long metrikaGoalId = 1234L;

    public static final CounterGoal.Type COUNTER_GOAL_TYPE = CounterGoal.Type.NUMBER;

    @Before
    public void before() {
        UserInfo defaultUser = steps.userSteps().createDefaultUser();
        var textCampaign = steps.campaignSteps().createActiveTextCampaign(defaultUser.getClientInfo());
        campaignId = textCampaign.getCampaignId();
        shard = defaultUser.getShard();
        clientId = defaultUser.getClientId();
        operatorUid = defaultUser.getUid();

        metrikaCounterId++;
        metrikaGoalId++;
        metrikaClientStub.addUserCounter(defaultUser.getUid(), metrikaCounterId.intValue());
        metrikaClientStub.addCounterGoal(metrikaCounterId.intValue(), new CounterGoal()
                .withId(metrikaGoalId.intValue())
                .withType(COUNTER_GOAL_TYPE));
        metrikaCounter = new MetrikaCounterWithAdditionalInformation()
                .withId(metrikaCounterId)
                .withHasEcommerce(false);
    }

    @Test
    public void get_Success() {
        Set<Goal> actualGoals = metrikaGoalsService.getAvailableMetrikaGoalsForClient(operatorUid, clientId,
                Set.of(metrikaCounter));

        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void get_Success_NoEcommerce_WhenColdStartOff() {
        MetrikaCounterWithAdditionalInformation ecommerceCounter =
                new MetrikaCounterWithAdditionalInformation()
                        .withId((long) RandomUtils.nextInt())
                        .withHasEcommerce(true);

        steps.featureSteps().addClientFeature(clientId, FeatureName.COLD_START_FOR_ECOMMERCE_GOALS, false);
        Set<Goal> actualGoals = metrikaGoalsService.getAvailableMetrikaGoalsForClient(operatorUid, clientId,
                Set.of(metrikaCounter, ecommerceCounter));

        assertThat(actualGoals).hasSize(1);

        Set<Long> actualGoalsIds = mapSet(actualGoals, Goal::getId);
        assertThat(actualGoalsIds).containsExactlyInAnyOrder(metrikaGoalId);
    }

    @Test
    public void get_Success_GetEcommerce_WhenColdStartOn() {
        MetrikaCounterWithAdditionalInformation ecommerceCounter =
                new MetrikaCounterWithAdditionalInformation()
                        .withId((long) RandomUtils.nextInt())
                        .withHasEcommerce(true);
        Long ecommerceGoalId = METRIKA_ECOMMERCE_BASE + ecommerceCounter.getId();

        steps.featureSteps().addClientFeature(clientId, FeatureName.COLD_START_FOR_ECOMMERCE_GOALS, true);
        Set<Goal> actualGoals = metrikaGoalsService.getAvailableMetrikaGoalsForClient(operatorUid, clientId,
                Set.of(metrikaCounter, ecommerceCounter));

        assertThat(actualGoals).hasSize(2);

        Set<Long> actualGoalsIds = mapSet(actualGoals, Goal::getId);
        assertThat(actualGoalsIds).containsExactlyInAnyOrder(ecommerceGoalId, metrikaGoalId);
    }

    @Test
    public void get_Success_CheckVisitCount() {
        long visitsCount = 19L;
        metrikaClientStub.addConversionVisitsCountToGoalIdForTwoWeeks(metrikaCounterId.intValue(), metrikaGoalId,
                visitsCount);

        Set<Goal> actualGoals = metrikaGoalsService.getAvailableMetrikaGoalsForClient(operatorUid, clientId,
                Set.of(metrikaCounter));

        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void get_WithoutCampaign_Success() {
        Set<Goal> actualGoals = metrikaGoalsService.getAvailableMetrikaGoalsForClient(operatorUid, clientId,
                Set.of(metrikaCounter));

        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void get_Success_CampMetrikaGoalsUpdatedWhenExisted() {
        addToCampMetrikaGoalsRepository(campaignId, metrikaGoalId);
        Set<Long> goalIdsFromCampMetrikaGoals = metrikaCampaignRepository.getGoalIds(shard, clientId.asLong(),
                List.of(campaignId));
        assertThat(goalIdsFromCampMetrikaGoals).contains(metrikaGoalId);

        Set<Goal> actualGoals = metrikaGoalsService.getAvailableMetrikaGoalsForClient(operatorUid, clientId,
                Set.of(metrikaCounter));
        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));

        goalIdsFromCampMetrikaGoals = metrikaCampaignRepository.getGoalIds(shard, clientId.asLong(),
                List.of(campaignId));
        assertThat(goalIdsFromCampMetrikaGoals).contains(metrikaGoalId);
    }

    @Test
    public void get_Success_MetrikaGoalsPpcdictUpdatedWhenExisted() {
        addToMetrikaGoalsPpcdictRepository(metrikaGoalId);
        List<Goal> goalIdsFromMetrikaGoals =
                retargetingGoalsPpcDictRepository.getMetrikaGoalsFromPpcDict(List.of(metrikaGoalId));
        assertThat(goalIdsFromMetrikaGoals).isNotEmpty();

        Set<Goal> actualGoals = metrikaGoalsService.getAvailableMetrikaGoalsForClient(operatorUid, clientId,
                Set.of(metrikaCounter));
        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));

        goalIdsFromMetrikaGoals = retargetingGoalsPpcDictRepository.getMetrikaGoalsFromPpcDict(List.of(metrikaGoalId));
        assertThat(goalIdsFromMetrikaGoals).isNotEmpty();

        Goal expectedPpcDictGoal = (Goal) new Goal()
                .withId(metrikaGoalId)
                .withMetrikaCounterGoalType(MetrikaCounterGoalType.NUMBER);

        assertThat(goalIdsFromMetrikaGoals.get(0)).is(matchedBy(beanDiffer(expectedPpcDictGoal)
                .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void get_Success_MetrikaGoalsPpcdictNotUpdatedWhenEmpty_ReedOnly() {
        List<Goal> goalIdsFromMetrikaGoals =
                retargetingGoalsPpcDictRepository.getMetrikaGoalsFromPpcDict(List.of(metrikaGoalId));
        assertThat(goalIdsFromMetrikaGoals).isEmpty();

        Set<Goal> actualGoals = metrikaGoalsService.getAvailableMetrikaGoalsForClient(operatorUid, clientId,
                Set.of(metrikaCounter));
        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));

        goalIdsFromMetrikaGoals = retargetingGoalsPpcDictRepository.getMetrikaGoalsFromPpcDict(List.of(metrikaGoalId));
        assertThat(goalIdsFromMetrikaGoals).isEmpty();
    }

    @Test
    public void get_Success_MetrikaGoalsPpcdictNotUpdatedWhenExisted_ReedOnly() {
        addToMetrikaGoalsPpcdictRepository(metrikaGoalId);
        List<Goal> goalIdsFromMetrikaGoals =
                retargetingGoalsPpcDictRepository.getMetrikaGoalsFromPpcDict(List.of(metrikaGoalId));
        assertThat(goalIdsFromMetrikaGoals).isNotEmpty();

        Set<Goal> actualGoals = metrikaGoalsService.getAvailableMetrikaGoalsForClient(operatorUid, clientId,
                Set.of(metrikaCounter));
        assertThat(actualGoals).hasSize(1);
        Goal actualGoal = actualGoals.iterator().next();

        Goal expectedGoal = (Goal) new Goal()
                .withId(metrikaGoalId);

        assertThat(actualGoal).is(matchedBy(beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields())));

        goalIdsFromMetrikaGoals = retargetingGoalsPpcDictRepository.getMetrikaGoalsFromPpcDict(List.of(metrikaGoalId));
        assertThat(goalIdsFromMetrikaGoals).isNotEmpty();

        Goal expectedPpcDictGoal = (Goal) new Goal()
                .withId(metrikaGoalId)
                .withMetrikaCounterGoalType(MetrikaCounterGoalType.NUMBER);

        assertThat(goalIdsFromMetrikaGoals.get(0)).is(matchedBy(beanDiffer(expectedPpcDictGoal)
                .useCompareStrategy(onlyExpectedFields())));
    }

    private void addToCampMetrikaGoalsRepository(long campaignId, long goalId) {
        metrikaCampaignRepository.addGoalIds(shard, campaignId, Set.of(goalId));
    }

    private void addToMetrikaGoalsPpcdictRepository(long goalId) {
        Set<Goal> goals = Set.of((Goal) new Goal()
                .withId(goalId)
                .withMetrikaCounterGoalType(MetrikaCounterGoalType.NUMBER));

        retargetingGoalsPpcDictRepository.addMetrikaGoalsToPpcDict(goals);
    }
}
