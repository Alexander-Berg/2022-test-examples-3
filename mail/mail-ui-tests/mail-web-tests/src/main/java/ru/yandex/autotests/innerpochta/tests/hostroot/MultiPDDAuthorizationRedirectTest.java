package ru.yandex.autotests.innerpochta.tests.hostroot;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.ProxyServerRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetUrlForDomainRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX;
import static ru.yandex.autotests.innerpochta.proxy.LittleHostFilter.hostFilter;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_URL_WITHOUT_DOMAIN;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDD_URL;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDD_URL_FOR;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Редиректы ПДД при наличии авторизованного пользователя")
@Features(FeaturesConst.HOSTROOT)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class MultiPDDAuthorizationRedirectTest extends BaseTest {

    private static final String CREDS_PDD = "AuthorizationTestPdd";
    private static final String RU_CREDS = "AuthorizationTestRu";
    private static final String PDD_EMAIL_DOMAIN = "kida-lo-vo.name";
    private static final String PDD_PATH = "pdd_domain=kida-lo-vo.name";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().names(CREDS_PDD, RU_CREDS);

    @Test
    @Title("Авторизация ПДД юзером по урлу с ПДД доменом")
    @TestCaseId("206")
    @DataProvider({PDD_URL, PDD_URL_FOR})
    public void shouldAuthPDDUser(String pddUrl) {
        user.defaultSteps().opensUrl(pddUrl + PDD_EMAIL_DOMAIN)
            .shouldBeOnUrl(containsString(PDD_PATH));
        user.loginSteps().logInFromPassport(
            lock.acc(CREDS_PDD).getLogin().split("@")[0],
            lock.acc(CREDS_PDD).getPassword()
        );
    }

    @Test
    @Title("При переходе по ссылке с get параметром ПДД, дефолтным становится ПДД юзер")
    @TestCaseId("207")
    @DataProvider({PDD_URL, PDD_URL_FOR})
    public void shouldChangeUserToPDD(String pddUrl) {
        user.loginSteps().forAcc(lock.acc(CREDS_PDD)).logins()
            .multiLoginWith(lock.acc(RU_CREDS));
        user.defaultSteps()
            .shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), lock.acc(RU_CREDS).getLogin())
            .opensUrl(pddUrl + PDD_EMAIL_DOMAIN)
            .shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), lock.acc(CREDS_PDD).getLogin())
            .clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .shouldSee(onHomePage().userMenuDropdown())
            .clicksOnElementWithText(onHomePage().userMenuDropdown().userList(), lock.acc(RU_CREDS).getLogin())
            .shouldBeOnUrlWith(INBOX)
            .shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), lock.acc(RU_CREDS).getLogin());
    }

    @Test
    @Title("Переход по ссылке с get параметром ПДД авторизованным обычным юзером")
    @TestCaseId("208")
    @DataProvider({PDD_URL, PDD_URL_FOR})
    public void shouldOpenPassportForPDD(String pddUrl) {
        user.loginSteps().forAcc(lock.acc(RU_CREDS)).logins();
        user.defaultSteps()
            .opensUrl(pddUrl + PDD_EMAIL_DOMAIN)
            .shouldBeOnUrl(containsString(PDD_PATH));
        user.loginSteps().logInFromPassport(
            lock.acc(CREDS_PDD).getLogin().split("@")[0],
            lock.acc(CREDS_PDD).getPassword()
        );
        user.defaultSteps().shouldContainText(
            onHomePage().mail360HeaderBlock().userMenu(),
            lock.acc(CREDS_PDD).getLogin()
        );
    }
}
