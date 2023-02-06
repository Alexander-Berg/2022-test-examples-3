package ru.yandex.autotests.innerpochta.tests.screentests.MessageList;

import org.junit.Before;
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

import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Прыщ прочитанности/непрочитанности")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class TogglerScreenTest {

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule accLock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 2);
    }

    @Test
    @Title("По тапу на прыщик письмо становится прочитанным/непрочитанным")
    @TestCaseId("21")
    public void shouldToggleLetterReadUnread() {
        Consumer<InitStepsRule> actions = steps -> {
            steps.user().apiMessagesSteps().markAllMsgUnRead()
                .markLetterRead(stepsProd.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0))
                .markLetterUnRead(stepsProd.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(1));
            steps.user().defaultSteps().refreshPage()
                .clicksOn(
                    steps.pages().touch().messageList().messages().get(0).toggler(),
                    steps.pages().touch().messageList().messages().get(1).toggler()
                );
        };
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).runSequentially();
    }
}