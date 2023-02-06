package ru.yandex.autotests.innerpochta.tests.touch.screentests;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author marchart
 */
@Aqua.Test
@Title("[Тач] Вёрстка Todo")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.TODO_BLOCK)
public class TouchTodoScreenTest {

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> stepsProd.user()));

    @Test
    @Title("Открыть список дел")
    @TestCaseId("1072")
    public void shouldSeeTodo() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().calTouchTodoSteps().openTodo();
            st.user().defaultSteps().shouldSee(st.pages().cal().touchHome().todo().tabs());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выполнение дела из вкладки «Просроченные»")
    @TestCaseId("1121")
    public void shouldCompleteExpiredItem() {
        String todoList = Utils.getRandomName();
        String todoItemExpired = getRandomName();
        String expiredDate = stepsProd.user().calTouchTodoSteps().getRegionDateFormat(LocalDateTime.now().minusDays(1));
        stepsProd.user().apiCalSettingsSteps().deleteAllTodoLists()
            .createNewTodoList(todoList);

        Consumer<InitStepsRule> actions = st -> {
            st.user().calTouchTodoSteps().openTodo()
                .createToDoItemWithDate(todoItemExpired, expiredDate);
            st.user().defaultSteps().clicksOn(st.pages().cal().touchHome().todo().tabExpired())
                .shouldSeeThatElementHasText(
                    st.pages().cal().touchHome().todo().todoLists().get(0).items().get(0).itemName(),
                    todoItemExpired
                )
                .turnTrue(st.user().pages().calTouch().todo().todoLists().get(0).items().get(0).doneCheckBox())
                .waitInSeconds(1)
                .shouldNotSeeElementInList(
                    st.user().pages().calTouch().todo().todoLists().get(0).items(),
                    todoItemExpired
                )
                .clicksOn(st.pages().cal().touchHome().todo().tabCompleted())
                .shouldSeeThatElementHasText(
                    st.pages().cal().touchHome().todo().todoLists().get(0).items().get(0).itemName(),
                    todoItemExpired
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
