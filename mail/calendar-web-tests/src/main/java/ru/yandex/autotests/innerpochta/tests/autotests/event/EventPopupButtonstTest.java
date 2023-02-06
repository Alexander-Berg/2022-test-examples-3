package ru.yandex.autotests.innerpochta.tests.autotests.event;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.DISABLE_ALERT_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Кнопки в попапе события")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.VIEW_EVENT_POPUP)
public class EventPopupButtonstTest {

    private Long layerID;
    private static final String OTHER_USER_EMAIL = "robbiter-9289715330@yandex.ru";
    private static final String EVENT_LINK = "event";

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
    @Title("Копировать ссылку на простое событие")
    @TestCaseId("1151")
    public void shouldCopyEventLink() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDate date = LocalDate.now();
        LocalDateTime dateTime = LocalDateTime.now();
        String parsedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String eventName = getRandomName();
        String description = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withDescription(description).withStartTs(dateFormat.format(dateTime).split("[T]")[0] + "T12:00:00")
            .withEndTs(dateFormat.format(dateTime).split("[T]")[0] + "T13:00:00");
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        copyEventLink(eventName, parsedDate, description);
    }

    @Test
    @Title("Копировать ссылку на повторяющееся событие")
    @TestCaseId("1152")
    public void shouldCopyRepeatingEventLink() {
        LocalDate date = LocalDate.now();
        String parsedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String eventName = getRandomName();
        String description = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultRepeatingEvent(layerID).withName(eventName)
            .withDescription(description);
        steps.user().apiCalSettingsSteps().createNewRepeatEvent(event);
        copyEventLink(eventName, parsedDate, description);
        steps.user().defaultSteps().shouldSee(steps.pages().cal().home().newEventPage().eventWarning());
    }

    @Test
    @Title("Написать участникам встречи")
    @TestCaseId("989")
    public void shouldSeeMail() {
        String eventName = getRandomName();
        String description = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName).withIsAllDay(true)
            .withDescription(description);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(OTHER_USER_EMAIL, DEV_NULL_EMAIL));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().writeMail())
            .switchOnJustOpenedWindow()
            .shouldSee(steps.pages().mail().composePopup().expandedPopup())
            .shouldHasValue(steps.pages().mail().composePopup().expandedPopup().sbjInput(), eventName)
            .shouldSeeThatElementHasText(
                steps.pages().mail().composePopup().yabbleToList().get(0),
                OTHER_USER_EMAIL.split("[@]")[0]
            )
            .shouldSeeThatElementHasText(
                steps.pages().mail().composePopup().yabbleToList().get(1),
                DEV_NULL_EMAIL.split("[@]")[0]
            )
            .shouldNotSeeElementInList(
                steps.pages().mail().composePopup().yabbleToList(),
                lock.firstAcc().getLogin()
            );
    }

    @Step("Открываем событие, копируем ссылку и переходим по ней")
    private void copyEventLink(String eventName, String parsedDate, String description) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().copyEventBtn())
            .clicksOn(steps.pages().cal().home().viewEventPopup().closeEventBtn())
            .clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .shouldSee(steps.pages().cal().home().newEventPage())
            .clicksOn(steps.pages().cal().home().newEventPage().nameInput());
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().cal().home().newEventPage().nameInput(),
            Keys.chord(Keys.CONTROL, "v")
        );
        String link = steps.pages().cal().home().newEventPage().nameInput().getAttribute("value");
        steps.user().defaultSteps().executesJavaScript(DISABLE_ALERT_SCRIPT)
            .opensUrl(link)
            .shouldBeOnUrl(containsString(EVENT_LINK))
            .shouldHasValue(steps.pages().cal().home().newEventPage().nameInput(), eventName)
            .shouldHasValue(steps.pages().cal().home().newEventPage().dateList().get(1), parsedDate)
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().descriptionInput(), description);
    }

}
