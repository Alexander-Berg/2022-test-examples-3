package ru.yandex.autotests.innerpochta.tests.autotests.messageView;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на смарт реплаи в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.QR)
public class SmartRepliesInNewsLetterTest {

    private static final String NEWS_MID = "171699735793500266";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().className());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Умные ответы не должны показываться в рассылочных письмах")
    @TestCaseId("1291")
    public void shouldNotSeeSRInNews() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(NEWS_MID))
            .shouldSee(steps.pages().touch().messageView().toolbar())
            .shouldNotSee(steps.pages().touch().messageView().quickReply().smartReply());
    }
}
