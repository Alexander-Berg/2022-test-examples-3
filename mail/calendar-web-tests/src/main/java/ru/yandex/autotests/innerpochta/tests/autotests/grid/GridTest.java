package ru.yandex.autotests.innerpochta.tests.autotests.grid;

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
import ru.yandex.autotests.innerpochta.cal.util.CalFragments;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.COLUMN_CENTER;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.MONTH_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TIME_11AM;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.DAY;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.MONTH;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.WEEK;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на элементы в сетках")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class GridTest {

    private static final String DAY_URL = "show_date=2020-09-21";
    private static final String DAY_URL_TEMPLATE = "show_date=%s";
    private static final String UID = "uid";
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
    public static Object[][] grids() {
        return new Object[][]{
            {DAY, 0},
            {WEEK, 1}
        };
    }

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Разворачиваем левую колонку",
            new Params().withIsAsideExpanded(true)
        );
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Кнопка Ещё n открывает сетку на день")
    @TestCaseId("188")
    public void shouldSeeDayGridFromMoreBtn() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        String today = dateFormat.format(LocalDateTime.now());
        steps.user().apiCalSettingsSteps().createCoupleOfEvents(12);
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(MONTH_GRID)
            .clicksOn(steps.pages().cal().home().moreEventBtn())
            .shouldBeOnUrl(containsString(DAY.makeUrlPart(String.format(DAY_URL_TEMPLATE, today))));
    }

    @Test
    @Title("Клик по дате открывает сетку на день из сетки на месяц")
    @TestCaseId("187")
    public void shouldSeeDayGridFromDayBtnMonthGrid() {
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(MONTH.makeUrlPart(DAY_URL))
            .clicksOn(steps.pages().cal().home().dayFifteenInMonthGridBtn())
            .shouldBeOnUrl(containsString(DAY.makeUrlPart(DAY_URL)));
    }

    @Test
    @Title("Клик по дате открывает сетку на день из сетки на неделю")
    @TestCaseId("617")
    public void shouldSeeDayGridFromDayBtnWeekGrid() {
        String day = steps.pages().cal().home().leftPanel().miniCalendar().currentDay().getText();
        changeView(1);
        steps.user().defaultSteps().shouldBeOnUrl(containsString(WEEK.makeUrlPart("")))
            .clicksOn(steps.pages().cal().home().currentDayInWeekGridBtn())
            .shouldBeOnUrl(containsString(DAY.makeUrlPart("")))
            .shouldSeeThatElementHasText(steps.pages().cal().home().dayGridHeaderDay(), day);
    }

    @Test
    @Title("Клик по слоту открывает попап создания события")
    @TestCaseId("194")
    public void shouldOpenCreateEventPopupInMonthGrid() {
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(MONTH.makeUrlPart(DAY_URL))
            // Первая неделя которая расположена в зоне видимости, вторая ячейка (в первой события)
            .offsetClick(steps.pages().cal().home().weeksInMonthGrid().get(4), 300, 70)
            .shouldSee(steps.pages().cal().home().newEventPopup())
            .shouldContainText(steps.pages().cal().home().newEventPopup().timeDateField(), "Весь день");
    }

    @Test
    @Title("Кнопка «Сегодня» редиректит на текущий день")
    @TestCaseId("407")
    public void shouldSeeCurrentDay() {
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(DAY.makeUrlPart(DAY_URL))
            .shouldSee(steps.pages().cal().home().calHeaderBlock().todayBtn())
            .clicksOn(steps.pages().cal().home().calHeaderBlock().todayBtn())
            .shouldBeOnUrl(containsString(DAY.makeUrlPart(UID)));
    }

    @Test
    @Title("Кнопка «Сегодня» редиректит на текущую неделю")
    @TestCaseId("406")
    public void shouldSeeCurrentMonth() {
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(WEEK.makeUrlPart(DAY_URL))
            .shouldSee(steps.pages().cal().home().calHeaderBlock().todayBtn())
            .clicksOn(steps.pages().cal().home().calHeaderBlock().todayBtn())
            .shouldBeOnUrl(containsString(WEEK.makeUrlPart(UID)));
    }


    @Test
    @Title("Кнопка «Сегодня» редиректит на текущий месяц")
    @TestCaseId("998")
    public void shouldSeeCurrentMonthByTodayButton() {
        String day = steps.pages().cal().home().leftPanel().miniCalendar().currentDay().getText();

        changeView(2);
        steps.user().defaultSteps().shouldBeOnUrl(containsString(MONTH.makeUrlPart("")))
            .clicksOn(
                steps.pages().cal().home().calHeaderBlock().nextPeriodBtn(),
                steps.pages().cal().home().calHeaderBlock().nextPeriodBtn()
            )
            .shouldNotSee(steps.pages().cal().home().currentDayInMonthGrid())
            .clicksOn(steps.pages().cal().home().calHeaderBlock().todayBtn())
            .shouldSeeThatElementHasText(steps.pages().cal().home().leftPanel().miniCalendar().selectedDay(), day)
            .shouldSee(steps.pages().cal().home().currentDayInMonthGrid());
    }

    @Test
    @Title("Остаёмся на выбранной дате при отмене создания события")
    @TestCaseId("645")
    @UseDataProvider("grids")
    public void shouldSeeSelectedDate(CalFragments period, int selector) {
        int outsideDaysCount = steps.pages().cal().home().leftPanel().miniCalendar().daysOutMonth().size();

        changeView(selector);
        steps.user().defaultSteps().shouldBeOnUrl(containsString(period.makeUrlPart("")))
            .clicksOn(steps.pages().cal().home().leftPanel().miniCalendar().daysOutMonth().get(outsideDaysCount - 1));
        String currentUrl = steps.user().defaultSteps().getsCurrentUrl();
        steps.user().defaultSteps()
            .offsetClick(steps.pages().cal().home().columnsList().get(0), COLUMN_CENTER, TIME_11AM)
            .shouldSee(steps.pages().cal().home().newEventPopup())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().nameInput(), getRandomString())
            .clicksOn(
                steps.pages().cal().home().newEventPopup().closePopup(),
                steps.pages().cal().home().warningPopup().agreeBtn()
            )
            .shouldBeOnUrl(currentUrl);
    }

    @Test
    @Title("Клик по контролу «Еще n» разворачивает список")
    @TestCaseId("336")
    @UseDataProvider("grids")
    public void shouldSeeAllDayEvents(CalFragments period, int selector) {
        createSimpleAllDayEvent();
        createSimpleAllDayEvent();
        changeView(selector);
        steps.user().defaultSteps().shouldBeOnUrl(containsString(period.makeUrlPart("")))
            .clicksOn(steps.pages().cal().home().moreEventBtn())
            .shouldNotSee(steps.pages().cal().home().moreEventBtn())
            .shouldSee(steps.pages().cal().home().allDayExpandedBlock());
    }

    @Test
    @Title("Переключение на следующий/предыдущий день стрелкой в шапке")
    @TestCaseId("7")
    public void shouldSwitchDay() {
        String nextDay = Integer.toString(LocalDateTime.now().plusDays(1).getDayOfMonth());
        String prevDay = Integer.toString(LocalDateTime.now().minusDays(1).getDayOfMonth());
        changeView(0);
        steps.user().defaultSteps().shouldBeOnUrl(containsString(DAY.makeUrlPart("")))
            .clicksOn(steps.pages().cal().home().calHeaderBlock().nextPeriodBtn())
            .shouldSeeThatElementHasText(steps.pages().cal().home().dayGridHeaderDay(), nextDay)
            .shouldSeeThatElementHasText(steps.pages().cal().home().leftPanel().miniCalendar().selectedDay(), nextDay)
            .clicksOn(
                steps.pages().cal().home().calHeaderBlock().todayBtn(),
                steps.pages().cal().home().calHeaderBlock().pastPeriodBtn()
            )
            .shouldSeeThatElementHasText(steps.pages().cal().home().dayGridHeaderDay(), prevDay)
            .shouldSeeThatElementHasText(steps.pages().cal().home().leftPanel().miniCalendar().selectedDay(), prevDay);
    }

    @Test
    @Title("Переключение на следующую/предыдущую неделю стрелкой в шапке")
    @TestCaseId("7")
    public void shouldSwitchWeek() {
        String nextWeekDay = Integer.toString(LocalDateTime.now().plusDays(7).getDayOfMonth());
        String prevWeekDay = Integer.toString(LocalDateTime.now().minusDays(7).getDayOfMonth());
        changeView(1);
        steps.user().defaultSteps().shouldBeOnUrl(containsString(WEEK.makeUrlPart("")))
            .clicksOn(steps.pages().cal().home().calHeaderBlock().nextPeriodBtn())
            //сетка при переключении не меняет селекторы, поэтому мы цепляемся за элементы, которые
            // уже пропали, прежде, чем что-то проверять, нужно дождаться исчезновения старой сетки
            .waitInSeconds(1)
            .shouldSeeElementInList(steps.pages().cal().home().weekGridHeaderDays(), nextWeekDay);
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().calHeaderBlock().todayBtn(),
            steps.pages().cal().home().calHeaderBlock().pastPeriodBtn()
        )
            .waitInSeconds(1)
            .shouldSeeElementInList(steps.pages().cal().home().weekGridHeaderDays(), prevWeekDay);
    }

    @Step("Создаем простое событие на весь день")
    private void createSimpleAllDayEvent() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withIsAllDay(true);
        steps.user().apiCalSettingsSteps().createNewEvent(event);
    }

    @Step("Переключаем вид")
    private void changeView(int viewNumber) {
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().leftPanel().view(),
            steps.pages().cal().home().selectView().get(viewNumber)
        );
    }
}
