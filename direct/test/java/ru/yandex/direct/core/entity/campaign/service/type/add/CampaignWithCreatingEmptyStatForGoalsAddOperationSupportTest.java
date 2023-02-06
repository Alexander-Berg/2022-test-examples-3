package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainerImpl;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository;
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoalId;
import ru.yandex.direct.core.entity.retargeting.model.ConversionLevel;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalStatus;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsPpcDictRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.stub.MetrikaClientStub.buildCounter;
import static ru.yandex.direct.utils.FunctionalUtils.filterToSet;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCreatingEmptyStatForGoalsAddOperationSupportTest {


    public static final int FIRST_GOAL_ID_OF_COUNTER_FIRST = 3;
    public static final int SECOND_GOAL_ID_OF_COUNTER_FIRST = 4;
    public static final int FIRST_GOAL_ID_OF_COUNTER_SECOND = 5;
    public static final int SECOND_GOAL_ID_OF_COUNTER_SECOND = 6;
    public static final int THIRD_GOAL_ID_OF_COUNTER_SECOND = 7;

    private int counterFirst;
    private int counterSecond;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignWithCreatingEmptyStatForGoalsAddOperationSupport support;

    @Autowired
    private MetrikaClientStub metrikaClient;

    @Autowired
    private MetrikaCampaignRepository metrikaCampaignRepository;

    @Autowired
    private RetargetingGoalsPpcDictRepository retargetingGoalsPpcDictRepository;

    private UserInfo userInfo;
    private RestrictedCampaignsAddOperationContainer addCampaignParametersContainer;
    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;

    @Before
    public void before() {
        counterFirst = RandomNumberUtils.nextPositiveInteger();
        counterSecond = RandomNumberUtils.nextPositiveInteger();
        userInfo = steps.userSteps().createDefaultUser();

        metrikaClient.addUserCounters(userInfo.getUid(), List.of(buildCounter(counterFirst),
                buildCounter(counterSecond)));
        metrikaClient.addCounterGoal(counterFirst, FIRST_GOAL_ID_OF_COUNTER_FIRST);
        metrikaClient.addCounterGoal(counterFirst, SECOND_GOAL_ID_OF_COUNTER_FIRST);
        metrikaClient.addCounterGoal(counterSecond, FIRST_GOAL_ID_OF_COUNTER_SECOND);
        metrikaClient.addCounterGoal(counterSecond, SECOND_GOAL_ID_OF_COUNTER_SECOND);
        metrikaClient.addCounterGoal(counterSecond, THIRD_GOAL_ID_OF_COUNTER_SECOND);

        metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClient, List.of(userInfo.getUid()),
                Set.of());

        addCampaignParametersContainer = new RestrictedCampaignsAddOperationContainerImpl(
                userInfo.getShard(),
                userInfo.getClientInfo().getClient().getChiefUid(),
                userInfo.getClientId(),
                userInfo.getClientInfo().getClient().getChiefUid(),
                userInfo.getClientInfo().getClient().getChiefUid(),
                null,
                new CampaignOptions(),
                metrikaClientAdapter,
                emptyMap()
        );
    }

    @Test
    public void afterExecutionCpaCampaignWithCounters() {
        TextCampaign campaign = defaultTextCampaignWithSystemFields(userInfo.getClientInfo());
        campaign.getStrategy().setStrategyName(StrategyName.AUTOBUDGET_AVG_CPA);
        campaign.getStrategy().getStrategyData().setGoalId((long) FIRST_GOAL_ID_OF_COUNTER_FIRST);
        campaign.setMetrikaCounters(List.of((long) counterFirst, (long) counterSecond));
        var campaignInfo = steps.textCampaignSteps().createCampaign(userInfo.getClientInfo(), campaign);

        metrikaClientAdapter.setCampaignsCounterIds(List.of(campaignInfo.getTypedCampaign()));
        support.afterExecution(addCampaignParametersContainer, List.of(campaign));
        Set<CampMetrikaGoalId> actualCampaignGoals =
                metrikaCampaignRepository.getCampMetrikaGoalIdsByCampaignIds(userInfo.getShard(),
                        userInfo.getClientId(),
                        List.of(campaign.getId()));

        Set<CampMetrikaGoalId> expectedCampaignGoals = Set.of(
                new CampMetrikaGoalId()
                        .withCampaignId(campaignInfo.getId())
                        .withGoalId((long) FIRST_GOAL_ID_OF_COUNTER_FIRST),
                new CampMetrikaGoalId()
                        .withCampaignId(campaignInfo.getId())
                        .withGoalId((long) SECOND_GOAL_ID_OF_COUNTER_FIRST),
                new CampMetrikaGoalId()
                        .withCampaignId(campaignInfo.getId())
                        .withGoalId((long) FIRST_GOAL_ID_OF_COUNTER_SECOND),
                new CampMetrikaGoalId()
                        .withCampaignId(campaignInfo.getId())
                        .withGoalId((long) SECOND_GOAL_ID_OF_COUNTER_SECOND),
                new CampMetrikaGoalId()
                        .withCampaignId(campaignInfo.getId())
                        .withGoalId((long) THIRD_GOAL_ID_OF_COUNTER_SECOND)
        );
        assertThat(actualCampaignGoals)
                .containsExactlyInAnyOrder(expectedCampaignGoals.toArray(new CampMetrikaGoalId[]{}));

        checkPpcDictGoals((long) FIRST_GOAL_ID_OF_COUNTER_FIRST, (long) SECOND_GOAL_ID_OF_COUNTER_FIRST,
                (long) FIRST_GOAL_ID_OF_COUNTER_SECOND, (long) SECOND_GOAL_ID_OF_COUNTER_SECOND,
                (long) THIRD_GOAL_ID_OF_COUNTER_SECOND);
    }

    public void checkPpcDictGoals(Long... goalIds) {
        Set<Long> goalIdSet = Set.of(goalIds);
        List<Goal> actualGoals =
                retargetingGoalsPpcDictRepository.getMetrikaGoalsFromPpcDict(goalIdSet);

        Set<Goal> expectedGoals = filterToSet(getExpectedGoalsInPpcDict(), goal -> goalIdSet.contains(goal.getId()));
        assertThat(actualGoals).containsExactlyInAnyOrder(expectedGoals.toArray(new Goal[]{}));
    }

    @NotNull
    public Set<Goal> getExpectedGoalsInPpcDict() {
        return Set.of(
                (Goal) new Goal()
                        .withId((long) FIRST_GOAL_ID_OF_COUNTER_FIRST)
                        .withType(GoalType.GOAL)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.URL)
                        .withConversionLevel(ConversionLevel.UNKNOWN)
                        .withStatus(GoalStatus.ACTIVE),
                (Goal) new Goal()
                        .withId((long) SECOND_GOAL_ID_OF_COUNTER_FIRST)
                        .withType(GoalType.GOAL)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.URL)
                        .withConversionLevel(ConversionLevel.UNKNOWN)
                        .withStatus(GoalStatus.ACTIVE),
                (Goal) new Goal()
                        .withId((long) FIRST_GOAL_ID_OF_COUNTER_SECOND)
                        .withType(GoalType.GOAL)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.URL)
                        .withConversionLevel(ConversionLevel.UNKNOWN)
                        .withStatus(GoalStatus.ACTIVE),
                (Goal) new Goal()
                        .withId((long) SECOND_GOAL_ID_OF_COUNTER_SECOND)
                        .withType(GoalType.GOAL)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.URL)
                        .withConversionLevel(ConversionLevel.UNKNOWN)
                        .withStatus(GoalStatus.ACTIVE),
                (Goal) new Goal()
                        .withId((long) THIRD_GOAL_ID_OF_COUNTER_SECOND)
                        .withType(GoalType.GOAL)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.URL)
                        .withConversionLevel(ConversionLevel.UNKNOWN)
                        .withStatus(GoalStatus.ACTIVE)
        );
    }

}
