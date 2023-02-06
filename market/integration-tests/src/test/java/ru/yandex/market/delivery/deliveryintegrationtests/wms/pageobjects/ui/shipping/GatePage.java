package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;

public class GatePage extends AbstractInputPage {

    public GatePage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "gatePage";
    }

    @Step("Сканируем ворота")
    public DropIdPage enterGate(String door) {
        super.performInput(door);
        ModalWindow modalWindow = new ModalWindow(driver);
        modalWindow.waitModalVisible();
        modalWindow.clickForward();
        notificationDialog.IsPresentWithMessage("Отгрузка началась");
        return new DropIdPage(driver);
    }
}
