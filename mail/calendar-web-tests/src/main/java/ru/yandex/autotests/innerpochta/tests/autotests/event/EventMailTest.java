package ru.yandex.autotests.innerpochta.tests.autotests.event;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.*;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.*;

import java.util.Arrays;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.*;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_BASE_URL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Письма с приглашением на встречу")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.COMMON)
public class EventMailTest {

    private Long layerID;

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
        steps.user().apiCalSettingsSteps().createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().loginSteps().forAcc(lock.accNum(0)).logins();
    }

    @Test
    @Title("Принятие встречи через письмо")
    @TestCaseId("514")
    public void shouldAcceptEvent() {
        createEventWithMemberAndOpenMailFromCal();
        String btnLink = steps.pages().mail().msgView().messageTextBlock().messageHref().get(1).getAttribute("href")
            .replace(CAL_BASE_URL, UrlProps.urlProps().getBaseUri());
        steps.user().defaultSteps().opensUrl(btnLink)
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().successNotify(),
                INVITE_NOTIFICATION_ACCEPTED
            )
            .clicksOn(steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0))
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().viewEventPopup().buttonDecision(),
                INVITE_BUTTON_YES
            );
    }

    @Test
    @Title("Отклонение встречи через письмо")
    @TestCaseId("515")
    public void shouldDeclineEvent() {
        createEventWithMemberAndOpenMailFromCal();
        String btnLink = steps.pages().mail().msgView().messageTextBlock().messageHref().get(3).getAttribute("href")
            .replace(CAL_BASE_URL, UrlProps.urlProps().getBaseUri());
        steps.user().defaultSteps().opensUrl(btnLink)
            .inputsTextInElement(steps.pages().cal().home().eventDecisionPopup().commentInput(), getRandomString())
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().rejectThisEventBtn())
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().successNotify(),
                INVITE_NOTIFICATION_REJECTED
            )
            .shouldNotSee(steps.pages().cal().home().eventDecisionPopup())
            .shouldSeeElementsCount(steps.pages().cal().home().eventsAllList(), 0);
    }

    @Test
    @Title("Показ модального окна при отказе от встречи из письма")
    @TestCaseId("804")
    public void shouldSeeDeclinePopup() {
        createEventWithMemberAndOpenMailFromCal();
        String btnLink = steps.pages().mail().msgView().messageTextBlock().messageHref().get(3).getAttribute("href")
            .replace(CAL_BASE_URL, UrlProps.urlProps().getBaseUri());
        steps.user().defaultSteps().opensUrl(btnLink)
            .shouldSee(steps.pages().cal().home().eventDecisionPopup())
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().cancelPopupBtn())
            .shouldNotSee(steps.pages().cal().home().eventDecisionPopup())
            .opensUrl(btnLink)
            .inputsTextInElement(steps.pages().cal().home().eventDecisionPopup().commentInput(), getRandomString())
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().rejectThisEventBtn())
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().successNotify(),
                INVITE_NOTIFICATION_REJECTED
            )
            .shouldNotSee(steps.pages().cal().home().eventDecisionPopup())
            .shouldSeeElementsCount(steps.pages().cal().home().eventsAllList(), 0);
    }

    @Test
    @Title("Не пойду в этот раз из письма")
    @TestCaseId("805")
    public void shouldDeclineEventOnce() {
        createRepeatEventWithMemberAndOpenMailFromCal();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().rejectThisEventBtn())
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().successNotify(),
                INVITE_NOTIFICATION_REJECTED
            )
            .shouldNotSee(steps.pages().cal().home().eventDecisionPopup())
            .shouldSeeElementsCount(steps.pages().cal().home().eventsTodayList(), 0)
            .shouldSee(steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0));
    }

    @Test
    @Title("Отклонить всю серию встреч из письма")
    @TestCaseId("842")
    public void shouldDeclineRepeatEvent() {
        createRepeatEventWithMemberAndOpenMailFromCal();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().eventDecisionPopup().rejectAllEventsBtn())
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().successNotify(),
                INVITE_NOTIFICATION_REJECTED
            )
            .shouldNotSee(steps.pages().cal().home().eventDecisionPopup())
            .shouldSeeElementsCount(steps.pages().cal().home().eventsAllList(), 0);
    }

    @Step("Создаём событие с участником, перезаходим в участника, открываем почту и пришедшее письмо")
    private void createEventWithMemberAndOpenMailFromCal() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(getRandomName());
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().clicksOnMessageByNumber(0);
    }

    @Step("Создаём повторяющееся событие с участником, перезаходим в участника, открываем календарь из кнопки в письме")
    private void createRepeatEventWithMemberAndOpenMailFromCal() {
        String eventName = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultRepeatingEvent(layerID).withName(eventName);
        steps.user().apiCalSettingsSteps()
            .createNewRepeatEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().clicksOnMessageWithSubject(eventName);
        String btnLink = steps.pages().mail().msgView().messageTextBlock().messageHref().get(3).getAttribute("href")
            .replace(CAL_BASE_URL, UrlProps.urlProps().getBaseUri());
        steps.user().defaultSteps().opensUrl(btnLink)
            .shouldSee(steps.pages().cal().home().eventDecisionPopup())
            .inputsTextInElement(steps.pages().cal().home().eventDecisionPopup().commentInput(), getRandomString());
    }
}
