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
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.rules.RemoveAllOldAndCreateNewLayer.removeAllOldAndCreateNewLayer;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.VIEW_MEMBERS;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author marchart
 */
@Aqua.Test
@Title("[Корп] Создание события")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.CORP)
public class CorpCreateEventTest {

    private static final String CREDS = "CorpCreateEventTest";
    private static final String OTHER_USER_CREDS = "CorpCreateEventTestDifferent";
    private static final String STAFF_CONTACT = "zomb-popuga";
    private static final String BUSY_CONTACT = "robot-mailcorp-4";
    private static final String RESOURCE_ERROR_MESSAGE = "На выбранное время переговорок нет";
    private static final String MORNING_TIME = "10:00";
    private static final String NIGHT_TIME = "23:00";
    private static final String ROOM_WIDGET_HEADER = "23:00 – 23:30 сегодня";
    private static final String DIFFERENT_OFFICE_USER = "marchart";
    private static final String DIFFERENT_OFFICE_NAME = "Бенуа";
    private static final String ALL_ROOMS_ON_THIS_TIME = "Все переговорки на это время";
    private static final int WIDGET_TIME_PERIOD = 30;
    private static final String URL_FORM = "/event?name=%s&description=%s&organizer=robot-carl-gustav&attendees=me()" +
        "&resources=conf_spb_psyh@yandex-team.ru&othersCanView=0";
    private static final String URL_FORM_DSCR = "This is a description";
    private static final String ORGANIZER = "Офисный Психотерапевт Бенуа";
    private static final String MEMBER_SELF = "Я";
    private static final String PSYCHO_ROOM = "3.Психотерапевт";

    private DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
    private String date = dateFormat.format(LocalDateTime.now());
    private Long layerID;

    private CalendarRulesManager rules = calendarRulesManager()
        .withLock(AccLockRule.use().names(CREDS, OTHER_USER_CREDS));
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();
    private AccLockRule lock2 = AccLockRule.use().names(OTHER_USER_CREDS);
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock2);

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
        .around(removeAllOldAndCreateNewLayer(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().withAuth(auth2)
            .deleteLayers()
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
    }

    @Test
    @Title("Показываем саджест по контактам из стаффа")
    @TestCaseId("1260")
    public void shouldSeeStaffContactsInSuggest() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().leftPanel().createEvent(),
            steps.pages().cal().home().newEventPage().membersField()
        )
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), STAFF_CONTACT)
            .shouldContainText(steps.pages().cal().home().suggestItem().get(0), STAFF_CONTACT);
    }

    @Test
    @Title("Появляется и изменяется саджест свободных переговорок")
    @TestCaseId("944")
    public void shouldSeeRoomSuggest() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent());
        steps.user().calCreateEventSteps().setPlace(7);
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().home().newEventPage().roomsList().get(0).roomInput()
            )
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().newEventPage().roomsList().get(0).errorMessage(),
                RESOURCE_ERROR_MESSAGE
            )
            .clicksOn(steps.pages().cal().home().newEventPage().time());
        steps.user().calCreateEventSteps().setStartTime(NIGHT_TIME);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().roomsList().get(0).officeResource())
            .clicksOn(
                steps.pages().cal().home().officesList().get(0),
                steps.pages().cal().home().newEventPage().roomsList().get(0).roomInput()
            )
            .shouldSee(steps.pages().cal().home().suggestItem().get(0));
    }

    @Test
    @Title("Выбираем переговорку в виджете рекомендуемых")
    @TestCaseId("926")
    public void shouldSelectResourceInRecommended() {
        openNotEmptyRecommendedRoomList();
        steps.user().defaultSteps().shouldNotSee(steps.pages().cal().home().newEventPage().roomsLoader())
            .clicksOn(steps.pages().cal().home().newEventPage().recomendedRoomList().waitUntil(not(empty())).get(0))
            .shouldSee(steps.pages().cal().home().newEventPage().roomsList().get(0).selectedRoom())
            .clicksOn(steps.pages().cal().home().newEventPage().recomendedRoomList().get(0))
            .shouldNotSee(steps.pages().cal().home().newEventPage().roomsList().get(0).selectedRoom());
    }

    @Test
    @Title("Нажимаем на кнопку «Ещё переговорки»")
    @TestCaseId("928")
    public void shouldSeeMoreRoomsButton() {
        openNotEmptyRecommendedRoomList();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().moreRoomsButton())
            .shouldSee(steps.pages().cal().home().newEventPage().recomendedRoomList().get(3))
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().newEventPage().selectedTimeForRoom(),
                ROOM_WIDGET_HEADER
            )
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().newEventPage().moreRoomsButton(),
                ALL_ROOMS_ON_THIS_TIME
            )
            .clicksOn(steps.pages().cal().home().newEventPage().moreRoomsButton())
            .shouldSee(steps.pages().cal().home().newEventPage().recomendedRoomList().get(10))
            .shouldNotSee(steps.pages().cal().home().newEventPage().moreRoomsButton());
    }

    @Test
    @Title("Добавляем офис переговорок для участника из другого города")
    @TestCaseId("936")
    public void shouldSeeDifferentOfficeForUser() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .shouldSeeElementsCount(steps.pages().cal().home().newEventPage().roomsList(), 1)
            .clicksOn(steps.pages().cal().home().newEventPage().membersField())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), DIFFERENT_OFFICE_USER);
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ENTER.toString());
        steps.user().defaultSteps().shouldSeeElementsCount(steps.pages().cal().home().newEventPage().roomsList(), 2)
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().newEventPage().roomsList().get(1).officeResource(),
                DIFFERENT_OFFICE_NAME
            );
    }

    @Test
    @Title("Отображаем занятость участников в ябблах и в саджесте")
    @TestCaseId("1212")
    public void shouldSeeBusyMembers() {
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(getRandomName())
            .withStartTs(date + "T00:00:00")
            .withEndTs(date + "T21:00:00");
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().loginSteps().forAcc(lock.acc(OTHER_USER_CREDS)).loginsToCorp();

        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), BUSY_CONTACT)
            .clicksOn(steps.pages().cal().home().suggestItem().get(0).busyContact())
            .shouldContainText(steps.pages().cal().home().newEventPage().busyMember(), BUSY_CONTACT)
            .onMouseHover(steps.pages().cal().home().newEventPage().membersList().get(1))
            .clicksOn(steps.pages().cal().home().newEventPage().memberDeleteBtn().get(0));
        steps.user().calCreateEventSteps().setStartTime(NIGHT_TIME);
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), BUSY_CONTACT)
            .shouldNotSee(steps.pages().cal().home().suggestItem().get(0).busyContact())
            .clicksOn(steps.pages().cal().home().suggestItem().get(0).contactName())
            .shouldNotSee(steps.pages().cal().home().newEventPage().busyMember())
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().membersList().get(1), BUSY_CONTACT);
    }

    @Test
    @Title("Показываем крестик удаления яббла по ховеру и удаляем его")
    @TestCaseId("752")
    public void shouldSeeDeleteYabbleButton() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().leftPanel().createEvent(),
            steps.pages().cal().home().newEventPage().membersField()
        )
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), STAFF_CONTACT)
            .clicksOn(steps.pages().cal().home().suggestItem().get(0))
            .onMouseHover(steps.pages().cal().home().newEventPage().membersList().get(1))
            .shouldSee(
                steps.pages().cal().home().newEventPage().hoveredMember(),
                steps.pages().cal().home().newEventPage().memberDeleteBtn().get(0)
            )
            .onMouseHover(steps.pages().cal().home().newEventPage().nameInput())
            .shouldNotSee(steps.pages().cal().home().newEventPage().hoveredMember())
            .shouldSee(steps.pages().cal().home().newEventPage().memberDeleteBtn().get(0))
            .onMouseHover(steps.pages().cal().home().newEventPage().membersList().get(1))
            .clicksOn(steps.pages().cal().home().newEventPage().memberDeleteBtn().get(0))
            .shouldSeeElementsCount(steps.pages().cal().home().newEventPage().membersList(), 1);
    }

    @Test
    @Title("Показываем занятость организатора")
    @TestCaseId("674")
    public void shouldSeeBusyOrganizer() {
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID);
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .shouldSee(steps.pages().cal().home().newEventPage().busyMember());
        steps.user().calCreateEventSteps().setStartTime(NIGHT_TIME);
        steps.user().defaultSteps().shouldNotSee(steps.pages().cal().home().newEventPage().busyMember());
    }

    @Test
    @Title("Показываем карточку переговорки по ховеру в виджете")
    @TestCaseId("832")
    public void shouldSeeRoomDetailsByHover() {
        openNotEmptyRecommendedRoomList();
        steps.user().defaultSteps()
            .scrollTo(steps.pages().cal().home().newEventPage().recomendedRoomList().get(0))
            .onMouseHover(steps.pages().cal().home().newEventPage().recomendedRoomList().get(0))
            .shouldSee(steps.pages().cal().home().roomCard());
    }

    @Test
    @Title("Листаем время стрелочками в виджете переговорок")
    @TestCaseId("927")
    public void shouldChangeDateFromRoomWidget() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .clicksOn(steps.pages().cal().home().newEventPage().time())
            .clicksOnElementWithText(steps.pages().cal().home().timesList(), MORNING_TIME)
            .shouldNotSee(steps.pages().cal().home().newEventPage().roomsLoader());
        String widgetTime =
            substringBefore(steps.pages().cal().home().newEventPage().selectedTimeForRoom().getText(), " ");
        steps.user().defaultSteps()
            .shouldSee(steps.pages().cal().home().newEventPage().roomWidgetArrowDisabled())
            .clicksOn(steps.pages().cal().home().newEventPage().roomWidgetArrows().get(1))
            .shouldContainText(
                steps.pages().cal().home().newEventPage().selectedTimeForRoom(),
                String.format("%s – ", LocalTime.parse(widgetTime).plusMinutes(WIDGET_TIME_PERIOD))
            )
            .clicksOn(steps.pages().cal().home().newEventPage().roomWidgetArrows().get(0))
            .shouldContainText(
                steps.pages().cal().home().newEventPage().selectedTimeForRoom(),
                String.format("%s – ", widgetTime)
            );
    }

    @Test
    @Title("Открытие предзаполненной формы создания по URL с параметрами")
    @TestCaseId("1195")
    public void shouldOpenFilledFormByURL() {
        String eventName = getRandomName();
        steps.user().loginSteps().forAcc(lock.acc(OTHER_USER_CREDS)).loginsToCorp();
        steps.user().defaultSteps().opensUrl(
            String.format(
                UrlProps.urlProps().getBaseUri() + URL_FORM,
                eventName,
                URL_FORM_DSCR.replace(" ", "+")
            )
        )
            .shouldContainCSSAttributeWithValue(
                steps.pages().cal().home().editEventPage().nameField(),
                "value",
                eventName
            )
            .shouldContainText(steps.pages().cal().home().editEventPage().descriptionField(), URL_FORM_DSCR)
            .shouldContainText(steps.pages().cal().home().newEventPage().membersList().get(0), ORGANIZER)
            .shouldContainText(steps.pages().cal().home().newEventPage().membersList().get(1), MEMBER_SELF)
            .shouldContainText(steps.pages().cal().home().newEventPage().membersList().get(2), PSYCHO_ROOM)
            .shouldContainText(steps.pages().cal().home().newEventPage().visibilityChecked().get(1), VIEW_MEMBERS);
    }


    @Test
    @Title("Событие со статусом «Свободен» не влияет на занятость")
    @TestCaseId("671")
    public void shouldSeeAvailabilityForMemberWithEvent() {
        steps.user().apiCalSettingsSteps()
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer().withIsDefault(true));
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(getRandomName())
            .withStartTs(date + "T00:00:00")
            .withEndTs(date + "T21:00:00")
            .withAvailability("available");
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().loginSteps().forAcc(lock.acc(OTHER_USER_CREDS)).loginsToCorp();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), BUSY_CONTACT)
            .clicksOn(steps.pages().cal().home().suggestItem().get(0).contactName())
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().membersList().get(1), BUSY_CONTACT)
            .shouldNotSee(steps.pages().cal().home().newEventPage().busyMember());
    }

    @Step("Устанавливаем позднее время для непустого списка рекомендованных переговорок")
    private void openNotEmptyRecommendedRoomList() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent());
        steps.user().calCreateEventSteps().setPlace(0)
            .setStartTime(NIGHT_TIME);
        steps.user().defaultSteps()
            .shouldNotSee(steps.pages().cal().home().newEventPage().roomsLoader())
            .shouldSee(steps.pages().cal().home().newEventPage().recomendedRoomList());
    }
}
