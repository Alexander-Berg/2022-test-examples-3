package ru.yandex.autotests.direct.cmd.bssynced.retargeting;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetConditionItemType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.retargeting.RetargetingConditionMap;


public abstract class AfterChangeRetCondBsSyncedTestBase {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected BannersRule bannersRule;
    protected Long retCondId;
    protected Long campaignId;

    protected abstract String getClient();

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();

        enableBsSynced();
    }

    protected void enableBsSynced() {
        BsSyncedHelper.setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
    }

    protected Long addRetargetingCondition() {
        TestEnvironment.newDbSteps().useShardForLogin(getClient()).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(getClient()).getClientID()));
        return cmdRule.apiSteps().retargetingSteps()
                .addRandomRetargetingCondition(getClient()).longValue();
    }

    protected void changeRetargeting() {
        ru.yandex.autotests.directapi.common.api45.RetargetingCondition retargetingCondition =
                cmdRule.apiSteps().retargetingSteps()
                        .getRetargetingCondition(getClient(), retCondId.intValue());
        RetargetingCondition retargeting = RetargetingCondition.fromApiRetargetingCondition(retargetingCondition);
        retargeting.getCondition().get(0).setType(RetConditionItemType.OR.getValue());
        cmdRule.cmdSteps().retargetingSteps().saveRetargetingConditionWithAssumption(retargeting, getClient());
    }

    protected void changeRetargetingToUnaccesable() {
        ru.yandex.autotests.directapi.common.api45.RetargetingCondition retargetingCondition =
                cmdRule.apiSteps().retargetingSteps().getRetargetingCondition(getClient(), retCondId.intValue());
        cmdRule.apiSteps().retargetingSteps().retargetingConditionUpdate(new RetargetingConditionMap(retargetingCondition));
        RetargetingCondition retargeting = RetargetingCondition.fromApiRetargetingCondition(retargetingCondition);
        int shard =
                cmdRule.apiSteps().getDirectJooqDbSteps().shardingSteps().getShardByCid(bannersRule.getCampaignId());
        cmdRule.apiSteps().getDirectJooqDbSteps().useShard(shard).retargetingGoalsSteps().setIsAccessible(retCondId,
                Long.parseLong(retargeting.getCondition().get(0).getGoals().get(0).getGoalId()), 0);
    }

    protected void reSave() {
        ru.yandex.autotests.directapi.common.api45.RetargetingCondition retargetingCondition =
                cmdRule.apiSteps().retargetingSteps().getRetargetingCondition(getClient(), retCondId.intValue());
        RetargetingCondition retargeting = RetargetingCondition.fromApiRetargetingCondition(retargetingCondition);
        cmdRule.cmdSteps().retargetingSteps().saveRetargetingConditionWithAssumption(retargeting, getClient());
    }

}
