package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

@Slf4j
public class CartParentInputPage extends AbstractInputPage {

    public CartParentInputPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "parentContainerInput$";
    }

    @Step("Вводим родительскую тару: {cart}")
    public BarcodeInputPage enterParentCart(String cart) {
        super.performInput(cart);
        return new BarcodeInputPage(driver);
    }
}
