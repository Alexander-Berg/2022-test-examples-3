package ru.yandex.autotests.innerpochta.tests.autotests.event;

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
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DAY_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.MONTH_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.EVENT;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Редактирование простого события на странице")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.SINGLE_EVENT)
@RunWith(DataProviderRunner.class)
public class SingleEventPageTest {

    private static final String EVENT_LINK = "event";
    private Long layerID;

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

    @DataProvider
    public static Object[][] grid() {
        return new Object[][]{
            {DAY_GRID, WEEK_GRID},
            {WEEK_GRID, WEEK_GRID},
            {MONTH_GRID, MONTH_GRID}
        };
    }

    @Before
    public void setUp() {
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID);
    }

    @Test
    @Title("Клик на «Да» в попапе подтверждения ухода из редактирования: возвращаемся в сетку календаря")
    @TestCaseId("591")
    public void shouldCloseEventPage() {
        createSimpleEvent();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().home().eventsTodayList().get(0),
                steps.pages().cal().home().viewEventPopup().editEventBtn(),
                steps.pages().cal().home().newEventPage().nameInput()
            )
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), getRandomName())
            .clicksOn(
                steps.pages().cal().home().calHeaderBlock().calLink(),
                steps.pages().cal().home().warningPopup().agreeBtn()
            )
            .shouldNotSee(
                steps.pages().cal().home().newEventPage(),
                steps.pages().cal().home().warningPopup()
            );
    }

    @Test
    @Title("Клик на «Нет» в попапе подтверждения ухода из редактирования: остаемся в форме редактирования")
    @TestCaseId("595")
    public void shouldNotCloseEventPage() {
        createSimpleEvent();
        String eventName = getRandomName();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().home().eventsTodayList().get(0),
                steps.pages().cal().home().viewEventPopup().editEventBtn()
            );
        steps.user().defaultSteps()
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().newEventPage().nameInput(),
                eventName
            )
            .clicksOn(
                steps.pages().cal().home().calHeaderBlock().calLink(),
                steps.pages().cal().home().warningPopup().cancelBtn()
            )
            .shouldNotSee(steps.pages().cal().home().warningPopup())
            .shouldSee(steps.pages().cal().home().newEventPage())
            .shouldHasValue(steps.pages().cal().home().newEventPage().nameInput(), eventName);
    }

    @Test
    @Title("Удаляем простое событие на странице")
    @TestCaseId("376")
    public void shouldDeleteEvent() {
        createSimpleEvent();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().home().eventsTodayList().get(0),
                steps.pages().cal().home().viewEventPopup().editEventBtn(),
                steps.pages().cal().home().newEventPage().deleteEventBtn(),
                steps.pages().cal().home().removeEventPopup().removeOneBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldSeeElementsCount(steps.pages().cal().home().eventsTodayList(), 0);
    }

    @Test
    @Title("Открываем простое событие в новой вкладке")
    @TestCaseId("1153")
    @UseDataProvider("grid")
    public void shouldOpenSimpleEventInNewTab(String grid, String checkGrid) {
        createSimpleEvent();
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(grid)
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0));
        steps.user().hotkeySteps()
            .clicksOnElementHoldingCtrlKey(steps.pages().cal().home().viewEventPopup().editEventBtn());
        steps.user().defaultSteps().switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(EVENT_LINK));
    }

    @Test
    @Title("Открыть событие в новой вкладке по ctrl + клик")
    @TestCaseId("1155")
    public void shouldOpenEventEditOnNewPage() {
        createSimpleEvent();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsTodayList().get(0));
        steps.user().hotkeySteps()
            .clicksOnElementHoldingCtrlKey(steps.pages().cal().home().viewEventPopup().editEventBtn());
        steps.user().defaultSteps().switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(EVENT.fragment("")));
    }

    @Test
    @Title("Преобразование простого события в серию")
    @TestCaseId("96")
    @UseDataProvider("grid")
    public void shouldEditSimpleEventToRepeat(String grid, String viewName) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid);
        createSimpleEvent();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(0))
                .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
                .turnTrue(steps.pages().cal().home().newEventPage().repeatEventCheckBox())
                .shouldSee(steps.pages().cal().home().repeatPopup())
                .clicksOn(steps.pages().cal().home().repeatPopup().repeatUntilInput())
                .shouldSee(steps.pages().cal().home().calendar())
                .clicksOn(steps.pages().cal().home().calendar().daysThisMonth().get(5))
                .clicksOn(steps.pages().cal().home().newEventPage().nameInput())
                .shouldNotSee(steps.pages().cal().home().repeatPopup())
                .clicksOn(steps.pages().cal().home().newEventPage().repeatEventChangeButton())
                .shouldSee(steps.pages().cal().home().repeatPopup())
                .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
                .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
                .shouldSee(steps.pages().cal().home().viewEventPopup().repeatSymbol());
    }

    @Step("Создаем простое событие")
    private void createSimpleEvent() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID);
        steps.user().apiCalSettingsSteps().createNewEvent(event);
    }
}
