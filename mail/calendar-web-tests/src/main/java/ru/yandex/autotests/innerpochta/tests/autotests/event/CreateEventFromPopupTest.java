package ru.yandex.autotests.innerpochta.tests.autotests.event;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.COLUMN_CENTER;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DAY_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.RANDOM_X_COORD;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.RANDOM_Y_COORD;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TIME_11AM;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.MONTH;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Создание события в попапе")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.NEW_EVENT_POPUP)
@RunWith(DataProviderRunner.class)
public class CreateEventFromPopupTest {

    private String name;
    private static final String TIME = "12:00";
    private static final String INVALID_TIME = "5789";
    private static final String EVENT_CREATE_DAY = "?show_date=2020-07-07";
    private static final String REPEAT_DAY_INFO = "по пн, вт и ср. ";

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
        name = getRandomName();
        steps.user().apiCalSettingsSteps().createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[][] grids() {
        return new Object[][]{
            {DAY_GRID},
            {WEEK_GRID}
        };
    }

    @Test
    @Title("Создаем встречу на весь день в сетке на месяц")
    @TestCaseId("14")
    public void shouldCreateEventWithPopupInMonthGrid() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().leftPanel().view())
            .clicksOn(steps.pages().cal().home().selectView().get(2))
            .shouldBeOnUrl(CoreMatchers.containsString(MONTH.makeUrlPart("")))
            .onMouseHoverAndClick(steps.pages().cal().home().daysInMonthView().get(2))
            .clicksOn(steps.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().nameInput(), name)
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPopup().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPopup().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().descriptionInput(), getRandomName())
            .shouldSeeCheckBoxesInState(
                true,
                steps.pages().cal().home().newEventPopup().allDayCheckBox()
            )
            .clicksOn(steps.pages().cal().home().newEventPopup().dateInputList().get(0))
            .shouldSee(steps.pages().cal().home().miniCalendar())
            .clicksOn(steps.pages().cal().home().miniCalendar().daysOutMonth().get(0))
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPopup().layerFieldSelect())
            .clicksOn(
                steps.pages().cal().home().layersList().get(1),
                steps.pages().cal().home().newEventPopup().createFromPopupBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPopup())
            .shouldContainText(steps.pages().cal().home().eventsAllList().get(0).eventName(), name);
    }

    @Test
    @Title("Закрытие попапа с условиями повторения при клике в поле Дата и время")
    @TestCaseId("938")
    public void shouldCloseRepeatPopup() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(steps.pages().cal().home().leftPanel().view())
            .clicksOn(steps.pages().cal().home().selectView().get(1))
            .shouldBeOnUrl(CoreMatchers.containsString(WEEK_GRID))
            .offsetClick(steps.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .shouldSee(steps.pages().cal().home().newEventPopup())
            .turnTrue(steps.pages().cal().home().newEventPopup().repeatEventCheckBox())
            .shouldSee(steps.pages().cal().home().repeatPopup())
            .clicksOn(steps.pages().cal().home().newEventPopup().timeInputList().get(0))
            .shouldNotSee(steps.pages().cal().home().repeatPopup())
            .clicksOn(steps.pages().cal().home().changeRepeatPopup())
            .shouldSee(steps.pages().cal().home().repeatPopup())
            .clicksOn(steps.pages().cal().home().newEventPopup().dateInputList().get(0))
            .shouldNotSee(steps.pages().cal().home().repeatPopup());
    }

    @Test
    @Title("Создать событие на весь день из попапа в сетке")
    @TestCaseId("134")
    @UseDataProvider("grids")
    public void shouldCreateAllDayEvent(String grid) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid)
            .offsetClick(RANDOM_X_COORD, RANDOM_Y_COORD)
            .shouldSee(steps.pages().cal().home().newEventPopup())
            .clicksOn(steps.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().nameInput(), name)
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPopup().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPopup().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().descriptionInput(), getRandomName())
            .turnTrue(steps.pages().cal().home().newEventPopup().allDayCheckBox())
            .shouldContainText(steps.pages().cal().home().allDayEventsAllList().get(0).eventName(), name)
            .clicksOn(steps.pages().cal().home().newEventPopup().createFromPopupBtn())
            .shouldContainText(steps.pages().cal().home().allDayEventsAllList().get(0).eventName(), name);
    }

    @Test
    @Title("Создать событие по клику на блок Весь день")
    @TestCaseId("133")
    @UseDataProvider("grids")
    public void shouldCreateAllDayEventFromAllDayBlock(String grid) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid)
            .clicksOn(steps.pages().cal().home().allDayEventsAllList().get(0))
            .shouldSee(steps.pages().cal().home().newEventPopup())
            .clicksOn(steps.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().nameInput(), name)
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPopup().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPopup().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().descriptionInput(), getRandomName())
            .shouldSeeCheckBoxesInState(
                true,
                steps.pages().cal().home().newEventPopup().allDayCheckBox()
            )
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPopup().layerFieldSelect())
            .clicksOn(
                steps.pages().cal().home().layersList().get(1),
                steps.pages().cal().home().newEventPopup().createFromPopupBtn()
            )
            .shouldContainText(steps.pages().cal().home().allDayEventsAllList().get(0).eventName(), name);
    }

    @Test
    @Title("Создание события по нажатию «Enter» в поле «Название»")
    @TestCaseId("570")
    @UseDataProvider("grids")
    public void shouldCreateEventByEnterInNameInput(String grid) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid);
        openPopupAndInputName();
        steps.user().hotkeySteps().pressSimpleHotKey(
            steps.pages().cal().home().newEventPopup().nameInput(),
            key(Keys.ENTER)
        );
        steps.user().defaultSteps()
            .shouldNotSee(steps.pages().cal().home().newEventPopup())
            .shouldSee(steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0))
            .shouldSeeElementInList(steps.pages().cal().home().eventsAllList(), name);
    }

    @Test
    @Title("Создание события по нажатию «Enter» в поле «Дата и время»")
    @TestCaseId("570")
    @UseDataProvider("grids")
    public void shouldCreateEventByEnterInDateAndTimeInput(String grid) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid);
        openPopupAndInputName();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPopup().time());
        steps.user().hotkeySteps().pressSimpleHotKey(
            steps.pages().cal().home().newEventPopup().time(),
            key(Keys.ENTER)
        );
        steps.user().defaultSteps().shouldNotSee(steps.pages().cal().home().newEventPopup())
            .shouldSeeElementInList(steps.pages().cal().home().eventsAllList(), name);
    }

    @Test
    @Title("Создание повторяющегося события")
    @TestCaseId("774")
    @UseDataProvider("grids")
    public void shouldCreateRepeatEvent(String grid) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid + EVENT_CREATE_DAY);
        openPopupAndInputName();
        steps.user().defaultSteps()
            .turnTrue(steps.pages().cal().home().newEventPopup().repeatEventCheckBox())
            .shouldSee(steps.pages().cal().home().repeatPopup())
            .turnTrue(
                steps.pages().cal().home().repeatPopup().weekDay().get(0),
                steps.pages().cal().home().repeatPopup().weekDay().get(2)
            )
            .clicksOn(steps.pages().cal().home().newEventPopup().popupTitle())
            .shouldNotSee(steps.pages().cal().home().repeatPopup())
            .shouldContainText(steps.pages().cal().home().repeatInfo(), REPEAT_DAY_INFO)
            .clicksOn(steps.pages().cal().home().changeRepeatPopup())
            .shouldSee(steps.pages().cal().home().repeatPopup())
            .clicksOn(steps.pages().cal().home().newEventPopup().popupTitle())
            .clicksOn(steps.pages().cal().home().newEventPopup().createFromPopupBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPopup())
            .shouldSeeElementInList(steps.pages().cal().home().eventsAllList(), name);
    }

    @Test
    @Title("Ввод времени события вручную")
    @TestCaseId("982")
    public void shouldInputEventTimeFromKeybord() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID);
        openPopupAndInputName();
        steps.user().defaultSteps().inputsTextInElementClearingThroughHotKeys(
            steps.pages().cal().home().newEventPopup().timeInputList().get(0),
            TIME.replace(":", "")
        )
            .shouldHasValue(steps.pages().cal().home().newEventPopup().timeInputList().get(0), TIME);
    }

    @Test
    @Title("Ввод некорректного времени события вручную")
    @TestCaseId("982")
    public void shouldNotInputInvalidEventTimeFromKeybord() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID);
        openPopupAndInputName();
        String time = steps.pages().cal().home().newEventPopup().time().getAttribute("value");
        steps.user().defaultSteps().inputsTextInElementClearingThroughHotKeys(
            steps.pages().cal().home().newEventPopup().timeInputList().get(0),
            INVALID_TIME
        )
            .clicksOn(steps.pages().cal().home().newEventPopup().nameInput())
            .shouldContainValue(steps.pages().cal().home().newEventPopup().timeInputList().get(0), time);
    }

    @Test
    @Title("Применение настроек выбранного слоя при создании встречи")
    @TestCaseId("924")
    public void shouldApplyLayerSettingsInEventCreate() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(steps.pages().cal().home().leftPanel().addCal())
            .inputsTextInElement(steps.pages().cal().home().addCalSideBar().nameInput(), getRandomName())
            .clicksOn(steps.pages().cal().home().addCalSideBar().createBtn())
            .onMouseHoverAndClick(steps.pages().cal().home().leftPanel().calSettings())
            .clicksOn(steps.pages().cal().home().editCalSideBar().addNotifyBtn())
            .clicksOn(steps.pages().cal().home().editCalSideBar().notifyList().get(0).offsetNotifyInput())
            .clicksOn(steps.pages().cal().home().valueUntilList().get(3))
            .clicksOn(steps.pages().cal().home().editCalSideBar().addNotifyBtn())
            .clicksOn(steps.pages().cal().home().editCalSideBar().notifyList().get(1).unitNotifySelect())
            .clicksOn(steps.pages().cal().home().typeUntilList().get(0))
            .clicksOn(steps.pages().cal().home().editCalSideBar().saveBtn())
            .clicksOn(steps.pages().cal().home().warningPopup().cancelBtn())
            .offsetClick(steps.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .clicksOn(steps.pages().cal().home().newEventPopup().createFromPopupBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPopup())
            .clicksOn(steps.pages().cal().home().eventsTodayList().waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .shouldContainValue(
                steps.pages().cal().home().editEventPage().notifyList().get(0).offsetNotifyInput(),
                "45"
            )
            .shouldContainText(
                steps.pages().cal().home().editEventPage().notifyList().get(0).unitNotifyText(),
                "минут"
            )
            .shouldContainText(
                steps.pages().cal().home().viewSomeoneElseEventPage().notifyList().get(1).unitNotifyText(),
                "в момент события"
            );
    }

    @Step
    @Description("Открытие попапа создания события и ввод названия")
    public void openPopupAndInputName() {
        steps.user().defaultSteps().offsetClick(RANDOM_X_COORD, RANDOM_Y_COORD)
            .clicksOn(steps.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().nameInput(), name);
    }
}
