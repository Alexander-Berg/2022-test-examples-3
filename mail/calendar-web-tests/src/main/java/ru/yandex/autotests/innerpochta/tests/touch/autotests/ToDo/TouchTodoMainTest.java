package ru.yandex.autotests.innerpochta.tests.touch.autotests.ToDo;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DISABLED_TAB_SELECTOR;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author marchart
 */
@Aqua.Test
@Title("[Тач] Todo основные тесты")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.TODO_BLOCK)
public class TouchTodoMainTest {

    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    private String todoItem;
    private String date;
    private String todoItemExpired;
    private String expiredDate;

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        todoItem = getRandomName();
        todoItemExpired = getRandomName();
        date = steps.user().calTouchTodoSteps().getRegionDateFormat(LocalDateTime.now().plusDays(1));
        expiredDate = steps.user().calTouchTodoSteps().getRegionDateFormat(LocalDateTime.now().minusDays(1));
        steps.user().apiCalSettingsSteps().createNewTodoList(getRandomName())
            .createNewTodoItem(0, getRandomString(), null);
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().calTouchTodoSteps().openTodo();
    }

    @Test
    @Title("Открыть/закрыть список дел")
    @TestCaseId("1072")
    public void shouldOpenAndCloseTodo() {
        steps.user().defaultSteps().shouldSee(steps.pages().cal().touchHome().todo().tabs())
            .clicksOn(steps.pages().cal().touchHome().todo().back())
            .shouldNotSee(steps.pages().cal().touchHome().todo());
    }

    @Test
    @Title("Создание списка дел")
    @TestCaseId("1112")
    public void shouldCreateTodoList() {
        String todoList = getRandomName();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().todo().createList())
            .inputsTextInElement(steps.pages().cal().touchHome().todo().inputTodoTitle(), todoList)
            .clicksOn(steps.pages().cal().touchHome().todo())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().todo().todoLists().waitUntil(not(empty())).get(0),
                todoList
            );
    }

    @Test
    @Title("Сворачивание/разворачивание списка дел")
    @TestCaseId("1133")
    public void shouldExpandTodoList() {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().cal().touchHome().todo().todoLists().waitUntil(not(empty())).get(0).items())
            .clicksOn(steps.pages().cal().touchHome().todo().todoLists().get(0).expanderBtn())
            .shouldNotSee(steps.pages().cal().touchHome().todo().todoLists().get(0).items())
            .clicksOn(steps.pages().cal().touchHome().todo().todoLists().get(0).expanderBtn())
            .shouldSee(steps.pages().cal().touchHome().todo().todoLists().get(0).items());
    }

    @Test
    @Title("Выполнение дела")
    @TestCaseId("1121")
    public void shouldCompleteItem() {
        steps.user().calTouchTodoSteps().createToDoItemWithDate(todoItem, date);
        steps.user().defaultSteps()
            .turnTrue(steps.user().pages().calTouch().todo().todoLists().get(0).items().get(1).doneCheckBox())
            .waitInSeconds(1)
            .shouldNotSeeElementInList(
                steps.user().pages().calTouch().todo().todoLists().get(0).items(),
                todoItem
            )
            .clicksOn(steps.pages().cal().touchHome().todo().tabCompleted())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().todo().todoLists().get(0).items().get(0).itemName(),
                todoItem
            );
    }

    @Test
    @Title("Снятие чекбокса выполнения дела на вкладке «Выполненные» для просроченных дел")
    @TestCaseId("1123")
    public void shouldUncheckCompletedExpiredItems() {
        createCompletedItems();
        steps.user().defaultSteps()
            .deselects(steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).doneCheckBox())
            .waitInSeconds(1)
            .shouldNotSeeElementInList(
                steps.pages().cal().touchHome().todo().todoLists().get(0).items(),
                todoItemExpired
            );
        steps.user().calTouchTodoSteps().openTab(steps.user().pages().calTouch().todo().tabExpired());
        steps.user().defaultSteps()
            .shouldSeeElementInList(steps.pages().cal().touchHome().todo().todoLists().get(0).items(), todoItemExpired);
        steps.user().calTouchTodoSteps().openTab(steps.pages().cal().touchHome().todo().tabAll());
        steps.user().defaultSteps()
            .shouldSeeElementInList(steps.pages().cal().touchHome().todo().todoLists().get(0).items(), todoItemExpired);
    }

    @Test
    @Title("Снятие чекбокса выполнения дела на вкладке «Выполненные» для дел из будущего")
    @TestCaseId("1123")
    public void shouldUncheckTomorrowCompletedItems() {
        createCompletedItems();
        steps.user().defaultSteps()
            .deselects(steps.user().pages().calTouch().todo().todoLists().get(0).items().get(1).doneCheckBox())
            .waitInSeconds(1)
            .shouldNotSeeElementInList(
                steps.pages().cal().touchHome().todo().todoLists().get(0).items(),
                todoItem
            );
        steps.user().calTouchTodoSteps().openTab(steps.pages().cal().touchHome().todo().tabAll());
        steps.user().defaultSteps()
            .shouldSeeElementInList(steps.pages().cal().touchHome().todo().todoLists().get(0).items(), todoItem);
    }

    @Test
    @Title("Снятие чекбокса выполнения дела на вкладке «Выполненные» для всех дел на вкладке")
    @TestCaseId("1123")
    public void shouldUncheckAllCompletedItems() {
        createCompletedItems();
        steps.user().defaultSteps()
            .deselects(
                steps.user().pages().calTouch().todo().todoLists().get(0).items().get(2).doneCheckBox(),
                steps.user().pages().calTouch().todo().todoLists().get(0).items().get(1).doneCheckBox(),
                steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).doneCheckBox()
            )
            .waitInSeconds(1)
            .shouldContainsAttribute(
                steps.pages().cal().touchHome().todo().tabCompleted(),
                "class",
                DISABLED_TAB_SELECTOR
            )
            .shouldSeeElementsCount(steps.user().pages().calTouch().todo().todoLists().get(0).items(), 3);
        steps.user().calTouchTodoSteps().openTab(steps.pages().cal().touchHome().todo().tabExpired());
        steps.user().defaultSteps()
            .shouldSeeElementInList(steps.pages().cal().touchHome().todo().todoLists().get(0).items(), todoItemExpired);
    }

    @Step("Создаём выполненные дела разных видов")
    private void createCompletedItems() {
        steps.user().calTouchTodoSteps().createToDoItemWithDate(todoItem, date)
            .createToDoItemWithDate(todoItemExpired, expiredDate);
        steps.user().defaultSteps()
            .turnTrue(steps.user().pages().calTouch().todo().todoLists().get(0).items().get(2).doneCheckBox())
            .shouldSeeElementsCount(steps.user().pages().calTouch().todo().todoLists().get(0).items(), 2)
            .turnTrue(steps.user().pages().calTouch().todo().todoLists().get(0).items().get(1).doneCheckBox())
            .shouldSeeElementsCount(steps.user().pages().calTouch().todo().todoLists().get(0).items(), 1)
            .turnTrue(steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).doneCheckBox())
            .shouldNotSee(steps.user().pages().calTouch().todo().todoLists().get(0).items());
        steps.user().calTouchTodoSteps().openTab(steps.pages().cal().touchHome().todo().tabCompleted());
        steps.user().defaultSteps()
            .shouldSeeElementsCount(steps.user().pages().calTouch().todo().todoLists().get(0).items(), 3);
    }
}
