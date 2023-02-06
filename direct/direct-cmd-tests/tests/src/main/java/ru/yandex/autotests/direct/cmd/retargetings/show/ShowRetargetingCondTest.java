package ru.yandex.autotests.direct.cmd.retargetings.show;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
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

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Просмотр условий ретаргетинга (cmd = showRetargetingCond)")
@Stories(TestFeatures.Retargeting.SHOW_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.SHOW_RETARGETING_COND)
@Tag(ObjectTag.RETAGRETING)
@Tag(TrunkTag.YES)
public class ShowRetargetingCondTest {

    private static final String RET_COND_TEMPLATE1 = "cmd.common.request.retargetingCondition.ShowRetargetingCondTest-1";
    private static final String RET_COND_TEMPLATE2 = "cmd.common.request.retargetingCondition.ShowRetargetingCondTest-2";
    private static final String CLIENT = "at-retargeting-incorrectparams";
    private static final String[] GOALS = {"6041454", "6041457", "4026504343"};

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT);


    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
    }

    @Test
    @Description("Просмотр условий ретаргетинга при их наличии (cmd = showRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9989")
    public void testShowRetargetingCondExistingRoles() {
        RetargetingCondition expectedRetCondition1 = createRetCondition(RET_COND_TEMPLATE1);
        RetargetingCondition expectedRetCondition2 = createRetCondition(RET_COND_TEMPLATE2);
        Map<Long, RetargetingCondition> expectedRetConditions = new HashMap<>();
        expectedRetConditions.put(expectedRetCondition1.getRetCondId(), expectedRetCondition1);
        expectedRetConditions.put(expectedRetCondition2.getRetCondId(), expectedRetCondition2);

        Map<Long, RetargetingCondition> actualRetConditions =
                cmdRule.cmdSteps().retargetingSteps().getShowRetargetingCond(CLIENT);
        assertThat("получен правильный список ретаргетингов", actualRetConditions,
                beanDiffer(expectedRetConditions).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Просмотр условий ретаргетинга при их отсутствии (cmd = showRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9990")
    public void testShowRetargetingCondUnexistingRoles() {
        Map<Long, RetargetingCondition> actualRetConditions =
                cmdRule.cmdSteps().retargetingSteps().getShowRetargetingCond(CLIENT);
        assertThat("получен правильный список ретаргетингов", actualRetConditions.values(), empty());
    }

    private RetargetingCondition createRetCondition(String templateName) {
        RetargetingCondition retCondition =
                BeanLoadHelper.loadCmdBean(templateName, RetargetingCondition.class);
        Long retCondId = cmdRule.cmdSteps().retargetingSteps().
                saveRetargetingConditionWithAssumption(retCondition, CLIENT);
        return retCondition.withRetCondId(retCondId).withIsAccessible(1);
    }
}
