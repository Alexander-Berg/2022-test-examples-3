package ui_tests.src.test.java.tests.testImportConfiguration;

import Classes.Comment;
import Classes.Import;
import Classes.ticket.Properties;
import Classes.ticket.Ticket;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Critical;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestsMassActionsWithTickets {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();

    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsMassActionsWithTickets.class);

    @Test
    @InfoTest(
            descriptionTest = "Проверка 'Создание исходящих обращений телефонии по номеру заказа и внутреннему комментарию'",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-941",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1048"
    )
    @Category({Critical.class})
    public void ocrm1048_CreatingOutgoingTelephonyRequestsByOrderNumberAndInternalComment() {
        //Получаю 2 заказа для импорта
        String[] orders = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{\n" +
                "eq('status','PROCESSING')\n" +
                "between('creationDate'," + Tools.date().getDateRangeForSeveralMonths(5) + ")\n" +
                "not(eq('recipientPhone',null))\n" +
                "}\n" +
                ".withOrders(api.db.orders.desc('creationDate'))\n" +
                ".limit(2)\n" +
                ".list()\n" +
                ".title").replaceAll("[\\[\\]]", "").split(", ");
        //Создаю комментарии для заказов
        String firstComment = Tools.other().getRandomText() + " order 1";
        String secondComment = Tools.other().getRandomText() + " order 2";
        //создаем файл для импорта
        String filePath = Tools.file()
                .createFile(Tools.other().getRandomText() + "tests.csv", "Номер заказа;Внутренний комментарий\n" +
                        orders[0] + ";" + firstComment + " \n" +
                        orders[1] + ";" + secondComment);
        // создаем запись импорта
        Import myImport = new Import()
                .setFilePath(filePath)
                .setServiceTitle("Покупки > Исходящие звонки");
        // Создаем эталонные обращения
        Ticket firstExistTicket = new Ticket()
                .setComments(Arrays.asList(
                        new Comment()
                                .setText(firstComment)
                                .setType("internal")
                                .setNameAndEmail("Система")))
                .setProperties(new Properties()
                        .setService("Покупки > Исходящие звонки")
                        .setOrder(orders[0])
                        .setContactPhoneNumber(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{\n" +
                                "eq('title','" + orders[0] + "')\n" +
                                "}\n" +
                                ".get()\n" +
                                ".recipientPhone").replaceAll("\\D", "").substring(1)));

        Ticket secondExistTicket = new Ticket()
                .setComments(Arrays.asList(
                        new Comment()
                                .setText(secondComment)
                                .setType("internal")
                                .setNameAndEmail("Система")))
                .setProperties(new Properties()
                        .setService("Покупки > Исходящие звонки")
                        .setOrder(orders[1])
                        .setContactPhoneNumber(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{\n" +
                                "eq('title','" + orders[1] + "')\n" +
                                "}\n" +
                                ".get()\n" +
                                ".recipientPhone").replaceAll("\\D", "").substring(1)));

        //Создаем импорт
        PageHelper.dataImportHelper(webDriver).createImportBeruOutgoingCallTicketByOrderId(myImport);

        // ищем и получаем данные со страницы по созданным обращениям
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt("Исходящий звонок по заказу №" + orders[0]);
        Pages.ticketPage(webDriver).tabs().openTab("Сообщения");
        Ticket firstTicketFromPage = new Ticket()
                .setComments(Pages.ticketPage(webDriver).messageTab().comments().getComments())
                .setProperties(new Properties()
                        .setOrder(Pages.ticketPage(webDriver).properties().getOrderNumber())
                        .setService(Pages.ticketPage(webDriver).properties().getService())
                        .setContactPhoneNumber(Pages.ticketPage(webDriver).properties().getContactPhoneNumber()));

        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt("Исходящий звонок по заказу №" + orders[1]);
        Pages.ticketPage(webDriver).tabs().openTab("Сообщения");
        Ticket secondTicketFromPage = new Ticket()
                .setComments(Pages.ticketPage(webDriver).messageTab().comments().getComments())
                .setProperties(new Properties()
                        .setOrder(Pages.ticketPage(webDriver).properties().getOrderNumber())
                        .setService(Pages.ticketPage(webDriver).properties().getService())
                        .setContactPhoneNumber(Pages.ticketPage(webDriver).properties().getContactPhoneNumber()));
        //Проверяем созданные обращения с эталонными
        boolean b1 = firstExistTicket.equals(firstTicketFromPage);
        boolean b2 = secondExistTicket.equals(secondTicketFromPage);

        Assert.assertTrue("У созданных обращений вывелись не те данные которые ожидаем. \n" +
                Tools.differ().format("Первое обращение не равно ожидаемому", firstExistTicket, firstTicketFromPage) +
                Tools.differ().format("второе обращение не равно ожидаемому", secondExistTicket, secondTicketFromPage), b1 & b2);

    }

    @Test
    @InfoTest(
            descriptionTest = "Создание исходящих обращений по e-mail и внешнему комментарию'",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-945",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1054"
    )
    @Category({Critical.class})
    public void ocrm1054_CreatingOutgoingTicketsByEmailAndExternalComments() {

        String firstComment = Tools.other().getRandomText() + " Ticket 1";
        String secondComment = Tools.other().getRandomText() + " Ticket 2";
        String firstEmail = Tools.other().getRandomText() + "@yandex.ru";
        String secondEmail = Tools.other().getRandomText() + "@yandex.ru";
        //создаем файл для импорта
        String filePath = Tools.file()
                .createFile(Tools.other().getRandomText() + "tests.csv", "email;Внешний комментарий\n" +
                        firstEmail + ";" + firstComment + "\n" +
                        secondEmail + ";" + secondComment + "\n");
        // создаем запись импорта
        Import myImport = new Import()
                .setFilePath(filePath)
                .setServiceTitle("Покупки > Общие вопросы");
        // Создаем эталонные обращения
        Ticket firstExistTicket = new Ticket()
                .setComments(Arrays.asList(
                        new Comment()
                                .setText(firstComment)
                                .setType("public")
                                .setNameAndEmail("Система")))
                .setProperties(new Properties()
                        .setService("Покупки > Общие вопросы")
                        .setContactEmail(firstEmail)
                        .setCategory(Arrays.asList("15-30-60")));

        Ticket secondExistTicket = new Ticket()
                .setComments(Arrays.asList(
                        new Comment()
                                .setText(secondComment)
                                .setType("public")
                                .setNameAndEmail("Система")))
                .setProperties(new Properties()
                        .setService("Покупки > Общие вопросы")
                        .setContactEmail(secondEmail)
                        .setCategory(Arrays.asList("15-30-60")));

        //Создаем импорт
        PageHelper.dataImportHelper(webDriver).createImportBeruOutgoingTicketByEmail(myImport, Arrays.asList("15-30-60"));

        // ищем и получаем данные со страницы по созданным обращениям
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt("Обращение (" + firstEmail + ")");
        Pages.ticketPage(webDriver).tabs().openTab("Сообщения");
        Ticket firstTicketFromPage = new Ticket()
                .setComments(Pages.ticketPage(webDriver).messageTab().comments().getComments())
                .setProperties(new Properties()
                        .setService(Pages.ticketPage(webDriver).properties().getService())
                        .setContactEmail(Pages.ticketPage(webDriver).properties().getContactEmail())
                        .setCategory(Pages.ticketPage(webDriver).properties().getCategory()));

        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt("Обращение (" + secondEmail + ")");
        Pages.ticketPage(webDriver).tabs().openTab("Сообщения");
        Ticket secondTicketFromPage = new Ticket()
                .setComments(Pages.ticketPage(webDriver).messageTab().comments().getComments())
                .setProperties(new Properties()
                        .setService(Pages.ticketPage(webDriver).properties().getService())
                        .setContactEmail(Pages.ticketPage(webDriver).properties().getContactEmail())
                        .setCategory(Pages.ticketPage(webDriver).properties().getCategory()));
        //Проверяем созданные обращения с эталонными
        boolean b1 = firstExistTicket.equals(firstTicketFromPage);
        boolean b2 = secondExistTicket.equals(secondTicketFromPage);

        Assert.assertTrue("У созданных обращений вывелись не те данные которые ожидаем. \n" +
                Tools.differ().format("Первое обращение не равно ожидаемому", firstExistTicket, firstTicketFromPage) +
                Tools.differ().format("\nвторое обращение не равно ожидаемому", secondExistTicket, secondTicketFromPage), b1 & b2);
    }

    @Test
    @InfoTest(descriptionTest = "Создание импорта 'Создание исходящих обращений по номеру заказа и внешнему комментарию '",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-947",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1127")
    @Category(Critical.class)
    public void ocrm1127_CreateImportBeruOutgoingTicketByOrderId() {
        List<Ticket> expectedTickets = new ArrayList<>();
        List<Ticket> ticketsFromPage = new ArrayList<>();
        // получаю два рандомных заказа
        String[] orders = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("" +
                "def orders = api.db.of('order').withFilters{\n" +
                String.format("between ('creationDate',%s)\n", Tools.date().getDateRangeForSeveralMonths(Tools.other().getRandomNumber(1, 6))) + "\n" +
                "}.limit(2).list().title").replaceAll("[\\[\\]]", "").split(", ");
        //Создаю эталонные обращения
        for (String order : orders) {
            Ticket ticket = new Ticket()
                    .setProperties(new Properties()
                            .setOrder(order)
                            .setCategory(Arrays.asList("15-30-60"))
                            .setService("Покупки > Общие вопросы"))
                    .setComments(Arrays.asList(new Comment()
                            .setNameAndEmail("Система")
                            .setType("public")
                            .setText(Tools.other().getRandomText())));
            expectedTickets.add(ticket);
        }

        //создаем файл для импорта
        String filePath = Tools.file()
                .createFile(Tools.other().getRandomText() + "tests.csv", "Номер заказа;Внешний комментарий\n" +
                        expectedTickets.get(0).getProperties().getOrder() + ";" + expectedTickets.get(0).getComments().get(0).getText() + "\n" +
                        expectedTickets.get(1).getProperties().getOrder() + ";" + expectedTickets.get(1).getComments().get(0).getText() + "\n");
        // создаем импорт
        Import myImport = new Import()
                .setFilePath(filePath)
                .setCategories(Arrays.asList("15-30-60"))
                .setServiceTitle("Покупки > Общие вопросы");

        PageHelper.dataImportHelper(webDriver).createImportBeruOutgoingTicketByOrderId(myImport);
        // Получаем данные по созданным обращениям
        for (Ticket ticket : expectedTickets) {
            Ticket ticketFromPage = new Ticket();
            String gid = PageHelper.otherHelper(webDriver)
                    .findEntityByTitle("ticket", "Обращение по заказу №" + ticket.getProperties().getOrder());

            Pages.navigate(webDriver).openPageByMetaClassAndID(gid);

            ticketFromPage
                    .setComments(Pages.ticketPage(webDriver).messageTab().comments().getComments())
                    .setProperties(new Properties()
                            .setOrder(Pages.ticketPage(webDriver).properties().getOrderNumber())
                            .setCategory(Pages.ticketPage(webDriver).properties().getCategory())
                            .setService(Pages.ticketPage(webDriver).properties().getService()));
            ticketsFromPage.add(ticketFromPage);
        }
        Assert.assertTrue(Tools.differ().format("Данные в созданных обращениях не равны тем, которые мы ожидали", expectedTickets, ticketsFromPage), expectedTickets.equals(ticketsFromPage));

    }

    @Test
    @InfoTest(descriptionTest = "Проверка импорта 'Создание обращений по ручным задачам'",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-948",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1128")
    @Category(Critical.class)
    public void ocrm1128_CreateImportOrderTicketsManualCreation() {
        List<Ticket> expectedTicket = new ArrayList<>();
        List<Ticket> ticketFromPage = new ArrayList<>();

        // Получаем 4 заказа из системы
        String[] orders = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def orders = api.db.of('order').withFilters{\n" +
                        "between('creationDate'," + Tools.date().getDateRangeForSeveralMonths(Tools.other().getRandomNumber(1, 5)) + ")\n" +
                        "}.limit(4).list().title").replaceAll("[\\[\\]]", "").split(", ");
        String textComment = Tools.other().getRandomText();
        // Создаем эталонные обращения
        for (String order : orders) {
            Ticket ticket = new Ticket()
                    .setComments(Arrays.asList(new Comment()
                            .setText(textComment)
                            .setType("internal")
                            .setNameAndEmail("Система")))
                    .setProperties(new Properties()
                            .setOrder(order)
                            .setService("Покупки > Другие задачи"));
            expectedTicket.add(ticket);
        }
        // создаем импорт
        String filePath = Tools.file()
                .createFile(Tools.other().getRandomText() + "tests.csv", "ID заказа в Маркете\n" +
                        orders[0] + "\n" +
                        orders[1] + "\n" +
                        orders[2] + "\n" +
                        orders[3] + "\n");

        Import myImport = new Import()
                .setFilePath(filePath)
                .setCommentText(textComment)
                .setServiceTitle("Покупки > Другие задачи");

        PageHelper.dataImportHelper(webDriver).createImportOrderTicketsManualCreation(myImport);

        //Получаем данные по созданным обращений
        for (String order : orders) {
            String gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", "Задача по заказу №" + order);
            Pages.navigate(webDriver).openPageByMetaClassAndID(gid);
            Pages.ticketPage(webDriver).tabs().openTab("Сообщения");
            Ticket ticket = new Ticket()
                    .setComments(Pages.ticketPage(webDriver).messageTab().comments().getComments())
                    .setProperties(new Properties()
                            .setService(Pages.ticketPage(webDriver).properties().getService())
                            .setOrder(Pages.ticketPage(webDriver).properties().getOrderNumber()));
            ticketFromPage.add(ticket);
        }

        Assert.assertTrue(
                Tools.differ().format("", expectedTicket, ticketFromPage),
                expectedTicket.equals(ticketFromPage));
    }

    @Test
    @InfoTest(
            descriptionTest = "Проверка импорта 'Смена статуса с добавлением комментария'",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-949",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1129"
    )
    @Category(Critical.class)
    public void ocrm1129_CreateImportTicketChangeStatusAddComment() {

        List<Ticket> ticketsFromPage = new ArrayList<>();
        List<Ticket> expectedTickets = new ArrayList<>();

        String[] tickets = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def orders = api.db.of('ticket').withFilters{\n" +
                        "eq('status','registered')\n" +
                        "    eq('service','beruQuestion')\n" +
                        "not(eq('metaclass','ticket$qualityManagementTicket'))\n" +
                        "}.limit(2).list().id\n" +
                        "\n" +
                        "orders += api.db.of('ticket').withFilters{\n" +
                        "eq('status','reopened')\n" +
                        "    eq('service','beruQuestion')\n" +
                        "  not(eq('metaclass','ticket$qualityManagementTicket'))\n" +
                        "}.limit(2).list().id").replaceAll("[\\[\\]]", "").split(", ");

        for (int i = 0; i < 4; i++) {
            Comment internalComment = new Comment()
                    .setText(Tools.other().getRandomText())
                    .setType("internal")
                    .setNameAndEmail("Система");
            Comment publicComment = new Comment()
                    .setText(Tools.other().getRandomText())
                    .setType("public")
                    .setNameAndEmail("Система");
            Ticket ticket = new Ticket()
                    .setComments(Arrays.asList(internalComment, publicComment))
                    .setProperties(new Properties()
                            .setStatus("В работе"));
            expectedTickets.add(ticket);
        }

        //создаем файл для импорта
        String filePath = Tools.file()
                .createFile(Tools.other().getRandomText() + "tests.csv", "Номер обращения;Статус;Комментарий для клиента;Внутренний комментарий\n" +
                        tickets[0] + ";processing;" + expectedTickets.get(0).getComments().get(1).getText() + ";" + expectedTickets.get(0).getComments().get(0).getText() + "\n" +
                        tickets[1] + ";processing;" + expectedTickets.get(1).getComments().get(1).getText() + ";" + expectedTickets.get(1).getComments().get(0).getText() + "\n" +
                        tickets[2] + ";processing;" + expectedTickets.get(2).getComments().get(1).getText() + ";" + expectedTickets.get(2).getComments().get(0).getText() + "\n" +
                        tickets[3] + ";processing;" + expectedTickets.get(3).getComments().get(1).getText() + ";" + expectedTickets.get(3).getComments().get(0).getText() + "\n");

        Import myImport = new Import().setFilePath(filePath);

        PageHelper.dataImportHelper(webDriver).createImportTicketChangeStatusAddComment(myImport);

        for (int i = 0; i < 4; i++) {
            Pages.navigate(webDriver).openPageByMetaClassAndID("ticket@" + tickets[i]);
            Pages.ticketPage(webDriver).tabs().openTab("Сообщения");
            Ticket ticketFromPage = new Ticket()
                    .setComments(Pages.ticketPage(webDriver).messageTab().comments().getComments())
                    .setProperties(new Properties().setStatus(Pages.ticketPage(webDriver).properties().getStatus()));
            ticketsFromPage.add(ticketFromPage);
        }

        Assert.assertTrue(Tools.differ().format("Данные в созданных обращениях не равны тем, которые мы ожидали", expectedTickets, ticketsFromPage), expectedTickets.equals(ticketsFromPage));
    }

    @Test
    @InfoTest(descriptionTest = "Создание исходящих обращений телефонии по номеру телефона и внутреннему комментарию",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-946",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1155")
    @Category(Critical.class)
    public void ocrm1155_CreateImportBeruOutgoingCallTicketByClientPhone() {

        List<Ticket> ticketsFromPage = new ArrayList<>();
        List<Ticket> expectedTickets = new ArrayList<>();
        //Создаем обращения которые должны получиться
        for (int i = 0; i < 2; i++) {
            Ticket ticket = new Ticket();
            ticket
                    .setProperties(new Properties()
                            .setContactPhoneNumber(String.valueOf(Tools.other().getRandomNumber(1000000000, 1000099999)))
                            .setService("Покупки > Исходящие звонки"))
                    .setComments(Arrays.asList(new Comment()
                            .setType("internal")
                            .setNameAndEmail("Система")
                            .setText(Tools.other().getRandomText() + " text " + i)));

            expectedTickets.add(ticket);
        }
        //создаем файл для импорта
        String filePath = Tools.file()
                .createFile(Tools.other().getRandomText() + "tests.csv", "Номер телефона;Внутренний комментарий\n" +
                        "7" + expectedTickets.get(0).getProperties().getContactPhoneNumber() + ";" + expectedTickets.get(0).getComments().get(0).getText() + "\n" +
                        "7" + expectedTickets.get(1).getProperties().getContactPhoneNumber() + ";" + expectedTickets.get(1).getComments().get(0).getText() + "\n");

        //Создаем импорт
        Import myImport = new Import()
                .setFilePath(filePath)
                .setServiceTitle("Покупки > Исходящие звонки");

        PageHelper.dataImportHelper(webDriver).createImportBeruOutgoingCallTicketByClientPhone(myImport);

        //Получаем данные из созданных обращений
        for (int i = 0; i < 2; i++) {
            String gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", "Исходящий звонок (7" + expectedTickets.get(i).getProperties().getContactPhoneNumber() + ")");
            Pages.navigate(webDriver).openPageByMetaClassAndID(gid);
            Pages.ticketPage(webDriver).tabs().openTab("Сообщения");
            Ticket ticketFromPage = new Ticket()
                    .setComments(Pages.ticketPage(webDriver).messageTab().comments().getComments())
                    .setProperties(new Properties()
                            .setService(Pages.ticketPage(webDriver).properties().getService())
                            .setContactPhoneNumber(Pages.ticketPage(webDriver).properties().getContactPhoneNumber()));
            ticketsFromPage.add(ticketFromPage);
        }

        Assert.assertEquals("Обращения созданные импортом не равны ожидаемым обращениям", expectedTickets, ticketsFromPage);
    }
}
