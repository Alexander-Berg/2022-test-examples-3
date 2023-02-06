package ru.yandex.direct.core.entity.retargeting.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_LAL_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_SHORTCUT_NAME;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingConditionServiceFindOrCreateShortcutsTest extends BaseRetargetingConditionServiceTest {
    @Autowired
    protected MetrikaClientStub metrikaClientStub;

    @Autowired
    private Steps steps;

    private List<Goal> goals;
    private Long campaignId;
    private Long shortcutId;

    @Override
    public void before() {
        super.before();
        goals = List.of(defaultGoalByType(GoalType.GOAL));
        steps.retargetingGoalsSteps().createMetrikaGoalsInPpcDict(goals);
        metrikaClientStub.addGoals(clientInfo.getUid(), new HashSet<>(goals));

        var campaign = activeTextCampaign(clientId, clientInfo.getUid());
        var strategy = TestCampaigns.averageCpaStrategy()
                .withGoalId(goals.get(0).getId());
        var campaignInfo = steps.campaignSteps().createCampaign(campaign.withStrategy(strategy), clientInfo);
        campaignId = campaignInfo.getCampaignId();
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var retargetingCondition = createShortcutRetargetingCondition(CAMPAIGN_GOALS_SHORTCUT_NAME, clientId);
        var retargetingConditionInfo = steps.retConditionSteps().createRetCondition(retargetingCondition, clientInfo);
        shortcutId = retargetingConditionInfo.getRetConditionId();
        steps.retargetingSteps().createRetargeting(defaultRetargeting(campaignId, adGroupId, shortcutId), adGroupInfo,
                new RetConditionInfo()
                        .withClientInfo(clientInfo)
                        .withRetCondition(retargetingCondition));

        var createdShortcuts = retConditionRepository
                .getFromRetargetingConditionsTable(shard, clientId, List.of(shortcutId));
        assertSoftly(softly -> {
            softly.assertThat(createdShortcuts).hasSize(1);
            softly.assertThat(createdShortcuts.get(0).getId()).isEqualTo(shortcutId);
        });
    }

    private RetargetingCondition createShortcutRetargetingCondition(String shortcutName, ClientId clientId) {
        Rule rule = new Rule()
                .withGoals(goals)
                .withType(RuleType.OR);
        return (RetargetingCondition) defaultRetCondition(clientInfo.getClientId())
                .withType(ConditionType.shortcuts)
                .withName(shortcutName)
                .withRules(singletonList(rule))
                .withAvailable(true)
                .withDeleted(false)
                .withClientId(clientId.asLong());
    }

    @Test
    public void findOrCreateRetargetingConditionShortcuts_shortcutAlreadyExists_noNewShortcutIsCreated() {
        var shortcutsByCid = retargetingConditionService.findOrCreateRetargetingConditionShortcuts(shard, clientId,
                Map.of(campaignId, List.of(CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID)));

        Assertions.assertThat(shortcutsByCid)
                .isEqualTo(Map.of(campaignId, Map.of(CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID, shortcutId)));
    }

    @Test
    public void findOrCreateRetargetingConditionShortcuts_shortcutDoesNotExist_newShortcutIsCreated() {
        var shortcutsByCid = retargetingConditionService.findOrCreateRetargetingConditionShortcuts(shard, clientId,
                Map.of(campaignId, List.of(CAMPAIGN_GOALS_LAL_SHORTCUT_DEFAULT_ID)));

        var createdShortcutId = shortcutsByCid.get(campaignId).get(CAMPAIGN_GOALS_LAL_SHORTCUT_DEFAULT_ID);
        assertSoftly(softly -> {
            softly.assertThat(createdShortcutId).isNotEqualTo(shortcutId);
            softly.assertThat(createdShortcutId).isNotEqualTo(CAMPAIGN_GOALS_LAL_SHORTCUT_DEFAULT_ID);
        });
    }
}
