package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class InboundByPlacesPage extends AbstractTsdPage {

    @FindBy(id = "doc_0")
    private HtmlElement inboundIdField;

    @FindBy(id = "palletcount_0")
    private HtmlElement countField;

    public InboundByPlacesPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номер поставки и количество")
    public void enterInboundAndPlaces(String inboundId, Integer count) {
        inboundIdField.sendKeys(inboundId);
        countField.sendKeys(count.toString());
        countField.sendKeys(Keys.ENTER);
        waitSpinner();
    }
}
