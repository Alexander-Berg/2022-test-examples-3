package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.initialReceiving;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;

public class OrderIdInputPage extends AbstractInputPage {

    @Override
    protected String getUrl() {
        return "orderInputPage";
    }

    public OrderIdInputPage(WebDriver driver) {
        super(driver);
        this.notificationDialog = new NotificationDialog(driver);
    }

    @Step("Вводим номер заказа")
    public BoxIdInputPage enterOrderId(String orderId) {
        super.performInput(orderId);

        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Коробка успешно привязана к заказу"));

        return new BoxIdInputPage(driver);
    }
}
