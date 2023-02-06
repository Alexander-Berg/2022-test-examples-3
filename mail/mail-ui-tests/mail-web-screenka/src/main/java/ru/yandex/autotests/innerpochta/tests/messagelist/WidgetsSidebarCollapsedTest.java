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
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_TO_COLLAPSE_WIDGETS;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_TO_UPDATE_WIDGETS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAMP_THEME;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Список писем - свернутый сайдбар виджетов")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class WidgetsSidebarCollapsedTest {

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
    @Title("Верстка свернутого сайдбара виджетов")
    @TestCaseId("6171")
    public void shouldSeeCollapsedWidgetsSideBar() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().executesJavaScript(SCRIPT_TO_COLLAPSE_WIDGETS + SCRIPT_TO_UPDATE_WIDGETS)
                .refreshPage()
                .switchTo(IFRAME_WIDGETS)
                .shouldSee(st.pages().mail().home().widgetsSidebarCollapsed().expandBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка свернутого сайдбара виджетов в темной теме")
    @TestCaseId("6214")
    public void shouldSeeCollapsedWidgetsSideBarInDarkTheme() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().executesJavaScript(SCRIPT_TO_COLLAPSE_WIDGETS + SCRIPT_TO_UPDATE_WIDGETS)
                .refreshPage()
                .switchTo(IFRAME_WIDGETS)
                .shouldSee(st.pages().mail().home().widgetsSidebarCollapsed().expandBtn());

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем темную тему",
            of(COLOR_SCHEME, LAMP_THEME)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на виджет телемоста")
    @TestCaseId("6177")
    public void shouldHoverOnTelemostWidget() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().executesJavaScript(SCRIPT_TO_COLLAPSE_WIDGETS + SCRIPT_TO_UPDATE_WIDGETS)
                .refreshPage()
                .switchTo(IFRAME_WIDGETS)
                .onMouseHover(st.pages().mail().home().widgetsSidebarCollapsed().telemost());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на виджет заметок")
    @TestCaseId("6190")
    public void shouldHoverOnNotesWidget() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().executesJavaScript(SCRIPT_TO_COLLAPSE_WIDGETS + SCRIPT_TO_UPDATE_WIDGETS)
                .refreshPage()
                .switchTo(IFRAME_WIDGETS)
                .onMouseHover(st.pages().mail().home().widgetsSidebarCollapsed().notes());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на создание документа в виджете диска")
    @TestCaseId("6191")
    public void shouldHoverOnDiskXlsxWidget() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().executesJavaScript(SCRIPT_TO_COLLAPSE_WIDGETS + SCRIPT_TO_UPDATE_WIDGETS)
                .refreshPage()
                .switchTo(IFRAME_WIDGETS)
                .onMouseHover(st.pages().mail().home().widgetsSidebarCollapsed().diskBtns().get(0));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на создание таблицы в виджете диска")
    @TestCaseId("6191")
    public void shouldHoverOnDiskPptxWidget() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().executesJavaScript(SCRIPT_TO_COLLAPSE_WIDGETS + SCRIPT_TO_UPDATE_WIDGETS)
                .refreshPage()
                .switchTo(IFRAME_WIDGETS)
                .onMouseHover(st.pages().mail().home().widgetsSidebarCollapsed().diskBtns().get(1));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на создание презентации в виджете диска")
    @TestCaseId("6191")
    public void shouldHoverOnDiskDocWidget() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().executesJavaScript(SCRIPT_TO_COLLAPSE_WIDGETS + SCRIPT_TO_UPDATE_WIDGETS)
                .refreshPage()
                .switchTo(IFRAME_WIDGETS)
                .onMouseHover(st.pages().mail().home().widgetsSidebarCollapsed().diskBtns().get(2));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
