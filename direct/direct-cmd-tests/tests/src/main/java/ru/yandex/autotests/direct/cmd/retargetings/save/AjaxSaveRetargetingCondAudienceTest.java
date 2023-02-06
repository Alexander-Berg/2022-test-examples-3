package ru.yandex.autotests.direct.cmd.retargetings.save;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
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
@Description("Сохранение условия ретаргетинга с Я.Аудиторией (cmd = ajaxSaveRetargetingCond)")
@Stories(TestFeatures.Retargeting.AJAX_SAVE_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.AJAX_SAVE_RETARGETING_COND)
@Tag(ObjectTag.RETAGRETING)
@Tag(TrunkTag.YES)
public class AjaxSaveRetargetingCondAudienceTest extends AjaxSaveRetargetingCondTestBase {

    private static final String CLIENT = "at-backend-retargeting3";
    private static final String AUDIENCE_RET_COND_TEMPLATE =
            "cmd.common.request.retargetingCondition.AjaxSaveRetargetingCondAudienceTest";
    // в бине у аудитории выставлено time = 90, т.к. при отправке в БК у time должно быть значение
    private static final String[] AUDIENCES = {"2000000119", "2000000120", "2000000121"};

    @Override
    public String getRetargetingTemplate() {
        return AUDIENCE_RET_COND_TEMPLATE;
    }

    @Override
    public String getAuthLogin() {
        return Logins.SUPER;
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
                new RetargetingGoal().withGoalId(AUDIENCES[2]).withGoalType(GoalType.AUDIENCE));
        return retCondition;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9960")
    public void testAddAtAjaxSaveRetargetingCond() {
        super.testAddAtAjaxSaveRetargetingCond();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9961")
    public void testEditAtAjaxSaveRetargetingCond() {
        super.testEditAtAjaxSaveRetargetingCond();
    }
}
