package ru.yandex.autotests.innerpochta.tests.autotests.event;

import io.qameta.allure.junit4.Tag;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
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
import ru.yandex.qatools.allure.annotations.Issue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.cal.rules.RemoveAllOldAndCreateNewLayer.removeAllOldAndCreateNewLayer;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.VIEW_ALL;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.VIEW_MEMBERS;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.EVENT;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author marchart
 */
@Aqua.Test
@Title("[Корп] Страница занятости переговорок")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.CORP)
public class CorpMeetingsTest {

    private static final String BENUA = "Бенуа";
    private static final String ROOM_NAME = "2.Весёлый посёлок";
    private static final int ROOM_ID = 5;
    private static final String FILTER_ROOM_NAME = "2.Сосновый Бор";
    private static final String MEMBER = "robot-mailcorp-5";
    private static final String MORNING_TIME = "08:00";
    private static final String NEW_INTERFACE = "/invite";
    private static final String STAFF_URL = "staff.yandex-team.ru/robot-mailcorp-3";
    private static final String RESERVED_BACKGROUND_COLOR = "rgba(122, 189, 119, 1)";
    private static final String TIME_9AM = "09:30";
    private static final String STYLE = "left: 9.375%; right: 89.0625%;";
    private static final String INVITE_EVENT_URL = "invite?show_date=";
    private static final String INVITE_BENUA_EVENT_URL = "invite?office=2&show_date=";
    private static final String ROOM_URL = "map/conference_room/name_exchange/conf_spb_ves_poselok";
    private static final String BUTTON_TEXT = "Все переговорки на это время";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();
    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();
    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(removeAllOldAndCreateNewLayer(() -> steps.user()));
    private AccLockRule lock = rules.getLock();

    @Before
    public void setUp() {
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
        steps.user().defaultSteps().clicksIfCanOn(steps.pages().cal().home().closeWidget())
            .opensDefaultUrlWithPostFix(NEW_INTERFACE);
        steps.user().apiCalSettingsSteps()
            .updateUserSettings(
                "Включаем недельный вид",
                new Params().withDefaultView("week")
            );
    }

    @Test
    @Title("Выбор офиса в ЛК на странице переговорок")
    @TestCaseId("1273")
    public void shouldSeeChangedRoomsList() {
        openBenuaRoomsList();
    }

    @Test
    @Title("Создаём событие после клика в слот переговорок")
    @TestCaseId("1316")
    public void shouldCreateEventFromRoomSlot() {
        String name = getRandomName();
        String layerName = getRandomName();
        steps.user().apiCalSettingsSteps().createNewLayer(steps.user().settingsCalSteps().formDefaultLayer())
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer().withName(layerName));
        steps.user().defaultSteps().refreshPage();
        openBenuaRoomsList();
        steps.user().defaultSteps().offsetClick(steps.pages().cal().home().meetingsPage().roomSlots().get(0), 5, 5)
            .shouldSee(steps.pages().cal().home().newEventPopup())
            .clicksOn(steps.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().nameInput(), name)
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPopup().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPopup().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().descriptionInput(), getRandomName())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().membersInput(), MEMBER)
            .onMouseHoverAndClick(steps.pages().cal().home().suggestItem().get(0))
            .clicksOn(steps.pages().cal().home().newEventPopup().layerField())
            .shouldSee(steps.pages().cal().home().layersList().waitUntil(not(empty())).get(0))
            .clicksOnElementWithText(steps.pages().cal().home().layersList(), layerName)
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().newEventPopup().visibilityChecked().get(1),
                VIEW_MEMBERS
            )
            .clicksOnElementWithText(steps.pages().cal().home().newEventPopup().visibility(), VIEW_ALL)
            .clicksOn(steps.pages().cal().home().newEventPopup().createFromPopupBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPopup())
            .onMouseHover(steps.pages().cal().home().meetingsPage().roomSlots().get(0).roomEvent().get(0))
            .shouldContainText(steps.pages().cal().home().roomEvent().roomEventName(), name);
    }

    @Test
    @Title("Видим карточку события при наведении на занятый слот")
    @TestCaseId("1279")
    public void shouldSeeRoomEventDetails() {
        String name = createEventWithRoom();
        steps.user().defaultSteps().refreshPage()
            .onMouseHover(steps.pages().cal().home().meetingsPage().roomSlots().get(ROOM_ID).roomEvent().get(0))
            .shouldContainText(steps.pages().cal().home().roomEvent().roomEventName(), name)
            .shouldContainText(steps.pages().cal().home().roomEvent().roomEventTime(), MORNING_TIME)
            .shouldContainText(steps.pages().cal().home().roomEvent().roomEventMembers(), MEMBER);
    }

    @Test
    @Title("Открываем событие при клике в название встречи")
    @TestCaseId("1309")
    public void shouldSeeEvent() {
        createEventWithRoom();
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().meetingsPage().roomSlots().get(ROOM_ID).roomEvent().get(0))
            .clicksOn(steps.pages().cal().home().roomEvent().roomEventName())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(EVENT.fragment("")));
    }

    @Test
    @Title("Открываем стафф при клике в участника")
    @TestCaseId("1310")
    public void shouldSeeStaff() {
        createEventWithRoom();
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().meetingsPage().roomSlots().get(ROOM_ID).roomEvent().get(0))
            .clicksOn(steps.pages().cal().home().roomEvent().roomEventYabbles().get(0))
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(STAFF_URL));
    }

    @Test
    @Title("При клике в слот он подсвечивается зеленым")
    @TestCaseId("1318")
    public void shouldSeeGreenTimeSlot() {
        openBenuaRoomsList();
        steps.user().defaultSteps()
            .offsetClick(steps.pages().cal().home().meetingsPage().roomBookSlots().get(0), 0, 0)
            .shouldSee(steps.pages().cal().home().meetingsPage().roomSlots().get(0).roomEventReserved())
            .shouldContainCSSAttributeWithValue(
                steps.pages().cal().home().meetingsPage().roomSlots().get(0).roomEventReserved(),
                "background-color",
                RESERVED_BACKGROUND_COLOR
            );
        steps.user().calCreateEventSteps().setStartTime(TIME_9AM);
        steps.user().defaultSteps().shouldContainCSSAttributeWithValue(
                steps.pages().cal().home().meetingsPage().roomSlots().get(0).roomEventReserved(),
                "style",
                STYLE
            );
    }

    @Test
    @Title("Выбор занятого времени в попапе")
    @TestCaseId("1321")
    public void shouldSeeBusyRoom() {
        openRoomsWidget();
    }

    @Test
    @Title("Фильтр переговорок по параметрам")
    @TestCaseId("1274")
    public void shouldFilterRooms() {
        openBenuaRoomsList();
        steps.user().defaultSteps().turnTrue(
            steps.pages().cal().home().leftPanel().filterList().roomFilterCheckbox().get(0),
            steps.pages().cal().home().leftPanel().filterList().roomFilterCheckbox().get(7)
        )
            .waitInSeconds(2)
            .shouldSeeThatElementHasText(steps.pages().cal().home().meetingsPage().roomName().get(0), FILTER_ROOM_NAME)
            .turnTrue(steps.pages().cal().home().leftPanel().filterList().roomFilterCheckbox().get(2))
            .waitInSeconds(2)
            .shouldNotHasText(steps.pages().cal().home().meetingsPage().roomName().get(0), FILTER_ROOM_NAME);
    }

    @Test
    @Title("Листание сетки переговорок стрелочками")
    @TestCaseId("1276")
    public void shouldNavigateByArrows() {
        LocalDateTime datePast = LocalDateTime.now().minusDays(1);
        String urlDatePast = DATE_FORMAT.format(datePast);
        LocalDateTime dateFuture = LocalDateTime.now().plusDays(1);
        String urlDateFuture = DATE_FORMAT.format(dateFuture);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().meetingsPage().arrows().get(1))
            .shouldBeOnUrl(CoreMatchers.containsString(INVITE_EVENT_URL + urlDatePast))
            .clicksOn(
                steps.pages().cal().home().meetingsPage().arrows().get(2),
                steps.pages().cal().home().meetingsPage().arrows().get(2)
            )
            .shouldBeOnUrl(CoreMatchers.containsString(INVITE_EVENT_URL + urlDateFuture));
    }

    @Test
    @Title("Показываем карточку переговорки по ховеру")
    @TestCaseId("1278")
    public void shouldSeeRoomCard() {
        steps.user().defaultSteps().onMouseHover(steps.pages().cal().home().meetingsPage().roomName().get(0))
            .shouldSee(steps.pages().cal().home().roomCard());
    }

    @Test
    @Title("Клик в название переговорки открывает карту на соседней вкладке")
    @TestCaseId("1308")
    public void shouldOpenMap() {
        openBenuaRoomsList();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().meetingsPage().roomName().get(ROOM_ID))
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(CoreMatchers.containsString(ROOM_URL));
    }

    @Test
    @Title("Переключение сетки переговорок при изменении даты в мини-календаре")
    @TestCaseId("1275")
    public void shouldChangeDate() {
        String name = createEventWithRoom();
        String day = steps.pages().cal().home().leftPanel().miniCalendar().daysOutMonth().get(0).getText();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().leftPanel().miniCalendar().daysOutMonth().get(0))
            .shouldBeOnUrl(CoreMatchers.containsString(INVITE_BENUA_EVENT_URL))
            .shouldContainTextInUrl(day)
            .clicksOn(steps.pages().cal().home().meetingsPage().roomSlots().get(ROOM_ID).roomEvent()
                .waitUntil(not(empty())).get(0))
            .shouldNotHasText(steps.pages().cal().home().roomEvent().roomEventName(), name);
    }

    @Test
    @Title("Отображаем доступные переговорки и время в виджете")
    @TestCaseId("1343")
    public void shouldSeeAvailaleRoomsAndTime() {
        openRoomsWidget();
        steps.user().defaultSteps().shouldSee(steps.pages().cal().home().roomsWidget().get(0).availableRoom().get(0))
            .scrollTo(steps.pages().cal().home().roomsWidget().get(1))
            .shouldSee(steps.pages().cal().home().roomsWidget().get(1).availableRoom().get(0));
    }

    @Test
    @Title("Виджет переговорок пропадает после выбора переговорки")
    @TestCaseId("1343")
    public void shouldChangeRoom() {
        openRoomsWidget();
        String roomName = steps.pages().cal().home().roomsWidget().get(0).availableRoom().get(0).getText();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().roomsWidget().get(0).availableRoom().get(0))
            .shouldSeeElementsCount(steps.pages().cal().home().roomsWidget(), 0)
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPopup().room(), roomName);
    }

    @Test
    @Title("Виджет переговорок - подгружаем переговорки по кнопке")
    @TestCaseId("1343")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("MAYA-2331")
    public void shouldSeeMoreRooms() {
        openRoomsWidget();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().roomsWidget().get(0).moreButton())
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().roomsWidget().get(0).moreButton(),
                BUTTON_TEXT
            );
    }

    @Step("Открыть список переговорок БЦ Бенуа")
    private void openBenuaRoomsList() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().office())
            .clicksOnElementWithText(steps.pages().cal().home().officesListAtOfficePage(), BENUA)
            .shouldSeeThatElementHasText(steps.pages().cal().home().meetingsPage().roomName().get(ROOM_ID), ROOM_NAME);
    }

    @Step("Создать встречу и выбрать занятое время в попапе")
    private void openRoomsWidget() {
        createEventWithRoom();
        steps.user().defaultSteps()
            .offsetClick(steps.pages().cal().home().meetingsPage().roomBookSlots().get(ROOM_ID), 50, 0);
        steps.user().calCreateEventSteps().setStartTime(MORNING_TIME);
        steps.user().defaultSteps().shouldSee(steps.pages().cal().home().newEventPopup().busyRoom());
    }


    @Step("Создать встречу с переговоркой")
    private String createEventWithRoom() {
        String name = getRandomName();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .clicksOn(steps.pages().cal().home().newEventPage().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), name)
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPage().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPage().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().descriptionInput(), getRandomName())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), MEMBER)
            .clicksOn(steps.pages().cal().home().suggestItem().waitUntil(not(empty())).get(0))
            .deselects(steps.pages().cal().home().newEventPage().allDayCheckBox());
        steps.user().calCreateEventSteps().setStartTime(MORNING_TIME)
            .setBC(BENUA);
        steps.user().defaultSteps()
            .inputsTextInElement(
                steps.pages().cal().home().newEventPage().roomsList().get(0).roomInput(),
                ROOM_NAME
            )
            .clicksOn(
                steps.pages().cal().home().suggestItem().waitUntil(not(empty())).get(0),
                steps.pages().cal().home().newEventPage().createFromPageBtn()
                )
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldContainText(
                steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0).eventName(),
                name
            )
            .opensDefaultUrlWithPostFix(NEW_INTERFACE);
        openBenuaRoomsList();

        return name;
    }
}
