package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class FinishGatePage extends AbstractInputPage {

    public FinishGatePage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "finishGatePage";
    }

    @Step("Сканируем ворота")
    public void enterFinishGate(String door) {
        super.performInput(door);
        notificationDialog.IsPresentWithMessage("Машина отгружена");
    }
}
