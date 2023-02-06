package ru.yandex.autotests.innerpochta.tests.autotests.event;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
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
import java.util.Arrays;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.COLUMN_CENTER;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.PARTICIPANTS;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.RANDOM_X_COORD;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.RANDOM_Y_COORD;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TIME_11AM;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.EVENT;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.WEEK;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.FREEZE_PAGE_WITH_WAIT_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Создание события")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.NEW_EVENT_POPUP)
public class CreateEventTest {

    private static final String EMAILS = "testbotauto@yandex.ru; test3.cosmo@yandex.ru;";
    private static final String LOCATION = "Сен-Бенуа, Реюньон";
    private static final String WARNING_MESSAGE = "У вас нет прав на редактирование события.";
    private static final String ABOOK_CONTACT = "test";
    private static final String TELEMOST_STR = "Ссылка на видеовстречу: https://telemost.yandex.ru/j/";
    private static final int NUM_OF_PARTICIPANTS_TO_ADD = 3;
    private static final int EVENT_TIME = 11;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount(2);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        // На юзере заготавливаем популярные контакты путём создания встреч на вчера
        String yesterdayDate = DATE_FORMAT.format(LocalDateTime.now().minusDays(1));
        steps.user().apiCalSettingsSteps().createNewLayer(steps.user().settingsCalSteps().formDefaultLayer())
            .createNewEvent(
                steps.user().settingsCalSteps().formDefaultEvent(
                    steps.user().apiCalSettingsSteps().getUserLayersIds().get(0)
                )
                    .withStartTs(yesterdayDate + "T00:00:00")
                    .withEndTs(yesterdayDate + "T01:00:00")
                    .withAttendeesArray(Arrays.copyOfRange(PARTICIPANTS, 0, NUM_OF_PARTICIPANTS_TO_ADD))
            );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создаем полностью заполненное событие из попапа")
    @TestCaseId("306")
    public void shouldCreateFullEventWithPopup() {
        String eventName = openPopupAndInputEventName();
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPopup().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPopup().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().descriptionInput(), getRandomName())
            .clicksOn(steps.pages().cal().home().newEventPopup().timeDateField())
            .clicksOn(steps.pages().cal().home().newEventPopup().timeInputList().get(0))
            .onMouseHoverAndClick(steps.pages().cal().home().timesList().get(EVENT_TIME))
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPopup().layerFieldSelect())
            .clicksOn(
                steps.pages().cal().home().layersList().get(1),
                steps.pages().cal().home().newEventPopup().createFromPopupBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPopup())
            .shouldContainText(steps.pages().cal().home().eventsTodayList().get(0).eventName(), eventName);
    }

    @Test
    @Title("Создаем полностью заполненное событие из страницы")
    @TestCaseId("24")
    public void shouldCreateFullEventWithPage() {
        String name = getRandomName();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(EVENT.makeUrlPart(""))
            .clicksOn(steps.pages().cal().home().newEventPage().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), name)
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPage().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPage().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().descriptionInput(), getRandomName())
            .clicksOn(steps.pages().cal().home().newEventPage().membersField())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), EMAILS)
            .clicksOn(steps.pages().cal().home().newEventPage().timeInputList().get(0))
            .onMouseHoverAndClick(steps.pages().cal().home().timesList().get(EVENT_TIME))
            .clicksOn(steps.pages().cal().home().newEventPage().locationfiled())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().locationInput(), LOCATION)
            .turnTrue(
                steps.pages().cal().home().newEventPage().repeatEventCheckBox(),
                steps.pages().cal().home().newEventPage().accessCanEditCheckBox()
            )
            .clicksOn(steps.pages().cal().home().newEventPage().layerFieldSelect())
            .clicksOn(
                steps.pages().cal().home().layersList().get(1),
                steps.pages().cal().home().newEventPage().createFromPageBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldContainText(steps.pages().cal().home().eventsTodayList().get(0).eventName(), name);
    }

    @Test
    @Title("Показываем ошибку при редактировании события без прав")
    @TestCaseId("984")
    public void shouldNotChangeBlockedEvent() {
        String name = getRandomName();
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(name);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK.makeUrlPart(""))
            .shouldContainText(steps.pages().cal().home().eventsTodayList().get(0).eventName(), name);

        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK.makeUrlPart(""))
            .shouldContainText(steps.pages().cal().home().eventsTodayList().get(0).eventName(), name);
        freezePageWithTimeout();
        dragAndDropByCoords(
            steps.pages().cal().home().eventsTodayList().get(0).eventName(),
            RANDOM_X_COORD, RANDOM_Y_COORD
        );
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().cal().home().notificationMessage(),
            WARNING_MESSAGE
        );
    }

    @Test
    @Title("Отображение и смена популярных контактов в саджесте")
    @TestCaseId("722")
    public void shouldSeeContactsSuggest() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().leftPanel().createEvent(),
            steps.pages().cal().home().newEventPage().membersField()
        )
            .shouldSee(steps.pages().cal().home().suggest());
        String contactName = steps.pages().cal().home().suggestItem().get(0).contactName().getText();
        steps.user().defaultSteps().onMouseHoverAndClick(steps.pages().cal().home().suggestItem().get(0))
            .shouldContainText(steps.pages().cal().home().newEventPage().membersList().get(0), contactName)
            .clicksOn(steps.pages().cal().home().newEventPage().membersField())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), ABOOK_CONTACT)
            .shouldSee(steps.pages().cal().home().suggest())
            .shouldContainText(steps.pages().cal().home().suggestItem().get(0), ABOOK_CONTACT)
            .clicksOn(steps.pages().cal().home().newEventPage().nameInput())
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPage().memberDeleteBtn().get(0))
            .clicksOn(steps.pages().cal().home().newEventPage().membersField())
            .shouldSee(steps.pages().cal().home().suggest())
            .shouldContainText(steps.pages().cal().home().suggestItem().get(0), contactName);
    }

    @Test
    @Title("Создаем событие со ссылкой на телемост")
    @TestCaseId("1391")
    public void shouldCreateEventWithTelemost() {
        String eventName = openPopupAndInputEventName();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().home().newEventPopup().telemostBtn(),
                steps.pages().cal().home().newEventPopup().createFromPopupBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPopup())
            .shouldContainText(steps.pages().cal().home().eventsTodayList().get(0).eventName(), eventName)
            .clicksOn(steps.pages().cal().home().eventsTodayList().get(0))
            .shouldContainText(steps.pages().cal().home().viewEventPopup().description(), TELEMOST_STR);
    }

    @Test
    @Title("Создаем событие со ссылкой на телемост и описанием")
    @TestCaseId("1394")
    public void shouldCreateEventWithTelemostAndDescription() {
        String eventName = openPopupAndInputEventName();
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().newEventPopup().descriptionAddBtn())
            .clicksOn(steps.pages().cal().home().newEventPopup().descriptionInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().descriptionInput(), eventName)
            .clicksOn(
                steps.pages().cal().home().newEventPopup().telemostBtn(),
                steps.pages().cal().home().newEventPopup().createFromPopupBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPopup())
            .shouldContainText(steps.pages().cal().home().eventsTodayList().get(0).eventName(), eventName)
            .clicksOn(steps.pages().cal().home().eventsTodayList().get(0))
            .shouldContainText(steps.pages().cal().home().viewEventPopup().description(), eventName)
            .shouldContainText(steps.pages().cal().home().viewEventPopup().description(), TELEMOST_STR);
    }

    @Step
    private void dragAndDropByCoords(WebElement element, int xOffset, int yOffset) {
        steps.user().defaultSteps().shouldSee(element);
        new Actions(steps.getDriver()).dragAndDropBy(element, xOffset, yOffset).perform();
    }

    @Step
    private void freezePageWithTimeout() {
        steps.user().defaultSteps().executesJavaScript(FREEZE_PAGE_WITH_WAIT_SCRIPT);
    }

    @Step
    private String openPopupAndInputEventName() {
        String eventName = getRandomName();
        steps.user().defaultSteps()
            .offsetClick(steps.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .clicksOn(steps.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().nameInput(), eventName);
        return eventName;
    }
}
