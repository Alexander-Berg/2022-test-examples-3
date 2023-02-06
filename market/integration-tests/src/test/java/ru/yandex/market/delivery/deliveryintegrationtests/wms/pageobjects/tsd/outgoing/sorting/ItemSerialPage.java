package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing.sorting;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class ItemSerialPage extends AbstractTsdPage {

    private static final String SCREEN_ID = "BP04LT";

    @Name("Товар/№UPC")
    @FindBy(id="SKU_0")
    private HtmlElement itemSerialField;

    public ItemSerialPage(WebDriver driver) {
        super(driver, SCREEN_ID);
    }

    @Step("Вводим серийник товара")
    public ItemSortPage enterItemSerial (String itemSerial) {
        assertScreenIsOpen();
        itemSerialField.sendKeys(itemSerial);
        itemSerialField.sendKeys(Keys.ENTER);

        return new ItemSortPage(driver);
    }
}
