package ru.yandex.autotests.innerpochta.tests.hostroot;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetUrlForDomainRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.RetryRule.baseRetry;
import static ru.yandex.autotests.innerpochta.util.MailConst.LITE_URL;
import static ru.yandex.autotests.passport.CommonConstants.YandexDomain.RU;

@Aqua.Test
@Title("Проверка домика на яндексовой морде")
@Features(FeaturesConst.HOSTROOT)
@Stories(FeaturesConst.MORDA)
public class DomikBlockTest extends BaseTest {

    @Parameterized.Parameter
    public boolean isLite;

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SetUrlForDomainRule setUrlForDomainRule = new SetUrlForDomainRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(baseRetry()).around(lock).around(auth);

    @Before
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Тест кнопки «Почта» для интерфейса Лизы")
    @TestCaseId("69")
    public void inboxLinkRedirect() {
        user.defaultSteps().opensMordaUrlWithDomain(RU.getDomain());
        user.loginSteps().clicksOnInboxLinkOnDomik();
        user.defaultSteps().switchOnJustOpenedWindow()
            .shouldBeOnUrl(lock.firstAcc(), QuickFragments.INBOX);
    }

    @Test
    @Title("Тест кнопки «Почта» для интерфейса Лайт")
    @TestCaseId("204")
    @Description("В лайте оторвали редиректы в урле")
    public void inboxLinkLiteRedirect() {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().liteMailLink())
            .opensMordaUrlWithDomain(RU.getDomain());
        user.loginSteps().clicksOnInboxLinkOnDomik();
        user.defaultSteps().switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(LITE_URL));
    }
}
