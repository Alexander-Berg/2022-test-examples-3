package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Resource;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Resource.Classpath("wms/webdriver.properties")
public class WasteNoticeActPage extends ReportHistoryTabPage<WasteNoticeActPage> {
    private static final Logger log = LoggerFactory.getLogger(WasteNoticeActPage.class);

    @FindBy(xpath = "//div[@data-e2e='orderkey']//input")
    private SelenideElement applicationNumberInput;

    @FindBy(xpath = "//div[@data-e2e='category']//input")
    private SelenideElement itemsCategoryInput;

    public WasteNoticeActPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    protected String getUrl() {
        return "waste_notice";
    }

    public WasteNoticeActPage runReport(String applicationNumber) {
        log.info("Creating report with {} application number", applicationNumber);
        applicationNumberInput.sendKeys(applicationNumber);
        itemsCategoryInput.sendKeys("Нонфуд");
        runReportBtn.click();
        return this;
    }

    @Override
    public String openXlsxReport(String reportId) {
        String fileName = String.format("waste_notice%s.xlsx", reportId);
        return super.openXlsxReport(fileName);
    }
}
