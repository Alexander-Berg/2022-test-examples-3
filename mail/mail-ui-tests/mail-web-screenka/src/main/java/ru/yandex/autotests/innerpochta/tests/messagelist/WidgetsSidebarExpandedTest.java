package ru.yandex.autotests.innerpochta.tests.messagelist;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.WidgetsSidebarCollapsed.IFRAME_WIDGETS;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_TO_EXPAND_WIDGETS;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_TO_UPDATE_WIDGETS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAMP_THEME;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Список писем - развернутый сайдбар виджетов")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class WidgetsSidebarExpandedTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Верстка развернутого сайдбара виджетов")
    @TestCaseId("6172")
    public void shouldSeeExpandedWidgetsSideBar() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().executesJavaScript(SCRIPT_TO_EXPAND_WIDGETS + SCRIPT_TO_UPDATE_WIDGETS)
                .refreshPage()
                .switchTo(IFRAME_WIDGETS)
                .clicksOn(st.pages().mail().home().widgetsSidebarCollapsed().expandBtn())
                .shouldSee(st.pages().mail().home().widgetsSidebarExpanded());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка развернутого сайдбара виджетов в темной теме")
    @TestCaseId("6215")
    public void shouldSeeExpandedWidgetsSideBarInDarkTheme() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().executesJavaScript(SCRIPT_TO_EXPAND_WIDGETS)
                .refreshPage()
                .switchTo(IFRAME_WIDGETS)
                .clicksOn(st.pages().mail().home().widgetsSidebarCollapsed().expandBtn())
                .shouldSee(st.pages().mail().home().widgetsSidebarExpanded());

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем темную тему",
            of(COLOR_SCHEME, LAMP_THEME)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
