package ru.yandex.autotests.innerpochta.tests.screentests.grid;

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
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DAY_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.IGNORED;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.MONTH_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_SCROLL_CAL_TO_10AM;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Верстка левой колонки")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.LEFT_PANEL)
@RunWith(DataProviderRunner.class)
public class LeftPanelTest {

    private static final String MONTH = "май";
    private static final String RU_LANG_IN_DROPDOWN = "0";
    private static final String EN_LANG_IN_DROPDOWN = "1";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED).withClosePromo().withScrollGrid();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> stepsProd.user()));

    @DataProvider
    public static Object[][] grids() {
        return new Object[][]{
            {DAY_GRID},
            {WEEK_GRID},
            {MONTH_GRID}
        };
    }

    @DataProvider
    public static Object[][] elements() {
        return new Object[][]{
            {".react-datepicker__month-read-view--selected-month"},
            {".react-datepicker__year-read-view--selected-year"},
            {".react-datepicker__navigation--next"},
            {".react-datepicker__navigation--previous"}
        };
    }

    @Before
    public void setUp() {
        stepsProd.user().apiCalSettingsSteps().updateUserSettings(
            "Включаем недельный вид, разворачиваем календари",
            new Params().withDefaultView("week").withIsCalendarsListExpanded(true)
        );
    }

    @Test
    @Title("Открываем выпадушку месяцев")
    @TestCaseId("427")
    public void shouldOpenMonthsDropdown() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().leftPanel().miniCalendar().currentMonth());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем выпадушку годов")
    @TestCaseId("428")
    public void shouldOpenYearsDropdown() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().leftPanel().miniCalendar().currentYear());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем сайдбар добавления календаря")
    @TestCaseId("68")
    public void shouldOpenAddCalSideBar() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().openTodoBtn())
            .shouldSee(st.pages().cal().home().todo())
            .clicksOn(st.pages().cal().home().leftPanel().addCal())
            .shouldNotSee(st.pages().cal().home().todo());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем сайдбар добавления подписки")
    @TestCaseId("599")
    public void shouldOpenAddSubscriptionSideBar() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .onMouseHoverAndClick(st.pages().cal().home().leftPanel().addSubscription());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Сворачиваем календари")
    @TestCaseId("311")
    public void shouldNotSeeCals() {
        stepsTest.user().apiCalSettingsSteps()
            .updateUserSettings("Разворачиваем календари", new Params().withIsCalendarsListExpanded(true));
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().cal().home().leftPanel().layersList())
                .clicksOn(st.pages().cal().home().leftPanel().expandCalendars())
                .shouldNotSee(st.pages().cal().home().leftPanel().layersList());
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Разворачиваем подписки")
    @TestCaseId("332")
    public void shouldSeeSubscribes() {
        stepsTest.user().apiCalSettingsSteps()
            .updateUserSettings("Сворачиваем подписки", new Params().withIsSubscriptionsListExpanded(false));
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldNotSee(st.pages().cal().home().leftPanel().subscriptionLayersNames())
                .clicksOn(st.pages().cal().home().leftPanel().expandSubscriptions())
                .shouldSee(st.pages().cal().home().leftPanel().subscriptionLayersNames());
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Сворачиваем левую колонку")
    @TestCaseId("996")
    @UseDataProvider("grids")
    public void shouldHideLeftPanel(String grid) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .opensDefaultUrlWithPostFix(grid)
            .clicksOn(st.pages().cal().home().leftPanel().manageLeftPanel())
            .shouldNotSee(st.pages().cal().home().expandedLeftPanel())
            .refreshPage()
            .executesJavaScript(SCRIPT_SCROLL_CAL_TO_10AM)
            .shouldNotSee(st.pages().cal().home().expandedLeftPanel());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Разворачиваем левую колонку")
    @TestCaseId("996")
    @UseDataProvider("grids")
    public void shouldExpandLeftPanel(String grid) {
        stepsProd.user().apiCalSettingsSteps().updateUserSettings(
            "Сворачиваем левую колонку",
            new Params().withIsAsideExpanded(false)
        );

        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .opensDefaultUrlWithPostFix(grid)
            .shouldNotSee(st.pages().cal().home().expandedLeftPanel())
            .clicksOn(st.pages().cal().home().leftPanel().manageLeftPanel())
            .shouldSee(st.pages().cal().home().expandedLeftPanel())
            .refreshPage()
            .executesJavaScript(SCRIPT_SCROLL_CAL_TO_10AM)
            .shouldSee(st.pages().cal().home().expandedLeftPanel());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Переключаем язык в левой колонке")
    @TestCaseId("618")
    @DataProvider({EN_LANG_IN_DROPDOWN, RU_LANG_IN_DROPDOWN})
    public void shouldChangeLang(int num) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(
                st.pages().cal().home().leftPanel().lang(),
                st.pages().cal().home().langDropdownItem().get(num)
            );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Изменение настроек слоя")
    @TestCaseId("646")
    public void shouldSaveSettings() {
        String newTitle = Utils.getRandomString();
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .onMouseHoverAndClick(st.pages().cal().home().leftPanel().calSettings())
            .shouldSee(st.pages().cal().home().editCalSideBar())
            .clicksOn(st.pages().cal().home().editCalSideBar().nameInput())
            .inputsTextInElement(st.pages().cal().home().editCalSideBar().nameInput(), newTitle)
            .clicksOn(st.pages().cal().home().editCalSideBar().colors().get(1))
            .clicksOn(st.pages().cal().home().editCalSideBar().eventVisibleOption())
            .clicksOn(st.pages().cal().home().editCalSideBar().saveBtn())
            .shouldNotSee(st.pages().cal().home().editCalSideBar())
            .onMouseHoverAndClick(st.pages().cal().home().leftPanel().calSettings());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("При наведении курсора на элементы мини-календаря они становятся серыми и кликабельными")
    @TestCaseId("38")
    @UseDataProvider("elements")
    public void shouldChangeColor(String selector) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .onMouseHover(st.pages().cal().home().leftPanel().miniCalendar())
            .onMouseHover(st.getDriver().findElement(By.cssSelector(selector)));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("В мини-календаре праздничные дни выделены красным цветом")
    @TestCaseId("38")
    public void shouldSeeHoliday() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().leftPanel().miniCalendar().currentMonth())
            .clicksOnElementWithText(st.pages().cal().home().leftPanel().miniCalendar().monthOption(), MONTH);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("В мини-календаре выбранная неделя выделена серой полосой, текущий день в красном кружке")
    @TestCaseId("38")
    public void shouldSeeToday() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().cal().home().leftPanel().miniCalendar().currentDay());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Иконка шаренных календарей")
    @TestCaseId("442")
    public void shouldSelectCurrentDate() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .onMouseHoverAndClick(st.pages().cal().home().leftPanel().calSettings())
            .clicksOn(st.pages().cal().home().settings().tabAccess())
            .clicksOn(st.pages().cal().home().settings().inputContact())
            .inputsTextInElement(st.pages().cal().home().settings().inputContact(), DEV_NULL_EMAIL)
            .clicksOn(st.pages().cal().home().suggestItem().waitUntil(not(empty())).get(0))
            .clicksOn(st.pages().cal().home().editCalSideBar().saveBtn())
            .shouldSee(st.pages().cal().home().unlockCalIcon());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
