package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class PalletLabelReportPage extends AbstractReportPage {

    public PalletLabelReportPage(WebDriver driver) {
        super(driver);
    }
    @Name("Выпадающее меню выбора службы доставки")
    @FindBy(xpath = "//div[@class = 'dropdown']")
    private HtmlElement deliveryServiceDropdownMenu;

    Actions actions = new Actions(driver);

    public void chooseDeliveryService(String deliveryServiceName) {
        String dropdownListXpath ="//div[@id = 'dropdown-list']";
        String deliveryServiceXpath = String.format("//div[@id = 'dropdown-list']/ul/li/a[text()='%s']", deliveryServiceName);

        driver.switchTo().frame(reportFrameId);
        waitSpinner();
        deliveryServiceDropdownMenu.click();

        //при одинарном клике выпадающее меню не закрывается
        actions.doubleClick(driver.findElement(By.xpath(deliveryServiceXpath))).perform();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(dropdownListXpath)));
    }
}
