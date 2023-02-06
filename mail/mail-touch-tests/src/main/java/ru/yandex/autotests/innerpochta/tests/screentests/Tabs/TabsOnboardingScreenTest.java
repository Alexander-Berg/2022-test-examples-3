package ru.yandex.autotests.innerpochta.tests.screentests.Tabs;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Cookie;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
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
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TOUCH_ONBOARDING;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на онбординг табов")
@Features({FeaturesConst.TABS})
@Stories(FeaturesConst.PROMO)
@RunWith(DataProviderRunner.class)
public class TabsOnboardingScreenTest {

    private static final String COOKIE_NAME = "debug-settings-delete";
    private static final String COOKIE_VALUE = "show_folders_tabs,touch_onboarding_timestamp,qu_last-time-promo";

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

    @Before
    public void prepare() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Сбрасываем показ онбординга табов",
            of(TOUCH_ONBOARDING, FALSE)
        );
    }

    @Test
    @Title("Должны увидеть все страницы онбординга")
    @TestCaseId("1130")
    @DataProvider({"0", "1", "2"})
    public void shouldSeeAllOnboardingScreens(int num) {
        Consumer<InitStepsRule> act = steps -> {
            Cookie c = new Cookie(COOKIE_NAME, COOKIE_VALUE, ".yandex.ru", "/", null);
            steps.getDriver().manage().addCookie(c);
            steps.user().defaultSteps().refreshPage()
                .shouldSee(steps.pages().touch().messageList().tabsOnboarding());
            for (int i = 0; i < num; i++)
                steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().tabsOnboarding().yesBtn());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).run();
    }
}