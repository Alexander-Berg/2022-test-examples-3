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
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на попап настроек")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SETTINGS_POPUP)
@RunWith(DataProviderRunner.class)
public class SettingsPopupTest {

    private static final String DARK_THEME_QUERY_PARAM = "?theme=lamp";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare()
        .withProdSteps(stepsProd)
        .withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Верстка попапа настроек в темной теме")
    @TestCaseId("6284")
    public void shouldSeeSettingsPopupInDarkTheme() {
        Consumer<InitStepsRule> actions = st ->
            openSettingsPopup(st);

        parallelRun.withActions(actions).withUrlPath(DARK_THEME_QUERY_PARAM).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка развернутого списка тем в попапе настроек")
    @TestCaseId("6270")
    public void shouldSeeExpandedThemesList() {
        Consumer<InitStepsRule> actions = st -> {
            openSettingsPopup(st);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().mainSettingsPopupNew().themesListButtons().get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Развернуть и свернуть список тем в попапе настроек")
    @TestCaseId("6270")
    public void shouldSeeCollapsedThemesList() {
        Consumer<InitStepsRule> actions = st -> {
            openSettingsPopup(st);
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().mainSettingsPopupNew().themesListButtons().get(0))
                .waitInSeconds(1)
                .clicksOn(st.pages().mail().home().mainSettingsPopupNew().themesListButtons().get(1));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на тему в списке тем в попапе настроек")
    @TestCaseId("6270")
    public void shouldSeeHoverOnTheme() {
        Consumer<InitStepsRule> actions = st -> {
            openSettingsPopup(st);
            st.user().defaultSteps().onMouseHover(st.pages().mail().home().mainSettingsPopupNew().themesList().get(1));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выбрать тему в списке быстрых тем в попапе настроек")
    @TestCaseId("6270")
    public void shouldSetTheme() {
        Consumer<InitStepsRule> actions = st -> {
            openSettingsPopup(st);
            st.user().defaultSteps()
                .onMouseHoverAndClick(st.pages().mail().home().mainSettingsPopupNew().themesList().get(3))
                .clicksOn(st.pages().mail().home().mainSettingsPopupNew().darkThemesList().get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Открываем попап настроек")
    private void openSettingsPopup(InitStepsRule st) {
        st.user().defaultSteps()
            .clicksOn(st.pages().mail().home().mail360HeaderBlock().settings())
            .shouldSee(st.pages().mail().home().mainSettingsPopupNew());
    }
}
