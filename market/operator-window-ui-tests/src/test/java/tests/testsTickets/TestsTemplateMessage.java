package ui_tests.src.test.java.tests.testsTickets;

import Classes.ticket.Properties;
import entity.Entity;
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
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;

import java.util.Collections;
import java.util.List;

public class TestsTemplateMessage {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsTemplateMessage.class);

    @InfoTest(descriptionTest = "Проверка отображения шаблона сообщения который не закреплен за категорией обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-503")
    @Category({Normal.class})
    @Test
    public void ocrm503_CheckingTemplateDisplay() {

        boolean requirement;
        Properties properties = new Properties();
        properties.setCategory(Collections.singletonList("test sanity"));

        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket').withFilters{\n" +
                        "eq('service','beruQuestion')\n" +
                        "eq('status','registered')}\n" +
                        ".withOrders(api.db.orders.desc('creationTime'))\n" +
                        ".limit(1)\n" +
                        ".get()");
        Pages.navigate(webDriver).openPageByMetaClassAndID(gid + "/edit");

        // Переходим на вкладку "Сообщения"
        PageHelper.ticketPageHelper(webDriver).openTabComments();

        // Нажимаем на кнопку "Выбрать ответ из шаблонов"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickMessageTemplateButton();

        // Получить список доступных шаблонов
        List<String> messageTemplates = Entity.modalWindow(webDriver).content().getTitleTemplates();

        requirement = messageTemplates.contains("Шаблон для автотестов");

        // Закрываем модельное окно выбора шаблонов
        Entity.modalWindow(webDriver).controls().clickButton("Закрыть");

        PageHelper.ticketPageHelper(webDriver).editProperties(properties);

        // Переходим на вкладку "Сообщения"
        PageHelper.ticketPageHelper(webDriver).openTabComments();

        // Нажимаем на кнопку "Выбрать ответ из шаблонов"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickMessageTemplateButton();

        // Получить список доступных шаблонов
        messageTemplates = Entity.modalWindow(webDriver).content().getTitleTemplates();

        requirement = requirement & messageTemplates.contains("Шаблон для автотестов");

        Assert.assertTrue("Шаблон без категории не виден", requirement);
    }

    @InfoTest(descriptionTest = "Проверка отображения шаблона сообщения который закреплен за категорией обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-504")
    @Category({Normal.class})
    @Test
    public void ocrm504_CheckingTemplateDisplay() {

        boolean requirement;
        Properties properties = new Properties();
        properties.setCategory(Collections.singletonList("Проблемы с изменением статуса"));

        // Открываем страницу очереди Покупки общие вопросы
        Pages.navigate(webDriver).openServicesBeruQuestion();
        // Переходим на вкладку обращения
        PageHelper.tableHelper(webDriver).openTab("Обращения");
        // Применяем сохранённый фильтр
        PageHelper.tableHelper(webDriver).setSavedFilter("Автотесты Тикеты в статусе Новый");
        Pages.ticketPage(webDriver).toast().hideNotificationError();
        // Открываем рандомную запись
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // переходим на страницу редактирования
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();

        // Переходим на вкладку "Сообщения"
        PageHelper.ticketPageHelper(webDriver).openTabComments();

        // Нажимаем на кнопку "Выбрать ответ из шаблонов"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickMessageTemplateButton();

        // Получить список доступных шаблонов
        List<String> messageTemplates = Entity.modalWindow(webDriver).content().getTitleTemplates();

        requirement = !messageTemplates.contains("Шаблон для автотестов с категорией");

        // Закрываем модельное окно выбора шаблонов
        Entity.modalWindow(webDriver).controls().clickButton("Закрыть");

        // Задаём категории обращения
        PageHelper.ticketPageHelper(webDriver).editProperties(properties);

        // Переходим на вкладку "Сообщения"
        PageHelper.ticketPageHelper(webDriver).openTabComments();

        // Нажимаем на кнопку "Выбрать ответ из шаблонов"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickMessageTemplateButton();

        // Получить список доступных шаблонов
        messageTemplates = Entity.modalWindow(webDriver).content().getTitleTemplates();

        requirement = requirement & messageTemplates.contains("Шаблон для автотестов с категорией");

        Assert.assertTrue("Шаблон c категорией не виден если выбрана категория", requirement);
    }


    @InfoTest(descriptionTest = "Проверка не отображения шаблона сообщения который закреплен за категорией обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-526")
    @Category({Normal.class})
    @Test
    public void ocrm526_CheckingTemplateDisplay() {

        boolean requirement;
        Properties properties = new Properties();
        properties.setCategory(Collections.singletonList("Проблемы с изменением статуса"));

        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticketGid = api.db.of('ticket$beru')\n" +
                        "    .withFilters {\n" +
                        "      eq('archived', false)\n" +
                        "    }\n" +
                        "    .withOrders(api.db.orders.desc('creationTime'))\n" +
                        "    .limit(1)\n" +
                        "    .get()");

        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid);

        // Переходим на вкладку "Сообщения"
        PageHelper.ticketPageHelper(webDriver).openTabComments();

        // Нажимаем на кнопку "Выбрать ответ из шаблонов"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickMessageTemplateButton();

        // Получить список доступных шаблонов
        List<String> messageTemplates = Entity.modalWindow(webDriver).content().getTitleTemplates();

        requirement = messageTemplates.contains("Шаблон для автотестов с категорией");

        Assert.assertFalse("Шаблон у которого закреплена категория, не отображается если не выбрана эта категория", requirement);
    }

    @InfoTest(descriptionTest = "Проверяем что выбранный нами шаблон сообщения применился",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-532")
    @Category({Blocker.class})
    @Test
    public void ocrm532_CheckingForInsertingMessageTemplate() {

        String messageFromPage;
        String message = "Материал из Википедии — свободной энциклопедииПерейти к навигацииПерейти к поискуОсновная статья: Научная литература Научный журнал (рецензируемый или реферируемый[1] научный журнал) — журнал, в котором присылаемые статьи перед публикацией представляются на рецензирование независимым специалистам, которые обычно не входят в состав редакции журнала и ведут исследования в областях, близких с тематикой статьи. Научный журнал является одной из главных составляющих научной литературы. Рецензирование материалов выполняется для того, чтобы постараться оградить читателей от методологических ошибок или фальсификаций. Печати в научном журнале может предшествовать печать препринта. Во многих странах, в том числе в России, научные журналы проходят аттестацию в правительственных или общественных организациях (в России эти функции выполняет Высшая аттестационная комиссия, ВАК).";

        Pages.navigate(webDriver).openServicesBeruQuestion();
        PageHelper.tableHelper(webDriver).openTab("Обращения");
        // Применяем сохранённый фильтр
        Entity.entityTable(webDriver).toolBar().setQuickSearch("Автотесты Тикеты в статусе Новый");
        // Открываем рандомный тикет
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Переходим на страницу редактирования
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();
        // Задаём категорию обращения
        Pages.ticketPage(webDriver).properties().setCategory(Collections.singletonList("test sanity"));
        String xpath = Entity.properties(webDriver).getXPathElement("default")+"/*";
        Tools.clickerElement(webDriver).clickElement(By.xpath(xpath));
        PageHelper.ticketPageHelper(webDriver).openOutputMailTabOnMailTab();
        // Открыть окно выбора шаблона сообщения
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickMessageTemplateButton();
        // Нажать на шаблон сообщения
        Entity.modalWindow(webDriver).content().clickButton("Шаблон с длинным названием для автотестов");
        // Нажать на кнопку "Использовать этот шаблон"
        Entity.modalWindow(webDriver).content().clickActionButton("Использовать этот шаблон");

        messageFromPage = Pages.ticketPage(webDriver).messageTab().commentsCreation().getEnteredComment();
        Assert.assertEquals("Вставленный текст не равен тексту в шаблоне", message, messageFromPage);
    }

    @InfoTest(descriptionTest = "Проверка вывода текста шаблона",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-557")
    @Category({Blocker.class})
    @Test
    public void ocrm557_DisplayTemplateText() {

        String textTemplateFromPage;
        String actualTextTemplate = "Материал из Википедии — свободной энциклопедииПерейти к навигацииПерейти к поискуОсновная статья: Научная литература Научный журнал (рецензируемый или реферируемый[1] научный журнал) — журнал, в котором присылаемые статьи перед публикацией представляются на рецензирование независимым специалистам, которые обычно не входят в состав редакции журнала и ведут исследования в областях, близких с тематикой статьи. Научный журнал является одной из главных составляющих научной литературы. Рецензирование материалов выполняется для того, чтобы постараться оградить читателей от методологических ошибок или фальсификаций. Печати в научном журнале может предшествовать печать препринта. Во многих странах, в том числе в России, научные журналы проходят аттестацию в правительственных или общественных организациях (в России эти функции выполняет Высшая аттестационная комиссия, ВАК).";
        // Ищем обращение  из очереди "Покупки > Общие вопросы", в статусе "Новый" и не архивное
        // Меняем его статус на "В работе"
        String gidTicket = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
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
                        // Берем обращение в работу
                        "api.bcp.edit(ticket, ['status':'processing'])\n" +
                        "return ticket;");
        // Открываем найденное обращение
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidTicket + "/edit");

        // Применяем категорию обращения
        Pages.ticketPage(webDriver).properties().setCategory(Collections.singletonList("test sanity"));
        // Нажать на кнопку "Выбрать ответ из шаблонов"
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickMessageTemplateButton();
        // Нажать на шаблон "Шаблон с длинным названием для автотестов"
        Entity.modalWindow(webDriver).content().clickButton("Шаблон с длинным названием для автотестов");
        // Получаем текст шаблона который вывелся на странице
        textTemplateFromPage = Entity.modalWindow(webDriver).content().getTemplateText("Шаблон с длинным названием для автотестов");

        Assert.assertEquals("Текст выведенного шаблона не совпадает с текстом который должен быть", actualTextTemplate, textTemplateFromPage);
    }
}
