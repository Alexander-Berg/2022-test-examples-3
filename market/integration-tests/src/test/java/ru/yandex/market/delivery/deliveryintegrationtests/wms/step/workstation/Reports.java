package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.ReportListPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports.EffectivenessOfOperationsReportPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports.GeneratedEffectivenessOfOperationsReport;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports.GeneratedLpnLabelReport;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports.GeneratedPalletLabelReport;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports.GeneratedParcelLabelReport;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports.GeneratedPltLabelReport;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports.LpnLabelReportPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports.PalletLabelReportPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports.ParcelLabelReportPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports.PltLabelReportPage;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import ru.qatools.properties.Resource;

@Resource.Classpath({"wms/infor.properties"})
public class Reports extends AbstractWSSteps {
    private final ReportListPage reportsPage;
    private final ParcelLabelReportPage parcelLabelReportPage;
    private final LpnLabelReportPage lpnLabelReportPage;
    private final PltLabelReportPage pltLabelReportPage;
    private final PalletLabelReportPage palletLabelReportPage;
    private final EffectivenessOfOperationsReportPage effectivenessOfOperationsReportPage;
    private final GeneratedParcelLabelReport generatedParcelLabelReport;
    private final GeneratedLpnLabelReport generatedLpnLabelReport;
    private final GeneratedPltLabelReport generatedPltLabelReport;
    private final GeneratedPalletLabelReport generatedPalletLabelReport;
    private final GeneratedEffectivenessOfOperationsReport generatedEffectivenessOfOperationsReport;

    public Reports(WebDriver drvr) {
        super(drvr);

        reportsPage = new ReportListPage(driver);
        parcelLabelReportPage = new ParcelLabelReportPage(driver);
        generatedParcelLabelReport = new GeneratedParcelLabelReport(driver);
        lpnLabelReportPage = new LpnLabelReportPage(driver);
        generatedLpnLabelReport = new GeneratedLpnLabelReport(driver);
        pltLabelReportPage = new PltLabelReportPage(driver);
        generatedPltLabelReport = new GeneratedPltLabelReport(driver);
        palletLabelReportPage = new PalletLabelReportPage(driver);
        generatedPalletLabelReport = new GeneratedPalletLabelReport(driver);
        effectivenessOfOperationsReportPage = new EffectivenessOfOperationsReportPage(driver);
        generatedEffectivenessOfOperationsReport = new GeneratedEffectivenessOfOperationsReport(driver);
    }

    @Step("?????????????????? ?????????? \"???????????????? ??????????????\"")
    public GeneratedParcelLabelReport openParcelLabelReport() {
        openReportPageByName("???????????????? ??????????????");
        parcelLabelReportPage.submitButtonClick();
        generatedParcelLabelReport.switchToGeneratedReportWindow();

        return new GeneratedParcelLabelReport(driver);
    }

    @Step("?????????????????? ?????????? \"???????????????? LPN\"")
    public GeneratedLpnLabelReport openLpnLabelReport() {
        openReportPageByName("???????????????? LPN");
        lpnLabelReportPage.submitButtonClick();
        generatedLpnLabelReport.switchToGeneratedReportWindow();

        return new GeneratedLpnLabelReport(driver);
    }

    @Step("?????????????????? ?????????? \"???????????????? PLT\"")
    public GeneratedPltLabelReport openPltLabelReport() {
        openReportPageByName("???????????????? PLT");
        pltLabelReportPage.submitButtonClick();
        generatedPltLabelReport.switchToGeneratedReportWindow();

        return new GeneratedPltLabelReport(driver);
    }

    @Step("?????????????????? ?????????? \"???????????????? ??????????????\"")
    public GeneratedPalletLabelReport openPalletLabelReport() {
        String submitButtonXpath = "//button[text()[contains(., '?????????????????? ??????????')]]";

        openReportPageByName("???????????????? ??????????????");
        palletLabelReportPage.chooseDeliveryService("DPD");
        driver.findElement(By.xpath(submitButtonXpath)).click();
        generatedPalletLabelReport.switchToGeneratedReportWindow();

        return new GeneratedPalletLabelReport(driver);
    }

    @Step("?????????????????? ?????????? \"?????????????????????????? ???????????????????? ???????????????? ???? ????????????\"")
    public GeneratedEffectivenessOfOperationsReport openEffectivenessOfOperationsReport() {
        openReportNameAndCategory("?????????????????????????? ???????????????????? ???????????????? ???? ????????????", "???????????????? ????????????????");
        generatedEffectivenessOfOperationsReport.switchToGeneratedReportWindow();

        return new GeneratedEffectivenessOfOperationsReport(driver);
    }

    private void openReportPageByName(String reportName) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.Reports().reports();
        reportsPage.inputReportTitle(reportName);
        reportsPage.filterButtonClick();
        reportsPage.openFirstReport();
    }

    private void openReportNameAndCategory(String reportName, String category) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.Reports().reports();
        reportsPage.chooseReportCategory(category);
        reportsPage.openReportByName(reportName);
        effectivenessOfOperationsReportPage.submitButtonClick();
    }
}
