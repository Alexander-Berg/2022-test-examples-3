package ru.yandex.autotests.innerpochta.tests.homer;

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
 * @author puffyfloof
 */

@Aqua.Test
@Title("Разнообразные залогины")
@Features(FeaturesConst.HOMER)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class LoginTest {

    private static final String MAIL_ACCOUNT = "LoginTest";
    private static final String CORP_ACCOUNT = "LoginYateamTest";
    private static final String PDD_ACCOUNT = "LoginPddTest";
    private static final String CORP_TOUCH_URL = "https://mail.yandex-team.ru/touch";
    private static final String PASSPORT_URL_LOGIN = "passport.yandex-team.ru/auth?";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    private TouchRulesManager rules = touchRulesManager()
        .withLock(AccLockRule.use().names(MAIL_ACCOUNT, CORP_ACCOUNT, PDD_ACCOUNT));
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Проверяем переход в паспорт при открытии корпа")
    @TestCaseId("216")
    public void shouldLoginFromHostroot() {
        steps.user().defaultSteps().opensUrl(CORP_TOUCH_URL)
            .shouldBeOnUrl(containsString(PASSPORT_URL_LOGIN));
    }
}
