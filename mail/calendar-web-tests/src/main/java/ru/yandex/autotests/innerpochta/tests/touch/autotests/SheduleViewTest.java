package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.AVAILABILITY_MAYBE_BUSY;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DAY_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.PLANNING_BUTTON;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.PURPLE_COLOR;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.SCHEDULE;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.SCHEDULE_BUTTON;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.SCHEDULE_VIEW;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("[Тач] Расписание")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.SCHEDULE)
public class SheduleViewTest {

    private static final String EVENT_PLACE = "Пискарёвский проспект, 1";
    private static final String TIME = "T12:00:00";
    private static final String TEXT_COLOR_ATTRIBUTE = "color";
    private static final String GRAY_COLOR = "rgba(153, 153, 153, 1)";
    private static final String BLACK_COLOR = "rgba(51, 51, 51, 1)";
    private static final int EVENTS_COUNT = 20;

    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().createNewLayer(
            steps.user().settingsCalSteps().formDefaultLayer().withColor(PURPLE_COLOR)
        )
            .updateUserSettings("Включаем вид Расписание", new Params().withTouchViewMode(SCHEDULE_VIEW));
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Переключение вида календаря")
    @TestCaseId("1224")
    public void shouldSeeScheduleView() {
        Event event = steps.user().settingsCalSteps()
            .formDefaultEvent(steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId());
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + DAY_GRID)
            .clicksOn(steps.pages().cal().touchHome().burger())
            .clicksOnElementWithText(steps.pages().cal().touchHome().sidebar().viewButtons(), SCHEDULE_BUTTON)
            .shouldSee(steps.pages().cal().touchHome().shedulePage())
            .clicksOn(steps.pages().cal().touchHome().burger())
            .clicksOnElementWithText(steps.pages().cal().touchHome().sidebar().viewButtons(), PLANNING_BUTTON)
            .shouldSee(steps.pages().cal().touchHome().grid());
    }

    @Test
    @Title("Создание простого события в сетке расписание")
    @TestCaseId("1226")
    public void createSimpleEvent() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + SCHEDULE);
        steps.user().calTouchCreateEventSteps().createEventBuilderShedule()
            .withTitle(Utils.getRandomName())
            .withDescription(Utils.getRandomName())
            .withPlace(EVENT_PLACE)
            .withParticipants(DEV_NULL_EMAIL)
            .withLayer(steps.user().apiCalSettingsSteps().getUserLayers().get(1).getName())
            .withAvailability(AVAILABILITY_MAYBE_BUSY)
            .submit()
            .openEventInSchedule()
            .checkFields();
    }

    @Test
    @Title("Создание события на будущую дату в сетке расписание")
    @TestCaseId("1226")
    public void createSimpleEventInFuture() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        String date = dateFormat.format(LocalDateTime.now().plusDays(1));
        String startDate = date + TIME;
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + SCHEDULE);
        steps.user().calTouchCreateEventSteps().createEventBuilderShedule()
            .withTitle(Utils.getRandomName())
            .withDescription(Utils.getRandomName())
            .withPlace(EVENT_PLACE)
            .withParticipants(DEV_NULL_EMAIL)
            .withLayer(steps.user().apiCalSettingsSteps().getUserLayers().get(1).getName())
            .withAvailability(AVAILABILITY_MAYBE_BUSY)
            .setDate(steps.pages().cal().touchHome().eventPage().startDateInput(), startDate)
            .submit()
            .openEventInSchedule()
            .checkFields();
        steps.user().defaultSteps().shouldBeOnUrl(containsString(SCHEDULE + "?show_date=" + date));
    }

    @Test
    @Title("Удаление простого события в сетке расписание")
    @TestCaseId("1227")
    public void deleteSimpleEvent() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + SCHEDULE);
        steps.user().apiCalSettingsSteps().createNewEvent(
            steps.user().settingsCalSteps()
                .formDefaultEvent(steps.user().apiCalSettingsSteps().getUserLayersIds().get(0))
        );
        steps.user().defaultSteps().refreshPage()
            .clicksOn(
                steps.pages().cal().touchHome().eventsShedule().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().delete(),
                steps.pages().cal().touchHome().deleteEvenPopup().confirmOrOneEventBtn()
            )
            .shouldNotSee(steps.pages().cal().touchHome().eventPage())
            .shouldSeeElementsCount(steps.pages().cal().touchHome().eventsShedule(), 0);
    }

    @Test
    @Title("Создание события на весь день в сетке расписание")
    @TestCaseId("1230")
    public void createAllDayEvent() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        String date = dateFormat.format(LocalDateTime.now().plusDays(1));
        String startDate = date + TIME;
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + SCHEDULE);
        steps.user().calTouchCreateEventSteps().createEventBuilderShedule()
            .withTitle(Utils.getRandomName())
            .withDescription(Utils.getRandomName())
            .setDate(steps.pages().cal().touchHome().eventPage().startDateInput(), startDate)
            .withPlace(EVENT_PLACE)
            .withAllDayCheckbox(true)
            .withParticipants(DEV_NULL_EMAIL)
            .withLayer(steps.user().apiCalSettingsSteps().getUserLayers().get(1).getName())
            .withAvailability(AVAILABILITY_MAYBE_BUSY)
            .submit()
            .openEventInSchedule()
            .checkFields();
        steps.user().defaultSteps().shouldBeOnUrl(containsString(SCHEDULE + "?show_date=" + date));
    }

    @Test
    @Title("Создание события на несколько дней в сетке расписание")
    @TestCaseId("1230")
    public void createLongAllDayEvent() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        String date = dateFormat.format(LocalDateTime.now().plusDays(1));
        String dateTwo = dateFormat.format(LocalDateTime.now().plusDays(3));
        String startDate = date + TIME;
        String finishDate = dateTwo + TIME;
        String startDateInEvent = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDateTime.now().plusDays(1));
        String finishDateInEvent = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDateTime.now().plusDays(3));
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + SCHEDULE);
        steps.user().calTouchCreateEventSteps().createEventBuilderShedule()
            .withTitle(Utils.getRandomName())
            .withDescription(Utils.getRandomName())
            .setDate(steps.pages().cal().touchHome().eventPage().startDateInput(), startDate)
            .setDate(steps.pages().cal().touchHome().eventPage().finishDateInput(), finishDate)
            .withPlace(EVENT_PLACE)
            .withAllDayCheckbox(true)
            .withParticipants(DEV_NULL_EMAIL)
            .withLayer(steps.user().apiCalSettingsSteps().getUserLayers().get(1).getName())
            .withAvailability(AVAILABILITY_MAYBE_BUSY)
            .submit()
            .openEventInSchedule();
        steps.user().defaultSteps().shouldContainText(
            steps.pages().cal().touchHome().eventPage().eventDate(),
            "С " + startDateInEvent + " по " + finishDateInEvent
        );
        steps.user().defaultSteps().shouldBeOnUrl(containsString("show_date=" + date));
    }

    @Test
    @Title("Редактирование события в сетке расписание")
    @TestCaseId("1258")
    public void editSimpleEvent() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        String date = dateFormat.format(LocalDateTime.now().plusDays(1));
        String startDate = date + TIME;
        String title = Utils.getRandomName();
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + SCHEDULE);
        steps.user().apiCalSettingsSteps().createNewEvent(
            steps.user().settingsCalSteps()
                .formDefaultEvent(steps.user().apiCalSettingsSteps().getUserLayersIds().get(0))
        );
        steps.user().defaultSteps().refreshPage()
            .clicksOn(
                steps.pages().cal().touchHome().eventsShedule().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().edit()
            );
        steps.user().defaultSteps()
            .inputsTextInElement(
                steps.pages().cal().touchHome().eventPage().editableTitle(),
                title
            );
        steps.user().calTouchCreateEventSteps()
            .setDate(steps.pages().cal().touchHome().eventPage().startDateInput(), startDate)
            .submit();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().eventsShedule().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps()
            .shouldContainText(
                steps.pages().cal().touchHome().eventPage().title(),
                title
            )
            .shouldContainText(
                steps.pages().cal().touchHome().eventPage().eventDate(),
                Integer.toString(LocalDateTime.now().plusDays(1).getDayOfMonth())
            )
            .shouldContainText(
                steps.pages().cal().touchHome().eventPage().eventDate(),
                "12:00"
            );
    }

    @Test
    @Title("Время серое для прошлых событий")
    @TestCaseId("1228")
    public void shouldSeePastTime() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + SCHEDULE);
        steps.user().apiCalSettingsSteps().createNewEvent(
            steps.user().settingsCalSteps()
                .formEventInPast(steps.user().apiCalSettingsSteps().getUserLayersIds().get(0), 1)
        );
        steps.user().defaultSteps().refreshPage()
            .shouldContainCSSAttributeWithValue(
                steps.pages().cal().touchHome().shedulePage().eventTimePast(),
                TEXT_COLOR_ATTRIBUTE,
                GRAY_COLOR
            );
    }

    @Test
    @Title("Время черное для будущих событий")
    @TestCaseId("1228")
    public void shouldSeeFutureTime() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + SCHEDULE);
        steps.user().apiCalSettingsSteps().createNewEvent(
            steps.user().settingsCalSteps()
                .formEventAfterNDays(steps.user().apiCalSettingsSteps().getUserLayersIds().get(0), 1)
        );
        steps.user().defaultSteps().refreshPage()
            .shouldNotSee(steps.pages().cal().touchHome().shedulePage().eventTimePast())
            .shouldContainCSSAttributeWithValue(
                steps.pages().cal().touchHome().shedulePage().eventTime(),
                TEXT_COLOR_ATTRIBUTE,
                BLACK_COLOR
            );
    }

    @Test
    @Title("Возвращаемся в расписание из просмотра событий")
    @TestCaseId("1256")
    public void viewScheduleFromEventPage() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        String date = dateFormat.format(LocalDateTime.now());
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(getRandomName())
            .withStartTs(date + TIME)
            .withEndTs(date + "T21:00:00");
        steps.user().apiCalSettingsSteps().createCoupleOfAllDayEvents(EVENTS_COUNT)
            .createNewEvent(event);
        steps.user().defaultSteps().refreshPage()
            .scrollTo(steps.pages().cal().touchHome().eventsShedule().waitUntil(not(empty())).get(EVENTS_COUNT))
            .clicksOn(steps.pages().cal().touchHome().eventsShedule().get(EVENTS_COUNT / 2))
            .shouldSee(steps.pages().cal().touchHome().eventPage())
            .clicksOn(steps.pages().cal().touchHome().eventPage().cancelEdit())
            .shouldSee(steps.pages().cal().touchHome().eventsShedule().waitUntil(not(empty())).get(EVENTS_COUNT));
    }
}
