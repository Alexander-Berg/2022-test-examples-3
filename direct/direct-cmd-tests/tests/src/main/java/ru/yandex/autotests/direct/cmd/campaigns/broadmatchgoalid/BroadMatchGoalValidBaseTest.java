package ru.yandex.autotests.direct.cmd.campaigns.broadmatchgoalid;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;

public abstract class BroadMatchGoalValidBaseTest {
    protected static final long GOAL_ID_1 = 16819515;
    protected static final long GOAL_ID_2 = 16819470;
    protected static final long GOAL_ID_3 = 19723215; // цель с parent_goal_id
    protected final static String CLIENT = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected String broadMatchGoalId;
    protected BannersRule bannersRule;

    public BroadMatchGoalValidBaseTest(String broadMatchGoalId, CampaignTypeEnum campaignType) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        this.broadMatchGoalId = broadMatchGoalId;
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        cmdRule.apiSteps().campaignFakeSteps().setOrderID(bannersRule.getCampaignId().intValue(),
                bannersRule.getCampaignId().intValue());

        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campMetrikaGoalsSteps().
                addOrUpdateMetrikaGoals(bannersRule.getCampaignId(),
                        GOAL_ID_2, 50L, 50L);
        TestEnvironment.newDbSteps().campMetrikaGoalsSteps().
                addOrUpdateMetrikaGoals(bannersRule.getCampaignId(),
                        GOAL_ID_1, 50L, 50L);
        TestEnvironment.newDbSteps().campMetrikaGoalsSteps().
                addOrUpdateMetrikaGoals(bannersRule.getCampaignId(),
                        GOAL_ID_3, 50L, 50L);
    }

}
