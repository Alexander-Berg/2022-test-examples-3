package ru.yandex.autotests.direct.cmd.retargetings;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingGoal;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxGetRetCondWithGoalsResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

@Aqua.Test
@Description("Получениe данных редактируемой записи \"условия ретаргетинга\" (cmd = ajaxGetRetCondWithGoals)")
@Stories(TestFeatures.Retargeting.AJAX_GET_RET_COND_WITH_GOALS)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.AJAX_GET_RET_COND_WITH_GOALS)
@Tag(ObjectTag.RETAGRETING)
@Tag(TrunkTag.YES)
public class AjaxGetRetCondWithGoalsTest {
    private static final String RET_COND_TEMPLATE =
            "cmd.common.request.retargetingCondition.AjaxGetRetCondWithGoalsTest";
    private final List<RetargetingGoal> expectedGoals;
    private final String CLIENT = "at-direct-b-getrc-goals";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT);

    private Long retargetingConditionId;


    public AjaxGetRetCondWithGoalsTest() {
        expectedGoals =  BeanLoadHelper.loadCmdBean(RET_COND_TEMPLATE, RetargetingCondition.class)
                .getCondition()
                .stream()
                .flatMap(x -> x.getGoals().stream())
                .collect(toList());

    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
        retargetingConditionId = cmdRule.cmdSteps().retargetingSteps()
                .saveRetargetingConditionWithAssumption(
                        BeanLoadHelper.loadCmdBean(RET_COND_TEMPLATE, RetargetingCondition.class), CLIENT);
    }

    @Test
    @Description("Проверка числа условий в полученных данных о ретаргетинге")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10920")
    public void getRetargetingConditionWithGoals() {
        AjaxGetRetCondWithGoalsResponse response =
                cmdRule.cmdSteps().retargetingSteps().getRetCondWithGoals(CLIENT, retargetingConditionId);
        assertThat("число условий соответствует ожиданиям", response.getCondition(), hasSize(expectedGoals.size()));

    }

    @Test
    @Description("Проверка целей в полученых данных о ретаргетинге")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10921")
    public void getRetargetingConditionWithGoalsCheckGoals() {
        AjaxGetRetCondWithGoalsResponse response =
                cmdRule.cmdSteps().retargetingSteps().getRetCondWithGoals(CLIENT, retargetingConditionId);

        CompareStrategy strategy = DefaultCompareStrategies.onlyFields(
                newPath("goalName"),
                newPath("goalDomain"),
                newPath("allowToUse"),
                newPath("goalId")
        );
        List<RetargetingGoal> actualGoals =
                response.getCondition().stream()
                        .flatMap(x -> x.getGoals().stream())
                        .collect(toList());

        assertThat("список целей соответствует ожидаемому", actualGoals,
                containsInAnyOrder(expectedGoals.stream()
                        .map(expectedGoal -> beanDiffer(expectedGoal).useCompareStrategy(strategy))
                        .collect(Collectors.toList())
                )
        );
    }

}
