package ru.yandex.autotests.innerpochta.tests.main;

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
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.WidgetsSidebarCollapsed.IFRAME_WIDGETS;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_TO_COLLAPSE_WIDGETS;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_TO_UPDATE_WIDGETS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_TODO;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Сайдбар виджетов - общее")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class WidgetsSidebarMainTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @DataProvider
    public static Object[][] layouts() {
        return new Object[][]{
            {SETTINGS_LAYOUT_3PANE_HORIZONTAL},
            {LAYOUT_3PANE_VERTICAL}
        };
    }

    @Before
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().executesJavaScript(SCRIPT_TO_COLLAPSE_WIDGETS + SCRIPT_TO_UPDATE_WIDGETS)
            .refreshPage()
            .switchTo(IFRAME_WIDGETS);
    }

    @Test
    @Title("Не видим сайдбар виджетов в компактном режиме")
    @TestCaseId("6175")
    public void shouldNotSeeWidgetsSidebar() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактную шапку",
            of(LIZA_MINIFIED_HEADER, STATUS_ON)
        );
        user.defaultSteps().refreshPage()
            .shouldNotSee(
                onMessagePage().widgetsSidebarCollapsed(),
                onMessagePage().widgetsSidebarExpanded()
            );
    }

    @Test
    @Title("Запоминаем состояние сайдбара")
    @TestCaseId("6176")
    public void shouldSaveSidebarState() {
        user.defaultSteps().shouldSee(onMessagePage().widgetsSidebarCollapsed())
            .refreshPage()
            .switchTo(IFRAME_WIDGETS)
            .clicksOn(onMessagePage().widgetsSidebarCollapsed().expandBtn())
            .shouldSee(onMessagePage().widgetsSidebarExpanded().collapseBtn())
            .waitInSeconds(3)
            .refreshPage()
            .switchTo(IFRAME_WIDGETS)
            .shouldSee(onMessagePage().widgetsSidebarExpanded().collapseBtn());
    }

    @Test
    @Title("Не видим сайдбар виджета в настройках")
    @TestCaseId("6198")
    public void shouldNotSeeSidebarInSettings() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS)
            .shouldNotSee(
                onMessagePage().widgetsSidebarCollapsed(),
                onMessagePage().widgetsSidebarExpanded()
            );
    }

    @Test
    @Title("Не видим сайдбар виджетов в 3-Pane")
    @TestCaseId("6199")
    @UseDataProvider("layouts")
    public void shouldNotSeeWidgetsSidebarIn3PaneMode(String layout) {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3-Pane",
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        user.defaultSteps().refreshPage()
            .shouldNotSee(
                onMessagePage().widgetsSidebarCollapsed(),
                onMessagePage().widgetsSidebarExpanded()
            );
    }

    @Test
    @Title("Не видим сайдбар виджетов с TODO")
    @TestCaseId("6216")
    public void shouldNotSeeWidgetsSidebarWithTodo() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем TODO",
            of(SHOW_TODO, TRUE)
        );
        user.defaultSteps().refreshPage()
            .shouldNotSee(
                onMessagePage().widgetsSidebarCollapsed(),
                onMessagePage().widgetsSidebarExpanded()
            );
    }
}
