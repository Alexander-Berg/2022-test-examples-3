package ui_tests.src.test.java.tests;

import Classes.Employee;
import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;
import unit.Config;

import java.util.Arrays;

/**
 * Функциональность Пользователь
 */
public class TestsEmployee {

    private static WebDriver webDriver;
    @Rule
    public final TestName name = new TestName();

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsEmployee.class);

    @After
    public void after() {
        if (name.getMethodName().contains("ocrm804_")) {
            // Вывести текущего пользователя из архивных и поставить флаг необходимости телефонии
            PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def employee = api.db.of('employee')\n" +
                    ".withFilters{ eq('staffLogin', '" + Config.getAdditionalUserLogin() + "') }\n" +
                    ".get() \n" +
                    "api.bcp.edit(obj, ['voximplantEnabled' : true]) \n" +
                    "api.bcp.edit(obj, ['status' : 'active'])", false);
        }
        if (name.getMethodName().contains("ocrm805_")) {
            PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def obj = api.db.get('employee@134304584') \napi.bcp.edit(obj, ['status' : 'active'])", false);
        }
        if (name.getMethodName().contains("ocrm1313_") && !(testEmployee.equals(""))) {
            PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "def empl = api.db.'" + testEmployee + "'\n" +
                            "api.bcp.edit(empl,[status:'active'])"
            );
        }
    }

    private String testEmployee = "";

    @InfoTest(descriptionTest = "проверка редактирования карточки пользователя",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-763")
    @Category({Blocker.class})
    @Test
    public void ocrm763_CheckingEditEmployeeRecord() {
        Employee expectedEmployee = new Employee()
                .setAlias(Tools.other().getRandomText())
                .setOu(Arrays.asList("Колл-центры"))
                .setRoles(Arrays.asList("Внешний оператор", "Администратор"))
                .setServices(Arrays.asList("Покупки > Входящая телефония VIP", "Покупки > Входящая телефония"))
                .setTeams(Arrays.asList("1ая линия телефония", "Рекламации Беру"))
                .setTitle(Tools.other().getRandomText());
        Employee employeeFromPage = new Employee();
        // Подготавливаем карточку пользователя
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def obj = api.db.get('employee@123198884') \n " +
                "api.bcp.edit(obj, " +
                "['title' : 'Тестовый сотрудник для Автотестов'," +
                "'ou':['ou@29020702']," +
                "'alias':'Autotest'," +
                "'roles':[]," +
                "'teams':[]," +
                "'services':[]])");
        // Открываем карточку пользователя
        Pages.navigate(webDriver).openPageByMetaClassAndID("employee@123198884");
        // Открываем, изменяем и сохраняем изменения
        PageHelper.employeeHelper(webDriver).editEmployee(expectedEmployee);
        // Получаем данные со страницы
        int i = 0;
        do {
            if (i < 5) {
                Tools.waitElement(webDriver).waitTime(3000);
                //Открываем карточку пользователя
                Pages.navigate(webDriver).openPageByMetaClassAndID("employee@123198884");
                //Получаем свойства пользователя
                employeeFromPage = PageHelper.employeeHelper(webDriver).getAllPropertiesFromPage();
                i++;
            } else {
                break;
            }
        } while (!expectedEmployee.equals(employeeFromPage));
        Assert.assertEquals("Карточка пользователя после редактирования не равна той что ожидаем",
                expectedEmployee, employeeFromPage);
    }

    @InfoTest(descriptionTest = "Проверяем отключение учётной записи телефонии при архивировании пользователя",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-273",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-804")
    @Category({Blocker.class})
    @Test
    @Ignore("тикет на багу - https://st.yandex-team.ru/OCRM-5767")
    public void ocrm804_DisablingATelephonyAccountWhenArchivingAUser() {
        // gid пользователя
        String gidRecord = "def employee = api.db.of('employee')\n" +
                ".withFilters{ eq('staffLogin', '" + Config.getAdditionalUserLogin() + "') }\n" +
                ".get()";
        // Результат запросов
        boolean b = false;

        // Открываем карточку пользователя
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidRecord);
        // Нажимаем на кнопку архивирования
        Pages.employeePage(webDriver).viewRecordPage().header().clickArchiveRecordButton();

        // Ждём пока пользователь архивируется
        for (int i = 0; i < 4; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            b = Boolean.valueOf(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.get('" + gidRecord + "').archived"));
            if (b) {
                break;
            }
        }

        if (b) // Если пользователь стал архивным
        {
            // Получаем состояние флага телефонии
            b = Boolean.parseBoolean(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.get('" + gidRecord + "').voximplantEnabled"));
            // Проверяем, отключился ли флаг телефонии
            Assert.assertFalse("После архивирования пользователя не выключилась учётная запись телефонии Ссылка на пользователя - " + Config.getProjectURL() + "/entity/" + gidRecord, b);
        } else //Если пользователь не стал архивным
        {
            throw new Error("Не получилось архивировать пользователя. Ссылка на пользователя - " + Config.getProjectURL() + "/entity/" + gidRecord);
        }
    }

    @InfoTest(descriptionTest = "Проверка отображения обращения в работе на карточке пользователя",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-274",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-806",
            requireYouToLogInUnderANewUser = true)
    @Category({Blocker.class})
    @Test
    public void ocrm806_checkingDisplayOfTicketsInWorkOnEmployeePage() {
        boolean result = false;
        // Переходим в статус Готов
        PageHelper.mainMenuHelper(webDriver).switchUserToStatus("Готов");
        // Получаем заголовок назначенного тикета
        String titleTicket = Pages.ticketPage(webDriver).header().getSubject();
        // Открываем страницу текущего пользователя
        Pages.mainMenuOfPage(webDriver).openEmployeePage();
        // Переходим на вкладку Обращения
        Pages.employeePage(webDriver).viewRecordPage().tabs().openTicketsTab();
        // Обновляем таблицу c обращениями и ищем среди обращений обращение, которое у нас в работе
        for (int i = 0; i < 3; i++) {
            if (i != 0) {
                Entity.entityTable(webDriver).toolBar().updateContent();
            }

            result = Pages.employeePage(webDriver).viewRecordPage().ticketsTab().getTitlesOfTicketInWork().contains(titleTicket);
            if (!result) {
                Tools.waitElement(webDriver).waitTime(1000);
            } else {
                break;
            }
        }
        // Получаем заголовки обращений из таблицы и проверяем что среди них есть заголовок обращения который был назначен на меня
        Assert.assertTrue("Среди обращений в статусе \"В работе\" нет обращения который у нас сейчас в работе - " + titleTicket, result);
    }

    @InfoTest(descriptionTest = "Проверка отображения недавно выполненных обращений на карточке пользователя",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-274",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-807")
    @Category(Blocker.class)
    @Test
    public void ocrm807_checkingDisplayOfRecentlyCompletedTicketsOnEmployeePage() {
        String expectedTitle;
        boolean result = false;
        //GID текущего пользователя
        String employeeGID;

        expectedTitle = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                // Ищем обращение с очередью "Покупки > Общие вопросы", статусом "Новый" и не архивные
                "def ticket = api.db.of('ticket$beru')\n" +
                        "  .withFilters {\n" +
                        "    eq('service','beruQuestion')\n" +
                        "    eq('archived', false)\n" +
                        "    eq('status','registered')\n" +
                        "  }\n" +
                        "  .withOrders(api.db.orders.desc('creationTime'))\n" +
                        "  .limit(1)\n" +
                        "  .get()\n" +
                        "\n" +
                        // Переводим обращение в статус "В работе"
                        "api.bcp.edit(ticket, ['status':'processing'])\n" +
                        // Переводим обращение в статус "спам"
                        "api.bcp.edit(ticket, ['status':'spam'])\n" +
                        "return ticket.title");

        employeeGID = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('employee')" +
                ".withFilters{ eq('staffLogin', '" + Config.getMainUserLogin() + "') }.get()");
        // Открываем страницу текущего пользователя
        Pages.navigate(webDriver).openPageByMetaClassAndID(employeeGID);

        // Переходим на вкладку Обращения
        Pages.employeePage(webDriver).viewRecordPage().tabs().openTicketsTab();
        for (int i = 0; i < 4; i++) {

            if (i != 0) {
                Entity.entityTable(webDriver).toolBar().updateContent();
                Tools.waitElement(webDriver).waitInvisibleLoadingElement();
            }

            // Получаем заголовки обращений из таблицы и проверяем, что среди них есть заголовок обращения который был назначен на меня
            result = Pages.employeePage(webDriver).viewRecordPage().ticketsTab().getTitlesOfRecentlyCompletedTicket().contains(expectedTitle);
            if (result) {
                break;
            } else {
                Tools.waitElement(webDriver).waitTime(1000);
            }
        }
        Assert.assertTrue("Среди недавно выполненных обращений нет обращения который мы только что выполнили. " + expectedTitle, result);
    }

    @InfoTest(requireYouToLogIn = false,
            descriptionTest = "Проверяем сообщение об ошибке при попытке авторизации без доступа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-58",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-809")
    @Ignore("Проверяется на фронте")
    @Category({Blocker.class})
    @Test
    public void ocrm809_CheckingErrorMessage() {

        // Результат проверки наличия текста ошибки
        boolean condition;
        Tools.waitElement(webDriver).waitVisibilityElementTheTime(By.xpath("//*[contains(text(),'Загрузка')]"), Config.DEF_TIME_WAIT_LOAD_PAGE);
        Tools.waitElement(webDriver).waitInvisibilityElementTheTime(By.xpath("//*[contains(text(),'Загрузка')]"), Config.DEF_TIME_WAIT_LOAD_PAGE);
        // Получаем текст сообщения блока с ошибкой
        String message = Tools.findElement(webDriver).findVisibleElement(By.xpath("//*[@class='info-block error-page__content']")).getText();
        // Проверяем полученный текст с оригиналом
        condition = message.contains("Доступ запрещен\nВозможно, Вам следует сменить учетную запись\nОшибка аутентификации. ");
        Assert.assertTrue(condition);
    }

    @InfoTest(requireYouToLogIn = false,
            descriptionTest = "Проверяем наличие ссылки на страницу авторизации",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-58",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-810")
    @Ignore("Проверяется на фронте")
    @Category({Blocker.class})
    @Test
    public void ocrm810_CheckingAuthorizationLink() {
        Tools.waitElement(webDriver).waitVisibilityElementTheTime(By.xpath("//*[contains(text(),'Загрузка')]"), Config.DEF_TIME_WAIT_LOAD_PAGE);
        Tools.waitElement(webDriver).waitInvisibilityElementTheTime(By.xpath("//*[contains(text(),'Загрузка')]"), Config.DEF_TIME_WAIT_LOAD_PAGE);
        // Получаем ссылку на страницу авторизации
        String link = Tools.findElement(webDriver).findVisibleElement(By.xpath("//*[@class='info-block error-page__content']//a")).getAttribute("href");
        // Проверяем полученную ссылку с оригиналом
        Assert.assertTrue(link.toLowerCase().contains("https://passport.yandex-team.ru/auth/?retpath=" + Config.getProjectURL().toLowerCase()));
    }

    @Test
    @InfoTest(descriptionTest = "Архивирование внешнего пользователя",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-767",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1313")
    @Category(Blocker.class)
    public void ocrm1313_ArchivedOutStaffEmployee() {
        Boolean isArchived = false;
        Boolean isUnarchived = false;
        //Получаем гид внешнего пользователя
        testEmployee = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('employee$externalUser').withFilters{\n" +
                        "eq('archived',false)\n" +
                        "}\n" +
                        ".limit(1).get()"
        );
        Pages.navigate(webDriver).openPageByMetaClassAndID(testEmployee);
        // архивируем пользователя
        Pages.employeePage(webDriver).viewRecordPage().header().clickArchiveRecordButton();
        // получаем статус архивирования
        for (int i = 0; i < 5; i++) {
            isArchived = Boolean.parseBoolean(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "api.db.'" + testEmployee + "'.archived"));
            if (isArchived) {
                break;
            } else {
                Tools.waitElement(webDriver).waitTime(3000);
            }
        }
        // принудительно архивируем пользователя
        for (int i = 0; i < 5; i++) {
            PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "def empl = api.db.'" + testEmployee + "'\n" +
                            "api.bcp.edit(empl,[status:'archived'])");
            boolean b = !Boolean.parseBoolean(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "api.db.'" + testEmployee + "'.archived"));
            if (b) {
                break;
            } else {
                Tools.waitElement(webDriver).waitTime(3000);
            }
        }

        Pages.navigate(webDriver).openPageByMetaClassAndID(testEmployee);
        // извлекаем пользователя из архива
        Pages.employeePage(webDriver).viewRecordPage().header().clickUnarchiveRecordButton();
        // получаем статус архивирования
        for (int i = 0; i < 5; i++) {
            isUnarchived = !Boolean.parseBoolean(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "api.db.'" + testEmployee + "'.archived"));
            if (isUnarchived) {
                break;
            } else {
                Tools.waitElement(webDriver).waitTime(3000);
            }
        }
        Assert.assertTrue("Не удалось архивировать пользователя - " + isArchived + "\n" +
                "Не удалось разархивировать пользователя - " + isUnarchived, isUnarchived && isArchived);

    }
}
