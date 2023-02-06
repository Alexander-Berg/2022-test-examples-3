package ru.yandex.autotests.innerpochta.tests.homer;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.rules.RetryRule.baseRetry;
import static ru.yandex.autotests.passport.api.common.data.YandexDomain.COM;

/**
 * @author vasily-k
 */

@Aqua.Test
@Title("Отправка лайт-юзера на дорегисрацию")
@Features(FeaturesConst.HOMER)
@Stories(FeaturesConst.GENERAL)
public class LiteUserTest extends BaseTest {

    private final static String PROFILE_UPGRADE_URL = "https://passport.yandex.com/profile/upgrade";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    private AccLockRule lock = AccLockRule.use().className();

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain ruleChain = outerRule(baseRetry())
        .around(lock);

    @Test
    @Title("Должны попасть на страницу с дорегистрацией")
    @TestCaseId("209")
    public void shouldRedirectToUpgradeProfilePage() {
        user.loginSteps().forAcc(lock.firstAcc()).tryLoginToDomain(COM);
        user.defaultSteps().shouldBeOnUrl(startsWith(PROFILE_UPGRADE_URL));
    }
}
