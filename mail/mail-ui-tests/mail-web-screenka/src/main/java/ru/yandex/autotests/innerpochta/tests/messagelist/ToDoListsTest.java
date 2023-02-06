package ru.yandex.autotests.innerpochta.tests.messagelist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Список дел")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TODO_BLOCK)
public class ToDoListsTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsTest.user().loginSteps().forAcc(lock.firstAcc()).logins();
        prepareToDo(stepsTest);
    }

    @Test
    @Title("Открываем список «Не забыть»")
    @TestCaseId("3021")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68461")
    public void shouldSeeDontForgetList() {
        openToDoList(stepsTest, 1);
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().refreshPage();

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем пустой список")
    @TestCaseId("3040")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68461")
    public void shouldSeeEmptyList() {
        openToDoList(stepsTest, 0);
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().refreshPage();

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем редактирование списка")
    @TestCaseId("3041")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68461")
    public void shouldSeeListEdit() {
        stepsTest.user().defaultSteps()
            .onMouseHover(stepsTest.pages().mail().home().todoListBlock().todoList().get(0))
            .clicksOn(stepsTest.pages().mail().home().todoListBlock().todoList().get(0).editTodoListBtn());

        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().refreshPage();

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем создание дела с датой")
    @TestCaseId("3042")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68461")
    public void shouldSeeCreateTaskForm() {
        openToDoList(stepsTest, 1);
        stepsTest.user().defaultSteps()
            .clicksOn(stepsTest.pages().mail().home().todoItemsBlock().makeWithDateLink())
            .shouldSee(stepsTest.pages().mail().home().todoItemEditBlock());

        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().refreshPage();

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем фрому отправки списка")
    @TestCaseId("3043")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68461")
    public void shouldSeeSendListForm() {
        openToDoList(stepsTest, 0);
        stepsTest.user().defaultSteps().onMouseHover(stepsTest.pages().mail().home().todoItemsBlock().todoListTitle())
            .clicksOn(stepsTest.pages().mail().home().todoItemsBlock().sentTodoItemsIcon())
            .shouldSee(stepsTest.pages().mail().home().todoListSentBlock());

        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().refreshPage();

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем список «Выполненные задачи»")
    @TestCaseId("3044")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68461")
    public void shouldSeeDoneTasksPage() {
        openToDoList(stepsTest, 4);
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().refreshPage();

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Открываем список дел")
    private void openToDoList(InitStepsRule steps, int LIST_NUM) {
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().mail().home().todoListBlock().todoList().get(LIST_NUM).title())
            .shouldSee(steps.pages().mail().home().todoItemsBlock().todoListTitle());
    }

    @Step("Подготавливаем тудушку")
    private void prepareToDo(InitStepsRule steps) {
        steps.user().defaultSteps().clicksIfCanOn(steps.user().pages().HomePage().toDoWindow())
            .clicksIfCanOn(steps.user().pages().HomePage().todoItemsBlock().itemsBackLink())
            .clicksIfCanOn(steps.user().pages().HomePage().todoItemsBlock().todoBackLink())
            /* ждем, когда закончится анимация смены блока */
            .waitInSeconds(1)
            .clicksOn(steps.user().pages().HomePage().todoListBlock().closeTodoBlockBtn())
            .refreshPage()
            .clicksOn(steps.pages().mail().home().toDoWindow());
    }

}
