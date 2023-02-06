package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.qatools.properties.Resource;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Resource.Classpath("wms/webdriver.properties")
public class OpsexecEffectivenessActPage extends ReportHistoryTabPage<OpsexecEffectivenessActPage> {

    @FindBy(xpath = "//div[@data-e2e='sysoper']//input")
    private SelenideElement typeReport;

    @FindBy(xpath = "//button[@data-e2e='sysoper-selectAll_Системные']/span")
    private SelenideElement selectFirstReportTypeFromList;

    @FindBy(xpath = "//div[@data-e2e='startdatetime_date']//input")
    private SelenideElement dateFromField;

    @FindBy(xpath = "//div[@data-e2e='enddatetime_date']//input")
    private SelenideElement dateToField;

    @FindBy(xpath = "//div[@data-e2e='startdatetime_time']//input")
    private SelenideElement timeFromField;

    @FindBy(xpath = "//div[@data-e2e='enddatetime_time']//input")
    private SelenideElement timeToField;

    @FindBy(xpath = "//a[@data-e2e='link_PDF']")
    private SelenideElement topReportPdfBtn;

    public OpsexecEffectivenessActPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    protected String getUrl() {
        return "opsexeceffectiveness";
    }

    public OpsexecEffectivenessActPage runReport(String currentDate, String timeFrom, String timeTo) {
        typeReport.click();
        selectFirstReportTypeFromList.click();
        dateFromField.sendKeys(currentDate);
        dateToField.sendKeys(currentDate);
        timeFromField.sendKeys(timeFrom);
        timeToField.sendKeys(timeTo);
        runReportBtn.click();
        return this;
    }

    public OpsexecEffectivenessActPage waitPrintNotification() {
        if (notificationDialog.IsPresentWithMessage("Запущена подготовка отчета")) {
            notificationDialog.waitUntilHidden();
        }
        return this;
    }

    @Override
    public String openPdfReport(String reportId) {
        String fileName = String.format("YM_efficiency_of_operations_%s.pdf", reportId);
        return super.openPdfReport(fileName);
    }

    @Override
    public String openXlsxReport(String reportId) {
        String fileName = String.format("YM_efficiency_of_operations_%s.xlsx", reportId);
        return super.openXlsxReport(fileName);
    }
}
