package ru.yandex.autotests.direct.cmd.retargetings.show;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetConditionItemType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingConditionItem;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingGoal;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Просмотр недоступного условия ретаргетинга (cmd = ajaxShowRetargetingCond)")
@Stories(TestFeatures.Retargeting.SHOW_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.SHOW_RETARGETING_COND)
@Tag(ObjectTag.RETAGRETING)
@Tag(TrunkTag.YES)
public class ShowRetargetingCondNoAccessTest {

    private static final String CLIENT = "at-direct-b-getgoals-noaccess";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT);

    private Map<Long, RetargetingCondition> expectedConditions = new HashMap<>();

    {
        long retCondId = 212810L;
        RetargetingCondition expectedCondition = new RetargetingCondition().
                withRetCondId(retCondId).
                withConditionName("Если ты удалишь это условие, тебя уволят").
                withConditionDesc("").
                withIsAccessible(0).
                withCondition(Collections.singletonList(new RetargetingConditionItem().
                        withType(RetConditionItemType.OR.getValue()).
                        withGoals(Collections.singletonList(new RetargetingGoal().
                                withGoalId("4028110999").
                                withTime("30")))));
        expectedConditions.put(retCondId, expectedCondition);
    }

    @Test
    @Description("Просмотр недоступного условия ретаргетинга (cmd = showRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9987")
    public void testShowRetargetingCondNoAccess() {
        Map<Long, RetargetingCondition> actualConditions =
                cmdRule.cmdSteps().retargetingSteps().getShowRetargetingCond(CLIENT);
        assertThat("список условий соответствует ожидаемому", actualConditions,
                beanDiffer(expectedConditions).useCompareStrategy(onlyExpectedFields()));
    }
}
