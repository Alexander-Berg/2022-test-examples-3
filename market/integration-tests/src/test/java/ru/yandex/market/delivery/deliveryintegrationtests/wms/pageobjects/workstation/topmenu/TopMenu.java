package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.topmenu;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

public class TopMenu extends AbstractWsPage {

    @FindBy(xpath = "//div[@id = '$cw862c']")
    private HtmlElement whSelector;

    public TopMenu(WebDriver driver) {
        super(driver);
    }

    public boolean isDisplayed(){
        return driver.findElements(By.xpath("//div[@id = '$cw862c']")).size() != 0;
    }

    public WHSelector whSelectorClick() {
        overlayBusy.waitUntilHidden();
        wait.until(elementToBeClickable(whSelector));
        whSelector.click();
        return new WHSelector(driver);
    }
}
