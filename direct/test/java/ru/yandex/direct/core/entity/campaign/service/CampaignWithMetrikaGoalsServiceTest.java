package ru.yandex.direct.core.entity.campaign.service;

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
import ru.yandex.direct.core.entity.retargeting.service.CampMetrikaGoalService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignWithMetrikaGoalsServiceTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CampMetrikaGoalService campMetrikaGoalService;
    @Autowired
    private CampaignWithMetrikaGoalsService service;

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
    public void getChildEntityIdsByParentIds_idIsReturned() {
        var goalId = 101L;
        var linksCount = 100L;
        var goal = new CampMetrikaGoal().withCampaignId(campaignId).withGoalId(goalId)
                .withLinksCount(linksCount)
                .withGoalsCount(0L).withContextGoalsCount(0L)
                .withStatDate(LocalDateTime.now())
                .withGoalRole(Set.of(GoalRole.SINGLE));

        var result = campMetrikaGoalService.add(clientId, operatorUid, List.of(goal), Applicability.PARTIAL);

        assertThat(result).is(matchedBy(isFullySuccessful()));

        CampMetrikaGoalId id = new CampMetrikaGoalId().withGoalId(goalId).withCampaignId(campaignId);
        var actual = service.getChildEntityIdsByParentIds(clientId, operatorUid, Set.of(campaignId));

        assertThat(actual).isEqualTo(Set.of(id));
    }
}
