package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.qatools.properties.Resource;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;


@Resource.Classpath("wms/webdriver.properties")
public class OutboundsActPage extends ReportHistoryTabPage<OutboundsActPage> {

    @FindBy(xpath = "//div[@data-e2e='orderkey']//input")
    private SelenideElement applicationNumberInput;

    @FindBy(xpath = "//button[@data-e2e='button_submit']")
    private SelenideElement runReportBtn;

    @FindBy(xpath = "//*[@data-e2e='createReport_tab']")
    private SelenideElement runReportTab;

    @FindBy(xpath = "//button[@data-e2e='expand_report_info_button']")
    private SelenideElement topReportBoxExpandBtn;

    @FindBy(xpath = "//a[@data-e2e='link_HTML']")
    private SelenideElement topReportHtmlBtn;

    @FindBy(xpath = "//a[@data-e2e='link_PDF']")
    private SelenideElement topReportPdfBtn;

    @FindBy(xpath = "//span[@data-e2e='data_field_ID отчёта']")
    private SelenideElement reportIdText;

    @FindBy(xpath = "//span[@data-e2e='data_field_№ заявки']")
    private SelenideElement applicationNumberText;

    @FindBy(xpath = "//button[@data-e2e='button_back']")
    private SelenideElement closePopUpButton;

    @FindBy(xpath = "//span[@data-e2e='report_status']")
    private SelenideElement reportGenerationStatus;

    public OutboundsActPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    protected String getUrl() {
        return "outboundsact";
    }

    public OutboundsActPage runReport(String applicationNumber) {
        applicationNumberInput.sendKeys(applicationNumber);
        runReportBtn.click();
        return this;
    }

    public OutboundsActPage waitTopReportIsReady() {
        WebDriverWait myWait = new WebDriverWait(getWebDriver(), 20, 2000);
        ExpectedCondition<Boolean> conditionToCheck = inputDriver -> {
            inputDriver.navigate().refresh();
            $(byXpath(HISTORY_BUTTON_XPATH)).click();
            return reportGenerationStatus.getText().equals("Готов");
        };
        myWait.until(conditionToCheck);
        return this;
    }

    public String getTopReportApplicationNumberInHistory() {
        topReportBoxExpandBtn.click();
        return applicationNumberText.getText();
    }

    public String getTopReportIdInHistory() {
        return reportIdText.getText();
    }

    public OutboundsActTaskHtmlPage openHtmlReport() {
        topReportHtmlBtn.click();
        openNextTab();
        return new OutboundsActTaskHtmlPage(driver);
    }

    @Override
    public String openPdfReport(String reportId) {
        topReportPdfBtn.click();
        String fileName = String.format("App_%s.pdf", reportId);
        return super.openPdfReport(fileName);
    }

    public OutboundsActPage waitPrintNotification() {
        if (notificationDialog.IsPresentWithMessage("Запущена подготовка отчета")) {
            notificationDialog.waitUntilHidden();
        }
        return this;
    }
}
