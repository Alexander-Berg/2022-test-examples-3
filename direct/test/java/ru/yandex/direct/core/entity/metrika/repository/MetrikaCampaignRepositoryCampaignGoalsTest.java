package ru.yandex.direct.core.entity.metrika.repository;

import java.util.List;
import java.util.Set;

import org.assertj.core.util.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaCampaignRepositoryCampaignGoalsTest {
    private static final Long GOAL_ID1 = 123456L;
    private static final Long GOAL_ID2 = 123457L;
    private static final Long GOAL_ID3 = 123458L;
    private static final String MEANINGFUL_GOALS_WITH_ENGAGED_SESSION_GOAL = "[" +
            "{\"value\": 1, \"goal_id\": \"" + GOAL_ID1 + "\"}," +
            "{\"value\": 1, \"goal_id\": \"12\"}," +
            "{\"value\": 1, \"goal_id\": \"" + GOAL_ID2 + "\"}]";
    private static final String MEANINGFUL_GOALS_WITHOUT_ENGAGED_SESSION_GOAL = "[" +
            "{\"value\": 1, \"goal_id\": \"" + GOAL_ID1 + "\"}," +
            "{\"value\": 1, \"goal_id\": \"" + GOAL_ID2 + "\"}," +
            "{\"value\": 1, \"goal_id\": \"" + GOAL_ID3 + "\"}]";
    private static final String MEANINGFUL_GOALS_SIMPLE = "[{\"value\": 1, \"goal_id\": \"" + GOAL_ID3 + "\"}]";

    @Autowired
    private MetrikaCampaignRepository repository;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private Campaign campaign;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        var clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        campaign = activeTextCampaign(clientId, clientInfo.getUid());
    }

    @Test
    public void getCampaignGoals_NoStrategyGoal_NoMeaningfulGoals_Empty() {
        var strategy = TestCampaigns.averageBidStrategy();

        var goalIds = createCampaignAndGetGoalIds(strategy, null);
        assertThat(goalIds, is(Set.of()));
    }

    @Test
    public void getCampaignGoals_WithStrategyGoal_NoMeaningfulGoals_StrategyGoal() {
        var strategy = TestCampaigns.averageCpaStrategy()
                .withGoalId(GOAL_ID1);

        var goalIds = createCampaignAndGetGoalIds(strategy, null);
        assertThat(goalIds, is(Set.of(GOAL_ID1)));
    }

    @Test
    public void getCampaignGoals_NoStrategyGoal_WithMeaningfulGoals_MeaningfulGoals() {
        var strategy = TestCampaigns.manualStrategy();

        var goalIds = createCampaignAndGetGoalIds(strategy, MEANINGFUL_GOALS_WITH_ENGAGED_SESSION_GOAL);
        assertThat(goalIds, is(Set.of(GOAL_ID1, GOAL_ID2)));
    }

    @Test
    public void getCampaignGoals_NoStrategyGoal_WithMeaningfulGoalsIncludingEngagedSession_MeaningfulGoals() {
        var strategy = TestCampaigns.manualStrategy();

        var goalIds = createCampaignAndGetGoalIds(strategy, MEANINGFUL_GOALS_WITHOUT_ENGAGED_SESSION_GOAL);
        assertThat(goalIds, is(Set.of(GOAL_ID1, GOAL_ID2, GOAL_ID3)));
    }

    @Test
    public void getCampaignGoals_WithStrategyGoalOptimizationOfMeaningfulGoals_WithMeaningfulGoals_MeaningfulGoals() {
        var strategy = TestCampaigns.averageCpaStrategy()
                .withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID);

        var goalIds = createCampaignAndGetGoalIds(strategy, MEANINGFUL_GOALS_SIMPLE);
        assertThat(goalIds, is(Set.of(GOAL_ID3)));
    }

    private Set<Long> createCampaignAndGetGoalIds(Strategy strategy, String meaningfulGoals) {
        campaignInfo = steps.campaignSteps()
                .createCampaign(campaign.withStrategy(strategy), clientInfo);
        var campaignId = campaignInfo.getCampaignId();

        if (!Strings.isNullOrEmpty(meaningfulGoals)) {
            setCampaignInfoMeaningfulGoals(meaningfulGoals);
        }
        return repository.getStrategyOrMeaningfulGoalIdsByCampaignId(shard, List.of(campaignId))
                .get(campaignId);
    }

    private void setCampaignInfoMeaningfulGoals(String meaningfulGoals) {
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.MEANINGFUL_GOALS, meaningfulGoals)
                .where(CAMP_OPTIONS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }
}
