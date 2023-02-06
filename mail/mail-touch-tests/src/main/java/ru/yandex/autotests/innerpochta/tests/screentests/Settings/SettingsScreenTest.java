package ru.yandex.autotests.innerpochta.tests.screentests.Settings;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на проверку верстки страниц настроек")
@Features(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class SettingsScreenTest {

    private static final String SETTINGS = "Настройки";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @DataProvider
    public static Object[][] settings() {
        return new Object[][]{
            {"Основные", "touch/settings/general"},
            {"Справка и поддержка", "touch/settings/support"},
            {"Папки и метки", "touch/settings/folders"}
        };
    }

    @Test
    @Title("Должны увидеть главную страницу настроек")
    @TestCaseId("1261")
    public void shouldSeeSettingsMainMenu() {
        Consumer<InitStepsRule> act = this::openSettings;

        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть разделы настроек")
    @TestCaseId("1263")
    @UseDataProvider("settings")
    public void shouldSeeSettingsPages(String text, String urlPart) {
        Consumer<InitStepsRule> act = st -> {
            openSettings(st);
            st.user().defaultSteps().clicksOnElementWithText(st.pages().touch().settings().settingsItem(), text)
                .shouldBeOnUrl(containsString(urlPart));
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Step("Открываем настройки из левой колонки")
    private void openSettings(InitStepsRule st) {
        st.user().defaultSteps()
            .shouldNotSee(st.pages().touch().messageList().bootPage())
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .scrollTo(st.pages().touch().sidebar().leftPanelItems().waitUntil(not(empty())).get(0))
            .clicksOnElementWithText(st.pages().touch().sidebar().leftPanelItems(), SETTINGS)
            .shouldBeOnUrlWith(SETTINGS_TOUCH);
    }
}
