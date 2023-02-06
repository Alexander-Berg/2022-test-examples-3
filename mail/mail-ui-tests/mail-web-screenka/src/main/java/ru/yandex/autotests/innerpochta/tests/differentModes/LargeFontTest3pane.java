package ru.yandex.autotests.innerpochta.tests.differentModes;


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
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.ATTACHMENTS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.DRAFT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SENT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SPAM;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.TRASH;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.UNREAD;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Крупный шрифт в 3pane")
@Features(FeaturesConst.LARGE_FONT)
@Tag(FeaturesConst.LARGE_FONT)
@Stories(FeaturesConst.GENERAL)
@Description("У юзера настроен сборщик")
@RunWith(DataProviderRunner.class)
public class LargeFontTest3pane {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @DataProvider
    public static Object[][] testData1() {
        return new Object[][]{
            {UNREAD},
            {ATTACHMENTS},
            {SENT},
            {TRASH},
            {SPAM},
            {DRAFT}
        };
    }

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams("Включаем 3 пейн", of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL));
    }

    @Test
    @Title("[3пейн] Крупный шрифт: базовые урлы")
    @TestCaseId("3493")
    @UseDataProvider("testData1")
    public void shouldSeeCorrectLargeFontPage1(QuickFragments urlPath) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().waitInSeconds(5);

        parallelRun.withActions(actions).withUrlPath(urlPath).withAcc(lock.firstAcc()).run();
    }

}
