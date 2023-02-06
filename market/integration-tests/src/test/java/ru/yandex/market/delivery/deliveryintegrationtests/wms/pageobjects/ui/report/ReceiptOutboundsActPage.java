package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.qatools.properties.Resource;
import ru.yandex.common.util.date.DateUtil;

import java.util.Date;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Resource.Classpath("wms/webdriver.properties")
public class ReceiptOutboundsActPage extends ReportHistoryTabPage<ReceiptOutboundsActPage> {

    @FindBy(xpath = "//div[@data-e2e='carrier']//input")
    private SelenideElement selectDeliveryService;

    @FindBy(xpath = "//div[@data-box='true']/div[@role='button']/button/span[text()]")
    private SelenideElement selectFirstDeliveryServiceFromList;

    @FindBy(xpath = "//div[@data-e2e='date']//input")
    private SelenideElement dateField;

    @FindBy(xpath = "//div[@data-e2e='starttime']//input")
    private SelenideElement timeFromField;

    @FindBy(xpath = "//div[@data-e2e='endtime']//input")
    private SelenideElement timeToField;

    @FindBy(xpath = "//button[@data-e2e='expand_report_info_button']")
    private SelenideElement topReportBoxExpandBtn;

    @FindBy(xpath = "//a[@data-e2e='link_PDF']")
    private SelenideElement topReportPdfBtn;

    public ReceiptOutboundsActPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    protected String getUrl() {
        return "receiptoutboundsact";
    }

    public ReceiptOutboundsActPage runReport() {
        Date currentDate = new Date(System.currentTimeMillis());
        selectDeliveryService.click();
        selectFirstDeliveryServiceFromList.click();
        dateField.sendKeys(DateUtil.getDotDateFormatString(currentDate));
        timeFromField.sendKeys("00:00");
        timeToField.sendKeys("23:00");
        runReportBtn.click();
        return this;
    }

    @Override
    public int getTopReportId() {
        topReportBoxExpandBtn.click();
        return super.getTopReportId();
    }

    @Override
    public String openPdfReport(String reportId) {
        String fileName = String.format("YM_act_of_receiving_transfer_%s.pdf", reportId);
        return super.openPdfReport(fileName);
    }

    @Override
    public String openXlsxReport(String reportId) {
        String fileName = String.format("YM_act_of_receiving_transfer_%s.xlsx", reportId);
        return super.openXlsxReport(fileName);
    }
}
