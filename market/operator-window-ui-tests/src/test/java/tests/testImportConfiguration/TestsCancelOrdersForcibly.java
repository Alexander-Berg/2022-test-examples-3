package ui_tests.src.test.java.tests.testImportConfiguration;

import Classes.Comment;
import Classes.Import;
import Classes.ImportsSummary;
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

public class TestsCancelOrdersForcibly {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();

    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsCancelOrdersForcibly.class);

    static boolean importCreated = false;

    private int countRetryCheckOrders = 5;


    //Заказ в статусе Delivery
    static List<String> cancelOrderImportOrdersInTheStatusDelivery;
    //Заказ в статусе Processing
    static List<String> cancelOrderImportOrdersInTheStatusProcessing;
    //Заказ в статусе Pickup
    static List<String> cancelOrderImportOrderInTheStatusPickup;
    //Заказ в статусе Delivered
    static List<String> cancelOrderImportOrderInTheStatusDelivered;
    //Заказ с буквами в номере
    static String allImportOrderWithLettersInOrderNumber = "56254g6";
    //Несуществующий номер заказа
    static String allImportNonExistentOrderNumber = "3524";
    //пустая строка
    static String allImportEmptyOrderNumber = "";

    //импорт отмены заказа
    static Import importCancelOrders = new Import();

    //импорт отмены заказа
    static Import importCancelOrdersForcibly = new Import();

    //Создаем импорт массовой отмены заказа
    private static void createImport() {
        if (importCreated) {
            return;
        }
        int datePeriod = Tools.other().getRandomNumber(5, 10);
        //Получаем заказ в статусе Delivery
        cancelOrderImportOrdersInTheStatusDelivery =
                Arrays.asList(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order')\n" +
                        ".withFilters{\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDateRangeForSeveralMonths(datePeriod)) +
                        "eq('cancellationRequestSubStatus', null)\n" +
                        "eq('color', 'BLUE')\n" +
                        "eq('status', 'DELIVERY')\n" +
                        "}.withOrders(api.db.orders.asc('creationDate')).limit(2).list().title").replaceAll("[\\[|\\]\\s]", "").split(","));

        //Получаем заказ в статусе Processing
        cancelOrderImportOrdersInTheStatusProcessing =
                Arrays.asList(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order')\n" +
                        ".withFilters{\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDateRangeForSeveralMonths(datePeriod)) +
                        "eq('cancellationRequestSubStatus', null)\n" +
                        "eq('color', 'BLUE')\n" +
                        "eq('status', 'PROCESSING')\n" +
                        "not(eq('yaDeliveryOrder',null))\n"+
                        "}.withOrders(api.db.orders.asc('creationDate')).limit(2).list().title").replaceAll("[\\[|\\]\\s]", "").split(","));

        //Получаем заказ в статусе Pickup
        cancelOrderImportOrderInTheStatusPickup =
                Arrays.asList(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order')\n" +
                        ".withFilters{\n" +
                        "eq('color', 'BLUE')\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDateRangeForSeveralMonths(datePeriod)) +
                        "eq('cancellationRequestSubStatus', null)\n" +
                        "eq('status', 'PICKUP')\n" +
                        "}.withOrders(api.db.orders.asc('creationDate')).limit(2).list().title").replaceAll("[\\[|\\]\\s]", "").split(","));

        //Получаем заказ в статусе Delivered
        cancelOrderImportOrderInTheStatusDelivered =
                Arrays.asList(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order')\n" +
                        ".withFilters{\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDateRangeForSeveralMonths(datePeriod)) +
                        "eq('color', 'BLUE')\n" +
                        "eq('cancellationRequestSubStatus', null)\n" +
                        "eq('status', 'DELIVERED')\n" +
                        "}.withOrders(api.db.orders.asc('creationDate')).limit(2).list().title").replaceAll("[\\[|\\]\\s]", "").split(","));

        //Создаем запись для импорта Массовой отмены заказа
        importCancelOrders
                .setFilePath(Tools.file().createFile(Tools.other().getRandomText() + "testOCRM989.csv", String.format(
                        "%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                        cancelOrderImportOrdersInTheStatusDelivery.toString().replaceAll("[\\[|\\]\\s]", "").replace(",", "\n"),
                        cancelOrderImportOrdersInTheStatusProcessing.toString().replaceAll("[\\[|\\]\\s]", "").replace(",", "\n"),
                        cancelOrderImportOrderInTheStatusPickup.toString().replaceAll("[\\[|\\]\\s]", "").replace(",", "\n"),
                        cancelOrderImportOrderInTheStatusDelivered.toString().replaceAll("[\\[|\\]\\s]", "").replace(",", "\n"),
                        allImportOrderWithLettersInOrderNumber,
                        allImportEmptyOrderNumber,
                        allImportNonExistentOrderNumber)))
                .setCommentText(Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yyyy") + " Отмена заказа автотестом" + Tools.other().getRandomText());

        //Создаем запись для импорта Принудительной отмены заказа
        importCancelOrdersForcibly
                .setFilePath(Tools.file().createFile(Tools.other().getRandomText() + "testOCRM989.csv", String.format(
                        "%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                        cancelOrderImportOrdersInTheStatusDelivery.toString().replaceAll("[\\[|\\]\\s]", "").replace(",", "\n"),
                        cancelOrderImportOrdersInTheStatusProcessing.toString().replaceAll("[\\[|\\]\\s]", "").replace(",", "\n"),
                        cancelOrderImportOrderInTheStatusPickup.toString().replaceAll("[\\[|\\]\\s]", "").replace(",", "\n"),
                        cancelOrderImportOrderInTheStatusDelivered.toString().replaceAll("[\\[|\\]\\s]", "").replace(",", "\n"),
                        allImportOrderWithLettersInOrderNumber,
                        allImportEmptyOrderNumber,
                        allImportNonExistentOrderNumber)))
                .setCommentText(Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yyyy") + " Принудительная отмена заказа автотестом" + Tools.other().getRandomText());


        PageHelper.dataImportHelper(webDriver).createImportCancelOrders(importCancelOrders);



        importCancelOrdersForcibly = PageHelper.dataImportHelper(webDriver).createImportCancelOrdersForcibly(importCancelOrdersForcibly);
        Tools.waitElement(webDriver).waitTime(10000);
        importCreated = true;
    }


    @Test
    @Ignore("тикет на багу - https://st.yandex-team.ru/OCRM-6973")
    @InfoTest(
            descriptionTest = "Импорт 'Принудительная отмена заказа' изменяют только заказы в статусах delivery или processing",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-620",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1035"
    )
    @Category({Blocker.class})
    public void ocrm1035_CancelOrdersOnlyWithDeliveryAndProcessingStatuses() {
        boolean b = false;
        createImport();
        boolean orderStatusDelivery = false;
        boolean orderStatusPickup = false;
        boolean orderStatusProcessing = false;
        boolean orderStatusDelivered = false;

        for (int x = 0; x < countRetryCheckOrders; x++) {

            if (!orderStatusPickup) {
                for (String order : cancelOrderImportOrderInTheStatusPickup) {
                    String status = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                            "api.db.of('order').withFilters{\n" +
                                    "eq('title','" + order + "')\n" +
                                    "}.get().status.title");
                    if (!status.equals("отменен")) {
                        orderStatusPickup = true;
                        break;
                    }
                }
            }

            if (!orderStatusDelivered) {
                for (String order : cancelOrderImportOrderInTheStatusDelivered) {
                    String status = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                            "api.db.of('order').withFilters{\n" +
                                    "eq('title','" + order + "')\n" +
                                    "}.get().status.title");
                    if (!status.equals("отменен")) {
                        orderStatusDelivered = true;
                        break;
                    }
                }
            }

            if (!orderStatusDelivery) {
                for (String order : cancelOrderImportOrdersInTheStatusDelivery) {
                    String status = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                            "api.db.of('order').withFilters{\n" +
                                    "eq('title','" + order + "')\n" +
                                    "}.get().status.title");
                    if (status.equals("отменен")) {
                        orderStatusDelivery = true;
                        break;
                    }
                }
            }

            if (!orderStatusProcessing) {
                for (String order : cancelOrderImportOrdersInTheStatusProcessing) {
                    String status = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                            "api.db.of('order').withFilters{\n" +
                                    "eq('title','" + order + "')\n" +
                                    "}.get().status.title");
                    if (status.equals("отменен")) {
                        orderStatusProcessing = true;
                        break;
                    }
                }
            }

            b = orderStatusDelivery & orderStatusProcessing & orderStatusDelivered & orderStatusPickup;
            if (b) {
                break;
            } else {
                Tools.waitElement(webDriver).waitTime(5000);
            }

        }
        Assert.assertTrue("Заказ в статусе 'Delivery' в верном статусе - " + orderStatusDelivery + "\n" +
                "Заказ в статусе 'Processing' в верном статусе - " + orderStatusProcessing + "\n" +
                "Заказ в статусе 'Delivered' в верном статусе - " + orderStatusDelivered + "\n" +
                "Заказ в статусе 'Pickup' в верном статусе - " + orderStatusPickup + "\n", b);
    }

    @Test
    @Ignore("тикет на багу - https://st.yandex-team.ru/OCRM-6973")
    @InfoTest(
            descriptionTest = "В отмененных заказах через принудительную отмену заказа добавился внутренний комментарий",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-620",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1036"
    )
    @Category({Blocker.class})
    public void ocrm1036_ACommentIsAddedToCanceledOrders() {
        boolean validCommentInStatusDelivery = false;
        boolean validCommentInStatusProcessing = false;
        boolean result = false;


        createImport();
        Comment comment = new Comment()
                .setText("Заказ был принудительно отменен с помощью импорта Импорт_.*с комментарием \"" + importCancelOrdersForcibly.getCommentText() + "\"")
                .setNameAndEmail("Система")
                .setType("internal");

        List<Comment> commentsFromOrderPageOnStatusDelivery;
        List<Comment> commentsFromOrderPageOnStatusProcessing;

        for (int x = 0; x < countRetryCheckOrders; x++) {

            if (!validCommentInStatusDelivery) {
                for (String order : cancelOrderImportOrdersInTheStatusDelivery) {
                    Pages.navigate(webDriver).openOrderPageByOrderNumber(order);
                    commentsFromOrderPageOnStatusDelivery = Pages.orderPage(webDriver).generalInformationTab().comments().getComments();
                    if (commentsFromOrderPageOnStatusDelivery.contains(comment)) {
                        validCommentInStatusDelivery = true;
                        break;
                    }
                }
            }

            if (!validCommentInStatusProcessing) {
                for (String order : cancelOrderImportOrdersInTheStatusProcessing) {
                    Pages.navigate(webDriver).openOrderPageByOrderNumber(order);
                    commentsFromOrderPageOnStatusProcessing = Pages.orderPage(webDriver).generalInformationTab().comments().getComments();
                    if (commentsFromOrderPageOnStatusProcessing.contains(comment)) {
                        validCommentInStatusProcessing = true;
                        break;
                    }
                }
            }

            result = validCommentInStatusDelivery & validCommentInStatusProcessing;
            if (result) {
                break;
            } else {
                Tools.waitElement(webDriver).waitTime(5000);
            }

        }

        Assert.assertTrue("В заказе со статусом DELIVERY добавился верный комментарий - " + validCommentInStatusDelivery + "\n" +
                "В заказе со статусом PROCESSING добавился верный комментарий - " + validCommentInStatusProcessing, validCommentInStatusDelivery & validCommentInStatusProcessing);

    }

    @Test
    @Ignore("тикет на багу - https://st.yandex-team.ru/OCRM-6973")
    @InfoTest(
            descriptionTest = "Заказы отменяются с верными подстатусами",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-620",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1017"
    )
    @Category({Blocker.class})
    public void ocrm1017_CanceledImportOrdersOfForcedCancellationHaveCorrectSubstatuses() {
        boolean b1 = false;
        boolean b2 = false;
        boolean result = false;

        createImport();

        String subStatusFromOrderPageOnStatusDelivery;
        String subStatusFromOrderPageOnStatusProcessing;

        for (int x = 0; x < countRetryCheckOrders; x++) {
            if (!b1) {
                for (String order : cancelOrderImportOrdersInTheStatusDelivery) {
                    subStatusFromOrderPageOnStatusDelivery = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                            "api.db.of('order').withFilters{\n" +
                                    "eq('title','" + order + "')\n" +
                                    "}.get().status.subStatus");
                    if (subStatusFromOrderPageOnStatusDelivery.equals("Заказ отменен по вине службы доставки (утеря, брак транспортировки)")) {
                        b1 = true;
                        break;
                    }
                }
            }

            if (!b2) {
                for (String order : cancelOrderImportOrdersInTheStatusProcessing) {
                    subStatusFromOrderPageOnStatusProcessing = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                            "api.db.of('order').withFilters{\n" +
                                    "eq('title','" + order + "')\n" +
                                    "}.get().status.subStatus");
                    if (subStatusFromOrderPageOnStatusProcessing.equals("Заказ отменен по вине службы доставки (утеря, брак транспортировки)")) {
                        b2 = true;
                        break;
                    }
                }
            }

            result = b1 & b2;
            if (result) {
                break;
            } else {
                Tools.waitElement(webDriver).waitTime(5000);
            }
        }
        Assert.assertTrue("Заказ всо статусом Delivery отменился с верным подстатусом - " + b1 + "\n" +
                "Заказ всо статусом Processing отменился с верным подстатусом - " + b2, b1 & b2);

    }

    @Test
    @Ignore("тикет на багу - https://st.yandex-team.ru/OCRM-6973")
    @InfoTest(
            descriptionTest = "Проверяем что на странице импорта 'Принудительная отмена заказа' выводятся верные данные",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-620",
            linkFromTestCaseAutoTest = "https://testpalm.yandex-team.ru/testcase/ocrm-1030"
    )
    @Category({Blocker.class})
    public void ocrm1030_OnImportPageCancelOrdersForciblyDisplaysCorrectData() {
        Import importFromPage = new Import();
        createImport();
        Pages.navigate(webDriver).openPageByMetaClassAndID(importCancelOrdersForcibly.getGid());

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


        Assert.assertEquals("У импорта количество обработанных строк не равно количеству строк в файле", importCancelOrdersForcibly, importFromPage);
    }

    @Test
    @Ignore("тикет на багу - https://st.yandex-team.ru/OCRM-6973")
    @InfoTest(
            descriptionTest = "Проверяем что на странице результатов импорта принудительной отмены заказа верные данные",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-620",
            linkFromTestCaseAutoTest = "https://testpalm.yandex-team.ru/testcase/ocrm-1031"
    )
    @Category({Blocker.class})
    public void ocrm1031_OnImportSummaryPageCancelOrdersForciblyShowCorrectData() {
        ImportsSummary importsSummaryFromPage = new ImportsSummary();
        boolean failedIds = false;
        boolean failed = false;
        boolean created = false;
        boolean received = false;
        boolean skipped = false;
        boolean successful = false;
        boolean updated = false;

        createImport();

        Pages.navigate(webDriver).openPageByMetaClassAndID(importCancelOrdersForcibly.getImportsSummary().getGid());
        importsSummaryFromPage
                .setFailedIds(Pages.importsSummaryPage(webDriver).getFailedIds())
                .setFailed(Pages.importsSummaryPage(webDriver).getFailed())
                .setCreated(Pages.importsSummaryPage(webDriver).getCreated())
                .setReceived(Pages.importsSummaryPage(webDriver).getReceived())
                .setSkipped(Pages.importsSummaryPage(webDriver).getSkipped())
                .setSuccessful(Pages.importsSummaryPage(webDriver).getSuccessful())
                .setUpdated(Pages.importsSummaryPage(webDriver).getUpdated());

        if (Integer.parseInt(importsSummaryFromPage.getFailed()) > 0) {
            failed = true;
        }
        if (importsSummaryFromPage.getCreated().equals("0")) {
            created = true;
        }
        if (Integer.parseInt(importsSummaryFromPage.getReceived()) > 0) {
            received = true;
        }
        if (Integer.parseInt(importsSummaryFromPage.getSkipped()) > 0) {
            skipped = true;
        }
        if (Integer.parseInt(importsSummaryFromPage.getSuccessful()) > 0) {
            successful = true;
        }
        if (importsSummaryFromPage.getUpdated().equals("0")) {
            updated = true;
        }
        if (!importsSummaryFromPage.getFailedIds().contains(allImportNonExistentOrderNumber)) {
            failedIds = false;
        } else if (!importsSummaryFromPage.getFailedIds().contains(cancelOrderImportOrderInTheStatusPickup.get(0))) {
            failedIds = false;
        } else if (importsSummaryFromPage.getFailedIds().contains(cancelOrderImportOrderInTheStatusDelivered.get(0))) {
            failedIds = false;
        } else {
            failedIds = true;
        }

        Assert.assertTrue("На странице результатов импорта вывелись не верные данные",failed & created & received & skipped & successful & updated & failedIds);
    }

    @Test
    @Ignore("тикет на багу - https://st.yandex-team.ru/OCRM-6973")
    @InfoTest(
            descriptionTest = "В файле лога принудительной отмены заказа есть информация об успешно отмененных заказах и пропущенных строках",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-620",
            linkFromTestCaseAutoTest = "https://testpalm.yandex-team.ru/testcase/ocrm-1032"
    )
    @Category({Blocker.class})
    public void ocrm1032_onImportSummaryPageCancelOrdersForciblyShowCorrectData() {
        boolean b1;
        boolean b2;
        boolean b3;
        boolean b4;

        createImport();
        Pages.navigate(webDriver).openPageByMetaClassAndID(importCancelOrdersForcibly.getGid());

        String href = Pages.importPage(webDriver).getLinkOnLogsFile();

        String logFile = Tools.file().readFileFromTheLink(webDriver, href, "utf-8");

        b1 = Tools.other().isContainsSubstring("Заказ \\\\\".*\\\\\" имеет неправильный формат. Строка будет пропущена.", logFile);
        b2 = Tools.other().isContainsSubstring("Отправлен запрос на отмену заказов со статусом \\\\\"в обработке\\\\\":.*?Успешно", logFile);
        b3 = Tools.other().isContainsSubstring("Отправлен запрос на отмену заказов со статусом \\\\\"передан в доставку\\\\\":.*?Успешно", logFile);
        b4 = Tools.other().isContainsSubstring("Заказов с неправильными статусами.*?\\]", logFile);

        Assert.assertTrue("В файле импорта есть информация:\n" +
                "- о том что в файле есть номера заказов в неправильном формате - " + b1 + "\n" +
                "- о том что отправлен запрос на отмену заказов в статусе 'в обработке' - " + b2 + "\n" +
                "- о том что отправлен запрос на отмену заказов в статусе 'передан в доставку' - " + b3 + "\n" +
                "- о том что есть заказы с неправильными статусами - " + b4, b1 & b2 & b3 & b4);
    }


}
