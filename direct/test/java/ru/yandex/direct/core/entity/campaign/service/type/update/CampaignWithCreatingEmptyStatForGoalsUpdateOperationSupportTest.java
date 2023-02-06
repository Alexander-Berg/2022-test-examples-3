package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCreatingEmptyStatForGoals;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository;
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoal;
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoalId;
import ru.yandex.direct.core.entity.retargeting.model.ConversionLevel;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalRole;
import ru.yandex.direct.core.entity.retargeting.model.GoalStatus;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsPpcDictRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TypedCampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.stub.MetrikaClientStub.buildCounter;
import static ru.yandex.direct.utils.FunctionalUtils.filterToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCreatingEmptyStatForGoalsUpdateOperationSupportTest {

    public static final int FIRST_GOAL_ID_OF_COUNTER_FIRST = 101;
    public static final int SECOND_GOAL_ID_OF_COUNTER_FIRST = 102;
    public static final int FIRST_GOAL_ID_OF_COUNTER_SECOND = 201;
    public static final int SECOND_GOAL_ID_OF_COUNTER_SECOND = 202;
    public static final int THIRD_GOAL_ID_OF_COUNTER_SECOND = 203;

    private int counterFirst;
    private int counterSecond;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignWithCreatingEmptyStatForGoalsUpdateOperationSupport support;

    @Autowired
    private MetrikaClientStub metrikaClient;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private MetrikaCampaignRepository metrikaCampaignRepository;

    @Autowired
    private RetargetingGoalsPpcDictRepository retargetingGoalsPpcDictRepository;

    private UserInfo userInfo;
    private RestrictedCampaignsUpdateOperationContainer updateCampaignParametersContainer;
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

        metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClient, List.of(userInfo.getUid()), Set.of());

        updateCampaignParametersContainer = new RestrictedCampaignsUpdateOperationContainerImpl(
                userInfo.getShard(),
                userInfo.getClientInfo().getClient().getChiefUid(),
                userInfo.getClientId(),
                userInfo.getClientInfo().getClient().getChiefUid(),
                userInfo.getClientInfo().getClient().getChiefUid(),
                metrikaClientAdapter,
                new CampaignOptions(),
                null,
                emptyMap()
        );
    }

    @Test
    public void updateRelatedEntitiesOutOfTransactionWithModelChanges_addOneCounter() {
        TypedCampaignInfo campaignInfo = createTestCampaignWithMetrikaCounters(
                emptyList());
        CampaignWithCreatingEmptyStatForGoals campaign = getCampaignFromRepository(campaignInfo.getId());

        ModelChanges<CampaignWithCreatingEmptyStatForGoals> modelChanges = getModelChanges(campaign,
                List.of((long) counterFirst));
        var appliedChanges = modelChanges.applyTo(campaign);
        metrikaClientAdapter.setCampaignsCounterIds(List.of(appliedChanges.getModel()));

        support.updateRelatedEntitiesOutOfTransactionWithModelChanges(updateCampaignParametersContainer,
                List.of(modelChanges),
                List.of(appliedChanges));

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
                        .withGoalId((long) SECOND_GOAL_ID_OF_COUNTER_FIRST)
        );
        assertThat(actualCampaignGoals)
                .containsExactlyInAnyOrder(expectedCampaignGoals.toArray(new CampMetrikaGoalId[]{}));

        checkPpcDictGoals((long) FIRST_GOAL_ID_OF_COUNTER_FIRST, (long) SECOND_GOAL_ID_OF_COUNTER_FIRST);
    }

    @Test
    public void updateRelatedEntitiesOutOfTransactionWithModelChanges_addSeveralCounters() {
        TypedCampaignInfo campaignInfo = createTestCampaignWithMetrikaCounters(
                emptyList());
        CampaignWithCreatingEmptyStatForGoals campaign = getCampaignFromRepository(campaignInfo.getId());

        ModelChanges<CampaignWithCreatingEmptyStatForGoals> modelChanges = getModelChanges(campaign,
                List.of((long) counterFirst, (long) counterSecond));
        var appliedChanges = modelChanges.applyTo(campaign);
        metrikaClientAdapter.setCampaignsCounterIds(List.of(appliedChanges.getModel()));

        support.updateRelatedEntitiesOutOfTransactionWithModelChanges(updateCampaignParametersContainer,
                List.of(modelChanges),
                List.of(appliedChanges));

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

    @Test
    public void updateRelatedEntitiesOutOfTransactionWithModelChanges_changeCounter() {
        //Цели для первого счетчика уже записаны. Проверяем добавятся новые цели, а старые останутся без изменений
        TypedCampaignInfo campaignInfo = createTestCampaignWithMetrikaCounters(
                List.of((long) counterFirst));

        metrikaCampaignRepository.addCampMetrikaGoals(userInfo.getShard(),
                getCampMetrikaGoalsWithTestData(campaignInfo.getId(),
                        List.of((long) FIRST_GOAL_ID_OF_COUNTER_FIRST, (long) SECOND_GOAL_ID_OF_COUNTER_FIRST)));

        CampaignWithCreatingEmptyStatForGoals campaign = getCampaignFromRepository(campaignInfo.getId());

        ModelChanges<CampaignWithCreatingEmptyStatForGoals> modelChanges = getModelChanges(campaign,
                List.of((long) counterSecond));

        var appliedChanges = modelChanges.applyTo(campaign);
        metrikaClientAdapter.setCampaignsCounterIds(List.of(appliedChanges.getModel()));

        support.updateRelatedEntitiesOutOfTransactionWithModelChanges(updateCampaignParametersContainer,
                List.of(modelChanges),
                List.of(appliedChanges));

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

    @Test
    public void updateRelatedEntitiesOutOfTransactionWithModelChanges_countersDidNotChange() {
        TypedCampaignInfo campaignInfo = createTestCampaignWithMetrikaCounters(
                List.of((long) counterFirst, (long) counterSecond));
        CampaignWithCreatingEmptyStatForGoals campaign = getCampaignFromRepository(campaignInfo.getId());

        ModelChanges<CampaignWithCreatingEmptyStatForGoals> modelChanges = getModelChanges(campaign,
                List.of((long) counterFirst, (long) counterSecond));

        var appliedChanges = modelChanges.applyTo(campaign);
        metrikaClientAdapter.setCampaignsCounterIds(List.of(appliedChanges.getModel()));

        support.updateRelatedEntitiesOutOfTransactionWithModelChanges(updateCampaignParametersContainer,
                List.of(modelChanges),
                List.of(appliedChanges));

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

    @Test
    public void updateRelatedEntitiesOutOfTransactionWithModelChanges_deleteCounter() {
        TypedCampaignInfo campaignInfo = createTestCampaignWithMetrikaCounters(
                List.of((long) counterFirst, (long) counterSecond));
        CampaignWithCreatingEmptyStatForGoals campaign = getCampaignFromRepository(campaignInfo.getId());

        ModelChanges<CampaignWithCreatingEmptyStatForGoals> modelChanges = getModelChanges(campaign,
                List.of((long) counterFirst));

        var appliedChanges = modelChanges.applyTo(campaign);
        metrikaClientAdapter.setCampaignsCounterIds(List.of(appliedChanges.getModel()));

        support.updateRelatedEntitiesOutOfTransactionWithModelChanges(updateCampaignParametersContainer,
                List.of(modelChanges),
                List.of(appliedChanges));

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
                        .withGoalId((long) SECOND_GOAL_ID_OF_COUNTER_FIRST)
        );
        assertThat(actualCampaignGoals)
                .containsExactlyInAnyOrder(expectedCampaignGoals.toArray(new CampMetrikaGoalId[]{}));

        checkPpcDictGoals((long) FIRST_GOAL_ID_OF_COUNTER_FIRST, (long) SECOND_GOAL_ID_OF_COUNTER_FIRST);
    }

    @Test
    public void updateRelatedEntitiesOutOfTransactionWithModelChanges_deleteLastCounter() {
        TypedCampaignInfo campaignInfo = createTestCampaignWithMetrikaCounters(
                List.of((long) counterFirst));
        CampaignWithCreatingEmptyStatForGoals campaign = getCampaignFromRepository(campaignInfo.getId());

        ModelChanges<CampaignWithCreatingEmptyStatForGoals> modelChanges = getModelChanges(campaign, null);

        support.updateRelatedEntitiesOutOfTransactionWithModelChanges(updateCampaignParametersContainer,
                List.of(modelChanges),
                List.of(modelChanges.applyTo(campaign)));

        Set<CampMetrikaGoalId> actualCampaignGoals =
                metrikaCampaignRepository.getCampMetrikaGoalIdsByCampaignIds(userInfo.getShard(),
                        userInfo.getClientId(),
                        List.of(campaign.getId()));

        Set<CampMetrikaGoalId> expectedCampaignGoals = Set.of();
        assertThat(actualCampaignGoals)
                .containsExactlyInAnyOrder(expectedCampaignGoals.toArray(new CampMetrikaGoalId[]{}));

        checkPpcDictGoals();
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

    private List<CampMetrikaGoal> getCampMetrikaGoalsWithTestData(Long campaignId, List<Long> goalIds) {
        return mapList(goalIds, goalId -> getTestCampMetrikaGoal(campaignId, goalId));
    }

    private static CampMetrikaGoal getTestCampMetrikaGoal(Long campaignId, Long goalId) {
        return new CampMetrikaGoal()
                .withId(new CampMetrikaGoalId()
                        .withCampaignId(campaignId)
                        .withGoalId(goalId))
                .withCampaignId(campaignId)
                .withGoalId(goalId)
                .withGoalRole(Set.of(GoalRole.SINGLE))
                .withStatDate(LocalDateTime.now().minusDays(1L))
                .withGoalsCount(100L)
                .withContextGoalsCount(200L)
                .withLinksCount(42L);
    }

    private TypedCampaignInfo createTestCampaignWithMetrikaCounters(List<Long> metrikaCounters) {
        TextCampaign campaign = defaultTextCampaignWithSystemFields(userInfo.getClientInfo());
        campaign.getStrategy().setStrategyName(StrategyName.AUTOBUDGET_AVG_CPA);
        campaign.getStrategy().getStrategyData().setGoalId((long) FIRST_GOAL_ID_OF_COUNTER_FIRST);
        if (!metrikaCounters.isEmpty()) {
            campaign.setMetrikaCounters(metrikaCounters);
        }
        return steps.typedCampaignSteps().createTextCampaign(userInfo,
                userInfo.getClientInfo(),
                campaign);
    }

    private CampaignWithCreatingEmptyStatForGoals getCampaignFromRepository(Long campaignId) {
        List<? extends BaseCampaign> campaigns = campaignTypedRepository
                .getTypedCampaigns(userInfo.getShard(), List.of(campaignId));
        assertThat(campaigns).hasSize(1);
        return (CampaignWithCreatingEmptyStatForGoals) campaigns.get(0);
    }

    private ModelChanges<CampaignWithCreatingEmptyStatForGoals> getModelChanges(CampaignWithCreatingEmptyStatForGoals campaign, List<Long> counterIds) {
        return ModelChanges.build(campaign, CampaignWithCreatingEmptyStatForGoals.METRIKA_COUNTERS, counterIds);
    }

}
