package ru.yandex.autotests.innerpochta.tests.autotests.todo;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DAY_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.MONTH_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на дела в сетке")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.TODO_BLOCK)
@RunWith(DataProviderRunner.class)
public class TodoInGridTest {

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    private String itemName;
    private Long layerID;

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        itemName = getRandomString() + getRandomString();
        String dueDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now());
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Включаем настройку Показывать дела",
            new Params().withShowTodosInGrid(true)
        )
            .createNewTodoList(itemName + itemName)
            .createNewTodoItem(0, itemName, dueDate);
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(steps.pages().cal().home().todoItemsList().get(0));
    }

    @DataProvider
    public static Object[][] grids() {
        return new Object[][]{
            {DAY_GRID},
            {MONTH_GRID}
        };
    }

    @Test
    @Title("Выполнение дела в попапе в сетке")
    @TestCaseId("474")
    public void shouldCompleteTodoItem() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().todoItemPopup().items().get(0).doneCheckBox())
            .shouldSeeElementsCount(steps.pages().cal().home().todoItemsList(), 0)
            .clicksOn(steps.pages().cal().home().openTodoBtn())
            .shouldSee(steps.pages().cal().home().todo().lists().get(0))
            .clicksOn(steps.pages().cal().home().todo().completedListBtn())
            .shouldSeeElementsCount(steps.pages().cal().home().todo().lists().get(0).items(), 1);
    }

    @Test
    @Title("Удаление дела в попапе в сетке")
    @TestCaseId("578")
    public void shouldDeleteTodoItem() {
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().todoItemPopup().items().get(0).deleteBtn())
            .shouldSeeElementsCount(steps.pages().cal().home().todoItemsList(), 0);
    }

    @Test
    @Title("Закрытие попапа по клику вне него")
    @TestCaseId("579")
    public void shouldCloseTodoPopupWithClick() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel())
            .shouldNotSee(steps.pages().cal().home().todoItemPopup());
    }

    @Test
    @Title("Закрытие попапа по клику ESС")
    @TestCaseId("579")
    public void shouldCloseTodoPopupWithEsc() {
        steps.user().defaultSteps().shouldSee(steps.pages().cal().home().todoItemPopup());
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ESCAPE.toString());
        steps.user().defaultSteps().shouldNotSee(steps.pages().cal().home().todoItemPopup());
    }

    @Test
    @Title("Обновление названия в попапе")
    @TestCaseId("583")
    public void shouldEditTodoTitle() {
        String newName = Utils.getRandomName();
        steps.user().defaultSteps()
            .shouldHasText(steps.pages().cal().home().todoItemPopup().items().get(0).itemTitle(), itemName)
            .clicksOn(steps.pages().cal().home().openTodoBtn())
            .clicksOn(steps.pages().cal().home().todo().lists().get(0).items().get(0).itemTitle())
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().todo().lists().get(0).items().get(0).inputName(),
                newName
            )
            .clicksOn(steps.pages().cal().home().leftPanel())
            .shouldHasText(steps.pages().cal().home().todo().lists().get(0).items().get(0).itemTitle(), newName)
            .clicksOn(steps.pages().cal().home().todo().closeTodoBtn())
            .clicksOn(steps.pages().cal().home().todoItemsList().get(0))
            .shouldHasText(steps.pages().cal().home().todoItemPopup().items().get(0).itemTitle(), newName);
    }

    @Test
    @Title("Не показываем дела с выключенной настройкой")
    @TestCaseId("1170")
    public void shouldNotSeeTodoWithoutRefresh() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().calHeaderBlock().settingsButton())
            .deselects(steps.pages().cal().home().showTodos())
            .clicksOn(steps.pages().cal().home().generalSettings().enabledSaveButton())
            .shouldNotSee(steps.pages().cal().home().todoItemsList());
    }

    @Test
    @Title("Включаем настройку «Показывать дела»")
    @TestCaseId("1170")
    public void shouldSeeTodoWithoutRefresh() {
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Выключаем настройку «Показывать дела»",
            new Params().withShowTodosInGrid(false)
        );
        steps.user().defaultSteps().refreshPage()
            .shouldNotSee(steps.pages().cal().home().todo())
            .clicksOn(steps.pages().cal().home().calHeaderBlock().settingsButton())
            .turnTrue(steps.pages().cal().home().showTodos())
            .clicksOn(steps.pages().cal().home().generalSettings().enabledSaveButton())
            .shouldSee(steps.pages().cal().home().todoItemsList().get(0));
    }

    @Test
    @Title("Открываем попап дел в разных сетках")
    @TestCaseId("473")
    @UseDataProvider("grids")
    public void shouldSeeToDoPopup(String grid) {
        createSimpleAllDayEvent();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid)
            .clicksOn(steps.pages().cal().home().todoItemsList().get(0))
            .shouldSee(steps.pages().cal().home().todoItemPopup());
    }

    @Step("Создаем простое событие на весь день")
    private void createSimpleAllDayEvent() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withIsAllDay(true);
        steps.user().apiCalSettingsSteps().createNewEvent(event);
    }
}
