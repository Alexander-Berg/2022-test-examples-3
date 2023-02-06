package ru.yandex.autotests.direct.cmd.retargetings.save;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.GoalType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetConditionItemType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingGoal;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Сохранение условия ретаргетинга (cmd = ajaxSaveRetargetingCond)")
@Stories(TestFeatures.Retargeting.AJAX_SAVE_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.AJAX_SAVE_RETARGETING_COND)
@Tag(ObjectTag.RETAGRETING)
@Tag(TrunkTag.YES)
public class AjaxSaveRetargetingCondTest extends AjaxSaveRetargetingCondTestBase {

    private static final String CLIENT = "at-backend-retargeting";
    private static final String RET_COND_TEMPLATE = "cmd.common.request.retargetingCondition.AjaxSaveRetargetingCondTest";
    private static final String[] GOALS = {"5974032", "5974035", "4026398689"};

    @Override
    public String getRetargetingTemplate() {
        return RET_COND_TEMPLATE;
    }

    @Override
    public String getClient() {
        return CLIENT;
    }

    @Override
    public RetargetingCondition getEditRetargetingCondition() {
        RetargetingCondition retCondition = BeanLoadHelper.loadCmdBean(getRetargetingTemplate(), RetargetingCondition.class).
                withConditionName("new name").
                withConditionDesc("new desc");
        retCondition.getCondition().forEach(c -> c.setType(RetConditionItemType.ALL.getValue()));
        retCondition.getCondition().get(0).getGoals().add(
                new RetargetingGoal().withGoalId(GOALS[2]).withGoalType(GoalType.GOAL).withTime("30"));
        return retCondition;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9963")
    public void testAddAtAjaxSaveRetargetingCond() {
        super.testAddAtAjaxSaveRetargetingCond();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9964")
    public void testEditAtAjaxSaveRetargetingCond() {
        super.testEditAtAjaxSaveRetargetingCond();
    }
}
