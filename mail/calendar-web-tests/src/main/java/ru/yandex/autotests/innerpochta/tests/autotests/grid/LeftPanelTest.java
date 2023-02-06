package ru.yandex.autotests.innerpochta.tests.autotests.grid;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.cal.util.CalFragments;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.DAY;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.MONTH;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.SETTINGS_CAL;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на левую колонку")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.LEFT_PANEL)
@RunWith(DataProviderRunner.class)
public class LeftPanelTest {

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Включаем недельный вид, разворачиваем календари и левую колонку",
            new Params().withDefaultView("week").withIsCalendarsListExpanded(true).withIsAsideExpanded(true)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[][] testData() {
        return new Object[][]{
            {0, DAY},
            {2, MONTH},
        };
    }

    @Test
    @Title("Переключаем вид")
    @TestCaseId("26")
    @UseDataProvider("testData")
    public void shouldSelectView(int numberOfView, CalFragments viewName) {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().leftPanel().view())
            .clicksOn(steps.pages().cal().home().selectView().get(numberOfView))
            .shouldBeOnUrl(containsString(viewName.makeUrlPart("")));
    }

    @Test
    @Title("Переходим в настройки таймзоны")
    @TestCaseId("316")
    public void shouldOpenTimeSettings() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().leftPanel().changeTime())
            .shouldSee(steps.pages().cal().home().timezoneItems().waitUntil(not(empty())).get(20));
    }

    @Test
    @Title("Переходим в настройки слоя")
    @TestCaseId("69")
    public void shouldOpenCalSettings() {
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().leftPanel().calSettings())
            .shouldBeOnUrl(containsString(SETTINGS_CAL.fragment()));
    }

    @Test
    @Title("Выбираем другой месяц")
    @TestCaseId("36")
    public void shouldChangeMonth() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().miniCalendar().currentMonth());
        String newMonth = steps.pages().cal().home().leftPanel().monthList().get(4).getText();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().monthList().get(4))
            .shouldHasText(steps.pages().cal().home().leftPanel().miniCalendar().currentMonth(), newMonth);
    }

    @Test
    @Title("Выбираем другой год")
    @TestCaseId("363")
    public void shouldChangeYear() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().miniCalendar().currentYear());
        String newYear = steps.pages().cal().home().leftPanel().yearList().get(4).getText();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().yearList().get(4))
            .shouldHasText(steps.pages().cal().home().leftPanel().miniCalendar().currentYear(), newYear);
    }

    @Test
    @Title("Вылючаем/Включаем слой")
    @TestCaseId("109")
    public void shouldTurnOnOffLayer() {
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID);
        steps.user().apiCalSettingsSteps().createNewEvent(event)
            .togglerLayer(layerID, true);
        steps.user().defaultSteps().refreshPage()
            .shouldSee(steps.pages().cal().home().eventsTodayList().get(0))
            .deselects(steps.pages().cal().home().leftPanel().layerCalCheckBox())
            .shouldSeeElementsCount(steps.pages().cal().home().eventsTodayList(), 0)
            .turnTrue(steps.pages().cal().home().leftPanel().layerCalCheckBox())
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().home().eventsTodayList().get(0).eventName(),
                event.getName()
            );
    }

    @Test
    @Title("Стрелки навигации в мини-календаре видны только по ховеру")
    @TestCaseId("38")
    public void shouldSeeElementsOnHover() {
        steps.user().defaultSteps()
            .shouldNotSee(
                steps.pages().cal().home().leftPanel().miniCalendar().nextMonth(),
                steps.pages().cal().home().leftPanel().miniCalendar().previousMonth()
            )
            .onMouseHover(steps.pages().cal().home().leftPanel().miniCalendar())
            .shouldSee(
                steps.pages().cal().home().leftPanel().miniCalendar().nextMonth(),
                steps.pages().cal().home().leftPanel().miniCalendar().previousMonth()
            );
    }

    @Test
    @Title("Сохранение настройки свернутости левой колонки")
    @TestCaseId("997")
    public void shouldSaveLeftPanelSetting() {
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().leftPanel().manageLeftPanel())
            .shouldNotSee(steps.pages().cal().home().expandedLeftPanel());
        logoutFromCal();
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().shouldNotSee(steps.pages().cal().home().expandedLeftPanel());
    }

    @Test
    @Title("Сохранение настройки вида сетки при переключении в левой колонке")
    @TestCaseId("999")
    public void shouldSaveGridViewSetting() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().leftPanel().view())
            .clicksOn(steps.pages().cal().home().selectView().get(2))
            .shouldBeOnUrl(containsString(MONTH.makeUrlPart("")));
        logoutFromCal();
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().shouldBeOnUrl(containsString(MONTH.makeUrlPart("")));
    }

    @Step
    @Title("Разлогин из календаря")
    private void logoutFromCal() {
        steps.getDriver().manage().deleteAllCookies();
        steps.user().defaultSteps().refreshPage();
    }
}
