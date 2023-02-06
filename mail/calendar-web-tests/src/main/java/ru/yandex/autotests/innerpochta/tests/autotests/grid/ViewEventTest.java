package ru.yandex.autotests.innerpochta.tests.autotests.grid;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
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

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DAY_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.LOCATION;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.MONTH_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.EVENT;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на просмотр события в сетке")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.VIEW_EVENT_POPUP)
@RunWith(DataProviderRunner.class)
public class ViewEventTest {

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    private static final String MAP_URL = "https://yandex.ru/maps/2/saint-petersburg/";
    private static final String TIME_TOOLTIP = "12:00 - 13:00\n";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> steps.user()));

    @DataProvider
    public static Object[][] urlPath() {
        return new Object[][]{
            {"https://habrahabr.ru", "habr"},
            {"http://запутевкой.рф", "xn--80aeignf2ae1aj.xn--p1ai"},
            {"https://ru.wikipedia.org/wiki/Заглавная_страница", "wikipedia"},
            {"http://wmconvirus.narod.ru", "wmconvirus"}
        };
    }

    @DataProvider
    public static Object[][] grid() {
        return new Object[][]{
            {DAY_GRID},
            {WEEK_GRID},
            {MONTH_GRID}
        };
    }

    @Before
    public void setUp() {
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID);
    }

    @Test
    @Title("Редактируем событие")
    @TestCaseId("18")
    @UseDataProvider("grid")
    public void shouldEditEventInFullView(String layoutPath) {
        createCustomEvent(" ", LOCATION);
        String eventName = getRandomName();
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(layoutPath)
            .clicksOn(
                steps.pages().cal().home().eventsAllList().get(0),
                steps.pages().cal().home().viewEventPopup().editEventBtn()
            )
            .shouldBeOnUrl(containsString(EVENT.fragment("")))
            .clicksOn(steps.pages().cal().home().newEventPage().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), eventName)
            .clicksOn(steps.pages().cal().home().newEventPage().locationfiled())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().locationInput(), LOCATION);
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ENTER.toString());
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup())
            .shouldContainText(steps.pages().cal().home().viewEventPopup().name(), eventName)
            .shouldContainText(steps.pages().cal().home().viewEventPopup().location(), LOCATION);
    }

    @Test
    @Title("Удаляем событие")
    @TestCaseId("65")
    @UseDataProvider("grid")
    public void shouldDeleteEvent(String layoutPath) {
        createCustomEvent(" ", " ");
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(layoutPath)
            .clicksOn(
                steps.pages().cal().home().eventsAllList().get(0),
                steps.pages().cal().home().viewEventPopup().deleteEventBtn(),
                steps.pages().cal().home().removeEventPopup().removeOneBtn()
            )
            .shouldSeeElementsCount(steps.pages().cal().home().eventsAllList(), 0);
    }

    @Test
    @Title("Клик по ссылке в описании")
    @TestCaseId("843")
    @UseDataProvider("urlPath")
    public void shouldOpenLink(String urlPath, String openUrl) {
        createCustomEvent(urlPath, " ");
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().eventsTodayList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup())
            .clicksOnLink(urlPath)
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(openUrl));
    }

    @Test
    @Title("Клик по ссылке в месте")
    @TestCaseId("843")
    public void shouldOpenMap() {
        createCustomEvent(" ", MAP_URL);
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().home().eventsTodayList().get(0),
                steps.pages().cal().home().viewEventPopup().location()
            )
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(MAP_URL));
    }

    @Test
    @Title("Клик по email в описании")
    @TestCaseId("853")
    public void shouldOpenComposeFromDescription() {
        createCustomEvent(DEV_NULL_EMAIL, " ");
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().eventsTodayList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup())
            .clicksOnLink(DEV_NULL_EMAIL)
            .switchOnJustOpenedWindow()
            .shouldSee(steps.pages().mail().composePopup().expandedPopup());
        steps.user().composeSteps().shouldSeeSendToAreaContains(DEV_NULL_EMAIL);
    }

    @Test
    @Title("Клик по email в месте")
    @TestCaseId("853")
    public void shouldOpenComposeFromLocation() {
        createCustomEvent(" ", DEV_NULL_EMAIL);
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().home().eventsTodayList().get(0),
                steps.pages().cal().home().viewEventPopup().location()
            )
            .switchOnJustOpenedWindow()
            .shouldSee(steps.pages().mail().composePopup().expandedPopup());
        steps.user().composeSteps().shouldSeeSendToAreaContains(DEV_NULL_EMAIL);
    }

    @Test
    @Title("Закрываем попап просмотра встречи")
    @TestCaseId("17")
    @UseDataProvider("grid")
    public void shouldCloseEvent(String grid) {
        createCustomEvent(" ", DEV_NULL_EMAIL);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(grid)
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup())
            .offsetClick(steps.pages().cal().home().viewEventPopup(), -11, 11)
            .shouldNotSee(steps.pages().cal().home().viewEventPopup())
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup())
            .clicksOn(steps.pages().cal().home().viewEventPopup().closeEventBtn())
            .shouldNotSee(steps.pages().cal().home().viewEventPopup())
            .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup());
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ESCAPE.toString());
        steps.user().defaultSteps().shouldNotSee(steps.pages().cal().home().viewEventPopup());
    }

    @Test
    @Title("Ховер на встречу в сетке")
    @TestCaseId("43")
    public void shouldSeeEventTooltip() {
        String name = getRandomName();
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(name)
            .withStartTs(dateFormat.format(date).split("[T]")[0] + "T12:00:00")
            .withEndTs(dateFormat.format(date).split("[T]")[0] + "T13:00:00");
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .shouldHasTitle(steps.pages().cal().home().eventsAllList().get(0), (TIME_TOOLTIP + name));
    }

    @Step
    @Title("Создаем кастомное событие с описанием и местом")
    private void createCustomEvent(String description, String location) {
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID)
            .withDescription(description)
            .withLocation(location);
        steps.user().apiCalSettingsSteps().createNewEvent(event);
    }
}
