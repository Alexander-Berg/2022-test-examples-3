package ru.yandex.autotests.innerpochta.tests.autotests.Corp;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
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

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Разнообразные залогины")
@Features(FeaturesConst.AUTH)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class LoginCorpTest {

    private static final String CORP_ACCOUNT = "LoginYateamTest";
    private static final String CORP_TOUCH_URL = "https://mail.yandex-team.ru/touch";
    private static final String PASSPORT_URL_LOGIN_CORP = "passport.yandex-team.ru/auth?";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lockRule = AccLockRule.use().names(CORP_ACCOUNT);

    private TouchRulesManager rules = touchRulesManager().withLock(null);
    private InitStepsRule steps = rules.getSteps();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Проверяем переход в паспорт при открытии корпа")
    @TestCaseId("3")
    public void shouldLoginOnCorp() {
        steps.user().defaultSteps().opensUrl(CORP_TOUCH_URL)
            .shouldBeOnUrl(containsString(PASSPORT_URL_LOGIN_CORP))
            .inputsTextInElement(steps.pages().touch().passport().inputLogin(), lockRule.acc(CORP_ACCOUNT).getLogin())
            .inputsTextInElement(steps.pages().touch().passport().inputPass(), lockRule.acc(CORP_ACCOUNT).getPassword())
            .clicksOn(steps.pages().touch().passport().logInBtnCorp())
            .shouldBeOnUrl(containsString(CORP_TOUCH_URL))
            .shouldSee(steps.pages().touch().messageList().headerBlock());
    }
}
