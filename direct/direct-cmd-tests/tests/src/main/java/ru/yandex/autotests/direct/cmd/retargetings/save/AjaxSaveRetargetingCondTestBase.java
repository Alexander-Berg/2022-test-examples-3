package ru.yandex.autotests.direct.cmd.retargetings.save;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;


public abstract class AjaxSaveRetargetingCondTestBase {

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(getAuthLogin());


    protected RetargetingCondition retCondition;

    public abstract String getRetargetingTemplate();

    public String getAuthLogin() {
        return getClient();
    }

    public abstract String getClient();

    public abstract RetargetingCondition getEditRetargetingCondition();

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(getClient()).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(getClient()).getClientID()));
        retCondition = BeanLoadHelper.loadCmdBean(getRetargetingTemplate(), RetargetingCondition.class);
    }

    @Description("Создание условия ретаргетинга (cmd = ajaxSaveRetargetingCond)")
    public void testAddAtAjaxSaveRetargetingCond() {
        Long retCondId = cmdRule.cmdSteps().retargetingSteps()
                .saveRetargetingConditionWithAssumption(retCondition, getClient());

        Map<Long, RetargetingCondition> allRetConditions = cmdRule.cmdSteps().retargetingSteps()
                .getShowRetargetingCond(getClient());

        RetargetingCondition actualRetCondition = allRetConditions.get(retCondId);
        RetargetingCondition expectedRetCondition = retCondition.withRetCondId(retCondId).withIsAccessible(1);
        assertThat("условие ретаргетинга правильно сохранилось", actualRetCondition,
                beanDiffer(expectedRetCondition).useCompareStrategy(
                        onlyFields(newPath("goal_id"), newPath("goal_type"), newPath("time"))));
    }

    @Description("Редактирование условия ретаргетинга (cmd = ajaxSaveRetargetingCond)")
    public void testEditAtAjaxSaveRetargetingCond() {
        Long retCondId = cmdRule.cmdSteps().retargetingSteps()
                .saveRetargetingConditionWithAssumption(retCondition, getClient());

        retCondition = getEditRetargetingCondition().
                withRetCondId(retCondId);

        Long retCondIdNew = cmdRule.cmdSteps().retargetingSteps()
                .saveRetargetingConditionWithAssumption(retCondition, getClient());
        assumeThat("при изменении условия ретаргетинга в ответе ret_cond_id соответствует переданному",
                retCondIdNew, equalTo(retCondId));

        Map<Long, RetargetingCondition> allRetConditions = cmdRule.cmdSteps().retargetingSteps()
                .getShowRetargetingCond(getClient());

        RetargetingCondition actualRetCondition = allRetConditions.get(retCondId);
        RetargetingCondition expectedRetCondition = retCondition.withIsAccessible(1);
        assertThat("условие ретаргетинга правильно сохранилось", actualRetCondition,
                beanDiffer(expectedRetCondition).useCompareStrategy(
                        onlyFields(newPath("goal_id"), newPath("goal_type"), newPath("time"))));
    }
}
