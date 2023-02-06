package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.cal.rules.RemoveAllOldAndCreateNewLayer.removeAllOldAndCreateNewLayer;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DAY_GRID;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("[Тач][Корп] Изменение параметров встречи")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.CORP)
public class CorpEditEventTest {

    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();

    private static final String NOT_TODAY_DATE = "/?show_date=2019-10-05";
    private static final String ROBOT_EMAIL = "robot-mailcorp-3@yandex-team.ru";
    private static final String ROOM_NAME = "2.Orange Soda - 2";
    private static final String SEARCH_ROOM_NAME = "2.Карты";
    private static final String BENUA_ROOM_NAME = "2.Шалаш";
    private static final String MOROZOV = "Морозов";
    private static final String MAMONTOV = "Мамонтов";
    private static final String BENUA = "Бенуа";
    private static final String DATACENTER = "Датацентр";
    private static final String MAP_URL = "m.staff.yandex-team.ru/map/conf";
    private static final String ADD_OFFICE = "Добавить ещё переговорку";
    private static final String WARNING_MESSAGE = "Одна из выбранных переговорок уже забронирована на это время.";
    private static final String AVAILABILITY = "Свободна до";
    private static final String CONFIRM_MESSAGE = "Переговорка %s уже забронирована на это время";
    private static final String SHOW_DATE_PREFIX = "/day?show_date=";
    private static final String REPEAT_EVERY_DAY = "Каждый день";
    private DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
    private Long layerID;

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(removeAllOldAndCreateNewLayer(() -> steps.user()));

    @Before
    public void setUp() {
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
    }

    @Test
    @Title("Поле поиска переговорок очищается крестиком")
    @TestCaseId("1200")
    public void shouldCloseSearchInput() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + NOT_TODAY_DATE)
            .clicksOn(
                steps.pages().cal().touchHome().addEventButton(),
                steps.pages().cal().touchHome().eventPage().addRoomButton()
            )
            .inputsTextInElement(steps.pages().cal().touchHome().editResourcesPage().input(), Utils.getRandomName())
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().clearInputBtn())
            .shouldHasText(steps.pages().cal().touchHome().editResourcesPage().input(), "");
    }

    @Test
    @Title("Поиск переговорки из другого офиса")
    @TestCaseId("1199")
    public void shouldNotSeeRooms() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + NOT_TODAY_DATE)
            .clicksOn(
                steps.pages().cal().touchHome().addEventButton(),
                steps.pages().cal().touchHome().eventPage().addRoomButton()
            )
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton())
            .clicksOnElementWithText(steps.pages().cal().touchHome().editOfficePage().menuItems(), BENUA)
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton(),
                BENUA
            );
        inputTextInRoomsSearchField(SEARCH_ROOM_NAME);
        steps.user().defaultSteps()
            .shouldSee(steps.pages().cal().touchHome().editResourcesPage().nothingFoundSuggestRooms());
    }

    @Test
    @Title("Поиск переговорки из своего офиса")
    @TestCaseId("1199")
    public void shouldSeeRoomsSuggest() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + NOT_TODAY_DATE)
            .clicksOn(
                steps.pages().cal().touchHome().addEventButton(),
                steps.pages().cal().touchHome().eventPage().addRoomButton()
            )
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton())
            .clicksOnElementWithText(steps.pages().cal().touchHome().editOfficePage().menuItems(), BENUA)
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton(),
                BENUA
            );
        inputTextInRoomsSearchField(BENUA_ROOM_NAME);
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().cal().touchHome().editResourcesPage().suggestedRooms().waitUntil(not(empty())).get(0),
            BENUA_ROOM_NAME
        );
    }

    @Test
    @Title("Изменение организатора в форме создания события")
    @TestCaseId("1060")
    public void shouldChangeOrganizer() {
        Event event = steps.user().settingsCalSteps()
            .formDefaultEvent(steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId())
            .withName(getRandomName());
        steps.user().apiCalSettingsSteps().createNewEventWithAttendees(event, Arrays.asList(ROBOT_EMAIL));
        steps.user().defaultSteps().refreshPage()
            .opensUrl(UrlProps.urlProps().getBaseUri() + NOT_TODAY_DATE)
            .clicksOn(
                steps.pages().cal().touchHome().addEventButton(),
                steps.pages().cal().touchHome().eventPage().addOrganizatorButton(),
                steps.pages().cal().touchHome().editOrganizerPage().input()
            );
        String organizerName = steps.pages().cal().touchHome().editOrganizerPage().suggestedList().get(0).memberName()
            .getText();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().editOrganizerPage().suggested().get(0))
            .shouldSee(steps.pages().cal().touchHome().eventPage())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().eventPage().addOrganizatorButton(),
                organizerName
            )
            .clicksOn(
                steps.pages().cal().touchHome().eventPage().addOrganizatorButton(),
                steps.pages().cal().touchHome().editOrganizerPage().input()
            )
            .inputsTextInElement(
                steps.pages().cal().touchHome().editOrganizerPage().input(),
                ROBOT_EMAIL
            );
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().cal().touchHome().editOrganizerPage().input(),
            Keys.ENTER.toString()
        );
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().cal().touchHome().eventPage().addOrganizatorButton(),
            "R\n" + ROBOT_EMAIL
        );
    }

    @Test
    @Title("Формируется яббл участника по Enter")
    @TestCaseId("1207")
    public void shouldSeeMemberYabble() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + NOT_TODAY_DATE)
            .clicksOn(
                steps.pages().cal().touchHome().addEventButton(),
                steps.user().pages().calTouch().eventPage().changeParticipants(),
                steps.user().pages().calTouch().editParticipantsPage().input()
            )
            .inputsTextInElement(
                steps.user().pages().calTouch().editParticipantsPage().input(),
                ROBOT_EMAIL
            );
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().cal().touchHome().editParticipantsPage().input(),
            Keys.ENTER.toString()
        );
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.user().pages().calTouch().editParticipantsPage().pickedMembers().waitUntil(not(empty())).get(1),
            "R\n" + ROBOT_EMAIL
        );
    }

    @Test
    @Title("Показать переговорку на карте на странице рекомендуемых")
    @TestCaseId("1202")
    public void shouldSeeMap() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + NOT_TODAY_DATE)
            .clicksOn(
                steps.pages().cal().touchHome().addEventButton(),
                steps.pages().cal().touchHome().eventPage().addRoomButton()
            )
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().suggestedRooms()
                .waitUntil(not(empty())).get(0))
            .shouldSee(steps.pages().cal().touchHome().roomCard())
            .clicksOn(steps.pages().cal().touchHome().roomCard().showMap())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(MAP_URL));
    }

    @Test
    @Title("Показать переговорку на карте на странице создания встречи")
    @TestCaseId("1203")
    public void shouldSeeMapOnEventPage() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + NOT_TODAY_DATE)
            .clicksOn(
                steps.pages().cal().touchHome().addEventButton(),
                steps.pages().cal().touchHome().eventPage().addRoomButton()
            )
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().suggestedRooms()
                .waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().touchHome().roomCard().chooseRoomButton())
            .shouldSee(steps.pages().cal().touchHome().eventPage())
            .clicksOn(
                steps.pages().cal().touchHome().eventPage().roomsList().get(0),
                steps.pages().cal().touchHome().roomCard().showMap()
            )
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(MAP_URL));
    }

    @Test
    @Title("Добавление/удаление переговорок")
    @TestCaseId("1198")
    public void shouldAddRooms() {
        createEventWithRoom();
        steps.user().defaultSteps().shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().eventPage().addRoomButton(),
                ADD_OFFICE
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().addRoomButton())
            .shouldSee(steps.pages().cal().touchHome().editResourcesPage());
        String RoomNameTwo = steps.pages().cal().touchHome().editResourcesPage().suggestedRooms().get(0)
            .getText();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().suggestedRooms()
                .waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().touchHome().roomCard().chooseRoomButton())
            .shouldSee(steps.pages().cal().touchHome().eventPage())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().eventPage().roomsList().get(1),
                RoomNameTwo
            )
            .clicksOn(
                steps.pages().cal().touchHome().eventPage().remove(),
                steps.pages().cal().touchHome().eventPage().remove()
            )
            .shouldNotSee(steps.pages().cal().touchHome().eventPage().roomsList());
    }

    @Test
    @Title("Редактирование переговорки")
    @TestCaseId("1204")
    public void shouldChangeRooms() {
        String RoomName = createEventWithRoom();
        steps.user().defaultSteps().clicksOn(
                steps.pages().cal().touchHome().eventPage().roomsList().get(0),
                steps.pages().cal().touchHome().roomCard().chooseRoomButton()
            )
            .shouldSee(steps.pages().cal().touchHome().editResourcesPage().choosenSuggestRooms())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().editResourcesPage().suggestedRooms().get(0),
                RoomName
            );
        String RoomNameTwo = steps.pages().cal().touchHome().editResourcesPage().suggestedRooms().get(2).getText();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().suggestedRooms().get(2))
            .clicksOn(steps.pages().cal().touchHome().roomCard().chooseRoomButton())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().eventPage().roomsList().get(0),
                RoomNameTwo
            );
    }

    @Test
    @Title("Добавление переговорок из разных офисов")
    @TestCaseId("1205")
    public void shouldSeeOffice() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + NOT_TODAY_DATE)
            .clicksOn(
                steps.pages().cal().touchHome().addEventButton(),
                steps.user().pages().calTouch().eventPage().changeParticipants(),
                steps.user().pages().calTouch().editParticipantsPage().input()
            )
            .inputsTextInElement(
                steps.user().pages().calTouch().editParticipantsPage().input(),
                ROBOT_EMAIL
            );
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().cal().touchHome().editParticipantsPage().input(),
            Keys.ENTER.toString()
        );
        steps.user().defaultSteps()
            .clicksOn(
                steps.user().pages().calTouch().editParticipantsPage().save(),
                steps.pages().cal().touchHome().eventPage().addRoomButton()
            )
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton())
            .clicksOnElementWithText(steps.pages().cal().touchHome().editOfficePage().menuItems(), MOROZOV)
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton(),
                MOROZOV
            )
            .clicksOn(
                steps.pages().cal().touchHome().editResourcesPage().suggestedRooms().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().roomCard().chooseRoomButton()
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().addRoomButton())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton(),
                BENUA
            );
    }

    @Test
    @Title("Переключение офиса на странице переговорок")
    @TestCaseId("1201")
    public void shouldSwitchOffice() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + NOT_TODAY_DATE)
            .clicksOn(
                steps.pages().cal().touchHome().addEventButton(),
                steps.pages().cal().touchHome().eventPage().addRoomButton()
            )
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton())
            .clicksOnElementWithText(steps.pages().cal().touchHome().editOfficePage().menuItems(), MOROZOV)
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton(),
                MOROZOV
            )
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton())
            .shouldSee(steps.pages().cal().touchHome().editOfficePage());
        int lastItemIndex = steps.pages().cal().touchHome().editOfficePage().menuItems().size() - 1;
        steps.user().defaultSteps()
            .scrollTo(steps.pages().cal().touchHome().editOfficePage().menuItems().get(lastItemIndex))
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().editOfficePage().menuItems().get(lastItemIndex),
                DATACENTER
            )
            .shouldSeeInViewport(steps.pages().cal().touchHome().editOfficePage().menuItems().get(lastItemIndex))
            .clicksOn(steps.pages().cal().touchHome().editOfficePage().backToEditEvent())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton(),
                MOROZOV
            )
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton())
            .scrollTo(steps.pages().cal().touchHome().editOfficePage().menuItems().get(0))
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().editOfficePage().menuItems(),
                MAMONTOV
            )
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().editResourcesPage().chooseOfficeButton(),
                MAMONTOV
            );
    }

    @Test
    @Title("Ограниченная бронь для частично занятых переговорок при повторяющемся событии")
    @TestCaseId("1208")
    public void shouldSeeRoomAvailabilityForRepeatEvent() {
        String firstEventName = getRandomString();
        String firstEventDateStart = dateFormat.format(LocalDateTime.now().plusDays(3)) + "T06:%s:00";
        createEventWithTimeAndRoom(firstEventName, firstEventDateStart, ROOM_NAME);
        String secondEventName = getRandomString();
        String secondEventDateStart = dateFormat.format(LocalDateTime.now().plusDays(1)) + "T06:00:00";
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(DAY_GRID)
            .clicksOn(steps.pages().cal().touchHome().addEventButton())
            .inputsTextInElement(steps.pages().cal().touchHome().eventPage().editableTitle(), secondEventName);
        steps.user().calTouchCreateEventSteps().setDate(
            steps.pages().cal().touchHome().eventPage().startDateInput(),
            secondEventDateStart
        );
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().eventPage().editEventRepetition())
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().editRepetitionPage().menuItems().waitUntil(not(empty())),
                REPEAT_EVERY_DAY
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().addRoomButton());
        inputTextInRoomsSearchField(ROOM_NAME);
        steps.user().defaultSteps().shouldContainText(
                steps.pages().cal().touchHome().editResourcesPage().suggestedRooms().waitUntil(not(empty())).get(0)
                    .availability(),
                AVAILABILITY
            )
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().editResourcesPage().suggestedRooms(),
                ROOM_NAME
            )
            .clicksOn(steps.pages().cal().touchHome().roomCard().chooseRoomButton())
            .shouldContainText(
                steps.pages().cal().touchHome().eventPage().roomsList().get(0).availability(),
                AVAILABILITY
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().confirmSavePopup().confirmSaveText(),
                String.format(CONFIRM_MESSAGE, ROOM_NAME)
            )
            .clicksOn(steps.pages().cal().touchHome().confirmSavePopup().cancelBtn())
            .shouldNotSee(steps.pages().cal().touchHome().confirmSavePopup())
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm())
            .clicksOn(steps.pages().cal().touchHome().confirmSavePopup().addBtn())
            .scrollTo(steps.pages().cal().touchHome().gridRows().waitUntil(not(empty())).get(0))
            .shouldSeeElementInList(steps.pages().cal().touchHome().events(), secondEventName);
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(SHOW_DATE_PREFIX + firstEventDateStart.split("[T]")[0])
            .scrollTo(steps.pages().cal().touchHome().gridRows().waitUntil(not(empty())).get(0))
            .shouldNotSeeElementInList(steps.pages().cal().touchHome().events(), secondEventName);
    }

    @Test
    @Title("Выбор занятой переговорки при создании события")
    @TestCaseId("1210")
    public void shouldSeeBusyRoomError() {
        String firstEventName = getRandomString();
        String firstEventDateStart = dateFormat.format(LocalDateTime.now().plusDays(1)) + "T06:%s:00";
        createEventWithTimeAndRoom(firstEventName, firstEventDateStart, ROOM_NAME);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().addEventButton());
        steps.user().calTouchCreateEventSteps().setDate(
            steps.pages().cal().touchHome().eventPage().startDateInput(),
            String.format(firstEventDateStart, "00")
        );
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().eventPage().addRoomButton());
        inputTextInRoomsSearchField(ROOM_NAME);
        steps.user().defaultSteps()
            .shouldSee(steps.pages().cal().touchHome().editResourcesPage().suggestedRooms().waitUntil(not(empty()))
                .get(0))
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().editResourcesPage().suggestedRooms(),
                ROOM_NAME
            )
            .clicksOn(steps.pages().cal().touchHome().roomCard().chooseRoomButton())
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm())
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().notificationMessage(),
                WARNING_MESSAGE
            );
    }

    @Test
    @Title("Изменение занятости при редактировании времени события")
    @TestCaseId("1211")
    public void shouldSeeBusyRoomStatus() {
        LocalDateTime dateTime = LocalDateTime.now().plusDays(1);
        String firstEventName = getRandomString();
        String secondEventName = getRandomString();
        String firstEventDateStart = dateFormat.format(dateTime) + "T05:%s:00";
        String secondEventDateStart = dateFormat.format(dateTime) + "T06:%s:00";
        createEventWithTimeAndRoom(firstEventName, firstEventDateStart, ROOM_NAME);
        createEventWithTimeAndRoom(secondEventName, secondEventDateStart, ROOM_NAME);
        steps.user().defaultSteps().scrollTo(steps.pages().cal().touchHome().gridRows().waitUntil(not(empty())).get(0))
            .clicksOnElementWithText(steps.pages().cal().touchHome().events(), secondEventName)
            .clicksOn(steps.pages().cal().touchHome().eventPage().edit());
        steps.user().calTouchCreateEventSteps().setDate(
            steps.pages().cal().touchHome().eventPage().startDateInput(),
            String.format(firstEventDateStart, "00")
        );
        steps.user().defaultSteps()
            .shouldSee(steps.pages().cal().touchHome().eventPage().busyRoomsList())
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm())
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().notificationMessage(),
                WARNING_MESSAGE
            );
        steps.user().calTouchCreateEventSteps().setDate(
            steps.pages().cal().touchHome().eventPage().startDateInput(),
            String.format(secondEventDateStart, "00")
        );
        steps.user().defaultSteps().shouldSee(steps.pages().cal().touchHome().eventPage().roomsList())
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm())
            .shouldSee(steps.pages().cal().touchHome().grid());
    }

    @Step("Создание встречи и добавление переговорки")
    private String createEventWithRoom() {
        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + NOT_TODAY_DATE)
            .clicksOn(
                steps.pages().cal().touchHome().addEventButton(),
                steps.pages().cal().touchHome().eventPage().addRoomButton()
            );
        String RoomName = steps.pages().cal().touchHome().editResourcesPage().suggestedRooms().get(0).getText();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().editResourcesPage().suggestedRooms().waitUntil(
                not(empty())).get(0))
            .clicksOn(steps.pages().cal().touchHome().roomCard().chooseRoomButton())
            .shouldSee(steps.pages().cal().touchHome().eventPage())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().eventPage().roomsList().get(0),
                RoomName
            );
        return RoomName;
    }

    //В поле поиска переговорок вводим текст по одной букве, так как при вводе через inputsTextInElement в поле
    // остается только последняя буква введенного текста
    @Step("Ввод текста в поле поиска переговорок")
    private void inputTextInRoomsSearchField(String roomName) {
        for (int i = 0; i < roomName.length(); i++) {
            steps.user().defaultSteps().appendTextInElement(
                steps.pages().cal().touchHome().editResourcesPage().input(),
                Character.toString(roomName.charAt(i))
            );
        }
    }

    @Step("Создание встречи {0} на время {1} и в переговорке {2}")
    private void createEventWithTimeAndRoom(String eventName, String eventStartTime, String roomName) {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withStartTs(String.format(eventStartTime, "00"))
            .withEndTs(String.format(eventStartTime, "30"));
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().defaultSteps().refreshPage()
            .opensDefaultUrlWithPostFix(SHOW_DATE_PREFIX + eventStartTime.split("[T]")[0])
            .scrollTo(steps.pages().cal().touchHome().gridRows().waitUntil(not(empty())).get(0));
        addRoomInEvent(eventName, roomName);
    }

    @Step("Добавление переговорки {1} во встречу {0}")
    private void addRoomInEvent(String eventName, String roomName) {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .clicksOnElementWithText(steps.pages().cal().touchHome().events(), eventName)
            .clicksOn(steps.pages().cal().touchHome().eventPage().edit())
            .clicksOn(steps.pages().cal().touchHome().eventPage().addRoomButton())
            .shouldSee(steps.pages().cal().touchHome().editResourcesPage().suggestedRooms().waitUntil(not(empty())))
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().editResourcesPage().suggestedRooms(),
                roomName
            )
            .clicksOn(steps.pages().cal().touchHome().roomCard().chooseRoomButton())
            .shouldSee(steps.pages().cal().touchHome().eventPage())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().eventPage().roomsList().get(0),
                roomName
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm());
    }
}
