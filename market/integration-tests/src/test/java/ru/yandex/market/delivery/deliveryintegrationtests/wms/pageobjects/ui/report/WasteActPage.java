package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import com.codeborne.selenide.SelenideElement;
import ru.qatools.properties.Resource;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Resource.Classpath("wms/webdriver.properties")
public class WasteActPage extends ReportHistoryTabPage<WasteActPage> {

    @FindBy(xpath = "//div[@data-e2e='orderkey']//input")
    private SelenideElement applicationNumberInput;

    @FindBy(xpath = "//div[@data-e2e='category']//input")
    private SelenideElement itemsCategoryInput;

    public WasteActPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    protected String getUrl() {
        return "waste";
    }

    public WasteActPage runReport(String applicationNumber) {
        applicationNumberInput.sendKeys(applicationNumber);
        itemsCategoryInput.sendKeys("Фуд");
        runReportBtn.click();
        return this;
    }

    @Override
    public String openXlsxReport(String reportId) {
        String fileName = String.format("waste%s.xlsx", reportId);
        return super.openXlsxReport(fileName);
    }
}
