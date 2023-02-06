package ru.yandex.autotests.innerpochta.tests.hostroot;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.ProxyServerRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetUrlForDomainRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.proxy.LittleHostFilter.hostFilter;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_URL_WITHOUT_DOMAIN;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASSPORT_URL;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Авторизация в разных доменах в одном браузере")
@Features(FeaturesConst.HOSTROOT)
@Stories(FeaturesConst.GENERAL)
public class AuthorizationFromPassport extends BaseTest {

    private static final String RU_CREDS = "AuthorizationTestRu";

    @ClassRule
    public static SetUrlForDomainRule setUrlForDomainRule = new SetUrlForDomainRule();

    @ClassRule
    public static ProxyServerRule serverProxyRule = proxyServerRule(
        hostFilter(containsString(MAIL_URL_WITHOUT_DOMAIN))
    );

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().annotation();
    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Override
    public DesiredCapabilities setCapabilities() {
        return serverProxyRule.getCapabilities();
    }

    @Before
    public void setUp() {
        webDriverRule.getDriver().manage().deleteAllCookies();
    }

    @Test
    @UseCreds(RU_CREDS)
    @Title("Авторизуемся через паспорт")
    @TestCaseId("215")
    public void loginFromPassport() {
        user.defaultSteps().opensMordaUrlWithDomain("ru")
            .clicksOn(onHomePage().logInMordaBtn())
            .shouldBeOnUrl(containsString(PASSPORT_URL));
        user.loginSteps().logInFromPassport(lock.firstAcc().getLogin(), lock.firstAcc().getPassword());
    }
}
