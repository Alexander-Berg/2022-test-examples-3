package ui_tests.src.test.java.tests.testsTickets;

import Classes.ticket.Properties;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
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

import java.util.Collections;

public class TestsDisplayingFOS {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();

    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsDisplayingFOS.class);

    @InfoTest(descriptionTest = "Проверка отображения скрипта категории обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-454",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-454")
    @Category({Blocker.class})
    @Test
    public void ocrm454_CheckingDisplayOfTicketScripts() {
        String goldenScriptURL = "https://forms.yandex-team.ru/surveys/29977/?iframe=1";
        Properties properties = new Properties();
        properties.setCategory(Collections.singletonList("Категория для автотеста со скриптом"));

        // Открываем страницу очереди Покупки общие вопросы
        Pages.navigate(webDriver).openServicesBeruQuestion();
        // Переходим на вкладку обращения
        PageHelper.tableHelper(webDriver).openTab("Обращения");
        // Применяем сохранённый фильтр
        PageHelper.tableHelper(webDriver).setSavedFilter("Автотесты Тикеты в статусе Новый");
        // Открываем рандомную запись
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Переходим на страницу редактирования
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();

        // Добавляем категорию обращения к обращению
        PageHelper.ticketPageHelper(webDriver).editProperties(properties);

        // Открываем вкладку Скрипты
        PageHelper.ticketPageHelper(webDriver).openTabScripts();

        // Открываем вкладку {secondaryProperties.getCategory().get(0)}
        PageHelper.ticketPageHelper(webDriver).openTabOnScriptTab(properties.getCategory().get(0));

        // Получаем URL скрипта на странице
        String scriptFromPage = Pages.ticketPage(webDriver).scriptsTab().script().getURLScript();

        Assert.assertEquals("Открылся скрипт не тот который указан в категории обращения", goldenScriptURL, scriptFromPage);
    }

    @InfoTest(descriptionTest = "Проверка отображения скрипта очереди обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-482")
    @Category({Blocker.class})
    @Test
    public void ocrm482_CheckingDisplayOfTicketScripts() {
        String goldenScriptURL = "https://forms.yandex-team.ru/surveys/29977/?iframe=1";

        // Получаю gid обращения
        String gidTicket = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$beru').withFilters{\n" +
                        "eq('service','beruQuestion')\n" +
                        "  eq('archived',false)\n" +
                        "}\n" +
                        ".withOrders(api.db.orders.desc('creationTime'))\n" +
                        ".limit(1)\n" +
                        ".get()");
        // Открываю обращение по gid
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidTicket + "/edit");

        // Открываем вкладку Скрипты
        PageHelper.ticketPageHelper(webDriver).openTabScripts();

        // Открываем вкладку {secondaryProperties.getService()}
        PageHelper.ticketPageHelper(webDriver).openTabOnScriptTab("Покупки > Общие вопросы");

        // Получаем URL скрипта на странице
        String scriptFromPage = Pages.ticketPage(webDriver).scriptsTab().script().getURLScript();

        Assert.assertEquals("Открылся скрипт не тот который указан в свойствах очереди", goldenScriptURL, scriptFromPage);
    }

    @InfoTest(descriptionTest = "Проверка отображения скрипта очереди обращения и категории обращения одновременно",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-483")
    @Category({Blocker.class})
    @Test
    public void ocrm483_CheckingDisplayOfTicketScripts() {
        String goldenScriptURL = "https://forms.yandex-team.ru/surveys/29977/?iframe=1";
        Properties properties = new Properties();
        properties.setCategory(Collections.singletonList("Категория для автотеста со скриптом"));

        // Открываем страницу очереди Покупки общие вопросы
        Pages.navigate(webDriver).openServicesBeruQuestion();
        // Переходим на вкладку обращения
        PageHelper.tableHelper(webDriver).openTab("Обращения");
        // Применяем сохранённый фильтр
        PageHelper.tableHelper(webDriver).setSavedFilter("Автотесты Тикеты в статусе Новый");
        // Открываем рандомную запись
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Переходим на страницу редактирования
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();

        // Добавляем категорию обращения к обращению
        PageHelper.ticketPageHelper(webDriver).editProperties(properties);

        // Открываем вкладку Скрипты
        PageHelper.ticketPageHelper(webDriver).openTabScripts();

        // Открываем вкладку {secondaryProperties.getService()}
        PageHelper.ticketPageHelper(webDriver).openTabOnScriptTab("Покупки > Общие вопросы");

        // Получаем URL скрипта на странице
        String scriptServiceFromPage = Pages.ticketPage(webDriver).scriptsTab().script().getURLScript();

        // Открываем вкладку {secondaryProperties.getService()}
        Pages.ticketPage(webDriver).scriptsTab().tabs().openTab("Категория для автотеста со скриптом");

        // Получаем URL скрипта на странице
        String scriptCategoryFromPage = Pages.ticketPage(webDriver).scriptsTab().script().getURLScript();

        Boolean isServiceScript = scriptServiceFromPage.equals(goldenScriptURL);
        Boolean isCategoryScript = scriptCategoryFromPage.equals(goldenScriptURL);

        Assert.assertTrue("Открылся скрипт не тот который указан в свойствах очереди", isServiceScript & isCategoryScript);
    }

    @InfoTest(descriptionTest = "Проверка вывода скрипта на странице. Проверяем что на странице со скриптом сам скрипт выводится",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-484")
    @Category({Blocker.class})
    @Test
    public void ocrm484_CheckingDisplayOfTicketScripts() {
        // Открываем страницу очереди Покупки общие вопросы
        Pages.navigate(webDriver).openServicesBeruQuestion();
        // Переходим на вкладку обращения
        PageHelper.tableHelper(webDriver).openTab("Обращения");
        // Применяем сохранённый фильтр
        PageHelper.tableHelper(webDriver).setSavedFilter("Автотесты Тикеты в статусе Новый");
        // Открываем рандомную запись
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Переходим на страницу редактирования
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();

        // Открываем вкладку Скрипты
        PageHelper.ticketPageHelper(webDriver).openTabScripts();

        // Открываем вкладку Покупки > Общие вопросы
        PageHelper.ticketPageHelper(webDriver).openTabOnScriptTab("Покупки > Общие вопросы");

        // Получаем URL скрипта на странице
        String URLScript = Pages.ticketPage(webDriver).scriptsTab().script().getURLScript();
        int widthOfTagScript = Pages.ticketPage(webDriver).scriptsTab().script().getWidthOfTagScript();
        int heightOfTagScript = Pages.ticketPage(webDriver).scriptsTab().script().getHeightOfTagScript();

        Assert.assertTrue("Скрипт отображается не на весь размер вкладки", widthOfTagScript > 900 & heightOfTagScript > 1100 & URLScript.equals("https://forms.yandex-team.ru/surveys/29977/?iframe=1"));
    }
}
