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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.ALL_EVENT_WARNING;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.AVAILABILITY_MAYBE_BUSY;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DAY_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.EDIT_ALL_LINK;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.EDIT_ONE_LINK;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.EXCEPTION_WARNING;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.MONTH_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.ONE_EVENT_WARNING;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.VIEW_ALL;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Редактирование повторяющегося события на странице")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.REPEATING_EVENT)
@RunWith(DataProviderRunner.class)
public class RepeatEventPageTest {

    private static final String EVENT_LINK = "event";

    private Long layerID;
    private String eventName;
    private String changedEventName;
    private String changedEventDescription;

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
            {DAY_GRID, MONTH_GRID},
            {WEEK_GRID, MONTH_GRID},
            {MONTH_GRID, MONTH_GRID}
        };
    }

    @Before
    public void setUp() {
        changedEventName = getRandomName();
        changedEventDescription = getRandomString();
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();

        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Включаем настройку «Показывать выходные»",
            new Params().withShowWeekends(true)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID);
        createRepeatEvent();
    }

    @Test
    @Title("Удаляем серию повторяющихся событий на странице")
    @TestCaseId("114")
    @UseDataProvider("grid")
    public void shouldDeleteRepeatEvent(String grid, String checkGrid) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid);
        openAllRepeatEvents(0);
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().newEventPage().deleteEventBtn(),
            steps.pages().cal().home().removeEventPopup().removeAllBtn()
        )
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .opensDefaultUrlWithPostFix(checkGrid)
            .shouldSeeElementsCount(steps.pages().cal().home().eventsAllList(), 0);
    }

    @Test
    @Title("Переходим по ссылке для редактирования одного события и удаляем его")
    @TestCaseId("114")
    @UseDataProvider("grid")
    public void shouldDeleteOneInRepeatEvent(String grid, String checkGrid) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid)
            .scrollTo(steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0));
        openSingleEventRepeatEvents();
        steps.user().defaultSteps().shouldBeOnUrl(containsString(EVENT_LINK))
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().eventWarning(), ONE_EVENT_WARNING)
            .clicksOn(
                steps.pages().cal().home().newEventPage().deleteEventBtn(),
                steps.pages().cal().home().removeEventPopup().removeOneBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldSeeElementsCount(steps.pages().cal().home().eventsTodayList(), 0)
            .opensDefaultUrlWithPostFix(checkGrid)
            .shouldSeeElementInList(steps.pages().cal().home().eventsAllList(), eventName);
    }

    @Test
    @Title("Переходим по ссылке редактирования серии событий, но удаляем только одно событие")
    @TestCaseId("376")
    @UseDataProvider("grid")
    public void shouldDeleteOneEventFromLink(String grid, String checkGrid) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid);
        openSingleEventRepeatEvents();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().eventWarningLink())
            .shouldContainText(steps.pages().cal().home().newEventPage().eventWarning(), ALL_EVENT_WARNING)
            .clicksOn(
                steps.pages().cal().home().newEventPage().deleteEventBtn(),
                steps.pages().cal().home().removeEventPopup().removeOneBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldSeeElementsCount(steps.pages().cal().home().eventsTodayList(), 0)
            .opensDefaultUrlWithPostFix(checkGrid)
            .shouldSeeElementInList(steps.pages().cal().home().eventsAllList(), eventName);
    }

    @Test
    @Title("Открываем повторяющееся событие в новой вкладке")
    @TestCaseId("1154")
    @UseDataProvider("grid")
    public void shouldOpenRepeatEventInNewTab(String grid, String checkGrid) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid)
            .clicksOn(
                steps.pages().cal().home().eventsAllList().get(0),
                steps.pages().cal().home().viewEventPopup().editEventBtn()
            );
        steps.user().hotkeySteps().clicksOnElementHoldingCtrlKey(steps.pages().cal().home().editOneEvent());
        steps.user().defaultSteps().switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(EVENT_LINK));
    }

    @Test
    @Title("Меняем «Только это событие» для регулярного события")
    @TestCaseId("90")
    public void shouldChangeSingleEventForRepeatEvent() {
        openSingleEventRepeatEvents();
        steps.user().defaultSteps().shouldBeOnUrl(containsString(EVENT_LINK))
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().eventWarning(), ONE_EVENT_WARNING);
        addChangesInEvent();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn());
        openSingleEventRepeatEvents();
        checkChangesInEvent();
        openAllRepeatEvents(1);
        steps.user().defaultSteps()
            .shouldNotContainText(steps.pages().cal().home().newEventPage().nameInput(), changedEventName);
    }

    @Test
    @Title("Меняем «Все события серии» для регулярного события")
    @TestCaseId("101")
    public void shouldChangeAllRepeatEvents() {
        openAllRepeatEvents(0);
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().newEventPage().changeRepeatPopup(),
            steps.pages().cal().home().beforeDateField()
        )
            .shouldSee(steps.pages().cal().home().beforeDateMiniCalendar());
        int daysCount = steps.pages().cal().home().beforeDateMiniCalendar().daysOutMonth().size();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().beforeDateMiniCalendar().daysOutMonth()
            .get(daysCount - 1));
        addChangesInEvent();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn());
        openAllRepeatEvents(1);
        checkChangesInEvent();
    }

    @Test
    @Title("Переходим из редактирования «Все события серии» в «Только это событие» для серии без исключений")
    @TestCaseId("952")
    public void shouldChangeFromSeriesToSingleEvent() {
        openAllRepeatEvents(0);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().eventWarningLink())
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().eventWarning(), ONE_EVENT_WARNING)
            .clicksOn(steps.pages().cal().home().newEventPage().eventWarningLink())
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().eventWarning(), ALL_EVENT_WARNING)
        ;
    }

    @Test
    @Title("Переходим из редактирования «Все события серии» в «Только это событие» в исключении")
    @TestCaseId("953")
    public void shouldChangeFromSeriesToExceptionEvent() {
        openSingleEventRepeatEvents();
        steps.user().defaultSteps().shouldBeOnUrl(containsString(EVENT_LINK))
            .inputsTextInElement(steps.pages().cal().home().newEventPage().descriptionInput(), getRandomName())
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn());
        openAllRepeatEvents(0);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().eventWarningLink())
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().eventWarning(), EXCEPTION_WARNING)
            .clicksOn(steps.pages().cal().home().newEventPage().eventWarningLink())
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().eventWarning(), ALL_EVENT_WARNING);
    }

    @Test
    @Title("Создаём регулярное событие")
    @TestCaseId("593")
    public void shouldCreateRepeatEvent() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().leftPanel().createEvent(),
            steps.pages().cal().home().newEventPage().descriptionAddBtn()
        )
            .turnTrue(steps.pages().cal().home().newEventPage().repeatEventCheckBox())
            .clicksOn(steps.pages().cal().home().beforeDateField())
            .shouldSee(steps.pages().cal().home().beforeDateMiniCalendar());
        int daysCount = steps.pages().cal().home().beforeDateMiniCalendar().daysOutMonth().size();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().beforeDateMiniCalendar().daysOutMonth()
            .get(daysCount - 1));
        addChangesInEvent();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().createFromPageBtn());
        openAllRepeatEvents(0);
        checkChangesInEvent();
    }

    @Test
    @Title("Редактирование события-исключения в серии")
    @TestCaseId("375")
    public void shouldEditExceptionEvent() {
        String eventExceptionName = getRandomString();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MONTH_GRID);
        createSeriesEventWithException(eventExceptionName, 2);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(2))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .clicksOn(steps.pages().cal().home().editOneEvent())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), eventExceptionName)
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .clicksOn(steps.pages().cal().home().eventsAllList().get(2))
            .shouldContainText(steps.pages().cal().home().viewEventPopup().name(), eventExceptionName);
    }

    @Test
    @Title("Исключения события из серии")
    @TestCaseId("375")
    public void shouldExcludeEventFromSeries() {
        String eventExceptionName = getRandomString();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MONTH_GRID);
        createSeriesEventWithException(eventExceptionName, 2);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(2))
            .shouldContainText(steps.pages().cal().home().viewEventPopup().name(), eventExceptionName)
            .shouldNotSee(steps.pages().cal().home().viewEventPopup().repeatSymbol())
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .clicksOn(steps.pages().cal().home().editOneEvent())
            .shouldContainText(
                steps.pages().cal().home().newEventPage().editSeriesMsg(),
                String.format(EXCEPTION_WARNING, " ", EDIT_ALL_LINK)
            )
            .shouldNotSee(steps.pages().cal().home().newEventPage().repeatEventChangeButton())
            .shouldNotSee(steps.pages().cal().home().newEventPage().layerFieldSelect());
    }

    @Test
    @Title("Изменения события-исключения не переносятся в серию")
    @TestCaseId("375")
    public void shouldNotEditSeriesThroughException() {
        String evenExceptionName = getRandomString();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MONTH_GRID);
        createSeriesEventWithException(evenExceptionName, 2);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(2))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .clicksOn(steps.pages().cal().home().editOneEvent())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), evenExceptionName)
            .clicksOn(steps.pages().cal().home().newEventPage().changeToSeriesOrSingle())
            .shouldContainText(
                steps.pages().cal().home().newEventPage().editSeriesMsg(),
                String.format(ALL_EVENT_WARNING, " ", EDIT_ONE_LINK)
            )
            .shouldContainCSSAttributeWithValue(
                steps.pages().cal().home().editEventPage().nameField(),
                "value",
                eventName
            );
    }

    @Test
    @Title("Редактирование регулярного события без Query-параметров")
    @TestCaseId("962")
    public void shouldEditRepeatEventWithoutQueryParams() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .clicksOn(steps.pages().cal().home().editAllEvents());
        String[] currentUrl = steps.user().defaultSteps().getsCurrentUrl().split("\\?");
        steps.user().defaultSteps().opensUrl(currentUrl[0])
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), eventName)
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), DEV_NULL_EMAIL);
        steps.user().hotkeySteps()
            .pressSimpleHotKey(steps.pages().cal().home().newEventPage().membersInput(), key(Keys.ENTER));
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().editEventPage().saveChangesBtn())
            .clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .shouldContainText(steps.pages().cal().home().viewEventPopup().name(), eventName + eventName)
            .shouldContainText(steps.pages().cal().home().viewEventPopup().membersList().get(0), DEV_NULL_EMAIL);
    }

    @Step("Создаем повторяющиеся событие")
    private void createRepeatEvent() {
        eventName = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultRepeatingEvent(layerID)
            .withName(eventName);
        steps.user().apiCalSettingsSteps().createNewRepeatEvent(event);
    }

    @Step("Открываем все повторяющиеся события")
    private void openAllRepeatEvents(int eventNumber) {
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(eventNumber),
            steps.pages().cal().home().viewEventPopup().editEventBtn(),
            steps.pages().cal().home().editAllEvents()
        )
            .shouldBeOnUrl(containsString(EVENT_LINK))
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().eventWarning(), ALL_EVENT_WARNING);
    }

    @Step("Открываем экземпляр повторяющегося события")
    private void openSingleEventRepeatEvents() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().editEventBtn(),
            steps.pages().cal().home().editOneEvent()
        );
    }

    @Step("Добавляем изменения в экземпляр события")
    private void addChangesInEvent() {
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), changedEventName)
            .inputsTextInElement(steps.pages().cal().home().newEventPage().descriptionInput(), changedEventDescription)
            .turnTrue(steps.pages().cal().home().newEventPage().allDayCheckBox())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), DEV_NULL_EMAIL)
            .inputsTextInElement(steps.pages().cal().home().newEventPage().locationInput(), changedEventDescription)
            .clicksOn(
                steps.pages().cal().home().newEventPage().notifyField().addNotifyBtn(),
                steps.pages().cal().home().newEventPage().status()
            )
            .clicksOnElementWithText(steps.pages().cal().home().statusList(), AVAILABILITY_MAYBE_BUSY)
            .clicksOnElementWithText(steps.pages().cal().home().newEventPage().visibility(), VIEW_ALL)
            .turnTrue(steps.pages().cal().home().newEventPage().accessCanEditCheckBox());
    }

    @Step("Проверяем изменения в повторяющимся событие")
    private void checkChangesInEvent() {
        steps.user().defaultSteps()
            .shouldContainValue(steps.pages().cal().home().newEventPage().nameInput(), changedEventName)
            .shouldContainValue(steps.pages().cal().home().newEventPage().descriptionInput(), changedEventDescription)
            .shouldBeEnabled(steps.pages().cal().home().newEventPage().allDayCheckBox())
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().newEventPage().membersList().get(0),
                DEV_NULL_EMAIL
            )
            .shouldContainValue(steps.pages().cal().home().newEventPage().locationInput(), changedEventDescription)
            .shouldSee(steps.pages().cal().home().newEventPage().notifyField().deleteNotifyBtn())
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().status(), AVAILABILITY_MAYBE_BUSY)
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().visibilityChecked().get(0), VIEW_ALL)
            .shouldBeEnabled(steps.pages().cal().home().newEventPage().accessCanEditCheckBox())
            .clicksOn(steps.pages().cal().home().newEventPage().cancelButton());
    }

    @Step("Создание серии с событием-исключением")
    public void createSeriesEventWithException(String exceptionName, int index) {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(index))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .clicksOn(steps.pages().cal().home().editOneEvent())
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().newEventPage().nameInput(),
                exceptionName
            )
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn());
    }

}
