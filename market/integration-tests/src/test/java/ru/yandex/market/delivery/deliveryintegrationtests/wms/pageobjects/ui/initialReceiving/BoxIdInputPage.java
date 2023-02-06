package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;

public class BoxIdInputPage extends AbstractInputPage {

    @Override
    protected String getUrl() {
        return "boxInputPage";
    }

    public BoxIdInputPage(WebDriver driver) {
        super(driver);
        this.notificationDialog = new NotificationDialog(driver);
    }

    @Step("Вводим номер коробки")
    public OrderIdInputPage enterBoxId(String boxId) {
        super.performInput(boxId);

        return new OrderIdInputPage(driver);
    }
}
