package ru.yandex.autotests.innerpochta.tests.autotests.event;

import io.qameta.allure.junit4.Tag;
import org.hamcrest.CoreMatchers;
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
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;

import static ru.yandex.autotests.innerpochta.cal.rules.DeleteAllLayersRule.deleteAllLayers;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("[Корп] Попап просмотра встречи")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.EDIT_EVENT)
public class CorpViewEventPopupTest {

    private static final String STAFF_URL = "staff.yandex-team.ru/";
    private static final String ROOM_URL = "map/conference_room";
    private static final String ROBOT_1_EMAIL = "robot-mailcorp-1@yandex-team.ru";
    private static final String ROBOT_3_EMAIL = "robot-mailcorp-3@yandex-team.ru";
    private static final String DAY_URL = "/day?show_date=2019-07-28";
    private static final String BENUA = "Бенуа";
    private static final String ROOM_NAME = "Лисий Нос";

    private Long layerID;

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();


    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(deleteAllLayers(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
    }

    @Test
    @Title("Написать участникам встречи")
    @TestCaseId("846")
    public void shouldSeeMail() {
        String eventName = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(ROBOT_1_EMAIL, ROBOT_3_EMAIL));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(
                steps.pages().cal().home().eventsAllList().get(0),
                steps.pages().cal().home().viewEventPopup().writeMail()
            )
            .switchOnJustOpenedWindow()
            .shouldSee(steps.pages().mail().composePopup().expandedPopup())
            .shouldHasValue(steps.pages().mail().composePopup().expandedPopup().sbjInput(), eventName)
            .shouldSeeThatElementHasText(
                steps.pages().mail().composePopup().yabbleToList().get(0),
                ROBOT_1_EMAIL.split("[@]")[0]
            )
            .shouldSeeThatElementHasText(
                steps.pages().mail().composePopup().yabbleToList().get(1),
                ROBOT_3_EMAIL.split("[@]")[0]
            )
            .shouldNotSeeElementInList(
                steps.pages().mail().composePopup().yabbleToList(),
                lock.firstAcc().getLogin()
            );
    }

    @Test
    @Title("Кнопка «Написать участникам» есть при наличии участников в попапе просмотра события")
    @TestCaseId("845")
    public void shouldSeeWriteButton() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID);
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldNotSee(steps.pages().cal().home().viewEventPopup().writeMail())
            .clicksOn(
                steps.pages().cal().home().viewEventPopup().editEventBtn(),
                steps.pages().cal().home().newEventPage().membersField()
            )
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), ROBOT_1_EMAIL);
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ENTER.toString());
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup().writeMail())
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPage().memberDeleteBtn().get(0))
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldNotSee(steps.pages().cal().home().viewEventPopup().writeMail());
    }

    @Test
    @Title("Открывается стафф при клике на участника встречи")
    @TestCaseId("728")
    public void shouldSeeStaffInfo() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(ROBOT_1_EMAIL));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().membersList().get(0))
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(CoreMatchers.containsString(STAFF_URL + ROBOT_1_EMAIL.split("@")[0]));
    }

    @Test
    @Title("Открывается схема офиса при клике на переговорку в карточке переговорки")
    @TestCaseId("735")
    public void shouldSeeRoomInfoFromCard() {
        createEventWithRoom();
        steps.user().defaultSteps().onMouseHover(steps.pages().cal().home().viewEventPopup().roomYabble())
            .shouldSee(steps.pages().cal().home().roomCard())
            .clicksOn(steps.pages().cal().home().roomName())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(CoreMatchers.containsString(STAFF_URL + ROOM_URL));
    }

    @Test
    @Title("Открывается схема офиса при клике на переговорку в попапе просмотра события")
    @TestCaseId("735")
    public void shouldSeeRoomInfo() {
        createEventWithRoom();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().viewEventPopup().roomYabble())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(CoreMatchers.containsString(STAFF_URL + ROOM_URL));
    }

    @Test
    @Title("Открывается схема офиса при клике на флажок в карточке переговорки")
    @TestCaseId("735")
    public void shouldSeeRoomInfoFromFlag() {
        createEventWithRoom();
        steps.user().defaultSteps().onMouseHover(steps.pages().cal().home().viewEventPopup().roomYabble())
            .shouldSee(steps.pages().cal().home().roomCard())
            .clicksOn(steps.pages().cal().home().roomLocation())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(CoreMatchers.containsString(STAFF_URL + ROOM_URL));
    }

    @Step("Создаем событие с переговоркой")
    private void createEventWithRoom() {
        String eventName = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withStartTs("2019-07-28T16:00:00").withEndTs("2019-07-28T16:30:00");
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(DAY_URL)
            .clicksOn(
                steps.pages().cal().home().eventsAllList().get(0),
                steps.pages().cal().home().viewEventPopup().editEventBtn(),
                steps.pages().cal().home().newEventPage().office()
            )
            .clicksOnElementWithText(steps.pages().cal().home().officesList(), BENUA)
            .inputsTextInElement(steps.pages().cal().home().newEventPage().roomNameInput(), ROOM_NAME)
            .clicksOn(
                steps.pages().cal().home().suggestItem().get(0),
                steps.pages().cal().home().newEventPage().saveChangesBtn()
            )
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0));
    }

}
