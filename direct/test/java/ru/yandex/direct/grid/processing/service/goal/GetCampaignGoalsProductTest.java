package ru.yandex.direct.grid.processing.service.goal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.goal.GdCampaignGoalFilter;
import ru.yandex.direct.grid.processing.model.goal.GdGoal;
import ru.yandex.direct.grid.processing.model.goal.GdGoalType;
import ru.yandex.direct.grid.processing.model.goal.GdMetrikaCounterGoalType;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@GridProcessingTest
@RunWith(Parameterized.class)
public class GetCampaignGoalsProductTest {
    private static final String NAME = "Goal";
    private static final Integer COUNTER_ID = RandomNumberUtils.nextPositiveInteger(Integer.MAX_VALUE / 2);
    private static final Integer GOAL_ID = RandomNumberUtils.nextPositiveInteger(Integer.MAX_VALUE / 2);

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private MetrikaGoalsService metrikaGoalsService;
    @Autowired
    private CampaignGoalsService campaignGoalsService;
    @Autowired
    private GoalConversionsCacheService goalConversionsCacheService;
    @Autowired
    protected Steps steps;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private MetrikaClientStub metrikaClientStub;
    @Autowired
    private GoalDataService goalDataService;


    @Parameterized.Parameter
    public Map<Integer, Set<Integer>> countersWithGoals;

    @Parameterized.Parameter(1)
    public List<Long> campaignCounters;

    @Parameterized.Parameter(2)
    public List<GdGoal> expectedGoals;

    @Parameterized.Parameters
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {
                        Map.of(
                                COUNTER_ID + 1, Set.of(
                                        GOAL_ID + 1,
                                        GOAL_ID + 2),
                                COUNTER_ID + 2, Set.of(
                                        GOAL_ID + 3,
                                        GOAL_ID + 4,
                                        GOAL_ID + 5)
                        ),
                        List.of(
                                COUNTER_ID + 1,
                                COUNTER_ID + 2),
                        List.of(
                                getGdGoal(1),
                                getGdGoal(2),
                                getGdGoal(3),
                                getGdGoal(4),
                                getGdGoal(5)
                        )
                }
        };
        return asList(data);
    }

    UserInfo userinfo;
    TextCampaignInfo textCampaignInfo;

    @Before
    public void prepare() {
        userinfo = steps.userSteps().createDefaultUser();
        steps.featureSteps().addClientFeature(
                userinfo.getClientId(),
                FeatureName.MIGRATING_TO_NEW_METHODS_IN_GET_CAMPAIGN_GOALS,
                true);

        textCampaignInfo = steps.textCampaignSteps().createDefaultCampaign(userinfo.getClientInfo());
        Long campaignId = textCampaignInfo.getCampaignId();
        Set<Integer> counters = countersWithGoals.keySet();

        metrikaClientStub.addGoals(userinfo.getUid(),
                EntryStream.of(countersWithGoals)
                        .flatMapKeyValue((k, vs) -> vs.stream())
                        .map(goalId ->
                                (Goal)(new Goal()
                                        .withId(Integer.toUnsignedLong(goalId))
                                        .withName(NAME + goalId)))
                        .toSet());

        metrikaClientStub.addUserCounterIds(userinfo.getUid(), StreamEx.of(counters).toList());

        EntryStream.of(countersWithGoals).forKeyValue(
                (counter, goals) -> goals.forEach(
                        goal -> metrikaClientStub.addCounterGoal(counter, goal))
        );

        CampMetrikaCountersService campMetrikaCountersService = mock(CampMetrikaCountersService.class);

        Map<Long, List<Long>> counterIdsByCampaignId = Map.of(campaignId, StreamEx.of(counters)
                                                            .map(i -> (long)i).toList());

        when(campMetrikaCountersService.getCounterByCampaignIds(any(), any()))
                .thenReturn(counterIdsByCampaignId);

        goalDataService = new GoalDataService(metrikaGoalsService, null,
                campaignGoalsService, null, campMetrikaCountersService, null, null, null, featureService, goalConversionsCacheService,
                campaignRepository, shardHelper);
    }

    @Test
    public void test() {
        List<GdGoal> res = goalDataService.getCampaignsGoals(
                userinfo.getUid(),
                userinfo.getClientId(),
                new GdCampaignGoalFilter()
                        .withCampaignIdIn(
                                Set.of(textCampaignInfo.getCampaignId())),
                true);

        List<Long> goalIds = res.stream()
                .map(GdGoal::getId)
                .collect(Collectors.toList());
        List<Long> expectedGoalIds = expectedGoals.stream()
                .map(GdGoal::getId)
                .collect(Collectors.toList());

        assertThat(goalIds).containsExactlyElementsOf(expectedGoalIds);
    }

    private static GdGoal getGdGoal(int shift) {
        int id = GOAL_ID + shift;
        String name = NAME + id;
        return getGdGoal(
                name,
                name,
                Integer.toUnsignedLong(id),
                false);
    }


    private static GdGoal getGdGoal(String name, String simpleName, Long id, boolean isMobile) {
        return new GdGoal()
                .withId(id)
                .withIsMobileGoal(isMobile)
                .withType(GdGoalType.GOAL)
                .withMetrikaGoalType(GdMetrikaCounterGoalType.URL)
                .withName(name)
                .withSimpleName(simpleName)
                .withHasPrice(false)
                .withIsPerfectGoal(false);
    }
}
