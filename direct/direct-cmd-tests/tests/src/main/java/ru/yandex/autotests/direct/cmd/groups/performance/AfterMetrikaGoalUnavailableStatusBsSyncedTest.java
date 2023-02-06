package ru.yandex.autotests.direct.cmd.groups.performance;

import java.sql.Timestamp;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.RetargetingGoalsGoalType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.RetargetingGoalsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper.setGroupBsSynced;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка помещение в очередь resyncQueue при изменении ретаргетинга ДМО фильтра")
@Stories(TestFeatures.Retargeting.AJAX_SAVE_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(ObjectTag.RETAGRETING)
@Tag(CampTypeTag.PERFORMANCE)
public class AfterMetrikaGoalUnavailableStatusBsSyncedTest {

    @ClassRule
    public static final DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private static final String CLIENT = "at-direct-retargeting13";
    private static final Long UNAVAILABLE_GOAL_ID = 18554450L;
    private PerformanceBannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Long retCondId;
    private Long campaignId;
    private int shardId;

    public AfterMetrikaGoalUnavailableStatusBsSyncedTest() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
        retCondId = cmdRule.apiSteps().retargetingSteps()
                .addRandomRetargetingCondition(CLIENT).longValue();
        bannersRule.getGroup().getPerformanceFilters().get(0).
                withRetargeting(new RetargetingCondition().withRetCondId(retCondId));
        this.cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        shardId = TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(CLIENT);

        setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
    }

    @Test
    @Description("Проверяем помещение в очередь resyncQueue при недоступности цели метрики")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9818")
    public void checkAfterChangeRetargetingBSSyncedTest() {
        changeRetargetingGoalToUnavailable();

        assertThat("ид группы помещен в resyncQueue", TestEnvironment.newDbSteps(CLIENT)
                        .bsResyncQueueSteps().isPidInResyncQueue(campaignId, bannersRule.getGroupId()),
                equalTo(true));
    }

    private void changeRetargetingGoalToUnavailable() {
        RetargetingGoalsRecord goals = new RetargetingGoalsRecord()
                .setRetCondId(retCondId)
                .setGoalId(UNAVAILABLE_GOAL_ID)
                .setGoalType(RetargetingGoalsGoalType.goal)
                .setModtime(Timestamp.valueOf("2015-05-22 03:34:52"))
                .setIsAccessible(1);
        TestEnvironment.newDbSteps().useShard(shardId).retargetingGoalsSteps().addRetargetingGoals(goals);
        cmdRule.darkSideSteps().getRunScriptSteps()
                .runPpcRetargetingCheckGoals(shardId, User.get(CLIENT).getClientID());
    }
}
