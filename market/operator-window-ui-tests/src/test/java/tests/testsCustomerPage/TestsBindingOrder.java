package ui_tests.src.test.java.tests.testsCustomerPage;

import interfaces.other.InfoTest;
import interfaces.testPriorities.Minor;
import interfaces.testPriorities.Normal;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import unit.Customers;

import java.util.List;

/**
 * Сабфункциональность "Привязка заказа"
 */
public class TestsBindingOrder {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();;

    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsBindingOrder.class);

    @InfoTest(descriptionTest = "Привязка заказа к карточке клиента (неавторизованный заказ)",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1115",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1000"
    )
    @Category({Normal.class})
    @Test
    public void ocrm1115_bindingOrderToCustomer() {

        // uid авторизованного клиента
        String customerUid = "641898166";

        // Найти скриптом из админки gid неавторизованного заказа
        String orderGid = PageHelper.customerPageHelper(webDriver).getGidOfUnauthorizedOrder();

        // Получить из gid заказа id
        String orderId = PageHelper.customerPageHelper(webDriver).getOrderId(orderGid);

        // Перейти на карточку авторизованного клиента
        Pages.navigate(webDriver).openPageByMetaClassAndID(Customers.authorizedCustomerGid);

        // Привязать заказ
        PageHelper.customerPageHelper(webDriver).bindingOrder(orderId);

        // Проверить, что заказ привязался к клиенту
        Assert.assertEquals(
                "Заказ не привязался, т.к. uid'ы отличаются",
                customerUid,
                PageHelper
                        .otherHelper(webDriver)
                        .runningScriptFromAdministrationPage("api.db.'" + orderGid + "'.buyerUid")
        );
    }

    @InfoTest(descriptionTest = "Поле для ввода заказа очищается после привязки",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1115",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1190"
    )
    @Category({Minor.class})
    @Test
    public void ocrm1190_fieldClearAfterBinding() {

        // Найти скриптом из админки gid неавторизованного заказа
        String orderGid = PageHelper.customerPageHelper(webDriver).getGidOfUnauthorizedOrder();

        // Получить из gid заказа id
        String orderId = PageHelper.customerPageHelper(webDriver).getOrderId(orderGid);

        // Перейти на карточку авторизованного клиента
        Pages.navigate(webDriver).openPageByMetaClassAndID(Customers.authorizedCustomerGid);

        // Привязать заказ
        PageHelper.customerPageHelper(webDriver).bindingOrder(orderId);

        // Проверить, что поле ввода очистилось
        Assert.assertEquals(
                "Поле не пустое",
                "",
                webDriver.findElement(By.xpath(".//span[@data-tid='bee3f49 624f6689']/div/div/input")).getText()
        );
    }

    @InfoTest(descriptionTest = "Кнопка привязки заказа становится неактивной после привязки",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1191",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1000"
    )
    @Category({Normal.class})
    @Test
    public void ocrm1191_bindingButtonIsDisabledAfterBinding() {

        // Найти скриптом из админки gid неавторизованного заказа
        String orderGid = PageHelper.customerPageHelper(webDriver).getGidOfUnauthorizedOrder();

        // Получить из gid заказа id
        String orderId = PageHelper.customerPageHelper(webDriver).getOrderId(orderGid);

        // Перейти на карточку авторизованного клиента
        Pages.navigate(webDriver).openPageByMetaClassAndID(Customers.authorizedCustomerGid);

        // Привязать заказ
        PageHelper.customerPageHelper(webDriver).bindingOrder(orderId);

        // Проверить, что кнопка привязки неактивна
        Assert.assertFalse(
                "Кнопка привязки активна",
                webDriver.findElement(By.xpath(".//button[@title='Привязать заказ']")).isEnabled()
        );
    }

    @InfoTest(descriptionTest = "Уведомление об успешной привязке заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1192",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1000"
    )
    @Category({Normal.class})
    @Test
    public void ocrm1192_notificationOfSuccessfulOrderBinding() {
        // Список сообщений в тостах
        List<String> toastMessages;

        // Найти скриптом из админки gid неавторизованного заказа
        String orderGid = PageHelper.customerPageHelper(webDriver).getGidOfUnauthorizedOrder();

        // Получить из gid заказа id
        String orderId = PageHelper.customerPageHelper(webDriver).getOrderId(orderGid);

        // Перейти на карточку авторизованного клиента
        Pages.navigate(webDriver).openPageByMetaClassAndID(Customers.authorizedCustomerGid);

        // Привязать заказ
        PageHelper.customerPageHelper(webDriver).bindingOrder(orderId);

        Pages.ticketPage(webDriver).toast().showNotificationError();

        toastMessages = Pages.ticketPage(webDriver).toast().getToastMessages();
        for (String message : toastMessages) {
            boolean b = message
                    .contains("Заказ " + orderId + " успешно привязан");
            if (b) return;
        }
        throw new AssertionError("Сообщение не появилась, либо текст отличается от ожидаемого");
    }

    @InfoTest(descriptionTest = "Ошибка при привязке авторизованного заказа к карточке клиента",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1119",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1000"
    )
    @Category({Normal.class})
    @Test
    public void ocrm1119_bindingOrderToCustomerError() {
        // Список ошибок
        List<String> dangerMessages;

        // Номер авторизованного заказа от другого клиента
        String orderId = "32263457";

        // Перейти на карточку авторизованного клиента
        Pages.navigate(webDriver).openPageByMetaClassAndID(Customers.authorizedCustomerGid);

        // Привязать заказ
        PageHelper.customerPageHelper(webDriver).bindingOrder(orderId);

        // Получить текст ошибки и проверить, есть ли среди ошибок ожидаемая
        dangerMessages = Pages.ticketPage(webDriver).alertDanger().getAlertDangerMessages();
        for (String message : dangerMessages) {
            boolean b = message
                    .contains("Ошибка. Авторизованный заказ");
            if (b) return;
        }
        throw new AssertionError("Ошибка не появилась, либо текст отличается от ожидаемого");
    }
}
