package ru.yandex.autotests.direct.cmd.retargetings.goals;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxGetGoalsForRetargetingItem;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Поля недоступной цели Метрики (cmd = ajaxGetGoalsForRetargeting)")
@Stories(TestFeatures.Retargeting.AJAX_GET_GOALS_FOR_RETARGETING)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.AJAX_GET_GOALS_FOR_RETARGETING)
@Tag(ObjectTag.RETAGRETING)
@Tag(TrunkTag.YES)
public class AjaxGetGoalsForRetargetingNoAccessTest {

    private static final String CLIENT = "at-direct-b-getgoals-noaccess";

    private static final Long GOAL_ID = 4028110999L;

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.stepsClassRule().as(CLIENT);

    private static AjaxGetGoalsForRetargetingItem actualGoal;

    @BeforeClass
    public static void beforeClass() {
        List<AjaxGetGoalsForRetargetingItem> goals =
                cmdRule.cmdSteps().retargetingSteps().getAjaxGetGoalsForRetargeting(CLIENT);
        assumeThat("получили одну цель", goals, hasSize(1));
        actualGoal = goals.get(0);
    }

    @Test
    @Description("Поле goal_id цели Метрики (cmd = ajaxGetGoalsForRetargeting)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9952")
    public void testGoalIdAtAjaxGetGoalsForRetargeting() {
        assertThat("id цели соответствует ожидаемому",
                actualGoal.getGoalId(), equalTo(GOAL_ID));
    }

    @Test
    @Description("Поле allow_to_use цели Метрики (cmd = ajaxGetGoalsForRetargeting)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9953")
    public void testAllowToUseAtAjaxGetGoalsForRetargeting() {
        assertThat("доступность цели соответствует ожидаемой",
                actualGoal.getAllowToUse(), equalTo(0));
    }
}
