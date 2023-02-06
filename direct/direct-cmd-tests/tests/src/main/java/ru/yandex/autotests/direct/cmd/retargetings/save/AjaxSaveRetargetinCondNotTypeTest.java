package ru.yandex.autotests.direct.cmd.retargetings.save;


import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.GoalType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetConditionItemType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingGoal;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxSaveRetargetingCondResponse;
import ru.yandex.autotests.direct.cmd.data.retargeting.RetConditionErrorType;
import ru.yandex.autotests.direct.cmd.data.retargeting.RetConditionErrorsResource;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Map;

import static ru.yandex.autotests.direct.cmd.data.retargeting.CommonAjaxRetConditionResponse.error;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;

//Task: Testirt-9144.
@Aqua.Test
@Description("Сохранение условия ретаргетинга (cmd = ajaxSaveRetargetingCond) не один из")
@Stories(TestFeatures.Retargeting.AJAX_SAVE_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.AJAX_SAVE_RETARGETING_COND)
@Tag(ObjectTag.RETAGRETING)
@Tag(TrunkTag.YES)
public class AjaxSaveRetargetinCondNotTypeTest extends AjaxSaveRetargetingCondTestBase {

    private static final String CLIENT = "at-backend-retargeting-not";
    private static final String RET_COND_TEMPLATE = "cmd.common.request.retargetingCondition.AjaxSaveRetargetingCondTest";
    private static final String[] GOALS = {"5974032", "5974035", "4026398689"};

    protected RetargetingCondition retCondition;

    public String getAuthLogin() {
        return getClient();
    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(getClient()).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(getClient()).getClientID()));
        retCondition = BeanLoadHelper.loadCmdBean(getRetargetingTemplate(), RetargetingCondition.class);
    }

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
        RetargetingCondition retCondition = BeanLoadHelper.loadCmdBean(getRetargetingTemplate(),
                RetargetingCondition.class).
                withConditionName("new name").
                withConditionDesc("new desc");
        retCondition.getCondition().forEach(c -> c.setType(RetConditionItemType.NOT.getValue()));
        retCondition.getCondition().get(0).getGoals().add(
                new RetargetingGoal().withGoalId(GOALS[2]).withGoalType(GoalType.GOAL).withTime("30"));
        return retCondition;
    }

    @Test
    @Description("Создание условия ретаргетинга (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9958")
    public void testAddAtAjaxSaveRetargetingCond() {
        Long retCondId = cmdRule.cmdSteps().retargetingSteps().saveRetargetingConditionWithAssumption(
                this.getEditRetargetingCondition(), getClient()
        );

        Map<Long, RetargetingCondition> allRetConditions = cmdRule.cmdSteps().retargetingSteps().getShowRetargetingCond(getClient());

        RetargetingCondition actualRetCondition = allRetConditions.get(retCondId);
        RetargetingCondition expectedRetCondition = this.getEditRetargetingCondition().withIsAccessible(1);
        assertThat("условие ретаргетинга правильно сохранилось", actualRetCondition,
                beanDiffer(expectedRetCondition).useCompareStrategy(
                        onlyFields(newPath("goal_id"), newPath("goal_type"), newPath("time"))));
    }

    @Test
    @Description("Редактирование условия ретаргетинга (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9959")
    public void testEditAtAjaxSaveRetargetingCond() {
        Long retCondId = cmdRule.cmdSteps().retargetingSteps().saveRetargetingConditionWithAssumption(retCondition, getClient());

        retCondition = getEditRetargetingCondition().
                withRetCondId(retCondId);

        AjaxSaveRetargetingCondResponse actualResponse =
                cmdRule.cmdSteps().retargetingSteps().saveRetargetingCondition(retCondition, CLIENT);
        assertThat("ответ соответствует ожидаемому", actualResponse,
                beanDiffer(error(
                        RetConditionErrorType.ERROR, RetConditionErrorsResource.UNAVAILABLE_COND_CHANGE
                )).useCompareStrategy(
                        onlyFields(newPath("goal_id"), newPath("goal_type"), newPath("time"))));
    }
}
