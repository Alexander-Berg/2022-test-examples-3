package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
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
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Новая шапка почты")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class NewHeaderTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount(2);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Открываем выпадушку залогина")
    @TestCaseId("5762")
    public void shouldOpenUserMenuDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().userMenu())
                .shouldSee(st.pages().mail().home().mail360HeaderBlock().userMenuDropdown());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Шапка при узком окне браузера")
    @TestCaseId("5781")
    public void shouldSeeCompactHeader() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().setsWindowSize(1100, 1000)
                .shouldSee(st.pages().mail().home().mail360HeaderBlock().moreServices());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на сервисы в шапке")
    @TestCaseId("5763")
    public void shouldSeeHoverOnServices() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().home().mail360HeaderBlock().moreServices());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на настройки в шапке")
    @TestCaseId("5763")
    public void shouldSeeHoverOnSettings() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().home().mail360HeaderBlock().settings());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Поиск в компактном меню")
    @TestCaseId("5780")
    public void shouldSeeCompactSearch() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().searchBtnCompactMode())
                .shouldSee(st.pages().mail().search().searchSuggest());

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем компактную шапку",
            of(LIZA_MINIFIED_HEADER, STATUS_ON)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Аккаунты подсвечиваются по ховеру")
    @TestCaseId("5785")
    public void shouldSeeHoverOnAccs() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().loginSteps().multiLoginWith(lock.accNum(1));
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().userMenu())
                .onMouseHover(st.pages().mail().home().mail360HeaderBlock().userMenuDropdown().accs().get(1));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Шапка не ломается при сужении рабочей области")
    @TestCaseId("4231")
    public void shouldSeeHeader() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().setsWindowSize(1000, 1000)
                .refreshPage()
                .shouldSee(st.pages().mail().home().mail360HeaderBlock().moreServices());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
