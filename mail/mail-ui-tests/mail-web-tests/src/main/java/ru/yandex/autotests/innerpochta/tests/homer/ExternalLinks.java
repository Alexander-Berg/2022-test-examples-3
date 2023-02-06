package ru.yandex.autotests.innerpochta.tests.homer;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Проверяем внешние и портальные ссылки")
@Features(FeaturesConst.HOMER)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class ExternalLinks extends BaseTest {

    private static final String YA_360_LINK = "https://360.yandex.%s/?from=mail-landing-header-360";
    private static final String HELP_LINK_POSTFIX = "/support/mail/?from=mail";
    private static final String LEGAL_LINK = "https://yandex.ru/legal/mail_termsofuse/";
    private static final String TROUBLESHOOTING_LINK = "https://yandex.ru/support/mail/troubleshooting.html";
    private static final String TUNING_LINK = "https://360.yandex.ru/premium-plans";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().annotation();

    @DataProvider
    public static Object[][] domainAndMordaLink() {
        return new Object[][]{
            {"ru"},
            {"com"},
            {"com.tr", "com"}
        };
    }

    @Test
    @Title("Переходим на морду по клику на лого Яндекс")
    @TestCaseId("144")
    @UseDataProvider("domainAndMordaLink")
    public void shouldOpenMorda(String domain) {
        user.defaultSteps().opensDefaultUrlWithDomain(domain)
            .clicksOn(onHomerPage().logoYandex())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(equalTo(format(YA_360_LINK, domain)));
    }

    @Test
    @Title("Переходим по ссылке Справка")
    @TestCaseId("187")
    @DataProvider({"ru", "com", "com.tr"})
    public void shouldOpenHelp(String domain) {
        user.defaultSteps().opensDefaultUrlWithDomain(domain)
            .clicksOn(onHomerPage().footerBlock().helpLink())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(domain))
            .shouldBeOnUrl(containsString(HELP_LINK_POSTFIX));
    }

    @Test
    @Title("Переходим по ссылке на Условия использования")
    @TestCaseId("190")
    public void shouldOpenTerms() {
        user.defaultSteps().opensDefaultUrl()
            .clicksOn(onHomerPage().footerBlock().legalLink())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(LEGAL_LINK);
    }

    @Test
    @Title("Переходим по ссылке на Круглосуточную поддержку")
    @TestCaseId("190")
    public void shouldOpenTroubleshooting() {
        user.defaultSteps().opensDefaultUrl()
            .clicksOn(onHomerPage().footerBlock().troubleshootingLink())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(TROUBLESHOOTING_LINK);
    }

    @Test
    @Title("Переходим по ссылке на Тюнинг")
    @TestCaseId("190")
    public void shouldOpenTuning() {
        user.defaultSteps().opensDefaultUrl()
            .clicksOn(onHomerPage().premiumButton())
            .shouldBeOnUrl(TUNING_LINK);
    }
}