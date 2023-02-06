package ru.yandex.autotests.innerpochta.tests.autotests.todo;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на дела в тудушке")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.TODO_BLOCK)
public class TodoItemTest {

    private static final String TODO_DATE = "30.04.%s";
    private static final String DATE_TITLE = "30 апр.";

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().createNewTodoList(getRandomName())
            .createNewTodoItem(0, getRandomString(), null);
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().openTodoBtn());
    }

    @Test
    @Title("Создаем дело с датой потерей фокуса")
    @TestCaseId("566")
    public void shouldCreateTodoItem() {
        String year = DateTimeFormatter.ofPattern("yyyy").format(LocalDateTime.now());
        String name = getRandomName();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().todo().lists().get(0).createTodoItemBtn())
            .inputsTextInElement(steps.pages().cal().home().todo().lists().get(0).items().get(1).inputName(), name)
            .inputsTextInElement(
                steps.pages().cal().home().todo().lists().get(0).items().get(1).inputDate(),
                String.format(TODO_DATE, year)
            )
            .clicksOn(steps.pages().cal().home().todo().allListsBtn())
            .shouldSee(steps.pages().cal().home().todo().lists().get(0).items().get(1))
            .shouldHasText(steps.pages().cal().home().todo().lists().get(0).items().get(1).itemTitle(), name)
            .shouldHasText(steps.pages().cal().home().todo().lists().get(0).items().get(1).itemDate(), DATE_TITLE);
    }

    @Test
    @Title("Создаем дело без даты через enter")
    @TestCaseId("499")
    public void shouldCreateTodoWithEnter() {
        String name = getRandomName();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().todo().lists().get(0).createTodoItemBtn())
            .inputsTextInElement(steps.pages().cal().home().todo().lists().get(0).items().get(1).inputName(), name);
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ENTER.toString());
        steps.user().defaultSteps()
            .shouldSee(steps.pages().cal().home().todo().lists().get(0).items().get(1))
            .shouldHasText(steps.pages().cal().home().todo().lists().get(0).items().get(1).itemTitle(), name);
    }

    @Test
    @Title("Редактирование дела кликом в иконку")
    @TestCaseId("507")
    public void shouldEditTodoItemWithBtn() {
        String name = getRandomName();
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().todo().lists().get(0).items().get(0).editBtn())
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().todo().lists().get(0).items().get(0).inputName(),
                name
            )
            .clicksOn(steps.pages().cal().home().todo())
            .shouldSee(steps.pages().cal().home().todo().lists().get(0).items().get(0))
            .shouldHasText(steps.pages().cal().home().todo().lists().get(0).items().get(0).itemTitle(), name);
    }

    @Test
    @Title("Удаление дела")
    @TestCaseId("506")
    public void shouldDeleteTodoItem() {
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().todo().lists().get(0).items().get(0).deleteBtn())
            .shouldSeeElementsCount(steps.pages().cal().home().todo().lists().get(0).items(), 0);
    }

    @Test
    @Title("Выполнение дела")
    @TestCaseId("496")
    public void shouldSeeCompleteTodoItem() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().todo().lists().get(0).items().get(0).doneCheckBox())
            .shouldSeeElementsCount(steps.pages().cal().home().todo().lists().get(0).items(), 0)
            .clicksOn(steps.pages().cal().home().todo().completedListBtn())
            .shouldSeeElementsCount(steps.pages().cal().home().todo().lists().get(0).items(), 1);
    }

    @Test
    @Title("Сайдбар туду закрывается по ESC")
    @TestCaseId("849")
    public void shouldNotSeeToDoSidebar() {
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ESCAPE.toString());
        steps.user().defaultSteps().shouldNotSee(steps.pages().cal().home().addCalSideBar());
    }

    @Test
    @Title("Отображение дел в сетке при загрузке страницы")
    @TestCaseId("963")
    public void shouldNotSeeToDoPopup() {
        String dueDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now());
        steps.user().apiCalSettingsSteps().createNewTodoItem(0, getRandomString(), dueDate);
        steps.user().defaultSteps().refreshPage()
            .shouldSee(steps.pages().cal().home().todoItemsList().get(0))
            .clicksOn(steps.pages().cal().home().todo().closeTodoBtn())
            .opensDefaultUrl()
            .shouldSee(steps.pages().cal().home().todoItemsList().get(0))
            .shouldNotSee(steps.pages().cal().home().todo());
    }
}
