package ru.yandex.autotests.direct.cmd.retargetings.save;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.GoalType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetConditionItemType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingConditionItem;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingGoal;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxSaveRetargetingCondResponse;
import ru.yandex.autotests.direct.cmd.data.retargeting.RetConditionErrorType;
import ru.yandex.autotests.direct.cmd.data.retargeting.RetConditionErrorsResource;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.data.retargeting.AjaxSaveRetargetingCondResponse.RESULT_OK;
import static ru.yandex.autotests.direct.cmd.data.retargeting.AjaxSaveRetargetingCondResponse.error;
import static ru.yandex.autotests.direct.cmd.data.retargeting.AjaxSaveRetargetingCondResponse.formatError;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Сохранение условия ретаргетинга с невалидными параметрами (cmd = ajaxSaveRetargetingCond)")
@Stories(TestFeatures.Retargeting.AJAX_SAVE_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(CmdTag.AJAX_SAVE_RETARGETING_COND)
@Tag(ObjectTag.RETAGRETING)
@Tag(TrunkTag.YES)
public class AjaxSaveRetargetingCondValidationNegativeTest {

    private static final String CLIENT = "at-retargeting-invalidparams";
    private static final String RET_COND_TEMPLATE = "cmd.common.request.retargetingCondition.AjaxSaveRetargetingCondValidationNegativeTest";
    private static final String[] GOALS = {"6041343", "6041346", "4026504202", "1000363056"};

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT);


    private RetargetingCondition retCondition;

    @Before
    public void before() {

        retCondition = BeanLoadHelper.loadCmdBean(RET_COND_TEMPLATE, RetargetingCondition.class);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга с пустым именем (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9980")
    public void testAddAtAjaxSaveRetargetingCondEmptyName() {
        retCondition.withConditionName("");
        sendAndCheckLogicError(RetConditionErrorType.ERROR, RetConditionErrorsResource.NEED_COND_NAME);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга без имени (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9979")
    public void testAddAtAjaxSaveRetargetingCondNullName() {
        retCondition.withConditionName(null);
        sendAndCheckFormatError();
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга с именем, " +
            "как у существующего условия (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9981")
    public void testAddAtAjaxSaveRetargetingCondExistingName() {
        AjaxSaveRetargetingCondResponse response =
                cmdRule.cmdSteps().retargetingSteps().saveRetargetingCondition(retCondition, CLIENT);
        assumeThat("первое условие сохранилось", response.getResult(), equalTo(RESULT_OK));

        retCondition.getCondition().get(0).getGoals().add(retCondition.getCondition().get(1).getGoals().get(0));
        retCondition.getCondition().get(0).withType(RetConditionItemType.NOT.getValue());
        sendAndCheckLogicError(RetConditionErrorType.ERROR, RetConditionErrorsResource.RET_COND_WITH_NAME_EXISTS);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга без описания (== null) (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9982")
    public void testAddAtAjaxSaveRetargetingCondNullDesc() {
        retCondition.withConditionDesc(null);
        sendAndCheckFormatError();
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга с пустым списком групп (== null) (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9983")
    public void testAddAtAjaxSaveRetargetingCondEmptyCondition() {
        retCondition.withCondition(new ArrayList<>());
        sendAndCheckFormatError();
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга без групп (== null) (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9984")
    public void testAddAtAjaxSaveRetargetingCondNullCondition() {
        retCondition.withCondition(null);
        sendAndCheckFormatError();
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга с пустым типом группы целей (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9985")
    public void testAddAtAjaxSaveRetargetingCondEmptyCondType() {
        retCondition.getCondition().get(0).withType("");
        sendAndCheckLogicError(RetConditionErrorType.INVALID_DATA, RetConditionErrorsResource.INVALID_USERS_DATA);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга без типа группы целей (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9965")
    public void testAddAtAjaxSaveRetargetingCondNullCondType() {
        retCondition.getCondition().get(0).withType(null);
        sendAndCheckLogicError(RetConditionErrorType.INVALID_DATA, RetConditionErrorsResource.INVALID_USERS_DATA);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга с невалидным типом группы целей (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9966")
    public void testAddAtAjaxSaveRetargetingCondInvalidCondType() {
        retCondition.getCondition().get(0).withType("and");
        sendAndCheckLogicError(RetConditionErrorType.INVALID_DATA, RetConditionErrorsResource.INVALID_USERS_DATA);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга с пустым id цели (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9967")
    public void testAddAtAjaxSaveRetargetingCondEmptyGoalId() {
        retCondition.getCondition().get(0).getGoals().get(0).withGoalId("");
        sendAndCheckLogicError(RetConditionErrorType.INVALID_DATA, RetConditionErrorsResource.INVALID_GOAL_ID);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга без id цели (== null) (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9968")
    public void testAddAtAjaxSaveRetargetingCondNullGoalId() {
        retCondition.getCondition().get(0).getGoals().get(0).withGoalId(null);
        sendAndCheckLogicError(RetConditionErrorType.INVALID_DATA, RetConditionErrorsResource.INVALID_GOAL_ID);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга с нечисловым id цели (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9969")
    public void testAddAtAjaxSaveRetargetingCondTextGoalId() {
        retCondition.getCondition().get(0).getGoals().get(0).withGoalId("abc");
        sendAndCheckLogicError(RetConditionErrorType.INVALID_DATA, RetConditionErrorsResource.INVALID_GOAL_ID);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга с нулевым временем (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9970")
    public void testAddAtAjaxSaveRetargetingCondLowCondTime() {
        retCondition.getCondition().get(0).getGoals().get(0).withTime("0");
        sendAndCheckLogicError(RetConditionErrorType.ERROR, RetConditionErrorsResource.INVALID_GOAL_TIME);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга с превышением времени (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9971")
    public void testAddAtAjaxSaveRetargetingCondHighCondTime() {
        retCondition.getCondition().get(0).getGoals().get(0).withTime("91");
        sendAndCheckLogicError(RetConditionErrorType.ERROR, RetConditionErrorsResource.INVALID_GOAL_TIME);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга без указания времени (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9972")
    public void testAddAtAjaxSaveRetargetingCondNullCondTime() {
        retCondition.getCondition().get(0).getGoals().get(0).withTime(null);
        sendAndCheckLogicError(RetConditionErrorType.INVALID_DATA, RetConditionErrorsResource.INVALID_USERS_DATA);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга с текстом в поле для времени (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9973")
    public void testAddAtAjaxSaveRetargetingCondTextCondTime() {
        retCondition.getCondition().get(0).getGoals().get(0).withTime("opladioplada");
        sendAndCheckLogicError(RetConditionErrorType.INVALID_DATA, RetConditionErrorsResource.INVALID_USERS_DATA);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга, идентичного существующему (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9974")
    public void testAddAtAjaxSaveRetargetingCondSameAsExisting() {
        AjaxSaveRetargetingCondResponse response =
                cmdRule.cmdSteps().retargetingSteps().saveRetargetingCondition(retCondition, CLIENT);
        assumeThat("первое условие сохранилось", response.getResult(), equalTo(RESULT_OK));

        retCondition.withConditionName("new name");
        sendAndCheckLogicError(RetConditionErrorType.ERROR, RetConditionErrorsResource.RET_COND_EXISTS);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга, " +
            "без условий на выполнение целей (ALL или OR) (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9986")
    public void testAddAtAjaxSaveRetargetingCondWithOneNotMatchingCondition() {
        retCondition = new RetargetingCondition().
                withConditionName("one cond with not").
                withConditionDesc("").
                withCondition(Collections.singletonList(
                        new RetargetingConditionItem().
                                withType(RetConditionItemType.NOT.getValue()).
                                withGoals(Collections.singletonList(new RetargetingGoal().
                                        withGoalId(GOALS[0]).
                                        withGoalType(GoalType.GOAL)))));
        sendAndCheckLogicError(RetConditionErrorType.INVALID_DATA, RetConditionErrorsResource.INVALID_USERS_DATA);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга, " +
            "с превышением максимального количества целей (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9975")
    public void testAddAtAjaxSaveRetargetingCondWithGoalsOverflow() {
        RetargetingGoal goal = new RetargetingGoal().
                withGoalId(GOALS[1]).
                withGoalType(GoalType.GOAL).
                withTime("30");
        for (int i = 0; i < RetargetingConditionItem.MAX_GOALS; i++) {
            retCondition.getCondition().get(0).getGoals().add(goal);
        }
        sendAndCheckLogicError(RetConditionErrorType.ERROR, RetConditionErrorsResource.MAX_GOALS_EXCEEDED);
    }

    @Test
    @Description("Ошибка при сохранении условия ретаргетинга, " +
            "с превышением максимального количества групп (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9976")
    public void testAddAtAjaxSaveRetargetingCondWithGroupsOverflow() {
        RetargetingConditionItem item = new RetargetingConditionItem().
                withType(RetConditionItemType.ALL.getValue()).
                withGoals(Collections.singletonList(new RetargetingGoal().
                        withGoalId(GOALS[1]).
                        withGoalType(GoalType.GOAL).
                        withTime("30")));
        for (int i = 0; i < RetargetingCondition.MAX_CONDITION_ITEMS + 1; i++) {
            retCondition.getCondition().add(item);
        }
        sendAndCheckLogicError(RetConditionErrorType.ERROR, RetConditionErrorsResource.MAX_GROUPS_EXCEEDED);
    }

    @Test
    @Description("Сохраняем условие состоящее из блока \"Не выполнена ни одна\", " +
            "в блоке нет целей, но есть есть сегмент")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9977")
    public void testAddAtAjaxSaveRetargetingCondWithOnlyNoOneWithOneSegmentsWithoutGoal() {
        List<RetargetingConditionItem> conditionItems = new ArrayList<>(1);
        conditionItems.add(new RetargetingConditionItem().
                withType(RetConditionItemType.NOT.getValue()).
                withGoals(Collections.singletonList(new RetargetingGoal().
                        withGoalId(GOALS[3]).withTime("30")
                )));
        retCondition = new RetargetingCondition().
                withConditionName("NoOneWithoutSegments").
                withConditionDesc("").
                withCondition(conditionItems);
        sendAndCheckLogicError(RetConditionErrorType.ERROR, RetConditionErrorsResource.ADD_ONE_GOAL);
    }

    @Test
    @Description("Сохраняем условие состоящее из блока \"Не выполнена ни одна\", " +
            "в блоке нет целей, но есть есть сегмент")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9978")
    public void testAddAtAjaxSaveRetargetingCondWithOnlyNoOneWithOneSegmentsWithOneGoal() {
        List<RetargetingConditionItem> conditionItems = new ArrayList<>(1);
        conditionItems.add(new RetargetingConditionItem().
                withType(RetConditionItemType.NOT.getValue()).
                withGoals(Collections.singletonList(new RetargetingGoal().
                        withGoalId(GOALS[3]).withTime("30")
                )));
        conditionItems.add(new RetargetingConditionItem().
                withType(RetConditionItemType.NOT.getValue()).
                withGoals(Collections.singletonList(new RetargetingGoal().
                        withGoalId(GOALS[2]).withTime("30")
                )));
        retCondition = new RetargetingCondition().
                withConditionName("NoOneWithoutSegments").
                withConditionDesc("").
                withCondition(conditionItems);
        sendAndCheckLogicError(RetConditionErrorType.ERROR, RetConditionErrorsResource.ADD_ONE_GOAL);
    }


    private void sendAndCheckLogicError(RetConditionErrorType errorType, RetConditionErrorsResource errorText) {
        AjaxSaveRetargetingCondResponse actualResponse =
                cmdRule.cmdSteps().retargetingSteps().saveRetargetingCondition(retCondition, CLIENT);
        assertThat("ответ соответствует ожидаемому", actualResponse,
                beanDiffer(error(errorType, errorText)).useCompareStrategy(onlyExpectedFields()));
    }

    private void sendAndCheckFormatError() {
        AjaxSaveRetargetingCondResponse actualResponse =
                cmdRule.cmdSteps().retargetingSteps().saveRetargetingCondition(retCondition, CLIENT);
        assertThat("ответ соответствует ожидаемому", actualResponse,
                beanDiffer(formatError()).useCompareStrategy(onlyExpectedFields()));
    }
}
