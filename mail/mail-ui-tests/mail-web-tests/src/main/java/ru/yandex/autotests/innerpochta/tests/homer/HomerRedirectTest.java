package ru.yandex.autotests.innerpochta.tests.homer;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.containsString;
import static org.junit.rules.RuleChain.outerRule;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASSPORT_URL;
import static ru.yandex.autotests.passport.api.common.data.YandexDomain.COM;
import static ru.yandex.autotests.passport.api.common.data.YandexDomain.COMTR;
import static ru.yandex.autotests.passport.api.common.data.YandexDomain.RU;
import static ru.yandex.autotests.passport.api.core.cookie.YandexCookies.L;

/**
 * @author vasily-k
 */

@Aqua.Test
@Title("Редиректы из Гомера")
@Features(FeaturesConst.HOMER)
@Stories(FeaturesConst.REDIRECTS)
@RunWith(Parameterized.class)
public class HomerRedirectTest extends BaseTest {

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);
    private AccLockRule lock = AccLockRule.use().className();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain ruleChain = outerRule(lock);

    @Parameterized.Parameter
    public YandexDomain domain;

    @Parameterized.Parameters(name = "domain: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
            {RU},
            {COM},
            {COMTR}
        });
    }

    @Before
    public void setUp() {
        webDriverRule.getDriver().manage().deleteAllCookies();
    }

    @Test
    @Title("Нет авторизации, есть L-кука, редирект на паспорт")
    @TestCaseId("131")
    public void shouldGoToPassport() {
        user.loginSteps().forAcc(lock.firstAcc()).getsCookie(L, domain);
        user.defaultSteps().opensDefaultUrlWithDomain(domain.getDomain())
            .shouldBeOnUrl(containsString(PASSPORT_URL + domain.getDomain()));
    }

    @Test
    @Title("Нет авторизации, нет L-куки, остаемся на Гомере")
    @TestCaseId("176")
    public void shouldGoToHomer() {
        String hostname = webDriverRule.getBaseUrl().split("\\.(?=[^.]+$)")[0] + ".%s/";
        user.defaultSteps().opensDefaultUrlWithDomain(domain.getDomain())
            .shouldBeOnUrl(String.format(hostname, domain.getDomain()));
    }

    @Test
    @Title("Есть авторизация, есть L-кука, редирект в inbox")
    @TestCaseId("134")
    public void shouldGoToInbox() {
        user.loginSteps().forAcc(lock.firstAcc()).loginsToDomain(domain);
        user.defaultSteps().opensDefaultUrlWithDomain(domain.getDomain())
            .shouldBeOnUrlWith(INBOX);
    }
}
