package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.reporter;


import java.util.Date;
import java.util.Locale;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Resource;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report.OpsexecEffectivenessActPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report.OpsexecEffectivenessOptimizedActPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report.OutboundsActPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report.ReceiptOutboundsActPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report.TransferUtilActPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report.WasteActPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report.WasteNoticeActPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;

@DisplayName("Selenium: Reports")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/infor.properties"})
public class ReportsTest extends AbstractUiTest {
    private static final Logger log = LoggerFactory.getLogger(ReportsTest.class);

    @RetryableTest
    @DisplayName("Проверка отчета в новом интерфейсе \"Акт приема передачи изъятия\"")
    @ResourceLock("Проверка отчета в новом интерфейсе \"Акт приема передачи изъятия\"")
    void LpnLabelReportNewTest() {
        uiSteps.Login().PerformLogin();
        String applicationNumber = "123";
        OutboundsActPage outboundsActPage = uiSteps
                .Navigation()
                .menu()
                .openReports()
                .openAcceptanceTransferWithdrawalAct();

        OutboundsActPage topReport = outboundsActPage
                .runReport(applicationNumber)
                .waitPrintNotification()
                .openHistoryTab()
                .waitTopReportIsReady();
        String actualReportNumber = topReport.getTopReportApplicationNumberInHistory();

        String message = String.format("Expected %s report id, received %s", applicationNumber, actualReportNumber);
        Assertions.assertEquals(applicationNumber, actualReportNumber, message);

        String actualReportId = topReport.getTopReportIdInHistory();
        String pdfContent = outboundsActPage.openPdfReport(actualReportId);

        message = String.format("PDF does not contain required application number %s", applicationNumber);
        Assertions.assertTrue(pdfContent.contains(applicationNumber), message);

        outboundsActPage
                .openHtmlReport()
                .verifyApplicationNumber(applicationNumber);
    }

    @RetryableTest
    @DisplayName("Проверка отчета в новом интерфейсе \"Акт приема передачи\"")
    @ResourceLock("Проверка отчета в новом интерфейсе \"Акт приема передачи\"")
    void receiptOutboundsActTest() {
        uiSteps.Login().PerformLogin();

        ReceiptOutboundsActPage receiptOutboundsActPage = uiSteps
                .Navigation()
                .menu()
                .openReports()
                .openReceiptOutboundAct();

        int topReportId = receiptOutboundsActPage
                .openHistoryTab()
                .getTopReportId();
        receiptOutboundsActPage.openRunReportTab();

        int newReportId = receiptOutboundsActPage
                .runReport()
                .waitPrintNotification()
                .openHistoryTab()
                .waitTopReportIsReady(topReportId)
                .getTopReportId();

        String message = String.format("Expected report id greater than %s , received %s", topReportId, newReportId);
        Assertions.assertTrue(topReportId < newReportId, message);

        String pdfContent = receiptOutboundsActPage.openPdfReport(String.valueOf(newReportId));

        String expectedString = "Акт приема-передачи";
        message = String.format("PDF does not contain expected string %s", expectedString);
        Assertions.assertTrue(pdfContent.contains(expectedString), message);

        String xlsxContent = receiptOutboundsActPage.openXlsxReport(String.valueOf(newReportId));

        message = String.format("XLS does not contain expected string %s", expectedString);
        Assertions.assertTrue(xlsxContent.contains(expectedString), message);
    }

    @RetryableTest
    @DisplayName("Проверка отчета в новом интерфейсе \"Отчет по обращению с отходами\"")
    @ResourceLock("Проверка отчета в новом интерфейсе \"Отчет по обращению с отходами\"")
    void wasteActTest() {
        uiSteps.Login().PerformLogin();
        String applicationNumber = "123";

        WasteActPage wasteActPage = uiSteps
                .Navigation()
                .menu()
                .openReports()
                .openWasteAct();

        int topReportId = wasteActPage
                .openHistoryTab()
                .getTopReportId();
        wasteActPage.openRunReportTab();

        int newReportId = wasteActPage
                .runReport(applicationNumber)
                .waitPrintNotification()
                .openHistoryTab()
                .waitTopReportIsReady(topReportId)
                .getTopReportId();
        String message = String.format("Expected report id greater than %s , received %s", topReportId, newReportId);
        Assertions.assertTrue(topReportId < newReportId, message);

        String expectedString = "Отчет по обращению с отходами";

        String xlsxContent = wasteActPage.openXlsxReport(String.valueOf(newReportId));

        message = String.format("XLS does not contain expected string %s", expectedString);
        Assertions.assertTrue(xlsxContent.contains(expectedString), message);
    }

    @RetryableTest
    @DisplayName("Проверка отчета в новом интерфейсе \"Эффективность выполнения операций на складе\"")
    @ResourceLock("Проверка отчета в новом интерфейсе \"Эффективность выполнения операций на складе\"")
    void opsExecEffectivenessTest() {
        uiSteps.Login().PerformLogin();

        OpsexecEffectivenessActPage opsexecEffectivenessActPage = uiSteps
                .Navigation()
                .menu()
                .openReports()
                .openOpsExecEffectiveness();

        int topReportId = opsexecEffectivenessActPage
                .openHistoryTab()
                .getTopReportId();
        opsexecEffectivenessActPage.openRunReportTab();

        String currentDate = DateUtil.getDotDateFormatString(new Date(System.currentTimeMillis()));
        String timeFrom = "00:00";
        String timeTo = "23:59";
        OpsexecEffectivenessActPage topReport = opsexecEffectivenessActPage
                .runReport(currentDate, timeFrom, timeTo)
                .waitPrintNotification()
                .openHistoryTab()
                .waitTopReportIsReady(topReportId);
        int newReportId = topReport.getTopReportId();
        String message = String.format("Expected report id greater than %s , received %s", topReportId, newReportId);
        Assertions.assertTrue(topReportId < newReportId, message);

        String pdfContent = opsexecEffectivenessActPage.openPdfReport(String.valueOf(newReportId));
        checkPdfContainReportDate(pdfContent);

        //Сейчас по сути проверяем только что файлик скачался и там хоть что-то есть
        //ToDo: В начале теста выполнять какую-то операцию, которая попадет в отчет и проверять что сформировалась таблица с данными
        String expectedString = "тип операций";
        String xlsxContent = opsexecEffectivenessActPage.openXlsxReport(String.valueOf(newReportId));

        message = String.format("XLS does not contain expected string %s", expectedString);
        Assertions.assertTrue(xlsxContent.toLowerCase(Locale.ROOT).contains(expectedString), message);

    }

    @RetryableTest
    @DisplayName("Проверка отчета в новом интерфейсе \"Эффективность выполнения операций на складе. Оптимизированный\"")
    @ResourceLock("Проверка отчета в новом интерфейсе \"Эффективность выполнения операций на складе. Оптимизированный\"")
    void opsExecEffectivenessOptimizedTest() {
        uiSteps.Login().PerformLogin();

        OpsexecEffectivenessOptimizedActPage opsexecEffectivenessOptimizedActPage = uiSteps
                .Navigation()
                .menu()
                .openReports()
                .opsexecEffectivenessOptimizedActPage();

        int topReportId = opsexecEffectivenessOptimizedActPage
                .openHistoryTab()
                .getTopReportId();
        opsexecEffectivenessOptimizedActPage.openRunReportTab();

        String currentDate = DateUtil.getDotDateFormatString(new Date(System.currentTimeMillis()));
        String timeFrom = "00:00";
        String timeTo = "23:59";
        int newReportId = opsexecEffectivenessOptimizedActPage
                .runReport(currentDate, timeFrom, timeTo)
                .waitPrintNotification()
                .openHistoryTab()
                .waitTopReportIsReady(topReportId)
                .getTopReportId();
        String message = String.format("Expected report id greater than %s , received %s", topReportId, newReportId);
        Assertions.assertTrue(topReportId < newReportId, message);

        String pdfContent = opsexecEffectivenessOptimizedActPage.openPdfReport(String.valueOf(newReportId));
        checkPdfContainReportDate(pdfContent);

        String xlsxContent = opsexecEffectivenessOptimizedActPage.openXlsxReport(String.valueOf(newReportId));

        //Сейчас по сути проверяем только что файлик скачался и там хоть что-то есть
        //ToDo: В начале теста выполнять какую-то операцию, которая попадет в отчет и проверять что сформировалась таблица с данными
        String expectedString = "тип операций";
        message = String.format("XLS does not contain expected string %s", expectedString);
        Assertions.assertTrue(xlsxContent.toLowerCase(Locale.ROOT).contains(expectedString), message);

    }

    @RetryableTest
    @DisplayName("Проверка отчета в новом интерфейсе \"Акт передачи товаров утилизатору\"")
    @ResourceLock("Проверка отчета в новом интерфейсе \"Акт передачи товаров утилизатору\"")
    void actTransferUtilReportTest() {
        uiSteps.Login().PerformLogin();
        String applicationNumber = "123";
        TransferUtilActPage transferUtilActPage = uiSteps
                .Navigation()
                .menu()
                .openReports()
                .openTransferUtilActPage();

        String actualReportNumber = transferUtilActPage
                .runReport(applicationNumber)
                .waitPrintNotification()
                .openHistoryTab()
                .waitTopReportIsReady()
                .getTopReportApplicationNumberInHistory();
        String message = String.format("Expected %s report id, received %s", applicationNumber, actualReportNumber);
        Assertions.assertEquals(applicationNumber, actualReportNumber, message);

        String expectedString = "Акт приема-передачи";

        int newReportId = transferUtilActPage.getTopReportId();
        String xlsxContent = transferUtilActPage.openXlsxReport(String.valueOf(newReportId));

        message = String.format("XLS does not contain expected string %s", expectedString);
        Assertions.assertTrue(xlsxContent.contains(expectedString), message);

    }

    @RetryableTest
    @DisplayName("Проверка отчета в новом интерфейсе \"Уведомление о готовности имущества к отгрузке\"")
    @ResourceLock("Проверка отчета в новом интерфейсе \"Уведомление о готовности имущества к отгрузке\"")
    void wasteNoticeActTest() {
        uiSteps.Login().PerformLogin();
        String applicationNumber = "123";

        WasteNoticeActPage wasteNoticeActPage = uiSteps
                .Navigation()
                .menu()
                .openReports()
                .openWasteNoticeAct();

        int topReportId = wasteNoticeActPage
                .openHistoryTab()
                .getTopReportId();
        log.info(String.format("Top report id is %s", topReportId));
        wasteNoticeActPage.openRunReportTab();

        int newReportId = wasteNoticeActPage
                .runReport(applicationNumber)
                .waitPrintNotification()
                .openHistoryTab()
                .waitTopReportIsReady(topReportId)
                .getTopReportId();
        String message = String.format("Expected report id greater than %s , received %s", topReportId, newReportId);
        Assertions.assertTrue(topReportId < newReportId, message);

        String expectedString = "о готовности имущества к отгрузке";

        String xlsxContent = wasteNoticeActPage.openXlsxReport(String.valueOf(newReportId));

        message = String.format("XLS does not contain expected string %s", expectedString);
        Assertions.assertTrue(xlsxContent.toLowerCase(Locale.ROOT).contains(expectedString), message);
    }

    void checkPdfContainReportDate(String pdfContent)
    {
        String currentDate = DateUtil.getDotDateFormatString(new Date(System.currentTimeMillis()));
        String timeFrom = "00:00";
        String timeTo = "23:59";

        String fromDate = String.format("%s %s", currentDate, timeFrom);
        String message = String.format("PDF does not contain required start report date %s %s", fromDate, pdfContent);
        Assertions.assertTrue(pdfContent.contains(fromDate), message);

        String toDate = String.format("%s %s", currentDate, timeTo);
        message = String.format("PDF does not contain required end report date %s %s", toDate, pdfContent);
        Assertions.assertTrue(pdfContent.contains(toDate), message);
    }
}
