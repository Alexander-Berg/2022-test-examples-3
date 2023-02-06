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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на список дел в тудушке")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.TODO_BLOCK)
public class TodoListTest {

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
        steps.user().apiCalSettingsSteps().createNewTodoList(getRandomName());
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().openTodoBtn());
    }

    @Test
    @Title("Создаем список дел потерей фокуса")
    @TestCaseId("555")
    public void shouldCreateList() {
        String listName = getRandomName();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().todo().createListBtn())
            .clicksOn(steps.pages().cal().home().todo().lists().get(0).inputName())
            .inputsTextInElement(steps.pages().cal().home().todo().lists().get(0).inputName(), listName)
            .clicksOn(steps.pages().cal().home().todo())
            .shouldNotSee(steps.pages().cal().home().todo().lists().get(0).inputName())
            .shouldHasText(steps.pages().cal().home().todo().lists().get(0).listTitle(), listName);
    }

    @Test
    @Title("Создаем список дел через enter")
    @TestCaseId("482")
    public void shouldCreateListFromEnter() {
        String listName = getRandomName();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().todo().createListBtn())
            .inputsTextInElement(steps.pages().cal().home().todo().lists().get(0).inputName(), listName);
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ENTER.toString());
        steps.user().defaultSteps()
            .shouldSee(steps.pages().cal().home().todo().lists().get(0))
            .shouldHasText(steps.pages().cal().home().todo().lists().get(0).listTitle(), listName);
    }

    @Test
    @Title("Редактирование списка дел кликом в иконку")
    @TestCaseId("504")
    public void shouldEditTodoListWithBtn() {
        String name = getRandomName();
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().todo().lists().get(0).editBtn())
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().todo().lists().get(0).inputName(),
                name
            )
            .clicksOn(steps.pages().cal().home().todo())
            .shouldHasText(steps.pages().cal().home().todo().lists().get(0).listTitle(), name);
    }

    @Test
    @Title("Редактирование списка дел кликом в название")
    @TestCaseId("562")
    public void shouldEditTodoListWithTitle() {
        String name = getRandomName();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().todo().lists().get(0).listTitle())
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().todo().lists().get(0).inputName(),
                name
            )
            .clicksOn(steps.pages().cal().home().todo())
            .shouldHasText(steps.pages().cal().home().todo().lists().get(0).listTitle(), name);
    }

    @Test
    @Title("Удаление пустого списка дел")
    @TestCaseId("587")
    public void shouldDeleteEmptyTodoList() {
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().todo().lists().get(0).deleteBtn())
            .shouldNotSee(steps.pages().cal().home().warningPopup())
            .shouldSeeElementsCount(steps.pages().cal().home().todo().lists(), 0);
    }

    @Test
    @Title("Удаление не пустого списка дел")
    @TestCaseId("505")
    public void shouldDeleteNotEmptyTodoList() {
        steps.user().apiCalSettingsSteps().createNewTodoItem(0, getRandomName(), null);
        steps.user().defaultSteps()
            .refreshPage()
            .shouldSee(steps.pages().cal().home().todo())
            .onMouseHoverAndClick(steps.pages().cal().home().todo().lists().get(0).deleteBtn())
            .shouldSee(steps.pages().cal().home().warningPopup())
            .clicksOn(steps.pages().cal().home().warningPopup().cancelBtn())
            .shouldNotSee(steps.pages().cal().home().warningPopup())
            .onMouseHoverAndClick(steps.pages().cal().home().todo().lists().get(0).deleteBtn())
            .shouldSee(steps.pages().cal().home().warningPopup())
            .clicksOn(steps.pages().cal().home().warningPopup().agreeBtn())
            .shouldNotSee(steps.pages().cal().home().warningPopup())
            .shouldSeeElementsCount(steps.pages().cal().home().todo().lists(), 0);
    }

    @Test
    @Title("Удаление списка с выполненными делами")
    @TestCaseId("588")
    public void shouldDeleteCompletedTodoList() {
        steps.user().apiCalSettingsSteps().createNewTodoItem(0, getRandomName(), null);
        steps.user().defaultSteps()
            .refreshPage()
            .shouldSee(steps.pages().cal().home().todo())
            .turnTrue(steps.pages().cal().home().todo().lists().get(0).items().get(0).doneCheckBox())
            .onMouseHoverAndClick(steps.pages().cal().home().todo().lists().get(0).deleteBtn())
            .clicksOn(steps.pages().cal().home().warningPopup().agreeBtn())
            .shouldNotSee(steps.pages().cal().home().warningPopup())
            .shouldSeeElementsCount(steps.pages().cal().home().todo().lists(), 0);
    }

    @Test
    @Title("Разворачиваем - сворачиваем список дел")
    @TestCaseId("495")
    public void shouldExpandTodoList() {
        steps.user().apiCalSettingsSteps().createNewTodoItem(0, Utils.getRandomString(), null);
        steps.user().defaultSteps().refreshPage()
            .shouldSee(steps.pages().cal().home().todo().lists().get(0).items().get(0))
            .clicksOn(steps.pages().cal().home().todo().lists().get(0).expanderBtn())
            .shouldSeeElementsCount(steps.pages().cal().home().todo().lists().get(0).items(), 0)
            .clicksOn(steps.pages().cal().home().todo().lists().get(0).expanderBtn())
            .shouldSee(steps.pages().cal().home().todo().lists().get(0).items().get(0));
    }
}
