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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;

import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.DISABLE_ALERT_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Редактирование события по приглашению")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.EDIT_EVENT)
public class ChangeEventTest {

    private String eventName;
    private String newName;
    private Long layerID;

    private static final String ANOTHER_USER_EMAIL = "yandex-team-mailt-126@yandex.ru";
    private static final String TELEMOST_STR = "Ссылка на видеовстречу: https://telemost.yandex.ru/j/";

    private static final String[] MEMBERS = {
        "Я",
        ANOTHER_USER_EMAIL
    };

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
        eventName = getRandomName();
        steps.user().apiCalSettingsSteps().withAuth(auth2).deleteAllAndCreateNewLayer();
        steps.user().apiCalSettingsSteps()
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
    }

    @Test
    @Title("Отмена при редактировании события по приглашению")
    @TestCaseId("91")
    public void shouldCancelEditing() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withParticipantsCanEdit(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().editEventBtn()
        )
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), getRandomName())
            .clicksOn(steps.pages().cal().home().newEventPage().cancelButton())
            .shouldSeeElementInList(steps.pages().cal().home().eventsAllList(), eventName);
    }

    @Test
    @Title("Сохраняются изменения в событии по приглашению")
    @TestCaseId("91")
    public void shouldSaveEventChanges() {
        newName = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withParticipantsCanEdit(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().editEventBtn()
        );
        steps.user().defaultSteps()
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().newEventPage().nameInput(),
                newName
            )
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .executesJavaScript(DISABLE_ALERT_SCRIPT);
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().shouldSeeElementInList(steps.pages().cal().home().eventsAllList(), newName);
    }

    @Test
    @Title("Нельзя удалить себя и участников из события по приглашению без прав")
    @TestCaseId("104")
    public void shouldNotDeleteMe() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withParticipantsCanEdit(false).withParticipantsCanInvite(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail(), ANOTHER_USER_EMAIL));
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().editEventBtn()
        );
        steps.user().hotkeySteps().pressHotKeys(
            steps.pages().cal().home().newEventPage().membersInput(),
            Keys.CONTROL.toString(), "a", Keys.DELETE.toString()
        );
        steps.user().defaultSteps()
            .shouldSeeAllElementsInList(steps.pages().cal().home().newEventPage().membersList(), MEMBERS);
    }

    @Test
    @Title("Изменение участников видно на стороне организатора")
    @TestCaseId("104")
    public void shouldSeeAddPeople() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withParticipantsCanEdit(false).withParticipantsCanInvite(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().editEventBtn()
        )
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), ANOTHER_USER_EMAIL);
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ENTER.toString());
        steps.user().defaultSteps().onMouseHoverAndClick(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .clicksOn(
            steps.pages().cal().home().warPopups().get(0).agreeBtn()
        )
            .shouldNotSee(steps.pages().cal().home().warPopups().get(0))
            .executesJavaScript(DISABLE_ALERT_SCRIPT);
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().editEventBtn()
        )
            .shouldSeeElementsCount(steps.pages().cal().home().newEventPage().membersList(), 2)
            .shouldSeeElementInList(
                steps.pages().cal().home().newEventPage().membersList(),
                ANOTHER_USER_EMAIL
            );
    }

    @Test
    @Title("Изменение каленадря видно только на стороне пользователя")
    @TestCaseId("104")
    public void shouldNotSeeLayerChange() {
        steps.user().apiCalSettingsSteps().withAuth(auth2)
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        String layerName = steps.user().apiCalSettingsSteps().withAuth(auth2).getUserLayers().get(1).getName();
        Event event = steps.user().settingsCalSteps()
            .formDefaultEvent(steps.user().apiCalSettingsSteps().withAuth(auth2).getUserLayers().get(0).getId())
            .withName(eventName)
            .withParticipantsCanEdit(false).withParticipantsCanInvite(true);
        steps.user().apiCalSettingsSteps().withAuth(auth2)
            .createNewEventWithAttendees(event, Arrays.asList(lock.firstAcc().getSelfEmail()));
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().home().eventsAllList().get(0),
                steps.pages().cal().home().viewEventPopup().editEventBtn()
            )
            .clicksOn(steps.pages().cal().home().newEventPage().layerField())
            .clicksOn(
                steps.pages().cal().home().layersList().get(1),
                steps.pages().cal().home().newEventPage().saveChangesBtn()
            )
            .executesJavaScript(DISABLE_ALERT_SCRIPT);
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().editEventBtn()
        )
            .shouldNotContainText(
                steps.pages().cal().home().newEventPage().layerField(),
                layerName
            );
    }

    @Test
    @Title("Изменение уведомлений видно только на стороне пользователя")
    @TestCaseId("104")
    public void shouldNotSeeNotyfyChange() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withParticipantsCanEdit(false).withParticipantsCanInvite(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().home().eventsAllList().get(0),
                steps.pages().cal().home().viewEventPopup().editEventBtn()
            )
            .clicksOn(steps.pages().cal().home().newEventPage().notifyField().addNotifyBtn())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().notifyField().offsetNotifyInput(), "2")
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .executesJavaScript(DISABLE_ALERT_SCRIPT);
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().editEventBtn()
        )
            .shouldNotSee(steps.pages().cal().home().newEventPage().notifyField().offsetNotifyInput());
    }

    @Test
    @Title("Видим кнопку добавления телемоста при редактировании события")
    @TestCaseId("1425")
    public void shouldSeeTelemostInEditEvent() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withParticipantsCanEdit(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().editEventBtn()
        );
        steps.user().defaultSteps().shouldSee(steps.pages().cal().home().newEventPage().telemostBtn());
    }

    @Test
    @Title("Добавляем ссылку на видеовстречу при редактировании события")
    @TestCaseId("1392")
    public void shouldAddTelemostInEditEvent() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withParticipantsCanEdit(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().eventsAllList().get(0),
            steps.pages().cal().home().viewEventPopup().editEventBtn()
        );
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().telemostBtn())
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .clicksOn(steps.pages().cal().home().eventsTodayList().get(0))
            .shouldContainText(steps.pages().cal().home().viewEventPopup().description(), TELEMOST_STR);
    }
}
