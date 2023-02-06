package ru.yandex.autotests.innerpochta.tests.screentests.IframeCompose;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
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

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на саджест в композе у WS юзера")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.SUGGEST_COMPOSE)
public class ComposeSuggestForWSUserScreenTest {

    private static final String USER_WITH_WORKING_POSITION = "autotest@ava1-test.yaconnect.com";
    private static final String INPUT_FOR_SUGGEST = "иван";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().className());
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

    @Test
    @Title("Проверяем корректность адреса контакта с должностью, выбранного из саджеста у коннектовского юзера")
    @TestCaseId("779")
    public void shouldSeeUserWithJob() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().touch().composeIframe().inputTo(), INPUT_FOR_SUGGEST)
                .clicksOn(st.pages().touch().composeIframe().composeSuggestItems().waitUntil(not(empty())).get(0))
                .clicksOn(st.pages().touch().composeIframe().yabble())
                .shouldSeeThatElementHasText(
                    st.pages().touch().composeIframe().editableYabble(),
                    USER_WITH_WORKING_POSITION
                );
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }
}
