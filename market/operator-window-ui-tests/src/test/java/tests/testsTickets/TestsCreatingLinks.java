package ui_tests.src.test.java.tests.testsTickets;

import Classes.Email;
import Classes.RelatedTicket;
import Classes.ticket.Properties;
import Classes.ticket.Ticket;
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
import unit.Orders;
import unit.Partners;

import java.util.GregorianCalendar;
import java.util.HashMap;

public class TestsCreatingLinks {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsCreatingLinks.class);

    @InfoTest(
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-434",
            descriptionTest = "Создание связи обращения и заказа, когда в заголовке письма есть номер заказа")
    @Category({Blocker.class})
    @Test
    public void ocrm434_checkCreateTicketWithAnOrder() {
        Email newEmail = new Email();
        Ticket ticket = new Ticket();
        String order = Orders.getPostpaidOrder().getMainProperties().getOrderNumber();

        // Создаём исходящее письмо
        newEmail
                .setSubject("Проверка создания обращений " + (new GregorianCalendar()).getTime().toString() + " с заказ " + order)
                .setTo("beru")
                .setText("простой текст сообщения для проверки создания обращения")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru");

        // Создаём тикет, который должен получиться
        ticket.setProperties(new Properties().setOrder(order));

        // Отправляем созданное исходящее письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "beru");

        // Открываем страницу обращения
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(newEmail.getSubject());

        // Со страницы обращения получаем свойства, тему, сообщения
        String orderNumberFromPage = Pages.ticketPage(webDriver).properties().getOrderNumber();

        Assert.assertEquals("К обращению не привязался заказ " + order + ". Ссылка на обращение:" + webDriver.getCurrentUrl(), order, orderNumberFromPage);
    }

    @InfoTest(descriptionTest = "Создание связи обращения и заказа, когда в теле письма есть номер заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-435")
    @Category({Blocker.class})
    @Test
    public void ocrm435_checkCreateTicketWithAnOrder() {
        Email newEmail = new Email();
        Ticket ticket = new Ticket();
        String order = Orders.getPostpaidOrder().getMainProperties().getOrderNumber();

        // Создаём исходящее письмо
        newEmail
                .setSubject("Проверка создания обращений " + (new GregorianCalendar()).getTime().toString())
                .setTo("beru")
                .setText("простой текст сообщения для проверки создания обращения с заказ " + order)
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru");

        // Создаём тикет, который должен получиться
        ticket.setProperties(new Properties().setOrder(order));

        // Отправляем созданное исходящее письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "beru");

        // Открываем страницу обращения
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(newEmail.getSubject());

        // Со страницы обращения получаем свойства, тему, сообщения
        String orderNumberFromPage = Pages.ticketPage(webDriver).properties().getOrderNumber();

        Assert.assertEquals(order, orderNumberFromPage);
    }

    @InfoTest(
            descriptionTest = "Связанные обращения клиента",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1002",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-289"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1002_RelatedClientTickets() {
        // Сгенерировать темы писем и e-mail отправителя
        String date = Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm");
        String subject1 = "Автотест 1002 1 - " + date;
        String subject2 = "Автотест 1002 2 - " + date;
        String subject3 = "Автотест 1002 3 - " + date;
        String clientsEmail = Tools.date().generateCurrentDateAndTimeStringOfFormat("ddMMyyHHmm") + "@yandex.ru";

        // Создать письма, из которых будут сгенерированы обращения
        Email email1 = new Email();
        Email email2 = new Email();
        Email email3 = new Email();
        email1
                .setSubject(subject1)
                .setTo("beru")
                .setText("Я тикет для автотеста 1002 1. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(clientsEmail);
        email2
                .setSubject(subject2)
                .setTo("market")
                .setText("Я тикет для автотеста 1002 2. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(clientsEmail);
        email3
                .setSubject(subject3)
                .setTo("beruComplaints")
                .setText("Я тикет для автотеста 1002 3. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(clientsEmail);

        //Отправить письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(email1, "beru");
        PageHelper.otherHelper(webDriver).createTicketFromMail(email2, "market");
        PageHelper.otherHelper(webDriver).createTicketFromMail(email3, "beruComplaints");

        //Открыть одно из обращений
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(subject1);

        // Перейти на вкладку "Обращения клиента"
        Pages.ticketPage(webDriver).tabs().openTab("Обращения клиента");

        // Убедиться, что в таблице есть все 3 созданных обращения
        boolean ticketPresence1 = Tools.findElement(webDriver).findElements(By.xpath(String.format("//a[text()='%s']", subject1))).size() == 1;
        boolean ticketPresence2 = Tools.findElement(webDriver).findElements(By.xpath(String.format("//a[text()='%s']", subject2))).size() == 1;
        boolean ticketPresence3 = Tools.findElement(webDriver).findElements(By.xpath(String.format("//a[text()='%s']", subject3))).size() == 1;
        Assert.assertTrue("Не все обращения с одного e-mail'а отображаются во вкладке 'Обращения клиента'",
                ticketPresence1 && ticketPresence2 && ticketPresence3);

    }

    @InfoTest(
            descriptionTest = "Добавление связи между обращениями ЕО по gid",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1003",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-293"
    )
    @Ignore("Нужно понять как чинить")
    @Category({Blocker.class})
    @Test
    public void ocrm1003_LinkTicketUsingGid() {
        RelatedTicket expectedRelatedTicket = new RelatedTicket();

        // Создать обращение для создания связи
        Email email = new Email()
                .setSubject("ocrm1003 - " + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm"))
                .setTo("market")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.");
        // Отправляем письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(email, "market");

        // Наполнить переменную ожидаемыми значениями
        expectedRelatedTicket
                .setRelationType("Связан с (ведущий)")
                .setLink("")
                .setRelatedObject(email.getSubject())
                .setService("Маркет > Общий вопрос")
                .setStatus("Новый");

        // Получить gid созданного обращения
        String relatedTicketGid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", email.getSubject());

        // Найти в очереди "Покупки > Общие вопросы" любое обращение в статусе "Новый"
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket').withFilters{\n" +
                        "    eq('service','beruQuestion')\n" +
                        "    eq('status','registered')\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid);

        // Посмотреть, сколько у обращения связей
        Pages.ticketPage(webDriver).tabs().openTab("Обращения клиента");
        int relatedTicketsBefore = Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getNumberOfRows();

        // Нажать "Добавить связь"
        Pages.ticketPage(webDriver).header().clickLinkTicketButton();

        // Установить значения полей
        PageHelper.ticketPageHelper(webDriver).addRelatedTicket("Связан с (ведущий)", "Обращение",
                relatedTicketGid, email.getSubject());

        // Обновить страницу и перейти на вкладку "Обращения клиента"
        webDriver.navigate().refresh();
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        Pages.ticketPage(webDriver).tabs().openTab("Обращения клиента");

        // Убедиться, что обращение добавилось в табличку
        int relatedTicketsAfter = Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getNumberOfRows();
        Assert.assertTrue("Привязанное обращение не отображается в таблице связанных, либо в ней больше" +
                        " объектов, чем ожидалось",
                PageHelper.ticketPageHelper(webDriver).getAllRelatedTickets().contains(expectedRelatedTicket) &&
                        relatedTicketsAfter == relatedTicketsBefore + 1);

    }

    @InfoTest(
            descriptionTest = "Создание связи между обращением ЕО и тикетов ST с поиском по ключу",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1024",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-300"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1024_LinkTicketUsingKey() {
        RelatedTicket expectedRelatedTicket = new RelatedTicket();

        // Наполнить переменную ожидаемыми значениями
        expectedRelatedTicket
                .setRelationType("Связан с (ведущий)")
                .setLink("OCRM-6328")
                .setRelatedObject("OCRM-6328 [Автотест] Добавление связи между обращениями ЕО по gid")
                .setService("OCRM")
                .setStatus("Закрыт");

        // Создать обращение для создания связи
        Email email = new Email()
                .setSubject("ocrm1024 - " + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm"))
                .setTo("market")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.");
        // Отправляем письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(email, "market");

        // Получить gid созданного обращения
        String relatedTicketGid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", email.getSubject());
        Pages.navigate(webDriver).openPageByMetaClassAndID(relatedTicketGid);

        // Нажать "Добавить связь"
        Pages.ticketPage(webDriver).header().clickLinkTicketButton();

        // Установить значения полей
        PageHelper.ticketPageHelper(webDriver).addRelatedTicket("Связан с (ведущий)", "Тикет Startrek",
                expectedRelatedTicket.getLink(), expectedRelatedTicket.getRelatedObject());

        // Обновить страницу и перейти на вкладку "Обращения клиента"
        webDriver.navigate().refresh();
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        Pages.ticketPage(webDriver).tabs().openTab("Обращения клиента");

        // Убедиться, что обращение добавилось в табличку
        int relatedTickets = Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getNumberOfRows();
        Assert.assertTrue("Привязанное обращение не отображается в таблице связанных, либо в ней больше" +
                        " объектов, чем ожидалось",
                PageHelper.ticketPageHelper(webDriver).getAllRelatedTickets().contains(expectedRelatedTicket) &&
                        relatedTickets == 1);
    }

    @InfoTest(
            descriptionTest = "Добавление связи между обращениями ЕО по id",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1025",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-495"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1025_LinkTicketUsingId() {

        // Создать обращение для создания связи
        Email email = new Email()
                .setSubject("ocrm1025 - " + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm"))
                .setTo("beru")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.");

        // Отправляем письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(email, "beru");

        // Наполнить переменную ожидаемыми значениями
        RelatedTicket expectedRelatedTicket = new RelatedTicket()
                .setRelationType("Связан с (ведущий)")
                .setLink("")
                .setRelatedObject(email.getSubject())
                .setService("Покупки > Общие вопросы")
                .setStatus("Новый");

        // Получить gid созданного обращения
        String relatedTicketGid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", email.getSubject());

        // Преобразовать gid в id
        String id = relatedTicketGid.substring(relatedTicketGid.lastIndexOf("@") + 1);

        // Найти в очереди "Покупки > Общие вопросы" любое обращение в статусе "Новый"
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$beru').withFilters{\n" +
                        "    eq('service','beruQuestion')\n" +
                        "    eq('status','registered')\n" +
                        "    not(eq('metaclass','ticket$b2b'))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid);

        // Посмотреть, сколько у обращения связей
        Pages.ticketPage(webDriver).tabs().openTab("Обращения клиента");
        int relatedTicketsBefore = Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getNumberOfRows();

        // Нажать "Добавить связь"
        Pages.ticketPage(webDriver).header().clickLinkTicketButton();

        // Установить значения полей
        PageHelper.ticketPageHelper(webDriver).addRelatedTicket("Связан с (ведущий)", "Обращение",
                id, email.getSubject());

        // Обновить страницу и перейти на вкладку "Обращения клиента"
        webDriver.navigate().refresh();
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        Pages.ticketPage(webDriver).tabs().openTab("Обращения клиента");

        // Убедиться, что обращение добавилось в табличку
        int relatedTicketsAfter = Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getNumberOfRows();
        Assert.assertTrue(
                "Привязанное обращение не отображается в таблице связанных, либо в ней больше" +
                        " объектов, чем ожидалось",
                PageHelper.ticketPageHelper(webDriver).getAllRelatedTickets().contains(expectedRelatedTicket) &&
                        relatedTicketsAfter == relatedTicketsBefore + 1);
    }

    @InfoTest(
            descriptionTest = "Добавление связи между обращениями ЕО по названию",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1028",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-496"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1028_LinkTicketUsingTitle() {

        // Создать обращение для создания связи
        Email email = new Email()
                .setSubject("ocrm1028 - " + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm"))
                .setTo("beru")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.");

        // Отправляем письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(email, "beru");

        // Получить title созданного обращения
        String relatedTicketTitle = email.getSubject();

        // Наполнить переменную ожидаемыми значениями
        RelatedTicket expectedRelatedTicket = new RelatedTicket()
                .setRelationType("Связан с (ведущий)")
                .setLink("")
                .setRelatedObject(relatedTicketTitle)
                .setService("Покупки > Общие вопросы")
                .setStatus("Новый");

        // Найти в очереди "Покупки > Общие вопросы" любое обращение в статусе "Новый"
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$beru').withFilters{\n" +
                        "    eq('service','beruQuestion')\n" +
                        "    eq('status','registered')\n" +
                        "    not(eq('metaclass','ticket$b2b'))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid);

        // Посмотреть, сколько у обращения связей
        Pages.ticketPage(webDriver).tabs().openTab("Обращения клиента");
        int relatedTicketsBefore = Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getNumberOfRows();

        // Нажать "Добавить связь"
        Pages.ticketPage(webDriver).header().clickLinkTicketButton();

        // Установить значения полей
        PageHelper.ticketPageHelper(webDriver).addRelatedTicket("Связан с (ведущий)", "Обращение",
                relatedTicketTitle, relatedTicketTitle);

        Tools.waitElement(webDriver).waitTime(5000);

        // Обновить страницу и перейти на вкладку "Обращения клиента"
        webDriver.navigate().refresh();
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        Pages.ticketPage(webDriver).tabs().openTab("Обращения клиента");

        // Убедиться, что обращение добавилось в табличку
        int relatedTicketsAfter = Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getNumberOfRows();
        Assert.assertTrue(
                "Привязанное обращение не отображается в таблице связанных, либо в ней больше" +
                        " объектов, чем ожидалось",
                PageHelper.ticketPageHelper(webDriver).getAllRelatedTickets().contains(expectedRelatedTicket) &&
                        relatedTicketsAfter == relatedTicketsBefore + 1);
    }

    @InfoTest(
            descriptionTest = "Создание связи между обращением ЕО и тикетов ST с поиском по названию",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1029",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-494"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1029_LinkTicketUsingTitle() {
        // Переменная с названием тикета ST
        String starTrekTicketTitle = "Избавиться от зависимости уровня запроса";

        // Наполнить переменную ожидаемыми значениями
        RelatedTicket expectedRelatedTicket = new RelatedTicket()
                .setRelationType("Связан с (ведущий)")
                .setLink("OCRM-1111")
                .setRelatedObject("OCRM-1111 Избавиться от зависимости уровня запроса")
                .setService("OCRM")
                .setStatus("Закрыт");

        // Создать обращение для создания связи
        Email email = new Email()
                .setSubject("ocrm1028 - " + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm"))
                .setTo("beru")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.");

        // Отправляем письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(email, "beru");

        // Получить gid созданного обращения
        String ticketGid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", email.getSubject());

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid);

        // Нажать "Добавить связь"
        Pages.ticketPage(webDriver).header().clickLinkTicketButton();

        // Установить значения полей
        PageHelper.ticketPageHelper(webDriver).addRelatedTicket("Связан с (ведущий)", "Тикет Startrek",
                starTrekTicketTitle, expectedRelatedTicket.getRelatedObject());

        // Обновить страницу и перейти на вкладку "Обращения клиента"
        webDriver.navigate().refresh();
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        Pages.ticketPage(webDriver).tabs().openTab("Обращения клиента");

        // Убедиться, что тикет добавился в табличку
        int relatedTickets = Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getNumberOfRows();
        Assert.assertTrue(
                "Привязанный тикет не отображается в таблице связанных, либо в ней больше" +
                        " объектов, чем ожидалось",
                PageHelper.ticketPageHelper(webDriver).getAllRelatedTickets().contains(expectedRelatedTicket) &&
                        relatedTickets == 1);
    }

    @InfoTest(descriptionTest = "Определение партнера по заголовку x-market-shopid",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-964",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1152")
    @Category({Blocker.class})
    @Test
    public void ocrm1152_DetermineShopUsingXMarketShopIDHeader() {
        // Сгенерировать тему
        String subject = "Автотест ocrm-1152 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();
        String gid = "null";
        // Получить gid белого магазина с shop id 196213
        String expectedPartner = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('account$shop').withFilters{ eq('shopId','196213')}.get()");
        String partnerFromPage;

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> replays = new HashMap<>();
        replays.put("x-market-shopid", "196213");
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("b2b")
                .setHeaders(replays);
        // Отправляем созданное письмо
        Tools.email().sendAnEmail(newEmail);

        // Дождаться получения письма
        for (int i = 0; i < 20; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "api.db.of('ticket$b2b').withFilters{\n" +
                            "eq('title','"+subject+"')\n" +
                            "between('registrationDate',"+Tools.date().getDateRangeForSeveralMonths(2,0)+")}\n" +
                            ".limit(1)\n" +
                            ".get()");
            if (gid != null) break;
        }

        // Получить партнера из обращения
        partnerFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').partner", gid));

        Assert.assertEquals("По заголовку x-market-shopid определился неверный партнер.",
                expectedPartner, partnerFromPage);
    }

    @InfoTest(descriptionTest = "Определение партнера по заголовку x-market-supplierid",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1058",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1153")
    @Category({Blocker.class})
    @Test
    public void ocrm1153_DetermineSupplierUsingXMarketSupplierIDHeader() {
        // Сгенерировать тему
        String subject = "Автотест ocrm-1153 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();
        String gid = "null";
        // Получить gid синего магазина с supplier id 10436600
        String expectedPartner = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('account$supplier').withFilters{ eq('supplierId','10436600')}.get()");
        String partnerFromPage;

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> replays = new HashMap<>();
        replays.put("x-market-supplierid", "10436600");
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("b2b")
                .setHeaders(replays);
        // Отправляем созданное письмо
        Tools.email().sendAnEmail(newEmail);

        // Дождаться получения письма
        for (int i = 0; i < 20; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Получить партнера из обращения
        partnerFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').partner", gid));

        Assert.assertEquals("По заголовку x-market-supplierid определился неверный партнер.",
                expectedPartner, partnerFromPage);
    }

    @InfoTest(descriptionTest = "Определение партнера по заголовку x-market-vendorid",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1059",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1154")
    @Category({Blocker.class})
    @Test
    public void ocrm1154_DetermineVendorUsingXMarketVendorIDHeader() {
        // Сгенерировать тему
        String subject = "Автотест ocrm-1154 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();
        String gid = "null";
        // Получить gid синего магазина с supplier id 10436600
        String expectedPartner = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('account$vendor').withFilters{ eq('vendorId','5')}.get()");
        String partnerFromPage;

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> replays = new HashMap<>();
        replays.put("x-market-vendorid", "5");
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("b2b")
                .setHeaders(replays);
        // Отправляем созданное письмо
        Tools.email().sendAnEmail(newEmail);

        // Дождаться получения письма
        for (int i = 0; i < 20; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "api.db.of('ticket$b2b').withFilters{\n" +
                            "eq('title','"+subject+"')\n" +
                            "between('registrationDate',"+Tools.date().getDateRangeForSeveralMonths(2,0)+")}\n" +
                            ".limit(1)\n" +
                            ".get()");
            if (gid != null) break;
        }

        // Получить партнера из обращения
        partnerFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').partner", gid));

        Assert.assertEquals("По заголовку x-market-vendorid определился неверный партнер.",
                expectedPartner, partnerFromPage);
    }

    @InfoTest(descriptionTest = "Если не удалось определить партнера - не указывать его",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1071",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1156")
    @Category({Blocker.class})
    @Test
    public void ocrm1156_DontSetPartnerIfCannotDetermine() {
        // Сгенерировать тему
        String subject = "Автотест ocrm-1156 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();
        String gid = "null";

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> replays = new HashMap<>();
        replays.put("x-market-supplierid", "1702211612");
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("b2b")
                .setHeaders(replays);
        // Создать письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Проверить, что партнер не определился
        String s = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').partner", gid));

        Assert.assertNull("В обращении автоматически определился партнер, хотя не должен был.", s);
    }

    @InfoTest(descriptionTest = "Определение партнера по контакту, когда не удалось определить по X-заголовку",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1068",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1166")
    @Category({Blocker.class})
    @Test
    public void ocrm1166_UseContactToSetPartnerWhenCannotDetermineByXHeader() {
        String newEmails;
        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";

        // Сгенерировать тему
        String subject = "Автотест ocrm-1166 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> replays = new HashMap<>();
        replays.put("x-market-supplierid", "1802211636");
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress)
                .setTo("b2b")
                .setHeaders(replays);

        // Получить контакт, имеющий только 1 связь только с 1 партнером
        String contactGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def contactId = api.db.query('''\n" +
                        "SELECT r.contact.id\n" +
                        "FROM b2bAccountContactRelation r\n" +
                        "GROUP BY r.contact\n" +
                        "HAVING count(r.contact) = 1\n" +
                        "''').limit(1).get()\n" +
                        "\n" +
                        "api.db.of('b2bContact').get(contactId)"
        );

        // Получить список e-mail'ов контакта
        String contactEmails = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format(
                        "api.db.get('%s').emails",
                        contactGid)
        );
        // Получить связанного с контактом партнера
        String expectedAccount = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format(
                        "def obj = api.db.get('%s').accountContactRelations\n" +
                                "\n" +
                                "obj.account",
                        contactGid)
        );
        expectedAccount = expectedAccount.replaceAll("\\[|]", "");

        // Добавить тестовый e-mail в e-mail'ы контакта
        if (contactEmails.equals("[null]")) {
            newEmails = emailAddress;
        } else {
            newEmails = contactEmails.substring(0, contactEmails.length() - 1) + ", " + emailAddress + "]";
        }
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format("api.bcp.edit('%s', ['emails': '%s'])",
                        contactGid, newEmails)
        );

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Получить партнера из созданного обращения
        String accountFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').partner", gid));

        // Вернуть список e-mail'ов контакта в исходное состояние
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format("api.bcp.edit('%s', ['emails': '%s'])",
                        contactGid, contactEmails)
        );

        Assert.assertEquals("Партнер определился неверно", expectedAccount, accountFromPage);
    }

    @InfoTest(descriptionTest = "Определение партнера по контакту: несколько связей с 1 партнером",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1069",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1221")
    @Category({Critical.class})
    @Test
    public void ocrm1221_UseContactToSetPartnerWhenTwoConnectionsWithOneAccount() {
        String newEmails;
        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";

        // Сгенерировать тему
        String subject = "Автотест ocrm-1221 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress)
                .setTo("b2b");

        // Получить контакт, имеющий 2 связи только с 1 партнером
        String contactGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def contactId = api.db.query('''\n" +
                        "SELECT r.contact.id\n" +
                        "FROM b2bAccountContactRelation r\n" +
                        "GROUP BY r.contact\n" +
                        "HAVING count(r.contact) = 2\n" +
                        "''').limit(1).get()\n" +
                        "\n" +
                        "api.db.of('b2bContact').get(contactId)"
        );

        // Получить список e-mail'ов контакта
        String contactEmails = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format(
                        "api.db.get('%s').emails",
                        contactGid)
        );
        // Получить связанного с контактом партнера
        String expectedAccount = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format(
                        "def obj = api.db.get('%s').accountContactRelations\n" +
                                "\n" +
                                "obj.account",
                        contactGid)
        );
        expectedAccount = expectedAccount.substring(1, expectedAccount.indexOf(","));

        // Добавить тестовый e-mail в e-mail'ы контакта
        if (contactEmails.equals("[]")) {
            newEmails = emailAddress;
        } else {
            newEmails = contactEmails.substring(0, contactEmails.length() - 1) + ", " + emailAddress + "]";
        }
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format("api.bcp.edit('%s', ['emails': '%s'])",
                        contactGid, newEmails)
        );

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Получить партнера из созданного обращения
        String accountFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').partner", gid));

        // Вернуть список e-mail'ов контакта в исходное состояние
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format("api.bcp.edit('%s', ['emails': '%s'])",
                        contactGid, contactEmails)
        );

        Assert.assertEquals("Партнер определился неверно", expectedAccount, accountFromPage);
    }

    @InfoTest(descriptionTest = "Определение партнера по контакту, когда X-заголовка нет",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1066",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1246")
    @Category({Blocker.class})
    @Test
    public void ocrm1246_UseContactToSetPartnerWhenNoXHeader() {
        String newEmails;
        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";

        // Сгенерировать тему
        String subject = "Автотест ocrm-1246 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress)
                .setTo("b2b");

        // Получить контакт, имеющий только 1 связь только с 1 партнером
        String contactGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def contactId = api.db.query('''\n" +
                        "SELECT r.contact.id\n" +
                        "FROM b2bAccountContactRelation r\n" +
                        "GROUP BY r.contact\n" +
                        "HAVING count(r.contact) = 1\n" +
                        "''').limit(1).get()\n" +
                        "\n" +
                        "api.db.of('b2bContact').get(contactId)"
        );

        // Получить список e-mail'ов контакта
        String contactEmails = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format(
                        "api.db.get('%s').emails",
                        contactGid)
        );
        // Получить связанного с контактом партнера
        String expectedAccount = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format(
                        "def obj = api.db.get('%s').accountContactRelations\n" +
                                "\n" +
                                "obj.account",
                        contactGid)
        );
        expectedAccount = expectedAccount.replaceAll("\\[|]", "");

        // Добавить тестовый e-mail в e-mail'ы контакта
        if (contactEmails.equals("[]")) {
            newEmails = emailAddress;
        } else {
            newEmails = contactEmails.substring(0, contactEmails.length() - 1) + ", " + emailAddress + "]";
        }
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format("api.bcp.edit('%s', ['emails': '%s'])",
                        contactGid, newEmails)
        );

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Получить партнера из созданного обращения
        String accountFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').partner", gid));

        // Вернуть список e-mail'ов контакта в исходное состояние
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                String.format("api.bcp.edit('%s', ['emails': '%s'])",
                        contactGid, contactEmails)
        );

        Assert.assertEquals("Партнер определился неверно", expectedAccount, accountFromPage);
    }

    @InfoTest(descriptionTest = "Определение партнера по контакту: несколько контактов 1 партнера",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1070",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1247")
    @Category({Critical.class})
    @Test
    public void ocrm1247_UseContactToSetPartnerWhenMultipleContactsOfOnePartner() {
        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";

        // Сгенерировать тему
        String subject = "Автотест ocrm-1247 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress)
                .setTo("b2b");

        // Создать партнера
        String expectedAccount = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.bcp.create('account$supplier', [\n" +
                        "  'title': '%s',\n" +
                        "  'supplierId': '%s'\n" +
                        "])",
                subject,
                Tools.date().generateCurrentDateAndTimeStringOfFormat("YYMMddHHMMSS")
        ));
        // Создать 2 контакта, добавить им тестовый email и связь с партнером
        for (int i = 0; i < 2; i++) {
            String contactGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                    "api.bcp.create('b2bContact', [\n" +
                            "  'title': '%s',\n" +
                            "  'emails': ['%s'],\n" +
                            "  'sourceSystem': 'MBI'\n" +
                            "])",
                    subject + i, emailAddress));
            PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                    "api.bcp.create('b2bAccountContactRelation', [\n" +
                            "  'account': '%s',\n" +
                            "  'contact': '%s',\n" +
                            "  'contactRole': 'SHOP_OPERATOR',\n" +
                            "  'sourceSystem': 'MBI'" +
                            "])",
                    expectedAccount, contactGid));
        }

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Получить партнера из созданного обращения
        String accountFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').partner", gid));

        Assert.assertEquals("Партнер определился неверно", expectedAccount, accountFromPage);
    }

    @InfoTest(
            descriptionTest = "Поиск партнера в поле по названию - shop",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1383",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1075"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1383_FindPartnerInThePartnerFieldByShopName() {

        // Получить тикет из очереди Маркет.API: Affiliate без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Ввести имя магазина в поле "Партнёр"
        Pages.ticketPage(webDriver).properties().setPartner(Partners.shopName);

        // Сохранить изменения
        PageHelper.ticketPageHelper(webDriver).saveChangesToTicket();

        // Убедиться, что в поле "Партнёр" подставился нужный партнёр
        Assert.assertEquals("В поле Партнёр подставился другой партнёр", Partners.shopName, Pages.ticketPage(webDriver).properties().getPartner());
    }

    @InfoTest(
            descriptionTest = "Поиск партнера в поле по названию - supplier",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1384",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1076"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1384_FindPartnerInThePartnerFieldBySupplierName() {

        // Получить тикет из очереди Маркет.API: Affiliate без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Ввести имя поставщика в поле "Партнёр"
        Pages.ticketPage(webDriver).properties().setPartner(Partners.supplierName);

        // Сохранить изменения
        PageHelper.ticketPageHelper(webDriver).saveChangesToTicket();

        // Убедиться, что в поле "Партнёр" подставился нужный партнёр
        Assert.assertEquals("В поле Партнёр подставился другой партнёр", Partners.supplierName, Pages.ticketPage(webDriver).properties().getPartner());
    }

    @InfoTest(
            descriptionTest = "Поиск партнера в поле по названию - vendor",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1385",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1077"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1385_FindPartnerInThePartnerFieldByVendorName() {

        // Получить тикет из очереди Маркет.API: Affiliate без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Ввести имя вендора в поле "Партнёр"
        Pages.ticketPage(webDriver).properties().setPartner(Partners.vendorName);

        // Сохранить изменения
        PageHelper.ticketPageHelper(webDriver).saveChangesToTicket();

        // Убедиться, что в поле "Партнёр" подставился нужный партнёр
        Assert.assertEquals("В поле Партнёр подставился другой партнёр", Partners.vendorName, Pages.ticketPage(webDriver).properties().getPartner());
    }

    @InfoTest(
            descriptionTest = "Поиск партнера в поле по id - shop",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1386",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1078"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1386_FindPartnerInThePartnerFieldByShopId() {

        // Получить тикет из очереди Маркет.API: Affiliate без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Ввести id магазина в поле "Партнёр"
        Pages.ticketPage(webDriver).properties().setPartnerId(Partners.shopId, Partners.shopName);

        // Сохранить изменения
        PageHelper.ticketPageHelper(webDriver).saveChangesToTicket();

        // Убедиться, что в поле "Партнёр" подставился нужный партнёр
        Assert.assertEquals("В поле Партнёр подставился другой партнёр", Partners.shopName, Pages.ticketPage(webDriver).properties().getPartner());
    }

    @InfoTest(
            descriptionTest = "Поиск партнера в поле по id - supplier",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1387",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1079"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1387_FindPartnerInThePartnerFieldBySupplierId() {

        // Получить тикет из очереди Маркет.API: Affiliate без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Ввести id поставщика в поле "Партнёр"
        Pages.ticketPage(webDriver).properties().setPartnerId(Partners.supplierId, Partners.supplierName);

        // Сохранить изменения
        PageHelper.ticketPageHelper(webDriver).saveChangesToTicket();

        // Убедиться, что в поле "Партнёр" подставился нужный партнёр
        Assert.assertEquals("В поле Партнёр подставился другой партнёр", Partners.supplierName, Pages.ticketPage(webDriver).properties().getPartner());
    }

    @InfoTest(
            descriptionTest = "Поиск партнера в поле по id - vendor",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1388",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1080"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1388_FindPartnerInThePartnerFieldByVendorId() {

        // Получить тикет из очереди Маркет.API: Affiliate без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('service','marketApiAffiliate')\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Ввести id вендора в поле "Партнёр"
        Pages.ticketPage(webDriver).properties().setPartnerId(Partners.vendorId, Partners.vendorName);

        // Сохранить изменения
        PageHelper.ticketPageHelper(webDriver).saveChangesToTicket();

        // Убедиться, что в поле "Партнёр" подставился нужный партнёр
        Assert.assertEquals("В поле Партнёр подставился другой партнёр", Partners.vendorName, Pages.ticketPage(webDriver).properties().getPartner());
    }
}
