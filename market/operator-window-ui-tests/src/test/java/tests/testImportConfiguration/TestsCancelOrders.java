package ui_tests.src.test.java.tests.testImportConfiguration;

import Classes.Comment;
import Classes.Import;
import Classes.ImportsSummary;
import Classes.order.History;
import Classes.order.MainProperties;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;

import java.util.Arrays;
import java.util.List;

public class TestsCancelOrders {

    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();

    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsCancelOrders.class);

    static boolean importCreated = false;

    //Заказ в статусе Delivery
    static MainProperties cancelOrderImportOrdersInTheStatusDelivery = new MainProperties();
    //Заказ в статусе Processing
    static MainProperties cancelOrderImportOrdersInTheStatusProcessing = new MainProperties();
    //Заказ в статусе Pickup
    static MainProperties cancelOrderImportOrderInTheStatusPickup = new MainProperties();
    //Заказ в статусе Delivered
    static MainProperties cancelOrderImportOrderInTheStatusDelivered = new MainProperties();
    //Заказ с буквами в номере
    static MainProperties allImportOrderWithLettersInOrderNumber = new MainProperties().setOrderNumber("56254g6");
    //Несуществующий номер заказа
    static MainProperties allImportNonExistentOrderNumber = new MainProperties().setOrderNumber("3524");
    //пустая строка
    static MainProperties allImportEmptyOrderNumber = new MainProperties().setOrderNumber("");

    //импорт отмены заказа
    static Import importCancelOrders = new Import();

    //Создаем импорт массовой отмены заказа
    private static void createImport() {
        if (importCreated) {
            return;
        }
        int datePeriod = Tools.other().getRandomNumber(2, 9);
        //Получаем заказ в статусе Delivery
        cancelOrderImportOrdersInTheStatusDelivery.setOrderNumber(
                PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order')\n" +
                        ".withFilters{\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDateRangeForSeveralMonths(datePeriod)) +
                        "eq('cancellationRequestSubStatus', null)\n" +
                        "eq('color', 'BLUE')\n" +
                        "eq('status', 'DELIVERY')\n" +
                        "}.withOrders(api.db.orders.desc('creationDate')).limit(1).get().title"));

        //Получаем заказ в статусе Processing
        cancelOrderImportOrdersInTheStatusProcessing.setOrderNumber(
                PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order')\n" +
                        ".withFilters{\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDateRangeForSeveralMonths(datePeriod)) +
                        "eq('cancellationRequestSubStatus', null)\n" +
                        "eq('color', 'BLUE')\n" +
                        "eq('status', 'PROCESSING')\n" +
                        "}.withOrders(api.db.orders.desc('creationDate')).limit(1).get().title"));

        //Получаем заказ в статусе Pickup
        cancelOrderImportOrderInTheStatusPickup.setOrderNumber(
                PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order')\n" +
                        ".withFilters{\n" +
                        "eq('color', 'BLUE')\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDateRangeForSeveralMonths(datePeriod)) +
                        "eq('cancellationRequestSubStatus', null)\n" +
                        "eq('status', 'PICKUP')\n" +
                        "}.withOrders(api.db.orders.desc('creationDate')).limit(1).get().title"));

        //Получаем заказ в статусе Delivered
        cancelOrderImportOrderInTheStatusDelivered.setOrderNumber(
                PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order')\n" +
                        ".withFilters{\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDateRangeForSeveralMonths(datePeriod)) +
                        "eq('color', 'BLUE')\n" +
                        "eq('cancellationRequestSubStatus', null)\n" +
                        "eq('status', 'DELIVERED')\n" +
                        "}.withOrders(api.db.orders.desc('creationDate')).limit(1).get().title"));

        //Создаем запись для импорта Массовой отмены заказа
        importCancelOrders
                .setFilePath(Tools.file().createFile(Tools.other().getRandomText() + "testOCRM989.csv", String.format(
                        "%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                        cancelOrderImportOrdersInTheStatusDelivery.getOrderNumber(),
                        cancelOrderImportOrdersInTheStatusProcessing.getOrderNumber(),
                        cancelOrderImportOrderInTheStatusPickup.getOrderNumber(),
                        cancelOrderImportOrderInTheStatusDelivered.getOrderNumber(),
                        allImportOrderWithLettersInOrderNumber.getOrderNumber(),
                        allImportEmptyOrderNumber.getOrderNumber(),
                        allImportNonExistentOrderNumber.getOrderNumber())))
                .setCommentText(Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yyyy") + " Отмена заказа автотестом" + Tools.other().getRandomText());

        PageHelper.dataImportHelper(webDriver).createImportCancelOrders(importCancelOrders);

    }

    @Ignore("Тест не стабильный")
    @Test
    @InfoTest(
            descriptionTest = "Создаются запросы на отмену заказов только в статусе delivery или processing",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-610",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-989"
    )
    @Category({Blocker.class})
    public void ocrm989_CreatingAnOrderCancellationRequestOnlyWithDeliveryAndProcessingStatuses() {
        boolean orderStatusDelivery = false;
        boolean orderStatusPickup = false;
        boolean orderStatusProcessing = false;
        boolean orderStatusDelivered = false;

        //Создаем импорт для теста
        createImport();

        // Получаем историю событий по всем заказам
        Pages.navigate(webDriver).openOrderPageByOrderNumber(cancelOrderImportOrderInTheStatusPickup.getOrderNumber());
        Pages.orderPage(webDriver).tabs().clickHistoryTab();
        List<History> historyListByOrderStatusPickup = Pages.orderPage(webDriver).historyTab().getAllHistory();

        Pages.navigate(webDriver).openOrderPageByOrderNumber(cancelOrderImportOrderInTheStatusDelivered.getOrderNumber());
        Pages.orderPage(webDriver).tabs().clickHistoryTab();
        List<History> historyListByStatusDelivered = Pages.orderPage(webDriver).historyTab().getAllHistory();

        Pages.navigate(webDriver).openOrderPageByOrderNumber(cancelOrderImportOrdersInTheStatusProcessing.getOrderNumber());
        Pages.orderPage(webDriver).tabs().clickHistoryTab();
        List<History> historyListByOrderStatusProcessing = Pages.orderPage(webDriver).historyTab().getAllHistory();

        Pages.navigate(webDriver).openOrderPageByOrderNumber(cancelOrderImportOrdersInTheStatusDelivery.getOrderNumber());
        Pages.orderPage(webDriver).tabs().clickHistoryTab();
        List<History> historyListByOrderStatusDelivery = Pages.orderPage(webDriver).historyTab().getAllHistory();

        // Проверяем наличие создания запроса на отмену заказа в статусе Delivery
        for (History history : historyListByOrderStatusDelivery) {
            if (history.getTypeEntity().equals("Создан запрос на отмену парсела") & history.getAuthor().equals("робот CRM")) {
                orderStatusDelivery = true;
                break;
            }
        }
        // Проверяем отсутствие запроса на отмену заказа в статусе Pickup
        for (History history : historyListByOrderStatusPickup) {
            if (!history.getTypeEntity().equals("Создан запрос на отмену парсела")) {
                orderStatusPickup = true;
                break;
            }
        }
        // Проверяем наличие запроса на отмену заказа в статусе Processing
        for (History history : historyListByOrderStatusProcessing) {
            if (history.getTypeEntity().equals("Создан запрос на отмену парсела") & history.getAuthor().equals("робот CRM")) {
                orderStatusProcessing = true;
                break;
            }
        }
        // Проверяем отсутствие запроса на отмену заказа в статусе Delivered
        for (History history : historyListByStatusDelivered) {
            if (!history.getTypeEntity().equals("Создан запрос на отмену парсела")) {
                orderStatusDelivered = true;
                break;
            }
        }

        Assert.assertTrue(String.format("У заказа в статусе Delivery создался запрос на отмену заказа -%s\n" +
                        "У заказа в статусе Processing создался запрос на отмену заказа -%s\n" +
                        "У заказа в статусе Pickup не создался запрос на отмену заказа -%s\n" +
                        "У заказа в статусе Delivered не создался запрос на отмену заказа -%s\n",
                orderStatusDelivery,
                orderStatusProcessing,
                orderStatusPickup,
                orderStatusDelivered), orderStatusDelivery & orderStatusPickup & orderStatusProcessing & orderStatusDelivered);
        importCreated = true;
    }

    @Ignore("Тест не стабильный")
    @Test
    @InfoTest(
            descriptionTest = "Проверяем что на странице импорта выводятся верные данные",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-610",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1007"
    )
    @Category({Blocker.class})
    public void ocrm1007_onImportPageCancelOrdersDisplaysCorrectData() {
        Import importFromPage = new Import();
        createImport();
        Pages.navigate(webDriver).openPageByMetaClassAndID(importCancelOrders.getGid());
        importFromPage
                .setSummaryTitle(Pages.importPage(webDriver).getSummaryText())
                .setSummaryLink(Pages.importPage(webDriver).getLinkOnSummaryPage())
                .setConfigurationImportTitle(Pages.importPage(webDriver).getTitleConfigurationImport())
                .setLinkOnConfigurationImportPage(Pages.importPage(webDriver).getLinkOnConfigurationImportPage())
                .setCommentText(Pages.importPage(webDriver).getCommentText())
                .setLinkOnSourcesFile(Pages.importPage(webDriver).getLinkOnSourcesFile())
                .setSourcesFileTitle(Pages.importPage(webDriver).getTitleSourcesFile())
                .setLinkOnLogsFile(Pages.importPage(webDriver).getLinkOnLogsFile())
                .setLogsFileTitle(Pages.importPage(webDriver).getTitleLogsFile())
                .setStatus(Pages.importPage(webDriver).getStatus())
                .setProgress(Pages.importPage(webDriver).getProgress())
                .setImportsSummary(new ImportsSummary().setGid(Pages.importPage(webDriver).getLinkOnSummaryPage().replaceAll(".*@", "")));
        ;

        Assert.assertEquals("У импорта количество обработанных строк не равно количеству строк в файле", importCancelOrders, importFromPage);
        importCreated = true;
    }

    @Ignore("Тест не стабильный")
    @Test
    @InfoTest(
            descriptionTest = "Проверяем что в результатах импорта выводятся верные данные",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-610",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1012"
    )
    @Category({Blocker.class})
    public void ocrm1012_onImportSummaryPageCancelOrdersShowCorrectData() {
        ImportsSummary importsSummaryFromPage = new ImportsSummary();
        ImportsSummary existImportsSummary = new ImportsSummary();
        boolean result = false;
        for (int i = 0; i < 3; i++) {
            createImport();
            existImportsSummary
                    .setFailedIds(Arrays.asList(allImportNonExistentOrderNumber.getOrderNumber(), cancelOrderImportOrderInTheStatusPickup.getOrderNumber(), cancelOrderImportOrderInTheStatusDelivered.getOrderNumber()))
                    .setFailed("3")
                    .setCreated("0")
                    .setReceived("6")
                    .setSkipped("1")
                    .setSuccessful("2")
                    .setUpdated("0");
            Pages.navigate(webDriver).openPageByMetaClassAndID(importCancelOrders.getImportsSummary().getGid());
            importsSummaryFromPage
                    .setFailedIds(Pages.importsSummaryPage(webDriver).getFailedIds())
                    .setFailed(Pages.importsSummaryPage(webDriver).getFailed())
                    .setCreated(Pages.importsSummaryPage(webDriver).getCreated())
                    .setReceived(Pages.importsSummaryPage(webDriver).getReceived())
                    .setSkipped(Pages.importsSummaryPage(webDriver).getSkipped())
                    .setSuccessful(Pages.importsSummaryPage(webDriver).getSuccessful())
                    .setUpdated(Pages.importsSummaryPage(webDriver).getUpdated());
            result = existImportsSummary.equals(importsSummaryFromPage);
            if (result) {
                break;
            }

        }

        Assert.assertTrue("В результатах импорта вывелось не то что мы ожидали", result);
        importCreated = true;
    }

    @Ignore("Тест не стабильный")
    @Test
    @InfoTest(descriptionTest = "В отмененные заказы добавляется внутренний комментарий",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-610",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1008")
    @Category({Blocker.class})
    public void ocrm1008_ACommentIsAddedToCanceledOrders() {
        createImport();
        Comment comment = new Comment()
                .setText("Для заказа отправлен запрос на отмену с помощью импорта Импорт_.*с комментарием \"" + importCancelOrders.getCommentText() + "\"")
                .setNameAndEmail("Система")
                .setType("internal");

        List<Comment> commentsFromOrderPageOnStatusDelivery;
        List<Comment> commentsFromOrderPageOnStatusProcessing;
        boolean b1 = false;
        boolean b2 = false;

        for (int i = 0; i < 3; i++) {
            Pages.navigate(webDriver).openOrderPageByOrderNumber(cancelOrderImportOrdersInTheStatusDelivery.getOrderNumber());
            commentsFromOrderPageOnStatusDelivery = Pages.orderPage(webDriver).generalInformationTab().comments().getComments();

            Pages.navigate(webDriver).openOrderPageByOrderNumber(cancelOrderImportOrdersInTheStatusProcessing.getOrderNumber());
            commentsFromOrderPageOnStatusProcessing = Pages.orderPage(webDriver).generalInformationTab().comments().getComments();
            b1 = commentsFromOrderPageOnStatusDelivery.contains(comment);
            b2 = commentsFromOrderPageOnStatusProcessing.contains(comment);
            if (b1 && b2) {
                break;
            } else {
                Tools.waitElement(webDriver).waitTime(3000);
            }
        }

        Assert.assertTrue("В заказе со статусом DELIVERY добавился верный комментарий - " + b1 + "\n" +
                "В заказе со статусом PROCESSING добавился верный комментарий - " + b2, b1 & b2);
        importCreated = false;
    }

    @Ignore("Тест не стабильный")
    @Test
    @InfoTest(
            descriptionTest = "В файле лога есть информация об успешно отмененных заказах и пропущенных строках",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-610",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1011"
    )
    @Category({Blocker.class})
    public void ocrm1011_LogFileContainsInformationAboutSuccessfullyCanceledOrdersAndMissedLines() {
        createImport();
        Pages.navigate(webDriver).openPageByMetaClassAndID(importCancelOrders.getGid());

        String href = Pages.importPage(webDriver).getLinkOnLogsFile();

        String logFile = Tools.file().readFileFromTheLink(webDriver, href, "utf-8");

        boolean b1 = logFile.contains("Заказ \\\"" + allImportOrderWithLettersInOrderNumber.getOrderNumber() + "\\\" имеет неправильный формат. Строка будет пропущена.");
        boolean b2 = logFile.contains("Отправлен запрос на отмену заказов со статусом \\\"в обработке\\\":\\n[" + cancelOrderImportOrdersInTheStatusProcessing.getOrderNumber() + "]\\nУспешно: 1.");
        boolean b3 = logFile.contains("Отправлен запрос на отмену заказов со статусом \\\"передан в доставку\\\":\\n[" + cancelOrderImportOrdersInTheStatusDelivery.getOrderNumber() + "]\\nУспешно: 1.");
        boolean b4 = logFile.contains("Заказов с неправильными статусами (3):\\n[" + cancelOrderImportOrderInTheStatusPickup.getOrderNumber() + ", " + cancelOrderImportOrderInTheStatusDelivered.getOrderNumber() + ", " + allImportNonExistentOrderNumber.getOrderNumber() + "]");

        Assert.assertTrue("В файле импорта есть информация:\n" +
                "- о том что в файле есть номера заказов в неправильном формате - " + b1 + "\n" +
                "- о том что отправлен запрос на отмену заказов в статусе 'в обработке' - " + b2 + "\n" +
                "- о том что отправлен запрос на отмену заказов в статусе 'передан в доставку' - " + b3 + "\n" +
                "- о том что есть заказы с неправильными статусами - " + b4, b1 & b2 & b3 & b4);
        importCreated = true;
    }
}
