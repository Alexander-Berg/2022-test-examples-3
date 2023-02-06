package ru.yandex.direct.core.entity.retargeting.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoal;
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoalId;
import ru.yandex.direct.core.entity.retargeting.model.GoalRole;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampMetrikaGoalServiceTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CampMetrikaGoalService service;

    private ClientId clientId;
    private Long operatorUid;
    private Long campaignId;

    @Before
    public void setUp() {
        var campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientId = campaignInfo.getClientId();
        operatorUid = campaignInfo.getUid();
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void add_goalIsAdded() {
        var goalId = 101L;
        var linksCount = 100L;

        var goal = metrikaGoal(campaignId, goalId, linksCount);
        var result = service.add(clientId, operatorUid, List.of(goal), Applicability.PARTIAL);

        assertThat(result).is(matchedBy(isFullySuccessful()));

        CampMetrikaGoalId id = new CampMetrikaGoalId().withGoalId(goalId).withCampaignId(campaignId);
        var expected = goal.withId(id);
        var actual = service.get(clientId, operatorUid, List.of(id)).get(0);

        assertThat(actual).isEqualToIgnoringGivenFields(expected, "statDate");
    }

    @Test
    public void add_duplicate_doesNotFail() {
        var goalId = 101L;

        var firstGoal = metrikaGoal(campaignId, goalId, 0);
        service.add(clientId, operatorUid, List.of(firstGoal), Applicability.PARTIAL);

        var secondGoal = metrikaGoal(campaignId, goalId, 0)
                .withContextGoalsCount(10L)
                .withGoalsCount(10L);

        var result = service.add(clientId, operatorUid, List.of(secondGoal), Applicability.PARTIAL);

        assertThat(result).is(matchedBy(isFullySuccessful()));
    }

    private static CampMetrikaGoal metrikaGoal(long campaignId, long goalId, long linksCount) {
        return new CampMetrikaGoal()
                .withCampaignId(campaignId)
                .withGoalId(goalId)
                .withLinksCount(linksCount)
                .withGoalsCount(0L)
                .withContextGoalsCount(0L)
                .withStatDate(LocalDateTime.now())
                .withGoalRole(Set.of(GoalRole.SINGLE));
    }
}
