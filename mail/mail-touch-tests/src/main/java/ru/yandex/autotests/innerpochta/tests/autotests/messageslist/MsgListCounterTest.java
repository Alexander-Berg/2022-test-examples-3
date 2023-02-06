package ru.yandex.autotests.innerpochta.tests.autotests.messageslist;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;


/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Счетчик в шапке")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.HEAD)
public class MsgListCounterTest {

    private static final int MESSAGE_NUMBER = 3;

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), MESSAGE_NUMBER);
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("После удаления одного письма счетчик непрочитанных уменьшается на 1")
    @TestCaseId("347")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUpdateCounter() {
        steps.user().defaultSteps().shouldSeeThatElementTextEquals(
            steps.pages().touch().messageList().headerBlock().unreadCounter(),
            String.valueOf(MESSAGE_NUMBER)
        )
            .clicksOn(
                steps.pages().touch().messageList().messages().get(1).avatar(),
                steps.pages().touch().messageList().groupOperationsToolbarPhone().delete()
            )
            .shouldSeeThatElementTextEquals(
                steps.pages().touch().messageList().headerBlock().unreadCounter(),
                String.valueOf(MESSAGE_NUMBER - 1)
            );
    }

    @Test
    @Title("После удаления одного письма счетчик непрочитанных уменьшается на 1")
    @TestCaseId("347")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldUpdateCounterTablet() {
        steps.user().defaultSteps().shouldSeeThatElementTextEquals(
            steps.pages().touch().messageList().headerBlock().unreadCounter(),
            String.valueOf(MESSAGE_NUMBER - 1)
        )
            .clicksOn(
                steps.pages().touch().messageList().messages().get(1).avatar(),
                steps.pages().touch().messageView().groupOperationsToolbarTablet().delete()
            )
            .shouldSeeThatElementTextEquals(
                steps.pages().touch().messageList().headerBlock().unreadCounter(),
                String.valueOf(MESSAGE_NUMBER - 2)
            );
    }
}
