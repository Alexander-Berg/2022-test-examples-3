package ru.yandex.autotests.innerpochta.tests.differentModes;

import com.tngtech.java.junit.dataprovider.DataProvider;
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
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;

/**
 * @author mariya-murm
 */

@Aqua.Test
@Title("Тест на страницу ошибок")
@Features(FeaturesConst.GENERAL)
@Tag(FeaturesConst.GENERAL)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class FatalPageTest {

    private ScreenRulesManager rules = screenRulesManager();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();

    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    private static final String FATAL_URL = "/u2709/fatal";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Переходим на страницу fatal")
    @TestCaseId("5321")
    @DataProvider({"colorful", "lamp", "sea"})
    public void shouldSeeFatalPage(String theme) {
        setTheme(theme);
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().opensDefaultUrlWithPostFix(FATAL_URL);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    public void setTheme(String theme) {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем тему",
            of(COLOR_SCHEME, theme)
        );
    }

}
