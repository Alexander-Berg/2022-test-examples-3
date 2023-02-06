package ru.yandex.autotests.innerpochta.tests.autotests.event;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.SCHEDULE;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.SCHEDULE_VIEW;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCROLL_PAGE_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Расписание")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.SCHEDULE)
public class ScheduleViewTest {

    private static final String LOCATION = "Сен-Бенуа, Реюньон";
    private static final String ACCEPT_BUTTON = "Пойду";
    private static final String ALL_DAY = "Весь день";
    private static final String SCHEDULE_EVENT_URL = "schedule?show_date=";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount(2);

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
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Включаем вид Расписание",
            new Params().withTouchViewMode(SCHEDULE_VIEW).withIsCalendarsListExpanded(true).withIsAsideExpanded(true)
        );
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + SCHEDULE);
    }

    @Test
    @Title("Создание простого события из расписания")
    @TestCaseId("1270")
    public void shouldCreateEventFromSchedule() {
        LocalDateTime dateFuture = LocalDateTime.now().plusMonths(3);
        String urlDate = DATE_FORMAT.format(dateFuture);
        String name = createFutureEvent();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().createFromPageBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldBeOnUrl(containsString(SCHEDULE_EVENT_URL + urlDate))
            .shouldContainText(steps.pages().cal().home().schedulePage().eventsSchedule().get(0).eventName(), name);
    }

    @Test
    @Title("Открыть и закрыть простое событие")
    @TestCaseId("1264")
    public void shouldCloseEventFromSchedule() {
        createTodayEvent();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().schedulePage().eventsSchedule().get(0))
            .shouldSee(
                steps.pages().cal().home().schedulePage().eventPreviewSchedule(),
                steps.pages().cal().home().schedulePage().activeEvent()
            )
            .clicksOn(steps.pages().cal().home().schedulePage().eventPreviewSchedule().closeEventPreviewShedule())
            .shouldNotSee(
                steps.pages().cal().home().schedulePage().eventPreviewSchedule(),
                steps.pages().cal().home().schedulePage().activeEvent()
            );
    }

    @Test
    @Title("Удалить простое событие")
    @TestCaseId("1302")
    public void shouldDeleteEventFromSchedule() {
        createTodayEvent();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().schedulePage().eventsSchedule().get(0))
            .shouldSee(
                steps.pages().cal().home().schedulePage().eventPreviewSchedule(),
                steps.pages().cal().home().schedulePage().activeEvent()
            )
            .clicksOn(
                steps.pages().cal().home().schedulePage().eventPreviewSchedule().eventDeleteButton(),
                steps.pages().cal().home().removeEventPopup().removeOneBtn()
            )
            .shouldNotSee(steps.pages().cal().home().schedulePage().eventPreviewSchedule())
            .shouldSeeElementsCount(steps.pages().cal().home().schedulePage().eventsSchedule(), 0);
    }

    @Test
    @Title("Попадаем на дату события после создания события")
    @TestCaseId("1299")
    public void shouldBeOnEventDate() {
        LocalDateTime dateFuture = LocalDateTime.now().plusMonths(3);
        String urlDateFuture = DATE_FORMAT.format(dateFuture);
        String eventDateFuture = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(dateFuture);
        LocalDateTime date = LocalDateTime.now().minusWeeks(1);
        String urlDate = DATE_FORMAT.format(date);
        String name = getRandomName();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().calHeaderBlock().pastPeriodBtn())
            .shouldBeOnUrl(containsString(SCHEDULE_EVENT_URL + urlDate))
            .clicksOn(steps.pages().cal().home().schedulePage().addEventButtonSchedule())
            .shouldSee(steps.pages().cal().home().newEventPage())
            .clicksOn(steps.pages().cal().home().newEventPage().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), name)
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPage().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPage().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().descriptionInput(), getRandomName())
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().newEventPage().dateInputList().get(0),
                eventDateFuture
            )
            .clicksOn(steps.pages().cal().home().newEventPage().locationfiled())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().locationInput(), LOCATION)
            .executesJavaScript(SCROLL_PAGE_SCRIPT)
            .clicksOn(
                steps.pages().cal().home().newEventPage().title(),
                steps.pages().cal().home().newEventPage().createFromPageBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldBeOnUrl(containsString(SCHEDULE_EVENT_URL + urlDateFuture))
            .shouldContainText(steps.pages().cal().home().schedulePage().eventsSchedule().get(0).eventName(), name);
    }

    @Test
    @Title("Попадаем на сегодняшнюю дату при клике на календарь")
    @TestCaseId("1300")
    public void shouldBeOnTodayDate() {
        LocalDateTime date = LocalDateTime.now().minusWeeks(1);
        String urlDate = DATE_FORMAT.format(date);
        String day = steps.pages().cal().home().leftPanel().miniCalendar().currentDay().getText();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().calHeaderBlock().pastPeriodBtn())
            .shouldBeOnUrl(containsString(SCHEDULE_EVENT_URL + urlDate))
            .clicksOn(steps.pages().cal().home().calHeaderBlock().calLink())
            .shouldBeOnUrl(containsString("schedule?uid="))
            .shouldSeeThatElementHasText(steps.pages().cal().home().leftPanel().miniCalendar().selectedDay(), day);
    }

    @Test
    @Title("Создание регулярного события")
    @TestCaseId("1271")
    public void shouldCreateRegularEvent() {
        LocalDateTime date = LocalDateTime.now();
        String urlDate = DATE_FORMAT.format(date);
        String name = getRandomName();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().schedulePage().addEventButtonSchedule())
            .shouldSee(steps.pages().cal().home().newEventPage())
            .clicksOn(steps.pages().cal().home().newEventPage().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), name)
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPage().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPage().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().descriptionInput(), getRandomName())
            .turnTrue(steps.pages().cal().home().newEventPage().repeatEventCheckBox())
            .clicksOn(steps.pages().cal().home().beforeDateField())
            .shouldSee(steps.pages().cal().home().beforeDateMiniCalendar());
        int daysCount = steps.pages().cal().home().beforeDateMiniCalendar().daysOutMonth().size();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().beforeDateMiniCalendar().daysOutMonth()
            .get(daysCount - 1))
            .clicksOn(steps.pages().cal().home().newEventPage().createFromPageBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldBeOnUrl(containsString(SCHEDULE_EVENT_URL + urlDate))
            .shouldContainText(steps.pages().cal().home().schedulePage().eventsSchedule().get(0).eventName(), name)
            .shouldSee(steps.pages().cal().home().schedulePage().repeatIcon());
    }

    @Test
    @Title("Создание события на весь день из расписания")
    @TestCaseId("1272")
    public void shouldCreateAllDayEventFromSchedule() {
        LocalDateTime dateFuture = LocalDateTime.now().plusMonths(3);
        String urlDate = DATE_FORMAT.format(dateFuture);
        String name = createFutureEvent();
        steps.user().defaultSteps()
            .turnTrue(steps.pages().cal().home().newEventPage().allDayCheckBox())
            .clicksOn(steps.pages().cal().home().newEventPage().createFromPageBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldBeOnUrl(containsString(SCHEDULE_EVENT_URL + urlDate))
            .shouldContainText(steps.pages().cal().home().schedulePage().eventsSchedule().get(0).eventName(), name)
            .shouldContainText(steps.pages().cal().home().schedulePage().eventsSchedule().get(0).eventTime(), ALL_DAY);
    }

    @Test
    @Title("Навигация стрелочками в шапке")
    @TestCaseId("1267")
    public void shouldNavigateByArrows() {
        LocalDateTime datePast = LocalDateTime.now().minusWeeks(1);
        LocalDateTime dateFuture = LocalDateTime.now().plusWeeks(1);
        String urlDatePast = DATE_FORMAT.format(datePast);
        String urlDateFuture = DATE_FORMAT.format(dateFuture);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().calHeaderBlock().pastPeriodBtn())
            .shouldBeOnUrl(containsString(SCHEDULE_EVENT_URL + urlDatePast))
            .clicksOn(
                steps.pages().cal().home().calHeaderBlock().nextPeriodBtn(),
                steps.pages().cal().home().calHeaderBlock().nextPeriodBtn()
            )
            .shouldBeOnUrl(containsString(SCHEDULE_EVENT_URL + urlDateFuture));
    }

    @Test
    @Title("Попадаем на сегодняшнюю дату при клике на «Сегодня»")
    @TestCaseId("1268")
    public void shouldBeOnToday() {
        LocalDateTime date = LocalDateTime.now().minusWeeks(1);
        String urlDate = DATE_FORMAT.format(date);
        String day = steps.pages().cal().home().leftPanel().miniCalendar().currentDay().getText();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().calHeaderBlock().pastPeriodBtn())
            .shouldBeOnUrl(containsString(SCHEDULE_EVENT_URL + urlDate))
            .clicksOn(steps.pages().cal().home().calHeaderBlock().todayBtn())
            .shouldBeOnUrl(containsString("schedule?uid="))
            .shouldSeeThatElementHasText(steps.pages().cal().home().leftPanel().miniCalendar().selectedDay(), day);
    }

    @Test
    @Title("Вылючаем/Включаем слой")
    @TestCaseId("1280")
    public void shouldTurnOnOffLayer() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID);
        steps.user().apiCalSettingsSteps().createNewEvent(event).togglerLayer(layerID, true);
        steps.user().defaultSteps().refreshPage()
            .shouldSee(steps.pages().cal().home().schedulePage().eventsSchedule().get(0))
            .deselects(steps.pages().cal().home().leftPanel().layerCalCheckBox())
            .shouldSeeElementsCount(steps.pages().cal().home().schedulePage().eventsSchedule(), 0)
            .turnTrue(steps.pages().cal().home().leftPanel().layerCalCheckBox())
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().home().schedulePage().eventsSchedule().get(0).eventName(),
                event.getName()
            );
    }

    @Test
    @Title("Навигация с помощью мини календаря")
    @TestCaseId("1269")
    public void shouldNavigateWithMiniCalendar() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID);
        steps.user().apiCalSettingsSteps().createNewEvent(event).togglerLayer(layerID, true);
        String day = steps.pages().cal().home().leftPanel().miniCalendar().daysOutMonth().get(0).getText();
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().cal().home().leftPanel().miniCalendar().daysOutMonth().get(0))
            .shouldBeOnUrl(containsString(SCHEDULE_EVENT_URL))
            .shouldContainTextInUrl(day)
            .shouldSee(steps.pages().cal().home().schedulePage().eventsSchedule().get(0))
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().home().schedulePage().eventsSchedule().get(0).eventName(),
                event.getName()
            );
    }

    @Test
    @Title("Принятие решения о встрече")
    @TestCaseId("1281")
    public void shouldAcceptEvent() {
        String eventName = createEventWithAttendees();
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().refreshPage()
            .opensUrl(UrlProps.urlProps().getBaseUri() + SCHEDULE)
            .clicksOnElementWithText(steps.pages().cal().home().schedulePage().eventsSchedule(), eventName)
            .clicksOn(steps.pages().cal().home().schedulePage().eventPreviewSchedule().buttonYes())
            .clicksOnElementWithText(steps.pages().cal().home().schedulePage().eventsSchedule(), eventName)
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().schedulePage().eventPreviewSchedule().buttonDecision(),
                ACCEPT_BUTTON
            );
    }

    @Step
    private String createFutureEvent() {
        LocalDateTime date = LocalDateTime.now().plusMonths(3);
        String eventDate = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(date);
        String name = getRandomName();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().schedulePage().addEventButtonSchedule())
            .shouldSee(steps.pages().cal().home().newEventPage())
            .clicksOn(steps.pages().cal().home().newEventPage().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), name)
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPage().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPage().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().descriptionInput(), getRandomName())
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().newEventPage().dateInputList().get(0),
                eventDate
            )
            .clicksOn(steps.pages().cal().home().newEventPage().locationfiled())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().locationInput(), LOCATION)
            .clicksOn(steps.pages().cal().home().newEventPage().title());
        return name;
    }

    @Step
    private void createTodayEvent() {
        LocalDateTime dateTime = LocalDateTime.now();
        String eventName = getRandomName();
        String description = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withDescription(description).withStartTs(DATE_FORMAT.format(dateTime) + "T12:00:00")
            .withEndTs(DATE_FORMAT.format(dateTime) + "T13:00:00").withLocation(LOCATION);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
    }

    @Step("Создать событие по приглашению")
    private String createEventWithAttendees() {
        String eventName = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        return eventName;
    }
}
