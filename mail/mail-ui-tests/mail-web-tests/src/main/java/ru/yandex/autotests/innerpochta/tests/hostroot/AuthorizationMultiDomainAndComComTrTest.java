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
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.proxy.LittleHostFilter.hostFilter;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_URL_WITHOUT_DOMAIN;

@Aqua.Test
@Title("Авторизация в разных доменах в одном браузере")
@Features(FeaturesConst.HOSTROOT)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AuthorizationMultiDomainAndComComTrTest extends BaseTest {

    private static final String CREDS_RU = "AuthorizationTestRu";
    private static final String CREDS_COM = "AuthorizationTestCom";
    private static final String CREDS_TR = "AuthorizationTestTr";

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().annotation();
    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Test
    @UseCreds({CREDS_RU, CREDS_COM, CREDS_TR})
    @Title("Авторизация на ru, com, com.tr в одном браузере")
    @TestCaseId("51")
    public void shouldNotBeAuthorizedOnComDomainsWithRuCookies() {
        user.loginSteps().forAcc(lock.acc(CREDS_RU)).logins();
        user.messagesSteps().shouldBeOnMessagePageFor(lock.acc(CREDS_RU), YandexDomain.RU.getDomain());
        user.loginSteps().forAcc(lock.acc(CREDS_COM)).loginsToDomain(YandexDomain.COM);
        user.messagesSteps().shouldBeOnMessagePageFor(lock.acc(CREDS_COM), YandexDomain.COM.getDomain());
        user.loginSteps().forAcc(lock.acc(CREDS_TR)).loginsToDomain(YandexDomain.COMTR);
        user.messagesSteps().shouldBeOnMessagePageFor(lock.acc(CREDS_TR), YandexDomain.COMTR.getDomain());
    }

    @Test
    @Title("Проверяем отсутствие кроссдоменной авторизации для КУБР")
    @TestCaseId("52")
    @UseCreds(CREDS_RU)
    @DataProvider({"kz", "ua", "by"})
    public void shouldNotBeAuthorizedOnNationalDomains(String domain) {
        user.loginSteps().forAcc(lock.acc(CREDS_RU)).logins();
        user.messagesSteps().shouldBeOnMessagePageFor(lock.firstAcc(), YandexDomain.RU.getDomain());
        user.defaultSteps().opensDefaultUrlWithDomain(domain)
            .shouldSee(onHomerPage().logInBtnHeadBanner());
    }
}
