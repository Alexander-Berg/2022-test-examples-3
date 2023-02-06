package ru.yandex.autotests.innerpochta.tests.touch.screentests;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Боковое меню Календаря в корпе")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.CORP)
public class CorpSidebarScreenTest {

    private static final String REPORT_A_BUG = "Сообщить об ошибке";
    private static final String BUG_FORM_IFRAME = "[class*=TouchBugReporter__iframe]";

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain();

    @Test
    @Title("Верстка формы отправки репорта")
    @TestCaseId("1149")
    public void shouldSeeSidebar() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().touchHome().burger())
            .clicksOnElementWithText(
                st.pages().cal().touchHome().sidebar().menuItems().waitUntil(not(empty())),
                REPORT_A_BUG
            )
            .shouldSee(st.pages().cal().touchHome().reportIframe())
            .switchTo(BUG_FORM_IFRAME)
            .shouldNotSee(st.pages().cal().touchHome().loader());

        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).run();
    }
}

