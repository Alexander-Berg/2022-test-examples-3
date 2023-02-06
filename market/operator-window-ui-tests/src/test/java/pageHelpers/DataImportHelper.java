package ui_tests.src.test.java.pageHelpers;

import Classes.Import;
import Classes.ImportsSummary;
import org.openqa.selenium.WebDriver;
import pages.Pages;
import tools.Tools;
import unit.Config;

import java.util.List;

public class DataImportHelper {
    private WebDriver webDriver;
    private final static int COUNT_CHECKS_STATUS_OF_IMPORT = 120;

    public DataImportHelper(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Создать импорт Создание исходящих обращений телефонии по номеру заказа и внутреннему комментарию
     *
     * @param newImport импорт с путем к файлу и комментарию
     * @return получившийся импорт
     */
    public Import createImportBeruOutgoingTicketByEmail(Import newImport, List<String> categories) {
        Import myImport = newImport.clone();

        //Переходим на страницу импорта "Создание исходящих обращений телефонии по номеру заказа и внутреннему комментарию"
        webDriver.get(Config.getProjectURL() + "/dataimport/ticket/import$beruOutgoingTicketByEmail/form");
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        Pages.dataImportPage(webDriver).beruOutgoingTicketByEmailPage().tabForm()
                .setService(newImport.getServiceTitle())
                .setImportFile(newImport.getFilePath())
                .setCategory(categories);
        Pages.dataImportPage(webDriver).beruOutgoingCallTicketByOrderIdPage().header().clickSaveRecordButton();

        //ожидаем пока закончится импорт
        boolean b = false;
        for (int i = 0; i < COUNT_CHECKS_STATUS_OF_IMPORT; i++) {
            String result = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def attachment = api.db.of('attachment').withFilters{\n" +
                    "  eq('name','" + myImport.getFilePath().replaceAll(".*/", "") + "')\n" +
                    "                }.get().entity\n" +
                    "api.db.get(attachment).status.title");
            b = result.equals("Завершен");
            if (b) {
                break;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }
        if (!b) {
            throw new Error("Не дождались когда закончится импорт 'Создание исходящих обращений телефонии по номеру заказа и внутреннему комментарию'");
        }
        return myImport;

    }


    /**
     * Создать импорт Создание исходящих обращений телефонии по номеру заказа и внутреннему комментарию
     *
     * @param newImport импорт с путем к файлу и комментарию
     * @return получившийся импорт
     */
    public Import createImportBeruOutgoingCallTicketByOrderId(Import newImport) {
        Import myImport = newImport.clone();

        //Переходим на страницу импорта "Создание исходящих обращений телефонии по номеру заказа и внутреннему комментарию"
        webDriver.get(Config.getProjectURL() + "/dataimport/ticket/import$beruOutgoingCallTicketByOrderId/form");
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        Pages.dataImportPage(webDriver).beruOutgoingCallTicketByOrderIdPage().tabForm()
                .setService(newImport.getServiceTitle())
                .setImportFile(newImport.getFilePath());
        Pages.dataImportPage(webDriver).beruOutgoingCallTicketByOrderIdPage().header().clickSaveRecordButton();

        //ожидаем пока закончится импорт
        boolean b = false;
        for (int i = 0; i < COUNT_CHECKS_STATUS_OF_IMPORT; i++) {
            String result = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def attachment = api.db.of('attachment').withFilters{\n" +
                    "  eq('name','" + myImport.getFilePath().replaceAll(".*/", "") + "')\n" +
                    "                }.get().entity\n" +
                    "api.db.get(attachment).status.title");
            b = result.equals("Завершен");
            if (b) {
                break;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }
        if (!b) {
            throw new Error("Не дождались когда закончится импорт 'Создание исходящих обращений телефонии по номеру заказа и внутреннему комментарию'");
        }
        return myImport;

    }

    /**
     * Создать импорт Массовой отмены заказа
     *
     * @param newImport импорт с путем к файлу и комментарию
     * @return получившийся импорт
     */
    public void createImportCancelOrders(Import newImport) {
        Import myImport = newImport.clone();

        //Переходим на страницу импорта "массовая отмена заказ"
        webDriver.get(Config.getProjectURL() + "/dataimport/order/import$cancelOrders/form");
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        //Заполняем страницу создания импорта
        Pages.dataImportPage(webDriver).cancelOrdersPage().tabForm()
                .setImportFile(myImport.getFilePath())
                .setTextComment(myImport.getCommentText());
        //Сохраняем импорт
        Pages.dataImportPage(webDriver).cancelOrdersPage().header().clickSaveRecordButton();

        boolean b = false;
        for (int i = 0; i < 120; i++) {
            String result = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def attachment = api.db.of('attachment').withFilters{\n" +
                    "  eq('name','" + newImport.getFilePath().replaceAll(".*/", "") + "')\n" +
                    "                }.get().entity\n" +
                    "api.db.get(attachment).status.title");
            b = result.equals("Завершен");
            if (b) {
                newImport.setStatus("Завершен")
                        .setGid(Tools.other().getGidFromCurrentPageUrl(webDriver))
                        .setSummaryTitle("Результат импорта \"Импорт_" + Tools.date().generateCurrentDateAndTimeStringOfFormat("yyyy-MM-dd") + "T")
                        .setLinkOnConfigurationImportPage(Config.getProjectURL() + "/entity/importConfiguration@102229484")
                        .setConfigurationImportTitle("Массовая отмена заказов")
                        .setLinkOnSourcesFile(Config.getProjectURL() + "\\/api\\/jmf\\/ui\\/entity\\/attachment\\/attachment@\\d*\\/" + newImport.getFilePath().replaceAll(".*\\/", ""))
                        .setSourcesFileTitle(newImport.getFilePath().replaceAll(".*\\/", ""))
                        .setLinkOnLogsFile(Config.getProjectURL() + "\\/api\\/jmf\\/ui\\/entity\\/attachment\\/attachment@\\d*\\/cancelOrders*.")
                        .setLogsFileTitle("cancelOrders.*log")
                        .setStatus("Завершен")
                        .setProgress("6");
                newImport.setImportsSummary(new ImportsSummary().setGid(
                        PageHelper.otherHelper(webDriver)
                                .runningScriptFromAdministrationPage("api.db.of('import$cancelOrders').withFilters{\n" +
                                        "  eq('commentText','" + newImport.getCommentText() + "')\n" +
                                        "                }.limit(1).get().summary")));
                break;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }
        if (!b) {
            throw new Error("Не дождались когда закончится импорт массовой отмены заказа");
        }
    }

    /**
     * Создать импорт Принудительной отмены заказа
     *
     * @param newImport импорт с путем к файлу и комментарию
     * @return получившийся импорт
     */
    public Import createImportCancelOrdersForcibly(Import newImport) {
        Import myImport = newImport.clone();

        //Переходим на страницу импорта "принудительная отмена заказ"
        webDriver.get(Config.getProjectURL() + "/dataimport/order/import$cancelOrdersForcibly/form");
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        Pages.dataImportPage(webDriver).cancelOrdersPage().tabForm()
                .setImportFile(myImport.getFilePath())
                .setTextComment(myImport.getCommentText());
        //Сохраняем импорт
        Pages.dataImportPage(webDriver).cancelOrdersPage().header().clickSaveRecordButton();
        //ожидаем пока закончится импорт
        Boolean b = false;
        for (int i = 0; i < COUNT_CHECKS_STATUS_OF_IMPORT; i++) {
            String result = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def attachment = api.db.of('attachment').withFilters{\n" +
                    "  eq('name','" + myImport.getFilePath().replaceAll(".*/", "") + "')\n" +
                    "                }.get().entity\n" +
                    "api.db.get(attachment).status.title");
            b = result.equals("Завершен");
            if (b) {
                myImport.setStatus("Завершен")
                        .setGid(Tools.other().getGidFromCurrentPageUrl(webDriver))
                        .setSummaryTitle("Результат импорта \"Импорт_" + Tools.date().generateCurrentDateAndTimeStringOfFormat("yyyy-MM-dd") + "T")
                        .setLinkOnConfigurationImportPage(Config.getProjectURL() + "/entity/importConfiguration@106146884")
                        .setConfigurationImportTitle("Принудительная отмена заказов")
                        .setLinkOnSourcesFile(Config.getProjectURL() + "\\/api\\/jmf\\/ui\\/entity\\/attachment\\/attachment@\\d*\\/" + myImport.getFilePath().replaceAll(".*\\/", ""))
                        .setSourcesFileTitle(myImport.getFilePath().replaceAll(".*\\/", ""))
                        .setLinkOnLogsFile(Config.getProjectURL() + "\\/api\\/jmf\\/ui\\/entity\\/attachment\\/attachment@\\d*\\/cancelOrders*.")
                        .setLogsFileTitle("cancelOrders.*log")
                        .setStatus("Завершен")
                        .setProgress("6");
                myImport.setImportsSummary(new ImportsSummary().setGid(
                        PageHelper.otherHelper(webDriver)
                                .runningScriptFromAdministrationPage("api.db.of('import$cancelOrdersForcibly').withFilters{\n" +
                                        "  eq('commentText','" + myImport.getCommentText() + "')\n" +
                                        "                }.limit(1).get().summary")));

                break;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }
        if (!b) {
            throw new Error("Не дождались когда закончится импорт Принудительной отмены заказа");
        }
        return myImport;
    }

    /**
     * Создание импорта "Смена статуса с добавлением комментариев"
     *
     * @param newImport
     * @return
     */
    public Import createImportTicketChangeStatusAddComment(Import newImport) {
        Import myImport = newImport.clone();

        //Переходим на страницу импорта "принудительная отмена заказ"
        webDriver.get(Config.getProjectURL() + "/dataimport/ticket/import$ticketChangeStatusAddComment/form");
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        Pages.dataImportPage(webDriver).ticketChangeStatusAddComment().tabForm()
                .setImportFile(myImport.getFilePath());
        //Сохраняем импорт
        Pages.dataImportPage(webDriver).ticketChangeStatusAddComment().header().clickSaveRecordButton();

        //ожидаем пока закончится импорт
        Boolean b = false;
        for (int i = 0; i < COUNT_CHECKS_STATUS_OF_IMPORT; i++) {
            String result = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def attachment = api.db.of('attachment').withFilters{\n" +
                    "  eq('name','" + myImport.getFilePath().replaceAll(".*/", "") + "')\n" +
                    "                }.get().entity\n" +
                    "api.db.get(attachment).status.title");
            b = result.equals("Завершен");

            if (b) {
                myImport
                        .setStatus("Завершен")
                        .setProgress("4");

                break;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }
        if (!b) {
            throw new Error("Не дождались когда закончится импорт Смена статуса с добавлением комментариев");
        }
        return myImport;

    }

    /**
     * Создать импорт Создание исходящих обращений по номеру заказа и внешнему комментарию
     *
     * @param newImport импорт с путем к файлу и комментарию
     * @return получившийся импорт
     */
    public Import createImportBeruOutgoingTicketByOrderId(Import newImport) {
        Import myImport = newImport.clone();

        //Переходим на страницу импорта "Создание исходящих обращений по номеру заказа и внешнему комментарию"
        webDriver.get(Config.getProjectURL() + "/dataimport/ticket/import$beruOutgoingTicketByOrderId/form");
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        Pages.dataImportPage(webDriver).beruOutgoingTicketByOrderId().tabForm()
                .setImportFile(myImport.getFilePath())
                .setCategory(myImport.getCategories())
                .setService(myImport.getServiceTitle());
        //Сохраняем импорт
        Pages.dataImportPage(webDriver).cancelOrdersPage().header().clickSaveRecordButton();
        //ожидаем пока закончится импорт
        Boolean b = false;
        for (int i = 0; i < COUNT_CHECKS_STATUS_OF_IMPORT; i++) {
            String result = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def attachment = api.db.of('attachment').withFilters{\n" +
                    "  eq('name','" + myImport.getFilePath().replaceAll(".*/", "") + "')\n" +
                    "                }.get().entity\n" +
                    "api.db.get(attachment).status.title");
            b = result.equals("Завершен");
            if (b) {
                break;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }
        if (!b) {
            throw new Error("Не дождались когда закончится импорт Создание исходящих обращений по номеру заказа и внешнему комментарию");
        }
        return myImport;
    }

    /**
     * Создать импорт Создание обращений по ручным задачам
     *
     * @param newImport импорт с путем к файлу и комментарию
     * @return получившийся импорт
     */
    public Import createImportOrderTicketsManualCreation(Import newImport) {
        Import myImport = newImport.clone();

        //Переходим на страницу импорта "Создание обращений по ручным задачам"
        webDriver.get(Config.getProjectURL() + "/dataimport/ticket/import$orderTicketsManualCreation/form");
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        Pages.dataImportPage(webDriver).orderTicketsManualCreation().tabForm()
                .setImportFile(myImport.getFilePath())
                .setTextComment(myImport.getCommentText())
                .setService(myImport.getServiceTitle());
        //Сохраняем импорт
        Pages.dataImportPage(webDriver).cancelOrdersPage().header().clickSaveRecordButton();
        //ожидаем пока закончится импорт
        Boolean b = false;
        for (int i = 0; i < COUNT_CHECKS_STATUS_OF_IMPORT; i++) {
            String result = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def attachment = api.db.of('attachment').withFilters{\n" +
                    "  eq('name','" + myImport.getFilePath().replaceAll(".*/", "") + "')\n" +
                    "                }.get().entity\n" +
                    "api.db.get(attachment).status.title");
            b = result.equals("Завершен");
            if (b) {
                myImport.setStatus("Завершен")
                        .setProgress("3");
                break;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }
        if (!b) {
            throw new Error("Не дождались когда закончится импорт Создание обращений по ручным задачам");
        }
        return myImport;
    }

    /**
     * Создать импорт Создание исходящих обращений телефонии по номеру телефона и внутреннему комментарию
     *
     * @param newImport импорт с путем к файлу и комментарию
     * @return получившийся импорт
     */
    public Import createImportBeruOutgoingCallTicketByClientPhone(Import newImport) {
        Import myImport = newImport.clone();

        //Переходим на страницу импорта "Создание исходящих обращений телефонии по номеру телефона и внутреннему комментарию"
        webDriver.get(Config.getProjectURL() + "/dataimport/ticket/import$beruOutgoingCallTicketByClientPhone/form");
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        Pages.dataImportPage(webDriver).beruOutgoingCallTicketByClientPhonePage().tabForm()
                .setImportFile(myImport.getFilePath())
                .setService(myImport.getServiceTitle());
        //Сохраняем импорт
        Pages.dataImportPage(webDriver).cancelOrdersPage().header().clickSaveRecordButton();
        //ожидаем пока закончится импорт
        Boolean b = false;
        for (int i = 0; i < COUNT_CHECKS_STATUS_OF_IMPORT; i++) {
            String result = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def attachment = api.db.of('attachment').withFilters{\n" +
                    "  eq('name','" + myImport.getFilePath().replaceAll(".*/", "") + "')\n" +
                    "                }.get().entity\n" +
                    "api.db.get(attachment).status.title");
            b = result.equals("Завершен");
            if (b) {
                break;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }
        if (!b) {
            throw new Error("Не дождались когда закончится импорт Создание исходящих обращений по номеру заказа и внешнему комментарию");
        }
        return myImport;
    }
}

