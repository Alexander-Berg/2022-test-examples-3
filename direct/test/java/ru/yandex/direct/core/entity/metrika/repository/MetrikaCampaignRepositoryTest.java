package ru.yandex.direct.core.entity.metrika.repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoal;
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoalId;
import ru.yandex.direct.core.entity.retargeting.model.GoalRole;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_METRIKA_GOALS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaCampaignRepositoryTest {
    private static final int SHARD = 1;
    private static final long INITIAL_LINK_COUNT = 3L;
    private static final long GOAL_ID = 28L;
    private static final long ANOTHER_GOAL_ID = 132L;

    @Autowired
    private Steps steps;
    @Autowired
    private MetrikaCampaignRepository repository;
    @Autowired
    private DslContextProvider dslContextProvider;

    private long campaignId;
    private ClientId clientId;

    @Before
    public void setUp() throws Exception {
        var campaignInfo = steps.campaignSteps().createDefaultCampaignByCampaignType(CampaignType.TEXT);
        campaignId = campaignInfo.getCampaignId();
        clientId = campaignInfo.getClientId();
        insertGoal(GOAL_ID, INITIAL_LINK_COUNT, campaignId);
    }

    @After
    public void tearDown() {
        dslContextProvider.ppc(SHARD).deleteFrom(CAMP_METRIKA_GOALS)
                .where(CAMP_METRIKA_GOALS.GOAL_ID.eq(GOAL_ID))
                .and(CAMP_METRIKA_GOALS.CID.eq(campaignId))
                .execute();
    }

    @Test
    public void decreaseMetrikaGoalsLinksCountBatch_decreaseToZero_newValueIsZero() {
        repository.decreaseMetrikaGoalsLinksCountBatch(dslContextProvider.ppc(SHARD).dsl(),
                singletonMap(GOAL_ID, INITIAL_LINK_COUNT), campaignId).execute();
        validateResult(GOAL_ID, 0L);
    }

    @Test
    public void decreaseMetrikaGoalsLinksCountBatch_decreaseToMinusOne_newValueIsZero() {
        repository.decreaseMetrikaGoalsLinksCountBatch(dslContextProvider.ppc(SHARD).dsl(),
                singletonMap(GOAL_ID, INITIAL_LINK_COUNT + 1), campaignId).execute();
        validateResult(GOAL_ID, 0L);
    }

    @Test
    public void decreaseMetrikaGoalsLinksCountBatch_decreaseTwoGoals_correctValues() {
        long anotherLinkCount = 10;
        insertGoal(ANOTHER_GOAL_ID, anotherLinkCount, campaignId);
        Map<Long, Long> linksByGoal = new HashMap<>();
        linksByGoal.put(GOAL_ID, INITIAL_LINK_COUNT - 1);
        linksByGoal.put(ANOTHER_GOAL_ID, anotherLinkCount - 3);

        repository.decreaseMetrikaGoalsLinksCountBatch(dslContextProvider.ppc(SHARD).dsl(),
                linksByGoal, campaignId).execute();

        validateResult(GOAL_ID, 1L);
        validateResult(ANOTHER_GOAL_ID, 3L);
    }

    @Test
    public void getGoalsByIds_goalIsReturned() {
        var ids = List.of(new CampMetrikaGoalId().withCampaignId(campaignId).withGoalId(GOAL_ID));

        var metrikaGoals = repository.getCampMetrikaGoalsByIds(SHARD, clientId, ids);

        assertThat(metrikaGoals, hasSize(1));
        assertThat(metrikaGoals.get(0).getId(), is(equalTo(ids.get(0))));
    }

    @Test
    public void addGoals_goalIsAdded() {
        long linksCount = 100;
        var goal = new CampMetrikaGoal().withCampaignId(campaignId).withGoalId(GOAL_ID + 1)
                .withLinksCount(linksCount)
                .withGoalsCount(0L).withContextGoalsCount(0L)
                .withStatDate(LocalDateTime.now())
                .withGoalRole(Set.of(GoalRole.SINGLE));
        repository.addCampMetrikaGoals(SHARD, List.of(goal));

        validateResult(goal.getGoalId(), linksCount);
    }

    @Test
    public void getGoalIdsByCampaignIds_goalIdIsReturned() {
        var goalIds = repository.getCampMetrikaGoalIdsByCampaignIds(SHARD, clientId, List.of(campaignId));

        assertThat(goalIds, hasSize(1));
        var expected = new CampMetrikaGoalId().withCampaignId(campaignId).withGoalId(GOAL_ID);
        assertThat(goalIds, contains(equalTo(expected)));
    }

    private void insertGoal(long goalId, long count, long campaignId) {
        dslContextProvider.ppc(SHARD).insertInto(CAMP_METRIKA_GOALS)
                .set(CAMP_METRIKA_GOALS.GOAL_ID, goalId)
                .set(CAMP_METRIKA_GOALS.LINKS_COUNT, count)
                .set(CAMP_METRIKA_GOALS.CID, campaignId)
                .execute();
    }

    @QueryWithoutIndex("Используется только в тестах")
    private void validateResult(long goalId, long expected) {
        List<Long> result = dslContextProvider.ppc(SHARD).select(CAMP_METRIKA_GOALS.LINKS_COUNT)
                .from(CAMP_METRIKA_GOALS)
                .where(CAMP_METRIKA_GOALS.GOAL_ID.eq(goalId))
                .fetch(CAMP_METRIKA_GOALS.LINKS_COUNT);
        assertThat(result, is(singletonList(expected)));
    }
}
