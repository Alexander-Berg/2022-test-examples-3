package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class ItemCancelPage extends AbstractTsdPage {

    @FindBy(id="serialnumber_0")
    private HtmlElement itemIdField;

    public ItemCancelPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим УИТ товара")
    public void enterItemId (String itemId) {
        itemIdField.sendKeys(itemId);
        itemIdField.sendKeys(Keys.ENTER);
        itemIdField.sendKeys(Keys.ENTER);
    }
}
