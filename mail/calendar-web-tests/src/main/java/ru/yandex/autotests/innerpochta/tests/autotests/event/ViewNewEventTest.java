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
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_BASE_URL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Просмотр события по приглашению")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.VIEW_EVENT_POPUP)
public class ViewNewEventTest {

    private String eventName;
    private String eventComment;
    private Long layerID;

    private static final String SUBJECT = "Default Фамилия Default-Имя %s ваше приглашение на " +
        "встречу «%s»";
    private static final String REJECT = "отклонил";
    private static final String ACCEPT = "принял";
    private static final String MAYBE_ACCEPT = "условно принял";
    private static final String MAYBE_BUTTON = "Возможно, пойду";
    private static final String ACCEPT_BUTTON = "Пойду";
    private static final String EVENT_NOT_DECIDED_BACKGROUND_COLOR = "background-color: rgba(255, 255, 255, 0.8)";
    private static final String HIDE_BTN = "Скрыть";
    private static final int SUBSCRIBERS_COUNT = 17;
    private static final int SUBSCRIBERS_COUNT_HIDE = 10;
    private List<String> MANY_ATTENDEES = Arrays.asList(
        "yandex-team-mailt-190@yandex.ru",
        "yandex-team-mailt-191@yandex.ru",
        "yandex-team-mailt-192@yandex.ru",
        "yandex-team-mailt-193@yandex.ru",
        "yandex-team-mailt-194@yandex.ru",
        "yandex-team-mailt-195@yandex.ru",
        "yandex-team-mailt-196@yandex.ru",
        "yandex-team-mailt-198@yandex.ru",
        "yandex-team-mailt-199@yandex.ru",
        "yandex-team-mailt-200@yandex.ru",
        "yandex-team-mailt-201@yandex.ru",
        "yandex-team-mailt-202@yandex.ru",
        "yandex-team-mailt-203@yandex.ru",
        "yandex-team-mailt-204@yandex.ru",
        "yandex-team-mailt-205@yandex.ru",
        "yandex-team-mailt-206@yandex.ru"
    );

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().usePreloadedTusAccounts(2);
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock.accNum(1));

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(auth2)
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        eventName = getRandomString();
        eventComment = getRandomString();
        steps.user().apiCalSettingsSteps().withAuth(auth2).deleteTodayEvents();
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
    }

    @Test
    @Title("Модальное окно при отказе от простой встречи: кнопка Отменить")
    @TestCaseId("788")
    public void shouldCancelEditing() {
        createEvent();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().buttonNo()
        )
            .shouldSee(steps.pages().cal().home().eventDecisionPopup())
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().cancelPopupBtn())
            .shouldNotSee(steps.pages().cal().home().eventDecisionPopup())
            .shouldSee(steps.pages().cal().home().viewEventPopup());
    }

    @Test
    @Title("Отказ от простой встречи с комментарием организатору")
    @TestCaseId("797")
    public void shouldRejectEvent() {
        createEvent();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().buttonNo()
        )
            .shouldSee(steps.pages().cal().home().eventDecisionPopup())
            .inputsTextInElement(steps.pages().cal().home().eventDecisionPopup().commentInput(), eventComment)
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().rejectThisEventBtn())
            .shouldNotSee(
                steps.pages().cal().home().eventDecisionPopup(),
                steps.pages().cal().home().viewEventPopup()
            );
        shouldSeeRejectMessage();
    }

    @Test
    @Title("Согласие на простую встречу")
    @TestCaseId("20")
    public void shouldAcceptEvent() {
        createEvent();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().buttonYes()
        )
            .shouldNotSee(steps.pages().cal().home().viewEventPopup())
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().viewEventPopup().buttonDecision(),
                ACCEPT_BUTTON
            )
            .shouldSee(steps.pages().cal().home().viewEventPopup().acceptMark());
        steps.user().loginSteps().multiLoginWith(lock.firstAcc());
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup().acceptMark())
            .opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().shouldSeeMessageWithSubject(String.format(SUBJECT, ACCEPT, eventName));
    }

    @Test
    @Title("Изменение решения из попапа просмотра")
    @TestCaseId("954")
    public void shouldChangeDecision() {
        createEvent();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().buttonYes()
        )
            .shouldNotSee(steps.pages().cal().home().viewEventPopup())
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().viewEventPopup().buttonDecision(),
                ACCEPT_BUTTON
            )
            .shouldSee(steps.pages().cal().home().viewEventPopup().acceptMark())
            .clicksOn(
                steps.pages().cal().home().viewEventPopup().buttonDecision(),
                steps.pages().cal().home().solutionsBtnMaybe()
            )
            .shouldNotSee(steps.pages().cal().home().viewEventPopup())
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().viewEventPopup().buttonDecision(),
                MAYBE_BUTTON
            )
            .shouldSee(steps.pages().cal().home().viewEventPopup().questionMark());
        steps.user().loginSteps().multiLoginWith(lock.firstAcc());
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup().questionMark())
            .opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().shouldSeeMessageWithSubject(String.format(SUBJECT, MAYBE_ACCEPT, eventName));
    }

    @Test
    @Title("Согласие на простую повторяющуюся встречу")
    @TestCaseId("127")
    public void shouldAcceptRepeatedEvent() {
        createRepeatedEvent();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0),
            steps.pages().cal().home().viewEventPopup().buttonYes()
        )
            .shouldNotSee(steps.pages().cal().home().viewEventPopup())
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().viewEventPopup().buttonDecision(),
                ACCEPT_BUTTON
            );
        openFutureDayGrid(1);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().viewEventPopup().buttonDecision(),
                ACCEPT_BUTTON
            )
            .shouldSee(steps.pages().cal().home().viewEventPopup().acceptMark());
        steps.user().loginSteps().multiLoginWith(lock.firstAcc());
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup().acceptMark())
            .opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().shouldSeeMessageWithSubject(String.format(SUBJECT, ACCEPT, eventName));
    }

    @Test
    @Title("Отказ от одной встречи из серии с комментарием организатору")
    @TestCaseId("799")
    public void shouldRejectOneEvent() {
        createRepeatedEvent();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().buttonNo()
        )
            .shouldSee(steps.pages().cal().home().eventDecisionPopup())
            .inputsTextInElement(steps.pages().cal().home().eventDecisionPopup().commentInput(), eventComment)
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().rejectThisEventBtn())
            .shouldNotSee(
                steps.pages().cal().home().eventDecisionPopup(),
                steps.pages().cal().home().viewEventPopup()
            );
        openFutureDayGrid(0);
        steps.user().defaultSteps().shouldSeeElementsCount(
            steps.pages().cal().home().eventsAllList(),
            0
        );
        openFutureDayGrid(1);
        steps.user().defaultSteps().shouldContainsAttribute(
            steps.pages().cal().home().eventsAllList().get(0),
            "style",
            EVENT_NOT_DECIDED_BACKGROUND_COLOR
        );
        shouldSeeRejectMessage();
    }

    @Test
    @Title("Отказ от повторяющейся встречи с комментарием организатору")
    @TestCaseId("800")
    public void shouldRejectAllEvents() {
        createRepeatedEvent();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().buttonNo()
        )
            .shouldSee(steps.pages().cal().home().eventDecisionPopup())
            .inputsTextInElement(steps.pages().cal().home().eventDecisionPopup().commentInput(), eventComment)
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().rejectAllEventsBtn())
            .shouldNotSee(
                steps.pages().cal().home().eventDecisionPopup(),
                steps.pages().cal().home().viewEventPopup()
            )
            .shouldSeeElementsCount(
                steps.pages().cal().home().eventsAllList(),
                0
            );
        shouldSeeRejectMessage();
    }

    @Test
    @Title("Приглашенный с правом приглашать не может удалять уже имеющихся участников")
    @TestCaseId("732")
    public void shouldNotDeleteParticipants() {
        Event event = steps.user()
            .settingsCalSteps().formDefaultEvent(layerID).withName(eventName).withParticipantsCanInvite(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .onMouseHover(steps.pages().cal().home().newEventPage().membersList().get(0))
            .shouldNotSee(steps.pages().cal().home().newEventPage().hoveredMember());
    }

    @Test
    @Title("Приглашенный с правом приглашать может добавить участника")
    @TestCaseId("732")
    public void shouldAddParticipant() {
        Event event = steps.user()
            .settingsCalSteps().formDefaultEvent(layerID).withName(eventName).withParticipantsCanInvite(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), DEV_NULL_EMAIL);
        steps.user().hotkeySteps()
            .pressSimpleHotKey(steps.pages().cal().home().newEventPage().membersInput(), key(Keys.ENTER));
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().editEventPage().saveChangesBtn());
    }

    @Test
    @Title("Отображение кнопки «Ещё N человек» в поле «Участники»")
    @TestCaseId("976")
    public void shouldSeeMoreBtnForParticipants() {
        Event event = steps.user()
            .settingsCalSteps().formDefaultEvent(layerID).withName(eventName);
        ArrayList<String> totalAttendees = new ArrayList<>(MANY_ATTENDEES);
        totalAttendees.add(lock.accNum(1).getSelfEmail());
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, totalAttendees);
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .clicksOn(steps.pages().cal().home().editEventPage().moreParticipantsYabble())
            .shouldSeeElementsCount(
                steps.pages().cal().home().editEventPage().participantYabble(),
                SUBSCRIBERS_COUNT
            )
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().editEventPage().moreParticipantsYabble(),
                HIDE_BTN
            )
            .clicksOn(steps.pages().cal().home().editEventPage().moreParticipantsYabble())
            .shouldNotHasText(
                steps.pages().cal().home().editEventPage().moreParticipantsYabble(),
                HIDE_BTN
            )
            .shouldSeeElementsCount(
                steps.pages().cal().home().editEventPage().participantYabble(),
                SUBSCRIBERS_COUNT_HIDE
            );
    }


    @Step("Создать событие по приглашению")
    private void createEvent() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
    }

    @Step("Создать повторяющееся событие по приглашению")
    private void createRepeatedEvent() {
        Event event = steps.user().settingsCalSteps().formDefaultRepeatingEvent(layerID).withName(eventName);
        steps.user().apiCalSettingsSteps()
            .createNewRepeatEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
    }

    @Step("Открываем календарь на дне, который будет через {0} дней")
    public void openFutureDayGrid(int daysAfter) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDateTime date = LocalDateTime.now();
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix("/day?show_date=" + dateFormat.format(date.plusDays(daysAfter)));
    }

    @Step("Открываем почту и видим письмо с отказом от встречи")
    private void shouldSeeRejectMessage() {
        //Письма приходят с задержкой, если wait не поможет, то придётся тесты отправить на ферму
        steps.user().defaultSteps().waitInSeconds(10);
        steps.user().loginSteps().multiLoginWith(lock.firstAcc());
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup().declineMark())
            .opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().clicksOnMessageWithSubject(String.format(SUBJECT, REJECT, eventName));
        steps.user().messageViewSteps().shouldSeeCorrectMessageText(eventComment);
    }
}
