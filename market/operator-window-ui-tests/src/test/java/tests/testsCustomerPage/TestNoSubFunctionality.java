package ui_tests.src.test.java.tests.testsCustomerPage;

import Classes.customer.Customer;
import Classes.customer.MainProperties;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import interfaces.testPriorities.Normal;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;
import unit.Config;
import unit.Customers;

public class TestNoSubFunctionality {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestNoSubFunctionality.class);

    @InfoTest(
            descriptionTest = "Редирект на новую карточку клиента (авторизованный клиент)",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1208",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1202"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1208_RedirectToNewUidCustomerPage() {
        String customerUid = "641898166";

        webDriver.get(Config.getProjectURL() + "/customer/uid/" + customerUid);
        Tools.waitElement(webDriver).waitTime(2000);
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        String customerGid = PageHelper.customerPageHelper(webDriver).getCustomerGid(customerUid);

        Assert.assertEquals("Перенаправления на новую карточку клиента не произошло",
                Config.getProjectURL() + "/entity/" + customerGid,
                webDriver.getCurrentUrl());
    }

    @InfoTest(
            descriptionTest = "Редирект на новую карточку клиента (неавторизованный клиент)",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1209",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1202"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1209_RedirectToNewMuidCustomerPage() {
        String customerUid = "1152921504817345742";

        webDriver.get(Config.getProjectURL() + "/customer/muid/" + customerUid);
        Tools.waitElement(webDriver).waitTime(2000);
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        String customerGid = PageHelper.customerPageHelper(webDriver).getCustomerGid(customerUid);

        Assert.assertEquals("Перенаправления на новую карточку клиента не произошло",
                Config.getProjectURL() + "/entity/" + customerGid,
                webDriver.getCurrentUrl());
    }

    @InfoTest(
            descriptionTest = "Проверка блока основной информации в карточке авторизованного клиента",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1342",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-891 и https://testpalm2.yandex-team.ru/testcase/ocrm-909"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1342_CheckingBaseInfoFromAuthCustomerPage() {
        // Задать ожидаемые значения для клиента
        Customer expectedCustomer = new Customer()
                .setMainProperties(new MainProperties()
                        .setFullName("Pupkin Vasily (BloodFromPixels)")
                        .setPhone("+7 (000) 000-00-11")
                        .setEmail("bloodfrompixels@yandex.ru")
                        .setRegistrationDate("25.04.2018 14:10")
                        .setUid("641898166")
                        .setCashback("60045.00")
                );

        // Перейти на страницу авторизованного клиента
        Pages.navigate(webDriver).openPageByMetaClassAndID(Customers.authorizedCustomerGid);

        // Получить со страницы информацию о клиенте
        Customer customerFromPage = PageHelper.customerPageHelper(webDriver).getCustomer();

        Assert.assertEquals("Информация на странице клиента отличается от ожидаемой", expectedCustomer, customerFromPage);
    }

    @InfoTest(
            descriptionTest = "Проверка блока основной информации в карточке неавторизованного клиента",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1343",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-891 и https://testpalm2.yandex-team.ru/testcase/ocrm-909"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1343_CheckingBaseInfoFromNoAuthCustomerPage() {
        // Задать ожидаемые значения для клиента
        Customer expectedCustomer = new Customer()
                .setMainProperties(new MainProperties()
                        .setFullName("Нет данных")
                        .setPhone("нет данных")
                        .setEmail("нет данных")
                        .setRegistrationDate("нет данных")
                        .setUid("1152921504815705600")
                        .setCashback("нет данных")
                );

        // Перейти на страницу авторизованного клиента
        Pages.navigate(webDriver).openPageByMetaClassAndID(Customers.notAuthorizedCustomerGid);

        // Получить со страницы информацию о клиенте
        Customer customerFromPage = PageHelper.customerPageHelper(webDriver).getCustomer();

        Assert.assertEquals("Информация на странице клиента отличается от ожидаемой", expectedCustomer, customerFromPage);
    }
}
