package ru.yandex.autotests.innerpochta.tests.hostroot;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
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
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.ns.pages.commonblocks.UserMenuBlock.EXIT;
import static ru.yandex.autotests.innerpochta.proxy.LittleHostFilter.hostFilter;
import static ru.yandex.autotests.innerpochta.rules.ProxyServerRule.proxyServerRule;
import static ru.yandex.autotests.innerpochta.rules.RetryRule.baseRetry;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_URL_WITHOUT_DOMAIN;

@Aqua.Test
@Title("Разлогин в разных доменах")
@Features(FeaturesConst.HOSTROOT)
@Stories(FeaturesConst.GENERAL)
@RunWith(Parameterized.class)
@Issue("DARIA-61703")
//Пока оторван "Выход со всех устройств" будем постоянно кликать в просто "Выход"
public class MultiDomainCreateAndAuthorizationTest extends BaseTest {

    private static final String CREDS_PDD = "AuthorizationTestPdd";
    private static final String CREDS_PDD_RF = "AuthorizationTestPddRFAdminkapddrf";
    private static final String RU_CREDS = "AuthorizationTestRu";
    private static final String KIDALOVO_PDD_PATH = "/for/kida-lo-vo.name/";
    private static final String ADMINKAPDD_PDD_PATH = "/for/%D0%B0%D0%B4%D0%BC%D0%B8%D0%BD%D0%BA%D0%B0%D0%BF%D0%B" +
        "4%D0%B4.%D1%80%D1%84/";
    private static final String EXIT_COMTR = "Yandex servislerinden çık";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);
    private AccLockRule lock = AccLockRule.use().annotation();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(baseRetry()).around(lock);

    @Parameterized.Parameter
    public YandexDomain domain;

    @Parameterized.Parameter(1)
    public boolean thisOnly;

    @Parameterized.Parameter(2)
    public String exitLink;

    @Parameterized.Parameters(name = "domain [ {0} ]")
    public static Collection<Object[]> testData() {
        Object[][] data = new Object[][]{
            {YandexDomain.RU, true, EXIT},
            {YandexDomain.COMTR, true, EXIT_COMTR},
/*            {YandexDomain.RU, false},
            {YandexDomain.COMTR, false}*/
        };
        return Arrays.asList(data);
    }

    @Test
    @UseCreds({CREDS_PDD})
    @Title("Логинимся с обычного хострута ПДД-юзером с доменом на латинице")
    @TestCaseId("217")
    public void logoutFromMailWithPdd() {
        user.loginSteps().forAcc(lock.firstAcc()).loginsToDomain(domain);
        user.defaultSteps().shouldBeOnUrlWith(QuickFragments.INBOX)
            .clicksOn(onHomePage().mail360HeaderBlock().userMenu());
        exitAndCheckUnauthorised(thisOnly, KIDALOVO_PDD_PATH);
    }

    @Test
    @UseCreds({CREDS_PDD_RF})
    @Title("Логинимся с обычного хострута ПДД-юзером с кириллическим доменом")
    @TestCaseId("218")
    public void logoutFromMailWithPddRf() {
        user.loginSteps().forAcc(lock.firstAcc()).loginsToDomain(domain);
        user.defaultSteps().clicksOn(onHomePage().mail360HeaderBlock().userMenu());
        exitAndCheckUnauthorised(thisOnly, ADMINKAPDD_PDD_PATH);
    }

    @Test
    @UseCreds({RU_CREDS})
    @Title("Логинимся с обычного хострута юзером БП")
    @TestCaseId("219")
    public void logoutFromMail() {
        user.loginSteps().forAcc(lock.firstAcc()).loginsToDomain(domain);
        user.defaultSteps().clicksOn(onHomePage().mail360HeaderBlock().userMenu());
        exitOrExitAll(thisOnly, exitLink);
        shouldNotBeAuthorizedAtUsualMail();
    }

    @Step("Залогиниваемся с хострута юзером")
    private void loginToMail(String domainURLPath) {
        user.defaultSteps().opensDefaultUrlWithDomain(domain.getDomain() + domainURLPath);
        user.loginSteps().forAcc(lock.firstAcc());
        user.defaultSteps().clicksOn(onHomePage().mail360HeaderBlock().userMenu());
    }

    @Step("Вылогиниваемся из почты и проверяем, что при переходе по урлам почты мы не авторизованы")
    private void exitAndCheckUnauthorised(boolean exitType, String domainUrlPath) {
        exitOrExitAll(exitType, exitLink);
        shouldNotBeAuthorizedAtUsualMail();
        user.defaultSteps()
            .opensDefaultUrlWithDomain(domain.getDomain() + domainUrlPath)
            .shouldSee(onPassportPage().login());
    }

    @Step("Проверяем, что средиректило на морду, переходим на непддшный почтовый урл, смотрим, что неавторизованы")
    private void shouldNotBeAuthorizedAtUsualMail() {
        user.defaultSteps().shouldBeOnUrl(containsString(format("yandex.%s", domain.getDomain())))
            .opensDefaultUrlWithDomain(domain.getDomain())
            .shouldSee(onPassportPage().login());
    }

    private void exitOrExitAll(boolean exit, String exitLink) {
        if (exit) {
            user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().userMenuDropdown().userLogOutLink());
        } else {
            user.defaultSteps().clicksOn(onHomePage().userMenuDropdown().exitAll())
                .shouldSee(onHomePage().exitAllPopup())
                .clicksOn(onHomePage().exitAllPopup().confirmLink());
        }
    }
}
