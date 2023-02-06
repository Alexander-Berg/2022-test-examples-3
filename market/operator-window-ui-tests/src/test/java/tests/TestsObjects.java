package ui_tests.src.test.java.tests;

import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import interfaces.testPriorities.Critical;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;


/**
 * Объекты
 */

public class TestsObjects  {

    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();

    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsObjects.class);

    @InfoTest(
            descriptionTest = "Кнопка 'Сбросить выбор' неактивна, если значение не выбрано'",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-955",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-586"
    )
    @Category({Blocker.class})
    @Ignore("выпилили функциональность")
    @Test
    public void ocrm955_ResetSelectionButtonDisabledWhenOptionNotSelected() {
        // Найти очередь с пустым значением в 'Новая очередь при решении тикета'
        String serviceGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def obj = api.db.of('service').withFilters{\n" +
                        "  eq('serviceOnResolved', null)}\n" +
                        "  .limit(1)\n" +
                        "  .get()\n" +
                        "return obj.gid");

        // Открыть страницу редактирования очереди
        Pages.navigate(webDriver).openPageByMetaClassAndID(serviceGid + "/edit");

        // Нажать на селект "Новая очередь при решении тикета"
        Pages.createServicePage(webDriver).properties().clickSelectTypeField("serviceOnResolved");

        // Проверить значение атрибута disabled у кнопки 'сбросить выбор'
        String buttonStatus = Tools.findElement(webDriver).findElement(By.xpath("//button[text()='сбросить выбор']"))
                .getAttribute("disabled");

        // Сверить значение
        Assert.assertEquals("Кнопка 'Сбросить выбор' не задизейблена, хотя не выбрано ни одно значение",
                "true", buttonStatus);
    }

    @InfoTest(
            descriptionTest = "Работа кнопки 'Сбросить выбор'",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-956",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-586"
    )
    @Category({Blocker.class})
    @Ignore("устарело")
    @Test
    public void ocrm956_ResetSelectionButton() {
        // Установить значение в поле 'Новая очередь при решении тикета' очереди Bringly > Запросы
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def obj = api.db.get('service@30013906')\n" +
                        "  api.bcp.edit(obj, ['serviceOnResolved' : 'service@30013906'])", false);

        // Дождаться, пока изменения применятся
        for (int i = 0; i < 5; i++) {
            Tools.waitElement(webDriver).waitTime(3000);
            String serviceOnResolved = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "def obj = api.db.get('service@30013906')\n" +
                            "return obj.serviceOnResolved");
            if (serviceOnResolved == null) {
                if (i == 4) {
                    throw new Error("Не удалось установить значение в поле 'Новая очередь при решении тикета'");
                }
                continue;
            } else if (serviceOnResolved.equals("service@30013906")) {
                Tools.waitElement(webDriver).waitTime(5000);
                break;
            }
        }

        // Открыть страницу редактирования очереди Bringly > Запросы
        Pages.navigate(webDriver).openPageByMetaClassAndID("service@30013906/edit");

        // Открыть селект "Новая очередь при решении тикета" и нажать "сбросить выбор"
        Entity.properties(webDriver).clickResetSelectionButton("serviceOnResolved");

        // Сохранить изменения
        Pages.createServicePage(webDriver).header().saveButtonClick();

        Boolean b = false;
        // Убедиться, что измения применились
        for (int i = 0; i < 5; i++) {
            Tools.waitElement(webDriver).waitTime(3000);
            String serviceOnResolved = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "def obj = api.db.get('service@30013906')\n" +
                            "return obj.serviceOnResolved");
            if (serviceOnResolved == null) return;
            if (i == 4) {
                throw new AssertionError("Поле 'Новая очередь при решении тикета' не обнулилось.");
            }
        }
        Assert.assertTrue("Поле 'Новая очередь при решении тикета' не обнулилось.", b);
    }

    @Test
    @InfoTest(descriptionTest = "Отображение статуса объекта в заголовке",
    linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1350",
    linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1112")
    @Category(Critical.class)
    public void ocrm1350_DisplayingObjectStatusInHeader(){
        String[] gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def service = api.db.of('service').withFilters{\n" +
                        "eq('status','active')\n" +
                        "}.limit(1).get()\n" +
                        "\n" +
                        "return \\\"$service,$service.title\\\"").split(",");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid[0]);
        String statusFromPage = Tools.findElement(webDriver).findElement(By.xpath("//*[@title='"+gid[1]+"']/following::*[1]")).getText();
        Assert.assertTrue("На странице вывелся не тот статус который мы ожидали. \n" +
                "Вывелся - " +statusFromPage+"\n"+
                "А ожидали - Активный",statusFromPage.equals("Активный"));

    }
}
