package ru.yandex.autotests.innerpochta.tests.autotests.event;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.cal.util.CalFragments;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DAY_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.MONTH_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.VIEW_ALL;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.DAY;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.MONTH;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.WEEK;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Создание события на отдельной странице")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.NEW_EVENT_PAGE)
@RunWith(DataProviderRunner.class)
public class CreateEventOnPageTest {

    private static final String LOCATION = "Сен-Бенуа";
    private static final String NEW_EVENT_DATE = "05.12.2035";
    private static final String NEW_EVENT_DATE_URL = "2035-12-05";
    private static final String NEW_REPEAT_EVENT_DATE = "29.04.2020";
    private static final String NEW_REPEAT_EVENT_DAYOFWEEK = "Ср";
    private static final String NEW_REPEAT_EVENT_INFO = "Повторять по средам";
    private static final String URL_FORM = "/event?name=%s&description=%s&attendees=%s&attendees=%s&othersCanView=1";
    private static final String URL_FORM_DSCR = "This is a description";
    private static final String VALIDATE_MEMBERS_FIELD_MSG_1 = "Введите подходящий адрес почты";
    private static final String VALIDATE_MEMBERS_FIELD_MSG_2 = "Этот участник уже добавлен";
    private static final String REPEAT_PERIOD_WEEK = "Повторять по неделям";
    private static final String ALL_DAY = "Весь день";
    private static final String WRONG_DOMAIN = "qwe@rty";
    private static final String EVENT_TIME_START_TEXT = "01:00";
    private static final String EVENT_TIME_END_TEXT = "03:00";
    private static final String DATA_EMAIL_ATTRIBUTE = "data-email";
    private static final String BUSY_CONTACT = "yandex-team-mailt-76@yandex.ru";
    private static final String OTHER_EMAIL = "testbot2@yandex.ru";
    private static final int EVENT_TIME_START = 2;
    private static final int EVENT_TIME_END = 4;

    private Long layerID;
    private String eventName;

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().usePreloadedTusAccounts(2);
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock.accNum(1));
    private WebDriverRule webDriverRule2 = new WebDriverRule();
    private AllureStepStorage user2 = new AllureStepStorage(webDriverRule2, auth2);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(auth2)
        .around(clearAcc(() -> steps.user()))
        .around(clearAcc(() -> user2));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Включаем показ выходных в сетке",
            new Params().withShowWeekends(true)
        );
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[][] grids() {
        return new Object[][]{
            {DAY_GRID, DAY},
            {WEEK_GRID, WEEK},
            {MONTH_GRID, MONTH}
        };
    }

    @Test
    @Title("Редактирование встречи с датой начала в будущем")
    @TestCaseId("787")
    @UseDataProvider("grids")
    public void shouldEditEventInFuture(String period, CalFragments viewName) {
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formEventInFuture(layerID);
        steps.user().apiCalSettingsSteps().createNewEvent(event)
            .togglerLayer(layerID, true);
        String eventDate = event.getStartTs().split("[T]")[0];
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(period + "?show_date=" + eventDate)
            .clicksOn(
                steps.pages().cal().home().eventsAllList().get(0),
                steps.pages().cal().home().viewEventPopup().editEventBtn()
            )
            .clicksOn(steps.pages().cal().home().newEventPage().locationfiled())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().locationInput(), LOCATION)
            .clicksOn(
                steps.pages().cal().home().newEventPage().title(),
                steps.pages().cal().home().newEventPage().saveChangesBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldBeOnUrl(CoreMatchers.containsString(viewName.makeUrlPart("show_date=" + eventDate)));
    }

    @Test
    @Title("Создание встречи с датой начала в будущем")
    @TestCaseId("786")
    @UseDataProvider("grids")
    public void shouldCreateEventInFuture(String period, CalFragments viewName) {
        String name = getRandomName();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(period)
            .clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .shouldSee(steps.pages().cal().home().newEventPage())
            .clicksOn(steps.pages().cal().home().newEventPage().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), name)
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPage().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPage().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().descriptionInput(), getRandomName());
        steps.user().defaultSteps().inputsTextInElementClearingThroughHotKeys(
            steps.pages().cal().home().newEventPage().dateInputList().get(0),
            NEW_EVENT_DATE
        )
            .clicksOn(steps.pages().cal().home().newEventPage().locationfiled())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().locationInput(), LOCATION)
            .clicksOn(
                steps.pages().cal().home().newEventPage().title(),
                steps.pages().cal().home().newEventPage().createFromPageBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldBeOnUrl(CoreMatchers.containsString(viewName
                .makeUrlPart("show_date=" + NEW_EVENT_DATE_URL)))
            .shouldContainText(steps.pages().cal().home().eventsAllList().get(0).eventName(), name);
    }

    @Test
    @Title("Поле «Описание» разворачивается по кнопке")
    @TestCaseId("754")
    public void shouldSeeDescription() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .shouldSee(steps.pages().cal().home().newEventPage())
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPage().descriptionAddBtn())
            .shouldSee(steps.pages().cal().home().newEventPage().descriptionInput());
    }

    @Test
    @Title("Создание повторяющегося события на весь день")
    @TestCaseId("151")
    @UseDataProvider("grids")
    public void shouldCreateAllDayRepeatEvent(String period, CalFragments viewName) {
        String eventName = getRandomString();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(period);
        createEvent(eventName, true, true);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .shouldContainText(steps.pages().cal().home().viewEventPopup().name(), eventName)
            .shouldContainText(steps.pages().cal().home().viewEventPopup().timeAndDate(), ALL_DAY)
            .shouldSee(steps.pages().cal().home().viewEventPopup().repeatSymbol());
    }

    @Test
    @Title("Создание повторяющегося события на весь день через редактирование")
    @TestCaseId("151")
    @UseDataProvider("grids")
    public void shouldCreateAllDayRepeatEventThroughEdit(String period, CalFragments viewName) {
        String eventName = getRandomString();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(period);
        createEvent(eventName, true, false);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .turnTrue(steps.pages().cal().home().newEventPage().repeatEventCheckBox())
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup().repeatSymbol());
    }

    @Test
    @Title("Редактирование всех полей события на весь день")
    @TestCaseId("143")
    @UseDataProvider("grids")
    public void shouldEditAllFieldsAllDayEvent(String period, CalFragments viewName) {
        String eventName = getRandomString();
        String eventDscr = getRandomString();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(period);
        createEvent("", true, false);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().allDayEvents().waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().newEventPage().nameInput(),
                eventName
            )
            .clicksOn(steps.pages().cal().home().newEventPage().descriptionAddBtn())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().descriptionInput(), eventDscr)
            .deselects(steps.pages().cal().home().newEventPage().allDayCheckBox())
            .clicksOn(steps.pages().cal().home().newEventPage().time())
            .onMouseHoverAndClick(steps.pages().cal().home().timesList().get(EVENT_TIME_START))
            .clicksOn(steps.pages().cal().home().newEventPage().timeEnd())
            .onMouseHoverAndClick(steps.pages().cal().home().timeEndVariants().get(EVENT_TIME_END))
            .inputsTextInElement(
                steps.pages().cal().home().newEventPage().membersInput(),
                lock.firstAcc().getSelfEmail()
            )
            .inputsTextInElement(steps.pages().cal().home().newEventPage().locationInput(), LOCATION)
            .clicksOn(
                steps.pages().cal().home().suggestItem().waitUntil(not(empty())).get(0),
                steps.pages().cal().home().newEventPage().visibility().get(0)
            )
            .turnTrue(steps.pages().cal().home().newEventPage().accessCanEditCheckBox())
            .clicksOn(steps.pages().cal().home().newEventPage().status())
            .clicksOn(steps.pages().cal().home().statusList().get(2))
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .shouldContainText(steps.pages().cal().home().viewEventPopup().name(), eventName)
            .shouldContainText(steps.pages().cal().home().viewEventPopup().description(), eventDscr)
            .shouldNotContainText(steps.pages().cal().home().viewEventPopup().timeAndDate(), ALL_DAY)
            .shouldContainText(steps.pages().cal().home().viewEventPopup().location(), LOCATION)
            .shouldContainText(
                steps.pages().cal().home().viewEventPopup().timeAndDate(),
                String.format(EVENT_TIME_START_TEXT, " — ", EVENT_TIME_END_TEXT)
            );
        createEvent("", true, false);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().newEventPage().nameInput(),
                eventName
            )
            .clicksOn(steps.pages().cal().home().newEventPage().descriptionAddBtn())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().descriptionInput(), eventDscr)
            .deselects(steps.pages().cal().home().newEventPage().allDayCheckBox())
            .clicksOn(steps.pages().cal().home().newEventPage().time())
            .onMouseHoverAndClick(steps.pages().cal().home().timesList().get(EVENT_TIME_START))
            .clicksOn(steps.pages().cal().home().newEventPage().timeEnd())
            .scrollAndClicksOn(steps.pages().cal().home().timeEndVariants().get(EVENT_TIME_END))
            .inputsTextInElement(
                steps.pages().cal().home().newEventPage().membersInput(),
                lock.firstAcc().getSelfEmail()
            )
            .inputsTextInElement(steps.pages().cal().home().newEventPage().locationInput(), LOCATION)
            .clicksOn(
                steps.pages().cal().home().newEventPage().nameInput(),
                steps.pages().cal().home().newEventPage().visibility().get(0)
            )
            .turnTrue(steps.pages().cal().home().newEventPage().accessCanEditCheckBox())
            .clicksOn(steps.pages().cal().home().newEventPage().status())
            .clicksOn(steps.pages().cal().home().statusList().get(2))
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .clicksOn(steps.pages().cal().home().allDayEvents().waitUntil(not(empty())).get(0))
            .shouldContainText(steps.pages().cal().home().viewEventPopup().name(), eventName)
            .shouldContainText(steps.pages().cal().home().viewEventPopup().description(), eventDscr)
            .shouldNotContainText(steps.pages().cal().home().viewEventPopup().timeAndDate(), ALL_DAY)
            .shouldContainText(steps.pages().cal().home().viewEventPopup().location(), LOCATION)
            .shouldContainText(
                steps.pages().cal().home().viewEventPopup().timeAndDate(),
                String.format(EVENT_TIME_START_TEXT, " — ", EVENT_TIME_END_TEXT)
            );
    }

    @Test
    @Title("Редактирование события на весь день без сохранения")
    @TestCaseId("143")
    @UseDataProvider("grids")
    public void shouldEditButNotSaveAllDayEvent(String period, CalFragments viewName) {
        String eventName = getRandomString();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(period);
        createEvent("", true, false);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().allDayEvents().waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().newEventPage().nameInput(),
                eventName
            )
            .clicksOn(steps.pages().cal().home().newEventPage().cancelButton())
            .clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .shouldNotContainText(steps.pages().cal().home().viewEventPopup().name(), eventName);
    }

    @Test
    @Title("Открытие предзаполненной формы создания по URL с параметрами")
    @TestCaseId("736")
    public void shouldOpenFilledFormByURL() {
        String eventName = getRandomName();
        steps.user().defaultSteps().opensUrl(
            String.format(
                UrlProps.urlProps().getBaseUri() + URL_FORM,
                eventName,
                URL_FORM_DSCR.replace(" ", "+"),
                DEV_NULL_EMAIL,
                OTHER_EMAIL
            )
        )
            .shouldContainCSSAttributeWithValue(
                steps.pages().cal().home().editEventPage().nameField(),
                "value",
                eventName
            )
            .shouldContainText(steps.pages().cal().home().editEventPage().descriptionField(), URL_FORM_DSCR)
            .shouldContainText(steps.pages().cal().home().newEventPage().membersList().get(0), DEV_NULL_EMAIL)
            .shouldContainText(steps.pages().cal().home().newEventPage().membersList().get(1), OTHER_EMAIL)
            .shouldContainText(steps.pages().cal().home().newEventPage().visibilityChecked().get(0), VIEW_ALL);
    }

    @Test
    @Title("Валидация поля «Участники»")
    @TestCaseId("119")
    public void shouldValidateFieldMembers() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent());
        membersFieldInput(WRONG_DOMAIN);
        steps.user().defaultSteps().shouldContainText(
            steps.pages().cal().home().newEventPage().membersErrorMessage(),
            VALIDATE_MEMBERS_FIELD_MSG_1
        );
        membersFieldInput(getRandomString());
        steps.user().defaultSteps().shouldContainText(
            steps.pages().cal().home().newEventPage().membersErrorMessage(),
            VALIDATE_MEMBERS_FIELD_MSG_1
        );
        membersFieldInput(DEV_NULL_EMAIL);
        membersFieldInput(DEV_NULL_EMAIL);
        steps.user().defaultSteps().shouldContainText(
            steps.pages().cal().home().newEventPage().membersErrorMessage(),
            VALIDATE_MEMBERS_FIELD_MSG_2
        );
    }

    @Test
    @Title("Изменение описания повторения при изменении даты события")
    @TestCaseId("966")
    public void shouldChangeRepeatDescriptionAfterDataChanged() {
        String eventName = getRandomString();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), eventName)
            .turnTrue(steps.pages().cal().home().newEventPage().repeatEventCheckBox())
            .clicksOn(steps.pages().cal().home().newEventPage().dateStartInput())
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().newEventPage().dateStartInput(),
                NEW_REPEAT_EVENT_DATE
            );
        steps.user().hotkeySteps()
            .pressSimpleHotKey(steps.pages().cal().home().newEventPage().dateStartInput(), key(Keys.ENTER));
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().changeRepeatPopup())
            .shouldSee(steps.pages().cal().home().repeatPopup())
            .shouldContainText(steps.pages().cal().home().repeatPopup().repeatPeriodChosen(), REPEAT_PERIOD_WEEK)
            .shouldContainText(steps.pages().cal().home().repeatPopup().weekDayChosen(), NEW_REPEAT_EVENT_DAYOFWEEK)
            .shouldContainText(steps.pages().cal().home().repeatPopup().repeatInfo(), NEW_REPEAT_EVENT_INFO);
    }

    @Test
    @Title("Отключение настройки «События влияют на занятость»")
    @TestCaseId("669")
    public void shouldDisableStatusOption() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().expandCalendars())
            .onMouseHoverAndClick(steps.pages().cal().home().leftPanel().calSettings())
            .deselects(steps.pages().cal().home().editCalSideBar().setEmploymentCheckbox())
            .clicksOn(steps.pages().cal().home().editCalSideBar().saveBtn())
            .clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .shouldContainText(steps.pages().cal().home().newEventPage().statusField(), "Свободен");
    }

    @Test
    @Title("Нажимаем «Написать участникам» на странице редактирования события")
    @TestCaseId("1021")
    public void shouldOpenMailCompose() {
        eventName = getRandomName();
        Event event = steps.user().
            settingsCalSteps().formDefaultEvent(layerID).withName(eventName).withParticipantsCanEdit(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(BUSY_CONTACT, OTHER_EMAIL));
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .clicksOn(steps.pages().cal().home().editEventPage().writeMail())
            .switchOnJustOpenedWindow()
            .shouldSee(steps.pages().mail().composePopup().expandedPopup())
            .shouldContainsAttribute(
                steps.pages().mail().composePopup().yabbleToEmailList().get(0),
                DATA_EMAIL_ATTRIBUTE,
                BUSY_CONTACT
            )
            .shouldContainsAttribute(
                steps.pages().mail().composePopup().yabbleToEmailList().get(1),
                DATA_EMAIL_ATTRIBUTE,
                OTHER_EMAIL
            )
            .shouldContainValue(steps.pages().mail().composePopup().expandedPopup().sbjInput(), eventName);
    }

    @Test
    @Title("Учитывать занятость участников при включении повторения для единичного события")
    @TestCaseId("1136")
    public void shouldConsiderEmploymentWhenTurningOnRepeat() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(getRandomName())
            .withStartTs(dateFormat.format(date.plusDays(7)).split("[T]")[0] + "T" + "01:00:00")
            .withEndTs(dateFormat.format(date.plusDays(7)).split("[T]")[0] + "T" + "03:00:00");
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(getRandomName())
            .withStartTs(dateFormat.format(date).split("[T]")[0] + "T" + "01:00:00")
            .withEndTs(dateFormat.format(date).split("[T]")[0] + "T" + "03:00:00")
            .withParticipantsCanEdit(true);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().eventsAllList().waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPage().busyMember())
            .turnTrue(steps.pages().cal().home().editEventPage().repeatEventCheckBox())
            .shouldSee(steps.pages().cal().home().editEventPage().busyMember());
    }

    @Step("Создание повторяющегося/неповторяющегося события на весь день")
    public void createEvent(String name, boolean allDay, boolean repeat) {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), name);
        if (allDay)
            steps.user().defaultSteps().turnTrue(steps.pages().cal().home().newEventPage().allDayCheckBox());
        if (repeat)
            steps.user().defaultSteps().turnTrue(steps.pages().cal().home().newEventPage().repeatEventCheckBox());
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().createFromPageBtn());
    }

    @Step("Ввод данных в поле «Участники»")
    public void membersFieldInput(String data) {
        steps.user().defaultSteps().inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), data);
        steps.user().hotkeySteps()
            .pressSimpleHotKey(steps.pages().cal().home().newEventPage().membersInput(), key(Keys.ENTER));
    }
}
