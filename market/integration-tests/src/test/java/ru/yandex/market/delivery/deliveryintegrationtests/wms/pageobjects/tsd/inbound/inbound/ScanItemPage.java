package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class ScanItemPage extends AbstractTsdPage {

    @Name("Поле ввода штрихкода")
    @FindBy(id="ALTSKU_0")
    private HtmlElement barcodeField;

    public ScanItemPage(WebDriver driver) {
        super(driver);
    }

    public void enterBarcode(String barcode) {
        barcodeField.sendKeys(barcode);
        barcodeField.sendKeys(Keys.ENTER);
    }

    public Boolean isDisplayed() {
        return barcodeField.isDisplayed();
    }
}
