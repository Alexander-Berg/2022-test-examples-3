package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class NestedIdsMovementsScanParentIdPage extends AbstractInputPage {

    public NestedIdsMovementsScanParentIdPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "scanParentIdPage$";
    }

    @Step("Вводим родительскую тару: {cart}")
    public NestedIdsMovementsPickingItemsPage enterParentCart(String cart) {
        super.performInput(cart);
        return new NestedIdsMovementsPickingItemsPage(driver);
    }

}
