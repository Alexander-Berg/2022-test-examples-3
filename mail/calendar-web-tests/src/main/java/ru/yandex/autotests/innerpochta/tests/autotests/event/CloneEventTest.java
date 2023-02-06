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
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Клонирование событий")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.CLONE_EVENT)
public class CloneEventTest {

    private Long layerID;
    private static final String OTHER_USER_EMAIL = "robbiter-9289715330@yandex.ru";

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
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Клонирование своего события")
    @TestCaseId("1242")
    public void shouldCreateCloneEvent() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDate date = LocalDate.now();
        LocalDateTime dateTime = LocalDateTime.now();
        String parsedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String eventName = getRandomName();
        String description = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withDescription(description).withStartTs(dateFormat.format(dateTime).split("[T]")[0] + "T12:00:00")
            .withEndTs(dateFormat.format(dateTime).split("[T]")[0] + "T13:00:00");
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(OTHER_USER_EMAIL));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().cloneEventBtn())
            .switchOnJustOpenedWindow()
            .shouldHasValue(steps.pages().cal().home().newEventPage().nameInput(), eventName)
            .shouldHasValue(steps.pages().cal().home().newEventPage().time(), "12:00")
            .shouldHasValue(steps.pages().cal().home().newEventPage().timeEnd(), "13:00")
            .shouldHasValue(steps.pages().cal().home().newEventPage().dateList().get(1), parsedDate)
            .shouldHasValue(steps.pages().cal().home().newEventPage().dateList().get(0), parsedDate)
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().descriptionInput(), description)
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().membersField(), OTHER_USER_EMAIL);
    }

    @Test
    @Title("Клонирование своего события на весь день")
    @TestCaseId("1242")
    public void shouldCreateCloneAllDayEvent() {
        LocalDate date = LocalDate.now();
        String parsedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String eventName = getRandomName();
        String description = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName).withIsAllDay(true)
            .withDescription(description);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(OTHER_USER_EMAIL));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().cloneEventBtn())
            .switchOnJustOpenedWindow()
            .shouldHasValue(steps.pages().cal().home().newEventPage().nameInput(), eventName)
            .shouldHasValue(steps.pages().cal().home().newEventPage().dateList().get(0), parsedDate)
            .shouldHasValue(steps.pages().cal().home().newEventPage().dateList().get(1), parsedDate)
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().descriptionInput(), description)
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().membersField(), OTHER_USER_EMAIL)
            .shouldBeSelected(steps.pages().cal().home().newEventPage().allDayCheckBox());
    }
}
