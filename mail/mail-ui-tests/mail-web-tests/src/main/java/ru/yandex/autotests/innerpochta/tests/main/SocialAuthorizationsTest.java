package ru.yandex.autotests.innerpochta.tests.main;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.ProxyServerRule;
import ru.yandex.autotests.innerpochta.rules.SetUrlForDomainRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.proxy.LittleHostFilter.hostFilter;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_URL_WITHOUT_DOMAIN;

@Aqua.Test
@Title("Проверяем авторизацию через социальные сети")
@Features(FeaturesConst.AUTH)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class SocialAuthorizationsTest extends BaseTest {

    private static final String SOCIAL_LOGIN = "chakx15";
    private static final String SOCIAL_AUTHORIZATION_URL = "https://mail.yandex.com.tr/share/u2709?lang=en";
    private static final String PASSPORT_AUTH_URL = "https://passport.yandex.com.tr/auth?from=mail" +
        "&origin=hostroot_homer_auth_tr";
    private static final String TWITTER_USER_LOGIN = "chakx4@rambler.ru";
    private static final String TWITTER_USER_PWD = "chakp1";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SetUrlForDomainRule setUrlForDomainRule = new SetUrlForDomainRule();

    @ClassRule
    public static ProxyServerRule serverProxyRule = proxyServerRule(
        hostFilter(containsString(MAIL_URL_WITHOUT_DOMAIN)));
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
    @Title("Проверяем соцавторизацию")
    @TestCaseId("1010")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68733")
    public void shouldLoginFromHostrootWithTwitter() {
        user.defaultSteps().opensUrl(SOCIAL_AUTHORIZATION_URL)
            .clicksOn(onHomerPage().logInBtnHeadBanner())
            .shouldBeOnUrl(containsString(PASSPORT_AUTH_URL))
            .clicksOn(onPassportPage().twitterLogin())
            .switchOnJustOpenedWindow()
            .inputsTextInElement(onSocialAuthorizationPages().twitterLogin(), TWITTER_USER_LOGIN)
            .inputsTextInElement(onSocialAuthorizationPages().twitterPwd(), TWITTER_USER_PWD)
            .clicksOn(onSocialAuthorizationPages().twitterLogInButton())
            .clicksOn(onPassportPage().socialLogedInLogins().get(0))
            .switchOnWindow(0)
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .shouldContainText(onMessagePage().mail360HeaderBlock().userMenuDropdown().currentUser(), SOCIAL_LOGIN);
    }
}
