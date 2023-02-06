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
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_BASE_URL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Попапы подверждений редактирования события")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.EDIT_EVENT)
public class ChangeEventConfirmPopupTest {

    private static final String ANOTHER_USER_EMAIL = "yandex-team-mailt-126@yandex.ru";
    private static final int EVENT_TIME_POSITION_START = 31;
    private static final int EVENT_TIME_POSITION_END = 8;
    private String eventName;
    private Long layerID;
    private DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
    private String date = dateFormat.format(LocalDateTime.now());


    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount(2);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        eventName = getRandomString();
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Не показываем модальное окно при изменении участников - Изменение названия")
    @TestCaseId("781")
    public void shouldNotSeeConfirmPopupChangeSubject() {
        String subject = getRandomString();
        prepareEventForChanges();
        steps.user().defaultSteps().inputsTextInElement(steps.pages().cal().home().editEventPage().nameField(), subject)
            .clicksOn(steps.pages().cal().home().editEventPage().saveChangesBtn())
            .shouldNotSee(steps.pages().cal().home().warPopups());
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Не показываем модальное окно при изменении участников - Изменение описания")
    @TestCaseId("781")
    public void shouldNotSeeConfirmPopupChangeDescription() {
        prepareEventForChanges();
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().cal().home().editEventPage().descriptionField(), getRandomString())
            .clicksOn(steps.pages().cal().home().editEventPage().saveChangesBtn())
            .shouldNotSee(steps.pages().cal().home().warPopups());
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().shouldSeeMessageWithSubject(eventName);
    }

    @Test
    @Title("Не показываем модальное окно при изменении участников - Изменение времени начала")
    @TestCaseId("781")
    public void shouldNotSeeConfirmPopupChangeStartTime() {
        prepareEventForChanges();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().editEventPage().timeStart())
            .onMouseHoverAndClick(steps.pages().cal().home().timesList().get(EVENT_TIME_POSITION_START))
            .clicksOn(steps.pages().cal().home().editEventPage().saveChangesBtn())
            .shouldNotSee(steps.pages().cal().home().warPopups());
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().shouldSeeMessageWithSubject(eventName);
    }

    @Test
    @Title("Не показываем модальное окно при изменении участников - Изменение времени окончания")
    @TestCaseId("781")
    public void shouldNotSeeConfirmPopupChangeEndTime() {
        prepareEventForChanges();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().editEventPage().timeEnd())
            .scrollAndClicksOn(steps.pages().cal().home().timeEndVariants().get(EVENT_TIME_POSITION_END))
            .clicksOn(steps.pages().cal().home().editEventPage().saveChangesBtn())
            .shouldNotSee(steps.pages().cal().home().warPopups());
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().shouldSeeMessageWithSubject(eventName);
    }

    @Test
    @Title("Не показываем модальное окно при изменении участников - Весь день")
    @TestCaseId("781")
    public void shouldNotSeeConfirmPopupChangeAllDay() {
        prepareEventForChanges();
        steps.user().defaultSteps()
            .turnTrue(steps.pages().cal().home().editEventPage().allDayCheckBox())
            .clicksOn(steps.pages().cal().home().editEventPage().saveChangesBtn())
            .shouldNotSee(steps.pages().cal().home().warPopups());
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().shouldSeeMessageWithSubject(eventName);
    }

    @Test
    @Title("Не показываем модальное окно при изменении участников - Весь день")
    @TestCaseId("781")
    public void shouldNotSeeConfirmPopupChangeRepeat() {
        prepareEventForChanges();
        steps.user().defaultSteps()
            .turnTrue(steps.pages().cal().home().editEventPage().repeatEventCheckBox())
            .clicksOn(steps.pages().cal().home().editEventPage().saveChangesBtn())
            .shouldNotSee(steps.pages().cal().home().warPopups());
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().shouldSeeMessageWithSubject(eventName);
    }

    @Test
    @Title("Не показываем модальное окно при изменении участников - Место")
    @TestCaseId("781")
    public void shouldNotSeeConfirmPopupChangeLocation() {
        prepareEventForChanges();
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().cal().home().editEventPage().locationInput(), getRandomString())
            .clicksOn(steps.pages().cal().home().editEventPage().saveChangesBtn())
            .shouldNotSee(steps.pages().cal().home().warPopups());
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().shouldSeeMessageWithSubject(eventName);
    }

    @Step("Подготавливаем событие для редактирования")
    private void prepareEventForChanges() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withDescription(getRandomString())
            .withStartTs(date + "T13:00:00")
            .withEndTs(date + "T14:00:00")
            .withLocation(getRandomString());
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().defaultSteps().refreshPage()
            .clicksOn(
                steps.pages().cal().home().eventsAllList().get(0),
                steps.pages().cal().home().viewEventPopup().editEventBtn()
            )
            .inputsTextInElement(steps.pages().cal().home().editEventPage().membersInput(), ANOTHER_USER_EMAIL);
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ENTER.toString());
    }

}
