package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import ru.qatools.properties.Resource;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Resource.Classpath("wms/webdriver.properties")
public class TransferUtilActPage extends ReportHistoryTabPage<TransferUtilActPage> {

    @FindBy(xpath = "//div[@data-e2e='orderkey']//input")
    private SelenideElement applicationNumberInput;

    @FindBy(xpath = "//button[@data-e2e='button_submit']")
    private SelenideElement runReportBtn;

    @FindBy(xpath = "//*[@data-e2e='createReport_tab']")
    private SelenideElement runReportTab;

    @FindBy(xpath = "//button[@data-e2e='expand_report_info_button']")
    private SelenideElement topReportBoxExpandBtn;

    @FindBy(xpath = "//a[@data-e2e='link_XLS']")
    private SelenideElement topReportXlsBtn;

    @FindBy(xpath = "//span[@data-e2e='data_field_№ заявки']")
    private SelenideElement applicationNumberText;

    @FindBy(xpath = "//span[@data-e2e='data_field_ID отчёта']")
    private SelenideElement reportIdText;

    @FindBy(xpath = "//button[@data-e2e='button_back']")
    private SelenideElement closePopUpButton;

    @FindBy(xpath = "//span[@data-e2e='report_status']")
    private SelenideElement reportGenerationStatus;

    public TransferUtilActPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    protected String getUrl() {
        return "transfertoutilizeract";
    }

    public TransferUtilActPage runReport(String applicationNumber) {
        applicationNumberInput.sendKeys(applicationNumber);
        runReportBtn.click();
        return this;
    }

    public TransferUtilActPage waitTopReportIsReady() {
        WebDriverWait myWait = new WebDriverWait(driver, 30, 3000);
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

    public TransferUtilActPage waitPrintNotification() {
        if (notificationDialog.IsPresentWithMessage("Запущена подготовка отчета")) {
            notificationDialog.waitUntilHidden();
        }
        return this;
    }

    public int getTopReportId() {
        // It's possible that there are no reports on the History tab page
        try {
            return Integer.parseInt(reportIdText.getText());
        } catch (ElementNotFound e) {
            return 0;
        }
    }

    @Override
    public String openXlsxReport(String reportId) {
        String fileName = String.format("transfer_to_utilizer_act_%s.xlsx", reportId);
        return super.openXlsxReport(fileName);
    }
}
