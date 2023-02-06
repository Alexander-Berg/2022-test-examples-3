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
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
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
import java.util.Arrays;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.cal.rules.DeleteAllLayersRule.deleteAllLayers;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.INVITE_BUTTON_MAYBE;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.INVITE_BUTTON_YES;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.CORP_BASE_URL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author marchart
 */
@Aqua.Test
@Title("[Корп] Просмотр встречи на отдельной странице")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.EDIT_EVENT)
public class CorpViewEventTest {

    private static final String CREDS = "CorpViewEventTest";
    private static final String OTHER_USER_CREDS = "CorpViewEventTestDifferent";
    private static final String ROBOT_1_EMAIL = "robot-mailcorp-1@yandex-team.ru";
    private static final String ROBOT_3_EMAIL = "robot-mailcorp-3@yandex-team.ru";
    private static final String MANY_SUBSCRIBERS = "/event/51133678";
    private static final String FEW_SUBSCRIBERS = "/event/35056001";
    private static final String HIDE_BTN = "Скрыть";
    private static final int SUBSCRIBERS_COUNT = 101;
    private static final int SUBSCRIBERS_COUNT_HIDE = 10;
    private static final String MSG_DECLINE_PREF = "Комментарий %s";
    public static final String MAIL_URL_PART = "/compose?subject=%s&to=robot-mailcorp-1%%40yandex-team" +
        ".ru%%2Crobot-mailcorp-3%%40yandex-team.ru";

    private DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
    private String date = dateFormat.format(LocalDateTime.now());
    private Long layerID;

    private CalendarRulesManager rules = calendarRulesManager()
        .withLock(AccLockRule.use().names(CREDS, OTHER_USER_CREDS));
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();
    private AccLockRule lock2 = AccLockRule.use().names(OTHER_USER_CREDS);
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock2);
    private RestAssuredAuthRule auth = rules.getAuth();

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(lock2)
        .around(auth2)
        .around(deleteAllLayers(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        steps.user().apiCalSettingsSteps().withAuth(auth).updateUserSettings(
            "Включаем показ выходных в сетке",
            new Params().withShowWeekends(true)
        );
        steps.user().apiCalSettingsSteps().withAuth(auth2).updateUserSettings(
            "Включаем показ выходных в сетке",
            new Params().withShowWeekends(true)
        );
        steps.user().apiCalSettingsSteps().withAuth(auth2)
            .deleteLayers()
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
    }

    @Test
    @Title("Отображение кнопки «Написать участникам» на странице просмотра события")
    @TestCaseId("1019")
    public void shouldSeeWriteBtnOnViewEventPage() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID);
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .shouldNotSee(steps.pages().cal().home().editEventPage().writeMail())
            .clicksAndInputsText(steps.pages().cal().home().newEventPage().membersInput(), ROBOT_1_EMAIL);
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ENTER.toString());
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .clicksOn(steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .shouldSee(steps.pages().cal().home().editEventPage().writeMail())
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPage().memberDeleteBtn().get(0))
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .clicksOn(steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .shouldNotSee(steps.pages().cal().home().editEventPage().writeMail());
    }

    @Test
    @Title("Открываем композ кликом по «Написать участникам» на странице просмотра события")
    @TestCaseId("1020")
    public void shouldSeeComposeAfterClickOnWriteButton() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withParticipantsCanEdit(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(ROBOT_1_EMAIL, ROBOT_3_EMAIL));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .onMouseHover(steps.pages().cal().home().editEventPage().writeMail())
            .shouldContainsAttribute(steps.pages().cal().home().editEventPage().writeMail(), "href",
                String.format(MAIL_URL_PART, event.getName()));
    }

    @Test
    @Title("Отображение кнопки «Ещё N человек» в поле «Добавили в календарь»")
    @TestCaseId("809")
    public void shouldSeeMoreBtnForAddedToCalMembers() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MANY_SUBSCRIBERS)
            .clicksOn(steps.pages().cal().home().editEventPage().moreSubscribersYabble())
            .shouldSeeElementsCount(steps.pages().cal().home().editEventPage().subscriberYabble(), SUBSCRIBERS_COUNT)
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().editEventPage().moreSubscribersYabble(),
                HIDE_BTN
            )
            .clicksOn(steps.pages().cal().home().editEventPage().moreSubscribersYabble())
            .shouldNotHasText(
                steps.pages().cal().home().editEventPage().moreSubscribersYabble(),
                HIDE_BTN
            )
            .shouldSeeElementsCount(
                steps.pages().cal().home().editEventPage().subscriberYabble(),
                SUBSCRIBERS_COUNT_HIDE
            )
            .opensDefaultUrlWithPostFix(FEW_SUBSCRIBERS)
            .shouldNotSee(steps.pages().cal().home().editEventPage().moreSubscribersYabble());
    }

    @Test
    @Title("Принятие приглашения кнопкой «Пойду»")
    @TestCaseId("198")
    public void shouldAcceptEvent() {
        createEventWithMemberAndOpenEventFormMember();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().editEventPage().acceptEventBtn())
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().editEventPage().decisionEventBtn(),
                INVITE_BUTTON_YES
            )
            .shouldNotSee(steps.pages().cal().home().editEventPage().declineEventBtn())
            .clicksOn(steps.pages().cal().home().editEventPage().closeBtn())
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().viewEventPopup().buttonDecision(),
                INVITE_BUTTON_YES
            )
            .shouldSee(steps.pages().cal().home().viewEventPopup().acceptMark());
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup().acceptMark());
    }

    @Test
    @Title("Меняем приглашение на событие с «Пойду» на «Возможно, пойду»")
    @TestCaseId("198")
    public void shouldChangeFromAcceptOnMaybeEvent() {
        createEventWithMemberAndOpenEventFormMember();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().editEventPage().acceptEventBtn())
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().editEventPage().decisionEventBtn(),
                INVITE_BUTTON_YES
            )
            .shouldNotSee(steps.pages().cal().home().editEventPage().declineEventBtn())
            .clicksOn(steps.pages().cal().home().editEventPage().decisionEventBtn())
            .shouldSee(steps.pages().cal().home().decisionEventList())
            .clicksOn(steps.pages().cal().home().decisionEventList().maybe())
            .shouldNotSee(steps.pages().cal().home().decisionEventList())
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().editEventPage().decisionEventBtn(),
                INVITE_BUTTON_MAYBE
            );
    }

    @Test
    @Title("Отклоняем простую встречу с комментарием")
    @TestCaseId("798")
    public void shouldDeclineEvent() {
        String declineText = getRandomString();
        createEventWithMemberAndOpenEventFormMember();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().editEventPage().declineEventBtn())
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().cancelPopupBtn())
            .clicksOn(steps.pages().cal().home().editEventPage().declineEventBtn())
            .inputsTextInElement(steps.pages().cal().home().eventDecisionPopup().commentInput(), declineText)
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().rejectThisEventBtn());
        checkDeclineEvent(declineText);
    }

    @Test
    @Title("Отклоняем серию встреч через редактирование одного события с комментарием")
    @TestCaseId("803")
    public void shouldDeclineRepeatEvent() {
        String declineText = getRandomString();
        Event event = steps.user().settingsCalSteps().formDefaultRepeatingEvent(layerID).withName(getRandomName())
            .withParticipantsCanEdit(true);
        steps.user().apiCalSettingsSteps()
            .createNewRepeatEventWithAttendees(event, Arrays.asList(lock.acc(OTHER_USER_CREDS).getSelfEmail()));
        steps.user().loginSteps().forAcc(lock.acc(OTHER_USER_CREDS)).loginsToCorp();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .clicksOn(steps.pages().cal().home().editOneEvent());
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().editEventPage().declineEventBtn())
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().cancelPopupBtn())
            .clicksOn(steps.pages().cal().home().editEventPage().declineEventBtn())
            .inputsTextInElement(steps.pages().cal().home().eventDecisionPopup().commentInput(), declineText)
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().rejectAllEventsBtn());
        checkDeclineEvent(declineText);
    }

    @Test
    @Title("Не показываем модальное окно при изменении участников - Организатор")
    @TestCaseId("781")
    public void shouldNotSeeConfirmPopupChangeOrganizer() {
        String eventName = getRandomString();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(ROBOT_3_EMAIL));
        steps.user().defaultSteps().refreshPage()
            .clicksOn(
                steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0),
                steps.pages().cal().home().viewEventPopup().editEventBtn()
            )
            .inputsTextInElement(
                steps.pages().cal().home().editEventPage().membersInput(),
                ROBOT_1_EMAIL
            )
            .clicksOn(steps.pages().cal().home().editEventPage().nameField());
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().cal().home().editEventPage().orgInput(), "robot-mailcorp-5@yandex-team.ru")
            .clicksOn(steps.pages().cal().home().editEventPage().nameField())
            .clicksOn(steps.pages().cal().home().editEventPage().saveChangesBtn())
            .shouldNotSee(steps.pages().cal().home().warPopups());
        steps.user().loginSteps().forAcc(lock.acc(OTHER_USER_CREDS)).loginsToCorp();
        steps.user().defaultSteps().opensUrl(CORP_BASE_URL);
        steps.user().messagesSteps().shouldSeeMessageWithSubject(eventName);
    }

    @Step("Создаём событие с участником, перезаходим в участника и открываем событие на редактирование")
    private void createEventWithMemberAndOpenEventFormMember() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(getRandomName());
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.acc(OTHER_USER_CREDS).getSelfEmail()));
        steps.user().loginSteps().forAcc(lock.acc(OTHER_USER_CREDS)).loginsToCorp();

        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn());
    }

    @Step("Проверяем что отконённое событие пропало из сетки и пришло сообщение организатору с комментарием «{0}»")
    private void checkDeclineEvent(String declineText) {
        steps.user().defaultSteps().shouldNotSee(
            steps.pages().cal().home().eventDecisionPopup(),
            steps.pages().cal().home().editEventPage()
        )
            .shouldSeeElementsCount(steps.pages().cal().home().eventsAllList(), 0);
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
        steps.user().defaultSteps().opensUrl(CORP_BASE_URL);
        steps.user().messagesSteps().clicksOnMessageByNumber(0);
        steps.user().defaultSteps().shouldContainText(
            steps.pages().mail().msgView().messageTextBlock(),
            String.format(MSG_DECLINE_PREF, declineText)
        );
    }
}
