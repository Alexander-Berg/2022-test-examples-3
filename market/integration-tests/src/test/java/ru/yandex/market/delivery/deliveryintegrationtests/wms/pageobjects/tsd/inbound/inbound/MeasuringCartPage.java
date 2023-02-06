package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class MeasuringCartPage extends AbstractTsdPage {

    @FindBy(id="fromid_0")
    private HtmlElement cartInputField;

    public MeasuringCartPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номер тележки")
    public void enterCartId (String cartId) {
        cartInputField.sendKeys(cartId);
        cartInputField.sendKeys(Keys.ENTER);
    }
}
