package ru.yandex.autotests.innerpochta.tests.autotests.settings;

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
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на настройки занятости")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.SETTINGS)
public class OtherSettingsTest {

    private static final String CREDS = "SettingsTestUser";
    private static final String CREDS_2 = "SettingsTestUser2";
    private static final String CREDS_3 = "SettingsTestUser3";
    private static final String ACCEPT_BUTTON = "Пойду";
    private static final int TIME_POSITION = 24;

    private CalendarRulesManager rules = calendarRulesManager()
        .withLock(AccLockRule.use().names(CREDS, CREDS_2, CREDS_3));
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain();

    @Before
    public void login() {
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Выключаем настройку автопринятия встреч",
            new Params().withAutoAcceptEventInvitations(false)
        );
    }

    @Test
    @Title("Не показываем занятость участника")
    @TestCaseId("738")
    public void shouldNotSeeUserAvailability() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        String date = dateFormat.format(LocalDateTime.now());
        steps.user().apiCalSettingsSteps().deleteLayers()
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Выключаем настройку показа занятости",
            new Params().withShowAvailabilityToAnyone(false)
        );
        Event event = steps.user().settingsCalSteps()
            .formDefaultEvent(steps.user().apiCalSettingsSteps().getUserLayersIds().get(0))
            .withStartTs(date + "T12:00:00");
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        createEvent();
    }

    @Test
    @Title("Встреча принимается автоматически")
    @TestCaseId("1172")
    public void shouldAutoAgree() {
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Включаем настройку автопринятия встреч",
            new Params().withAutoAcceptEventInvitations(true)
        );
        steps.user().apiCalSettingsSteps().deleteLayers()
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        String eventName = createEvent();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().createFromPageBtn());
        steps.user().loginSteps().forAcc(lock.acc(CREDS)).logins();
        steps.user().defaultSteps().clicksOnElementWithText(steps.pages().cal().home().eventsAllList(), eventName)
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().viewEventPopup().buttonDecision(),
                ACCEPT_BUTTON
            );
    }

    @Test
    @Title("Должны видеть встречи из расшаренного календаря")
    @TestCaseId("1172")
    public void shouldSeeSharedCal() {
        String eventName = Utils.getRandomString();
        steps.user().loginSteps().forAcc(lock.acc(CREDS_2)).logins();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .shouldSee(steps.pages().cal().home().newEventPage())
            .inputsTextInElement(
                steps.pages().cal().home().newEventPage().nameInput(),
                eventName
            )
            .clicksOn(steps.pages().cal().home().newEventPage().time())
            .onMouseHoverAndClick(steps.pages().cal().home().timesList().waitUntil(not(empty())).get(TIME_POSITION))
            .clicksOn(steps.pages().cal().home().newEventPage().createFromPageBtn());
        steps.user().loginSteps().forAcc(lock.acc(CREDS_3)).logins();
        steps.user().defaultSteps().shouldHasTitle(
            steps.pages().cal().home().leftPanel().layersOwners().get(0),
            lock.acc(CREDS_2).getLogin()
        )
            .clicksOnElementWithText(steps.pages().cal().home().eventsAllList(), eventName)
            .shouldSee(steps.pages().cal().home().viewEventPopup());
    }

    @Step
    @Title("Создать встречу")
    private String createEvent(){
        String eventName = Utils.getRandomString();
        steps.user().loginSteps().forAcc(lock.acc(CREDS_2)).logins();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .shouldSee(steps.pages().cal().home().newEventPage())
            .inputsTextInElement(
                steps.pages().cal().home().newEventPage().nameInput(),
                eventName
            )
            .clicksOn(steps.pages().cal().home().newEventPage().time())
            .onMouseHoverAndClick(steps.pages().cal().home().timesList().waitUntil(not(empty())).get(TIME_POSITION))
            .inputsTextInElement(
                steps.pages().cal().home().newEventPage().membersInput(),
                lock.acc(CREDS).getSelfEmail()
            );
        steps.user().hotkeySteps().pressSimpleHotKey(
            steps.pages().cal().home().newEventPage().membersInput(),
            key(Keys.ENTER)
        );
        steps.user().defaultSteps().shouldNotSee(steps.pages().cal().home().newEventPage().busyMember())
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().newEventPage().membersList().get(0),
                lock.acc(CREDS).getSelfEmail()
            );

        return eventName;
    }
}
