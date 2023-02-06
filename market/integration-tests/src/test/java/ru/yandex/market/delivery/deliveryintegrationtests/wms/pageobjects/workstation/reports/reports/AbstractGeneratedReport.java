package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.numberOfWindowsToBe;

public abstract class AbstractGeneratedReport extends AbstractWsPage {

    protected static final Logger log = LoggerFactory.getLogger(AbstractGeneratedReport.class);

    @Name("Фрейм отчета")
    @FindBy(xpath = "//iframe")
    protected HtmlElement reportFrameId;

    public AbstractGeneratedReport(WebDriver driver) {
        super(driver);
    }

    public void switchToGeneratedReportWindow() {
        switchToSubWindow();
        driver.switchTo().frame(reportFrameId);
    }

    public String getElementTextInOpenedReport(String elementXpath) {

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(elementXpath)));

        return driver.findElement(By.xpath(elementXpath)).getText();
    }

    public void switchToDefaultWindow() {
        driver.switchTo().defaultContent();
        driver.close();
        wait.until(numberOfWindowsToBe(1));
        switchToMainWindow();
    }
}
