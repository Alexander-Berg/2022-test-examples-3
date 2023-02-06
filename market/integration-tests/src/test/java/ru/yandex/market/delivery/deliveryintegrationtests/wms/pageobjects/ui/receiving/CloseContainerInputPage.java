package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class CloseContainerInputPage extends AbstractInputPage {

    public CloseContainerInputPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "closeContainerInput";
    }

    public void enterContainer(String value) {
        super.performInput(value);
    }
}
