package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;

public class QtyInputPage extends AbstractInputPage {

    @Override
    protected String getUrl() {
        return "qtyInput";
    }

    public QtyInputPage (WebDriver driver) {
        super(driver);
    }

    @Step("Вводим количество паллет")
    public void enterPalletQty(String palletQty) {
        super.performInput(palletQty);
    }
}
